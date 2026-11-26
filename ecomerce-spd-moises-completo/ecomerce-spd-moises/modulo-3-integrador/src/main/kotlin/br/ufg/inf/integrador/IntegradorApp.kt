package br.ufg.inf.integrador

import br.ufg.inf.integrador.redis.RedisCamelListener
import br.ufg.inf.integrador.repository.IntegradorDatabase
import br.ufg.inf.integrador.repository.MapeamentoRepository
import br.ufg.inf.integrador.retry.RedisRetryQueue
import br.ufg.inf.integrador.routes.CrudTransformRoute
import org.apache.camel.impl.DefaultCamelContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

/**
 * Módulo 3 — Integrador.
 * Uso: `./gradlew :modulo-3-integrador:run`
 *      `./gradlew :modulo-3-integrador:run --args="--reprocessar"`  (esvazia a fila de retry ao iniciar)
 */
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("IntegradorApp")
    val cfg = IntegradorConfig.fromEnv()

    IntegradorDatabase.connect("jdbc:sqlite:${cfg.dbPath}")
    val repo = MapeamentoRepository()
    val retryQueue = RedisRetryQueue(cfg.redisUri)

    val camel = DefaultCamelContext()
    camel.addRoutes(CrudTransformRoute(cfg, repo, retryQueue))
    camel.start()
    val template = camel.createProducerTemplate()

    if ("--reprocessar" in args) {
        retryQueue.drain { evento -> template.sendBody("direct:crud", evento) }
    }

    val listener = RedisCamelListener(cfg.redisUri, cfg.channel, template)
    listener.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        listener.close()
        camel.stop()
        retryQueue.close()
    })

    log.info("Módulo 3 (Integrador) no ar. ODM={} ORM={} banco={}", cfg.odmUrl, cfg.ormUrl, cfg.dbPath)
    CountDownLatch(1).await()
}
