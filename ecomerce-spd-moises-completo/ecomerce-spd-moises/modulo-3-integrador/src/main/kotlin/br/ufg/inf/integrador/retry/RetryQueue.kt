package br.ufg.inf.integrador.retry

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import br.ufg.inf.events.EventJson
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory
import java.time.Instant

/** Fila de Retry: onde caem as mensagens que esgotaram as tentativas de redelivery. */
interface RetryQueue {
    /** Nunca deve lançar exceção. */
    fun enqueue(eventoJson: String?, erro: String?)
}

/** Implementação em lista Redis (`LPUSH` para entrar, `RPOP` para sair => FIFO). */
class RedisRetryQueue(
    redisUri: String,
    private val key: String = EventJson.RETRY_QUEUE
) : RetryQueue, AutoCloseable {

    private val log = LoggerFactory.getLogger(RedisRetryQueue::class.java)
    private val client: RedisClient = RedisClient.create(redisUri)
    private val connection: StatefulRedisConnection<String, String> by lazy { client.connect() }

    override fun enqueue(eventoJson: String?, erro: String?) {
        try {
            val item = JsonObject()
            item.addProperty("evento", eventoJson)
            item.addProperty("erro", erro)
            item.addProperty("em", Instant.now().toString())
            connection.sync().lpush(key, item.toString())
            log.error("[RETRY] Evento enviado à fila '{}' após esgotar tentativas. Erro: {}", key, erro)
        } catch (e: Exception) {
            log.error("[RETRY] Não foi possível enfileirar o evento (Redis fora do ar?): {} | evento={}", e.message, eventoJson)
        }
    }

    /** Reprocessa os itens que estavam na fila no momento da chamada (os que falharem de novo voltam para o fim). */
    fun drain(reprocessar: (String) -> Unit) {
        val total = connection.sync().llen(key)
        log.info("[RETRY] Reprocessando {} item(ns) da fila '{}'", total, key)
        repeat(total.toInt()) {
            val bruto = connection.sync().rpop(key) ?: return
            val evento = JsonParser.parseString(bruto).asJsonObject.get("evento")
            if (evento != null && !evento.isJsonNull) reprocessar(evento.asString)
        }
    }

    override fun close() {
        client.shutdown()
    }
}
