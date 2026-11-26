package br.ufg.inf.orm.repository

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.EventPublisher
import br.ufg.inf.events.OperationType
import br.ufg.inf.events.Source
import br.ufg.inf.orm.model.ClienteOrm
import br.ufg.inf.orm.model.ClientesTable
import br.ufg.inf.orm.model.StatusCredito
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Persiste no SQLite (via Exposed) e, DEPOIS do commit, publica o evento no Redis.
 *
 * O parâmetro `publicar = false` é usado quando a escrita vem do integrador
 * (fluxo reverso ODM -> ORM): sem ele, o evento voltaria ao barramento e geraria loop.
 */
class ClienteOrmRepository(private val publisher: EventPublisher) {

    fun criar(
        nomeCompleto: String,
        cpf: String,
        enderecoEntrega: String,
        statusCredito: StatusCredito,
        publicar: Boolean = true
    ): ClienteOrm {
        val cliente = transaction {
            val id = ClientesTable.insertAndGetId {
                it[ClientesTable.nomeCompleto] = nomeCompleto
                it[ClientesTable.cpf] = cpf
                it[ClientesTable.enderecoEntrega] = enderecoEntrega
                it[ClientesTable.statusCredito] = statusCredito
            }.value
            ClienteOrm(id, nomeCompleto, cpf, enderecoEntrega, statusCredito)
        }
        if (publicar) publicarEvento(OperationType.CREATE, EventJson.gson.toJson(cliente))
        return cliente
    }

    /** Atualização parcial: apenas os campos não-nulos são alterados. */
    fun atualizar(
        id: Long,
        nomeCompleto: String? = null,
        cpf: String? = null,
        enderecoEntrega: String? = null,
        statusCredito: StatusCredito? = null,
        publicar: Boolean = true
    ): ClienteOrm? {
        val temAlteracao = listOf(nomeCompleto, cpf, enderecoEntrega, statusCredito).any { it != null }
        val atualizado = transaction {
            if (temAlteracao) {
                ClientesTable.update({ ClientesTable.id eq id }) { stmt ->
                    nomeCompleto?.let { stmt[ClientesTable.nomeCompleto] = it }
                    cpf?.let { stmt[ClientesTable.cpf] = it }
                    enderecoEntrega?.let { stmt[ClientesTable.enderecoEntrega] = it }
                    statusCredito?.let { stmt[ClientesTable.statusCredito] = it }
                }
            }
            buscarNaTransacao(id)
        } ?: return null

        if (publicar) publicarEvento(OperationType.UPDATE, EventJson.gson.toJson(atualizado))
        return atualizado
    }

    fun remover(id: Long, publicar: Boolean = true): Boolean {
        val removidas = transaction {
            ClientesTable.deleteWhere { ClientesTable.id eq id }
        }
        if (removidas > 0 && publicar) {
            publicarEvento(OperationType.DELETE, """{"id":$id}""")
        }
        return removidas > 0
    }

    fun buscar(id: Long): ClienteOrm? = transaction { buscarNaTransacao(id) }

    fun listar(): List<ClienteOrm> = transaction {
        ClientesTable.selectAll().map { it.toCliente() }
    }

    private fun buscarNaTransacao(id: Long): ClienteOrm? =
        ClientesTable.selectAll().where { ClientesTable.id eq id }.map { it.toCliente() }.singleOrNull()

    private fun publicarEvento(tipo: OperationType, data: String) {
        publisher.publish(CrudOperation("Cliente", tipo, Source.ORM, data))
    }

    private fun ResultRow.toCliente() = ClienteOrm(
        id = this[ClientesTable.id].value,
        nomeCompleto = this[ClientesTable.nomeCompleto],
        cpf = this[ClientesTable.cpf],
        enderecoEntrega = this[ClientesTable.enderecoEntrega],
        statusCredito = this[ClientesTable.statusCredito]
    )
}
