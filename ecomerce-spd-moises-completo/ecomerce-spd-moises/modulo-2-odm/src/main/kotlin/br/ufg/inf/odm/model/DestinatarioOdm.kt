package br.ufg.inf.odm.model

import org.bson.Document
import org.bson.types.ObjectId

/** Objeto de domínio do Módulo 2 (Logística / Expedição). `id` é o ObjectId em hexadecimal. */
data class DestinatarioOdm(
    val id: String,
    val nomeExibicao: String,
    val logradouroDestino: String,
    val liberadoParaEnvio: Boolean
)

/**
 * Mapeamento objeto <-> documento (a parte "ODM" do módulo).
 * Coleção `destinatarios` no MongoDB; `id` <-> `_id` (ObjectId).
 */
object DestinatarioMapper {

    fun toDocument(d: DestinatarioOdm): Document =
        Document()
            .append("_id", ObjectId(d.id))
            .append("nomeExibicao", d.nomeExibicao)
            .append("logradouroDestino", d.logradouroDestino)
            .append("liberadoParaEnvio", d.liberadoParaEnvio)

    fun fromDocument(doc: Document): DestinatarioOdm =
        DestinatarioOdm(
            id = doc.getObjectId("_id").toHexString(),
            nomeExibicao = doc.getString("nomeExibicao") ?: "",
            logradouroDestino = doc.getString("logradouroDestino") ?: "",
            liberadoParaEnvio = doc.getBoolean("liberadoParaEnvio", false)
        )
}
