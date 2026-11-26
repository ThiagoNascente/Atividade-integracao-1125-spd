package br.ufg.inf.integrador

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.OperationType
import br.ufg.inf.events.Source
import br.ufg.inf.integrador.repository.IntegradorDatabase
import br.ufg.inf.integrador.repository.MapeamentoRepository
import br.ufg.inf.integrador.retry.RetryQueue
import br.ufg.inf.integrador.routes.CrudTransformRoute
import io.javalin.Javalin
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Teste de integração da rota Camel SEM Redis/MongoDB reais:
 * o ODM (porta 8080 no uso real) e o ORM (8081) são substituídos por servidores Javalin falsos
 * que gravam as requisições recebidas.
 */
class CrudTransformRouteTest {

    private class FakeRetryQueue : RetryQueue {
        val itens = CopyOnWriteArrayList<Pair<String?, String?>>()
        override fun enqueue(eventoJson: String?, erro: String?) {
            itens.add(eventoJson to erro)
        }
    }

    private val idOdm = "64b7f0c2a1b2c3d4e5f60718"
    private val chamadasOdm = CopyOnWriteArrayList<String>()
    private val chamadasOrm = CopyOnWriteArrayList<String>()
    private val retryQueue = FakeRetryQueue()

    @Volatile
    private var odmFora = false

    private lateinit var odm: Javalin
    private lateinit var orm: Javalin
    private lateinit var camel: DefaultCamelContext
    private lateinit var template: ProducerTemplate
    private lateinit var repo: MapeamentoRepository

    @BeforeTest
    fun setup() {
        val arquivo = File.createTempFile("integrador-route-test", ".db")
        arquivo.deleteOnExit()
        IntegradorDatabase.connect("jdbc:sqlite:${arquivo.absolutePath}")
        repo = MapeamentoRepository()

        odm = Javalin.create()
        odm.post("/destinatarios") { ctx ->
            if (odmFora) {
                ctx.status(503).result("indisponível")
            } else {
                chamadasOdm.add("POST ${ctx.body()} origem=${ctx.header(EventJson.ORIGIN_HEADER)}")
                ctx.status(201).contentType("application/json").result("""{"id":"$idOdm"}""")
            }
        }
        odm.put("/destinatarios/{id}") { ctx ->
            chamadasOdm.add("PUT ${ctx.pathParam("id")} ${ctx.body()}")
            ctx.status(200).contentType("application/json").result("{}")
        }
        odm.delete("/destinatarios/{id}") { ctx ->
            chamadasOdm.add("DELETE ${ctx.pathParam("id")}")
            ctx.status(204)
        }
        odm.start(0)

        orm = Javalin.create()
        orm.put("/clientes/{id}") { ctx ->
            chamadasOrm.add("PUT ${ctx.pathParam("id")} ${ctx.body()} origem=${ctx.header(EventJson.ORIGIN_HEADER)}")
            ctx.status(200).contentType("application/json").result("{}")
        }
        orm.start(0)

        val cfg = IntegradorConfig(
            odmUrl = "http://localhost:${odm.port()}",
            ormUrl = "http://localhost:${orm.port()}",
            maxRedeliveries = 1,
            redeliveryDelayMs = 10
        )
        camel = DefaultCamelContext()
        camel.addRoutes(CrudTransformRoute(cfg, repo, retryQueue))
        camel.start()
        template = camel.createProducerTemplate()
    }

    @AfterTest
    fun teardown() {
        camel.stop()
        odm.stop()
        orm.stop()
    }

    private val clienteJson =
        """{"id":1,"nomeCompleto":"Ana Souza","cpf":"12345678900","enderecoEntrega":"Rua 10, Goiânia","statusCredito":"APROVADO"}"""

    private fun enviar(tipo: OperationType, source: Source, data: String) {
        val json = EventJson.toJson(CrudOperation("Cliente", tipo, source, data))
        template.sendBody("direct:crud", json)
    }

