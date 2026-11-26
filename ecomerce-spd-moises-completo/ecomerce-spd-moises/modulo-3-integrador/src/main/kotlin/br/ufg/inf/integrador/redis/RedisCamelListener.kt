package br.ufg.inf.integrador.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import org.apache.camel.ProducerTemplate
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Event-Driven Consumer: assina o canal Pub/Sub (Lettuce) e injeta cada mensagem
 * na rota Camel `direct:crud` via ProducerTemplate.
 *
 * O processamento acontece em uma única thread própria (e não na thread de I/O do
 * Lettuce) por dois motivos: (1) a rota faz HTTP e pode dormir nas tentativas de
 * redelivery, o que travaria o recebimento de novas mensagens; (2) a ordem dos
 * eventos é preservada — o UPDATE nunca ultrapassa o CREATE do mesmo cliente.
 */
class RedisCamelListener(
    private val redisUri: String,
    private val channel: String,
    private val template: ProducerTemplate,
    private val endpoint: String = "direct:crud"
) : AutoCloseable {

    private val log = LoggerFactory.getLogger(RedisCamelListener::class.java)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "crud-consumer") }
    private var client: RedisClient? = null
    private var connection: StatefulRedisPubSubConnection<String, String>? = null

    fun start() {
        val c = RedisClient.create(redisUri)
        val conn = c.connectPubSub()
        conn.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                log.info("[REDIS] Mensagem recebida em '{}': {}", channel, message)
                executor.execute {
                    try {
                        template.sendBody(endpoint, message)
                    } catch (e: Exception) {
                        log.error("[CAMEL] Erro inesperado ao processar mensagem: {}", e.message, e)
                    }
                }
            }
        })
        conn.sync().subscribe(channel)
        client = c
        connection = conn
        log.info("[REDIS] Assinando o canal '{}' em {}", channel, redisUri)
    }

    override fun close() {
        connection?.close()
        client?.shutdown()
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }
}
