package br.ufg.inf.integrador

import br.ufg.inf.integrador.model.ClienteOrmPayload
import br.ufg.inf.integrador.model.DestinatarioOdmPayload
import br.ufg.inf.integrador.translator.ClienteTranslator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClienteTranslatorTest {

    private val ana = ClienteOrmPayload(
        id = 1,
        nomeCompleto = "Ana Souza",
        cpf = "12345678900",
        enderecoEntrega = "Rua 10, Goiânia",
        statusCredito = "APROVADO"
    )

    @Test
    fun `ORM para canonico separa o nome com split e preserva o CPF`() {
        val c = ClienteTranslator.ormParaCanonico(ana, "uuid-1")

        assertEquals("uuid-1", c.idCanonico)
        assertEquals("Ana", c.primeiroNome)
        assertEquals("Souza", c.sobrenome)
        assertEquals("12345678900", c.documento)
        assertEquals("Rua 10, Goiânia", c.endereco)
        assertEquals("APROVADO", c.statusFinanceiro)
        assertTrue(c.elegivelEntrega)
    }

    @Test
    fun `nome composto mantem o restante como sobrenome`() {
        assertEquals("Ana" to "Maria de Souza", ClienteTranslator.separarNome("  Ana   Maria de Souza "))
        assertEquals("Madonna" to "", ClienteTranslator.separarNome("Madonna"))
    }

    @Test
    fun `canonico para ODM usa Sobrenome virgula Primeiro e libera envio quando APROVADO`() {
        val odm = ClienteTranslator.canonicoParaOdm(ClienteTranslator.ormParaCanonico(ana, "uuid-1"))

        assertEquals("Souza, Ana", odm.nomeExibicao)
        assertEquals("Rua 10, Goiânia", odm.logradouroDestino)
        assertEquals(true, odm.liberadoParaEnvio)
    }

    @Test
    fun `BLOQUEADO nao libera envio no ODM`() {
        val bloqueado = ana.copy(statusCredito = "BLOQUEADO")

        val odm = ClienteTranslator.canonicoParaOdm(ClienteTranslator.ormParaCanonico(bloqueado, "uuid-1"))

        assertEquals(false, odm.liberadoParaEnvio)
    }

    @Test
    fun `CPF nao vaza para o payload do ODM`() {
        val odm = ClienteTranslator.canonicoParaOdm(ClienteTranslator.ormParaCanonico(ana, "uuid-1"))

        assertFalse(odm.toString().contains("12345678900"))
    }

    @Test
    fun `fluxo reverso ODM para canonico converte liberadoParaEnvio em status financeiro`() {
        val original = ClienteTranslator.ormParaCanonico(ana, "uuid-1")

        val atualizado = ClienteTranslator.aplicarOdm(
            original,
            DestinatarioOdmPayload(id = "abc", nomeExibicao = "Souza, Ana Maria", liberadoParaEnvio = false)
        )

        assertEquals("Ana Maria", atualizado.primeiroNome)
        assertEquals("Souza", atualizado.sobrenome)
        assertEquals("BLOQUEADO", atualizado.statusFinanceiro)
        assertFalse(atualizado.elegivelEntrega)
        assertEquals("12345678900", atualizado.documento) // CPF preservado
        assertEquals("Rua 10, Goiânia", atualizado.endereco) // campo ausente no payload não é apagado
    }

    @Test
    fun `canonico para update do ORM monta nome completo e status`() {
        val c = ClienteTranslator.ormParaCanonico(ana, "uuid-1").copy(statusFinanceiro = "BLOQUEADO")

        val update = ClienteTranslator.canonicoParaOrmUpdate(c)

        assertEquals("Ana Souza", update.nomeCompleto)
        assertEquals("BLOQUEADO", update.statusCredito)
        assertEquals(null, update.cpf)
    }

    @Test
    fun `ida e volta do nome de exibicao e consistente`() {
        val (primeiro, sobrenome) = ClienteTranslator.separarNome("Ana Souza")
        val exibicao = ClienteTranslator.formatarNomeExibicao(primeiro, sobrenome)

        assertEquals("Ana" to "Souza", ClienteTranslator.lerNomeExibicao(exibicao))
    }
}
