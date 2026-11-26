package br.ufg.inf.integrador

import br.ufg.inf.events.EventJson

data class IntegradorConfig(
    val redisUri: String = "redis://localhost:6379",
    val channel: String = EventJson.CHANNEL,
    val odmUrl: String = "http://localhost:8080",
    val ormUrl: String = "http://localhost:8081",
    val dbPath: String = "integrador.db",
    val maxRedeliveries: Int = 3,
    val redeliveryDelayMs: Long = 2000
) {
    companion object {
        fun fromEnv() = IntegradorConfig(
            redisUri = System.getenv("REDIS_URI") ?: "redis://localhost:6379",
            odmUrl = System.getenv("ODM_URL") ?: "http://localhost:8080",
            ormUrl = System.getenv("ORM_URL") ?: "http://localhost:8081",
            dbPath = System.getenv("INTEGRADOR_DB") ?: "integrador.db"
        )
    }
}
