package br.ufg.inf.events

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory

/** Abstração do publicador — permite trocar o Redis por um fake nos testes. */
interface EventPublisher {
    fun publish(op: CrudOperation)
}

/**
 * Publica operações CRUD no canal Pub/Sub do Redis (Publish-Subscribe Channel).
 *
 * Falhas de publicação NÃO propagam exceção: a escrita local já foi confirmada
 * e não deve ser desfeita porque o barramento está fora do ar. (Limitação
 * conhecida do Pub/Sub: o evento se perde — ver README, seção "Limitações".)
 */
class RedisPublisher(
    redisUri: String = System.getenv("REDIS_URI") ?: "redis://localhost:6379",
    private val channel: String = EventJson.CHANNEL
) : EventPublisher, AutoCloseable {

    private val log = LoggerFactory.getLogger(RedisPublisher::class.java)
    private val client: RedisClient = RedisClient.create(redisUri)
    private val connection: StatefulRedisConnection<String, String> by lazy { client.connect() }

    override fun publish(op: CrudOperation) {
        try {
            val json = EventJson.toJson(op)
            val receptores = connection.sync().publish(channel, json)
            log.info("[REDIS] PUBLISH {} (receptores={}) {}", channel, receptores, json)
        } catch (e: Exception) {
            log.error("[REDIS] Falha ao publicar evento {} {}: {}", op.entity, op.operation, e.message)
        }
    }

    override fun close() {
        client.shutdown()
    }
}