    @Test
    fun `CREATE do ORM faz POST no ODM e persiste canonico e mapeamento`() {
        enviar(OperationType.CREATE, Source.ORM, clienteJson)

        assertEquals(1, chamadasOdm.size)
        val chamada = chamadasOdm.first()
        assertTrue(chamada.startsWith("POST"))
        assertTrue(chamada.contains("\"nomeExibicao\":\"Souza, Ana\""))
        assertTrue(chamada.contains("\"liberadoParaEnvio\":true"))
        assertTrue(chamada.contains("origem=INTEGRADOR"))
        assertTrue(!chamada.contains("12345678900")) // CPF não vai para o ODM

        val mapeamento = repo.buscarPorIdOrm(1L)
        assertNotNull(mapeamento)
        assertEquals(idOdm, mapeamento.idOdm)
        assertNotNull(repo.buscarCanonico(mapeamento.idCanonico))
    }

    @Test
    fun `CREATE duplicado e ignorado pelo Idempotent Receiver`() {
        enviar(OperationType.CREATE, Source.ORM, clienteJson)
        enviar(OperationType.CREATE, Source.ORM, clienteJson)

        assertEquals(1, chamadasOdm.size)
    }

    @Test
    fun `UPDATE do ORM faz PUT no idOdm mapeado`() {
        enviar(OperationType.CREATE, Source.ORM, clienteJson)
        chamadasOdm.clear()

        enviar(
            OperationType.UPDATE, Source.ORM,
            """{"id":1,"nomeCompleto":"Ana Souza","cpf":"12345678900","enderecoEntrega":"Av. Goiás, 500","statusCredito":"BLOQUEADO"}"""
        )

        assertEquals(1, chamadasOdm.size)
        val chamada = chamadasOdm.first()
        assertTrue(chamada.startsWith("PUT $idOdm"))
        assertTrue(chamada.contains("Av. Goiás, 500"))
        assertTrue(chamada.contains("\"liberadoParaEnvio\":false"))
    }

    @Test
    fun `DELETE do ORM remove no ODM e apaga o mapeamento`() {
        enviar(OperationType.CREATE, Source.ORM, clienteJson)

        enviar(OperationType.DELETE, Source.ORM, """{"id":1}""")

        assertTrue(chamadasOdm.contains("DELETE $idOdm"))
        assertNull(repo.buscarPorIdOrm(1L))
    }

    @Test
    fun `fluxo reverso ODM para ORM bloqueia o cliente e nao republica`() {
        enviar(OperationType.CREATE, Source.ORM, clienteJson)

        enviar(
            OperationType.UPDATE, Source.ODM,
            """{"id":"$idOdm","nomeExibicao":"Souza, Ana","logradouroDestino":"Rua 10, Goiânia","liberadoParaEnvio":false}"""
        )

        assertEquals(1, chamadasOrm.size)
        val chamada = chamadasOrm.first()
        assertTrue(chamada.startsWith("PUT 1 "))
        assertTrue(chamada.contains("\"statusCredito\":\"BLOQUEADO\""))
        assertTrue(chamada.contains("origem=INTEGRADOR"))
    }

    @Test
    fun `ODM indisponivel esgota as tentativas e a mensagem vai para a fila de retry`() {
        odmFora = true

        enviar(OperationType.CREATE, Source.ORM, clienteJson)

        assertEquals(1, retryQueue.itens.size)
        assertTrue(retryQueue.itens.first().first!!.contains("Ana Souza"))
        assertNull(repo.buscarPorIdOrm(1L)) // nada foi persistido
    }

    @Test
    fun `mensagem invalida e entidade desconhecida sao ignoradas sem retry`() {
        template.sendBody("direct:crud", "isto nao e json")
        enviar(OperationType.CREATE, Source.ORM, clienteJson)
        val outra = EventJson.toJson(CrudOperation("Produto", OperationType.CREATE, Source.ORM, "{}"))
        template.sendBody("direct:crud", outra)

        assertEquals(1, chamadasOdm.size) // só o Cliente válido gerou chamada
        assertTrue(retryQueue.itens.isEmpty())
    }
}
