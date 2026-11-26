package br.ufg.inf.odm

import br.ufg.inf.odm.model.DestinatarioMapper
import br.ufg.inf.odm.model.DestinatarioOdm
import org.bson.types.ObjectId
import kotlin.test.Test
import kotlin.test.assertEquals

/** Testa o mapeamento objeto <-> documento sem precisar de um MongoDB rodando. */
class DestinatarioMapperTest {

    @Test
    fun `objeto vira documento e volta sem perder dados`() {
        val original = DestinatarioOdm(
            id = ObjectId().toHexString(),
            nomeExibicao = "Souza, Ana",
            logradouroDestino = "Rua 10, Goiânia",
            liberadoParaEnvio = true
        )

        val documento = DestinatarioMapper.toDocument(original)

        assertEquals(ObjectId(original.id), documento.getObjectId("_id"))
        assertEquals("Souza, Ana", documento.getString("nomeExibicao"))
        assertEquals(original, DestinatarioMapper.fromDocument(documento))
    }
}
