package br.ufg.inf.odm.repository

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.EventPublisher
import br.ufg.inf.events.OperationType
import br.ufg.inf.events.Source
import br.ufg.inf.odm.model.DestinatarioMapper
import br.ufg.inf.odm.model.DestinatarioOdm
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.types.ObjectId

/**
 * Persiste no MongoDB e publica o evento (source = ODM) no Redis.
 * `publicar = false` é usado quando a escrita vem do integrador (anti-loop).
 */
class DestinatarioRepository(
    private val colecao: MongoCollection<Document>,
    private val publisher: EventPublisher
) {

    fun criar(
        nomeExibicao: String,
        logradouroDestino: String,
        liberadoParaEnvio: Boolean,
        publicar: Boolean = true
    ): DestinatarioOdm {
        val novo = DestinatarioOdm(ObjectId().toHexString(), nomeExibicao, logradouroDestino, liberadoParaEnvio)
        colecao.insertOne(DestinatarioMapper.toDocument(novo))
        if (publicar) publicarEvento(OperationType.CREATE, EventJson.gson.toJson(novo))
        return novo
    }

    /** Atualização parcial: só os campos não-nulos são alterados. */
    fun atualizar(
        id: String,
        nomeExibicao: String? = null,
        logradouroDestino: String? = null,
        liberadoParaEnvio: Boolean? = null,
        publicar: Boolean = true
    ): DestinatarioOdm? {
        val atual = buscar(id) ?: return null
        val novo = atual.copy(
            nomeExibicao = nomeExibicao ?: atual.nomeExibicao,
            logradouroDestino = logradouroDestino ?: atual.logradouroDestino,
            liberadoParaEnvio = liberadoParaEnvio ?: atual.liberadoParaEnvio
        )
        colecao.replaceOne(Filters.eq("_id", ObjectId(id)), DestinatarioMapper.toDocument(novo))
        if (publicar) publicarEvento(OperationType.UPDATE, EventJson.gson.toJson(novo))
        return novo
    }

    fun remover(id: String, publicar: Boolean = true): Boolean {
        if (!ObjectId.isValid(id)) return false
        val removidos = colecao.deleteOne(Filters.eq("_id", ObjectId(id))).deletedCount
        if (removidos > 0 && publicar) publicarEvento(OperationType.DELETE, """{"id":"$id"}""")
        return removidos > 0
    }

    fun buscar(id: String): DestinatarioOdm? {
        if (!ObjectId.isValid(id)) return null
        return colecao.find(Filters.eq("_id", ObjectId(id))).first()?.let { DestinatarioMapper.fromDocument(it) }
    }

    fun listar(): List<DestinatarioOdm> =
        colecao.find().toList().map { DestinatarioMapper.fromDocument(it) }

    private fun publicarEvento(tipo: OperationType, data: String) {
        publisher.publish(CrudOperation("Cliente", tipo, Source.ODM, data))
    }
}
