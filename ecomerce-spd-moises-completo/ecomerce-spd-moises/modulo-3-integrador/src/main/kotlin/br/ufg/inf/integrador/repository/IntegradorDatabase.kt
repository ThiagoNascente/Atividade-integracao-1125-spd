package br.ufg.inf.integrador.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

object ClienteCanonicoTable : Table("cliente_canonico") {
    val idCanonico = varchar("id_canonico", 36)
    val primeiroNome = varchar("primeiro_nome", 100)
    val sobrenome = varchar("sobrenome", 200)
    val documento = varchar("documento", 14)
    val endereco = varchar("endereco", 300)
    val statusFinanceiro = varchar("status_financeiro", 20)
    val elegivelEntrega = bool("elegivel_entrega")
    override val primaryKey = PrimaryKey(idCanonico)
}

object MapeamentoIdTable : Table("mapeamento_id") {
    val idCanonico = varchar("id_canonico", 36)
    val idOrm = long("id_orm").uniqueIndex()
    val idOdm = varchar("id_odm", 24).uniqueIndex()
    override val primaryKey = PrimaryKey(idCanonico)
}

/** Banco local do integrador (`integrador.db`): canônico + mapeamento de IDs. */
object IntegradorDatabase {

    fun connect(jdbcUrl: String): Database {
        val db = Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(db) {
            SchemaUtils.create(ClienteCanonicoTable, MapeamentoIdTable)
        }
        return db
    }
}
