package br.ufg.inf.events

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventJsonTest {

    @Test
    fun `serializa e desserializa o envelope sem perder dados`() {
        val op = CrudOperation(
            entity = "Cliente",
            operation = OperationType.CREATE,
            source = Source.ORM,
            data = """{"id":1,"nomeCompleto":"Ana Souza"}""",
            timestamp = "2026-11-25T20:30:00Z"
        )

        val parsed = EventJson.parse(EventJson.toJson(op))

        assertNotNull(parsed)
        assertEquals(op, parsed)
    }

    @Test
    fun `aceita data como objeto JSON em vez de string`() {
        val json = """{"entity":"Cliente","operation":"UPDATE","source":"ODM","data":{"id":"abc"}}"""

        val parsed = EventJson.parse(json)

        assertNotNull(parsed)
        assertEquals("""{"id":"abc"}""", parsed.data)
    }

    @Test
    fun `devolve null para JSON invalido ou incompleto`() {
        assertNull(EventJson.parse("isto nao e json"))
        assertNull(EventJson.parse("""{"entity":"Cliente"}"""))
        assertNull(EventJson.parse("""{"entity":"Cliente","operation":"XPTO","source":"ORM","data":"{}"}"""))
    }
}
