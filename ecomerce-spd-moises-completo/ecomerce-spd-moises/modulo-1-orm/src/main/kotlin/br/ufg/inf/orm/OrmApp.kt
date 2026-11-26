package br.ufg.inf.orm

import br.ufg.inf.events.RedisPublisher
import br.ufg.inf.orm.repository.ClienteOrmRepository
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("OrmApp")
    val dbPath = System.getenv("ORM_DB") ?: "orm.db"
    val porta = (System.getenv("ORM_PORT") ?: "8081").toInt()

    OrmDatabase.connect("jdbc:sqlite:$dbPath")
    val publisher = RedisPublisher()
    val repo = ClienteOrmRepository(publisher)

    val app = OrmServer.create(repo)
    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop()
        publisher.close()
    })
    app.start(porta)
    log.info("Módulo 1 (ORM/SQLite) no ar em http://localhost:{}  [banco: {}]", porta, dbPath)
}
