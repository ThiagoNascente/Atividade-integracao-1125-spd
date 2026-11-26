package br.ufg.inf.odm

import br.ufg.inf.events.RedisPublisher
import br.ufg.inf.odm.repository.DestinatarioRepository
import com.mongodb.client.MongoClients
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("OdmServerApp")
    val mongoUri = System.getenv("MONGO_URI") ?: "mongodb://localhost:27017"
    val porta = (System.getenv("ODM_PORT") ?: "8080").toInt()

    val mongo = MongoClients.create(mongoUri)
    val colecao = mongo.getDatabase("logistica").getCollection("destinatarios")
    val publisher = RedisPublisher()
    val repo = DestinatarioRepository(colecao, publisher)

    val app = OdmServer.create(repo)
    Runtime.getRuntime().addShutdownHook(Thread {
        app.stop()
        publisher.close()
        mongo.close()
    })
    app.start(porta)
    log.info("Módulo 2 (ODM/MongoDB) no ar em http://localhost:{}  [mongo: {}]", porta, mongoUri)
}
