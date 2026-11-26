package br.ufg.inf.events

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.time.Instant

enum class OperationType { CREATE, UPDATE, DELETE }

/** Sistema que originou a escrita. */
enum class Source { ORM, ODM }

/**
 * Envelope de evento (Event Message) trafegado no canal Pub/Sub.
 * O campo [data] carrega o objeto da entidade já serializado em JSON (string),
 * conforme o contrato do tutorial Integrador Redis I.
 */
data class CrudOperation(
    val entity: String,
    val operation: OperationType,
    val source: Source,
    val data: String,
    val timestamp: String = Instant.now().toString()
)

object EventJson {
    const val CHANNEL = "crud-channel"
    const val RETRY_QUEUE = "crud-retry-queue"

    /**
     * Cabeçalho HTTP enviado pelo integrador ao escrever nos módulos 1 e 2.
     * Quem o recebe NÃO republica o evento — é o que evita o loop infinito
     * ORM -> ODM -> ORM -> ...
     */
    const val ORIGIN_HEADER = "X-Sync-Origin"
    const val ORIGIN_INTEGRADOR = "INTEGRADOR"

    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun toJson(op: CrudOperation): String = gson.toJson(op)

    /** Faz o parse do envelope; devolve null se o JSON for inválido ou incompleto. */
    fun parse(json: String): CrudOperation? = try {
        val o = JsonParser.parseString(json).asJsonObject
        val data = o.get("data")
        CrudOperation(
            entity = o.get("entity").asString,
            operation = OperationType.valueOf(o.get("operation").asString),
            source = Source.valueOf(o.get("source").asString),
            data = if (data.isJsonPrimitive) data.asString else data.toString(),
            timestamp = o.get("timestamp")?.asString ?: Instant.now().toString()
        )
    } catch (e: Exception) {
        null
    }
}
