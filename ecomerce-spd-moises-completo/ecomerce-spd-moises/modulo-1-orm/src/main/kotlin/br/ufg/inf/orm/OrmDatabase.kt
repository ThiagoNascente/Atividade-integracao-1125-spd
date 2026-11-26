package br.ufg.inf.orm

import br.ufg.inf.orm.model.ClientesTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object OrmDatabase {

    fun connect(jdbcUrl: String): Database {
        val db = Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
        // SQLite só suporta SERIALIZABLE / READ_UNCOMMITTED
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(db) {
            SchemaUtils.create(ClientesTable)
        }
        return db
    }
}
