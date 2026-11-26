package br.ufg.inf.orm

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventPublisher
import br.ufg.inf.events.OperationType
import br.ufg.inf.events.Source
import br.ufg.inf.orm.model.StatusCredito
import br.ufg.inf.orm.repository.ClienteOrmRepository
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClienteOrmRepositoryTest {

    private class FakePublisher : EventPublisher {
        val eventos = mutableListOf<CrudOperation>()
        override fun publish(op: CrudOperation) {
            eventos.add(op)
        }
    }

    private lateinit var publisher: FakePublisher
    private lateinit var repo: ClienteOrmRepository

    @BeforeTest
    fun setup() {
        val arquivo = File.createTempFile("orm-test", ".db")
        arquivo.deleteOnExit()
        OrmDatabase.connect("jdbc:sqlite:${arquivo.absolutePath}")
        publisher = FakePublisher()
        repo = ClienteOrmRepository(publisher)
    }

    @Test
    fun `criar persiste no SQLite e publica evento CREATE`() {
        val cliente = repo.criar("Ana Souza", "12345678900", "Rua 10, Goiânia", StatusCredito.APROVADO)

        assertEquals(cliente, repo.buscar(cliente.id))
        assertEquals(1, publisher.eventos.size)
        val evento = publisher.eventos.first()
        assertEquals("Cliente", evento.entity)
        assertEquals(OperationType.CREATE, evento.operation)
        assertEquals(Source.ORM, evento.source)
        assertTrue(evento.data.contains("\"nomeCompleto\":\"Ana Souza\""))
        assertTrue(evento.data.contains("\"statusCredito\":\"APROVADO\""))
    }

    @Test
    fun `atualizar altera apenas os campos informados e publica UPDATE`() {
        val criado = repo.criar("Ana Souza", "12345678900", "Rua 10", StatusCredito.APROVADO)
        publisher.eventos.clear()

        val atualizado = repo.atualizar(criado.id, statusCredito = StatusCredito.BLOQUEADO)

        assertNotNull(atualizado)
        assertEquals(StatusCredito.BLOQUEADO, atualizado.statusCredito)
        assertEquals("Ana Souza", atualizado.nomeCompleto)
        assertEquals(OperationType.UPDATE, publisher.eventos.single().operation)
    }

    @Test
    fun `escrita vinda do integrador nao republica evento`() {
        val criado = repo.criar("Ana Souza", "12345678900", "Rua 10", StatusCredito.APROVADO)
        publisher.eventos.clear()

        repo.atualizar(criado.id, enderecoEntrega = "Av. Goiás, 500", publicar = false)

        assertTrue(publisher.eventos.isEmpty())
        assertEquals("Av. Goiás, 500", repo.buscar(criado.id)?.enderecoEntrega)
    }

    @Test
    fun `atualizar cliente inexistente devolve null sem publicar`() {
        assertNull(repo.atualizar(999, nomeCompleto = "Fulano"))
        assertTrue(publisher.eventos.isEmpty())
    }

    @Test
    fun `remover apaga o registro e publica DELETE`() {
        val criado = repo.criar("Ana Souza", "12345678900", "Rua 10", StatusCredito.APROVADO)
        publisher.eventos.clear()

        assertTrue(repo.remover(criado.id))

        assertNull(repo.buscar(criado.id))
        assertEquals(OperationType.DELETE, publisher.eventos.single().operation)
        assertFalse(repo.remover(criado.id))
    }
}
