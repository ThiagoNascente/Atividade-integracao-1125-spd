package br.ufg.inf.integrador.repository

import br.ufg.inf.integrador.model.ClienteCanonico
import br.ufg.inf.integrador.model.MapeamentoId
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Persistência do Idempotent Receiver: guarda o cliente canônico e a tabela
 * `MapeamentoId` (idCanonico <-> idOrm <-> idOdm).
 */
class MapeamentoRepository {

    fun buscarPorIdOrm(idOrm: Long): MapeamentoId? = transaction {
        MapeamentoIdTable.selectAll().where { MapeamentoIdTable.idOrm eq idOrm }
            .map { it.toMapeamento() }.singleOrNull()
    }

    fun buscarPorIdOdm(idOdm: String): MapeamentoId? = transaction {
        MapeamentoIdTable.selectAll().where { MapeamentoIdTable.idOdm eq idOdm }
            .map { it.toMapeamento() }.singleOrNull()
    }

    fun buscarCanonico(idCanonico: String): ClienteCanonico? = transaction {
        ClienteCanonicoTable.selectAll().where { ClienteCanonicoTable.idCanonico eq idCanonico }
            .map { it.toCanonico() }.singleOrNull()
    }

    /** Grava canônico + mapeamento na mesma transação (ou os dois, ou nenhum). */
    fun salvarNovo(canonico: ClienteCanonico, mapeamento: MapeamentoId) {
        transaction {
            inserirCanonico(canonico)
            MapeamentoIdTable.insert {
                it[idCanonico] = mapeamento.idCanonico
                it[idOrm] = mapeamento.idOrm
                it[idOdm] = mapeamento.idOdm
            }
        }
    }

    /** Upsert do canônico (atualiza se existir, senão insere). */
    fun salvarCanonico(c: ClienteCanonico) {
        transaction {
            val atualizadas = ClienteCanonicoTable.update({ ClienteCanonicoTable.idCanonico eq c.idCanonico }) {
                it[primeiroNome] = c.primeiroNome
                it[sobrenome] = c.sobrenome
                it[documento] = c.documento
                it[endereco] = c.endereco
                it[statusFinanceiro] = c.statusFinanceiro
                it[elegivelEntrega] = c.elegivelEntrega
            }
            if (atualizadas == 0) inserirCanonico(c)
        }
    }

    fun removerPorIdOrm(idOrm: Long) {
        transaction {
            val m = MapeamentoIdTable.selectAll().where { MapeamentoIdTable.idOrm eq idOrm }
                .map { it.toMapeamento() }.singleOrNull()
            if (m != null) {
                MapeamentoIdTable.deleteWhere { MapeamentoIdTable.idCanonico eq m.idCanonico }
                ClienteCanonicoTable.deleteWhere { ClienteCanonicoTable.idCanonico eq m.idCanonico }
            }
        }
    }

    private fun inserirCanonico(c: ClienteCanonico) {
        ClienteCanonicoTable.insert {
            it[idCanonico] = c.idCanonico
            it[primeiroNome] = c.primeiroNome
            it[sobrenome] = c.sobrenome
            it[documento] = c.documento
            it[endereco] = c.endereco
            it[statusFinanceiro] = c.statusFinanceiro
            it[elegivelEntrega] = c.elegivelEntrega
        }
    }

    private fun ResultRow.toMapeamento() = MapeamentoId(
        idCanonico = this[MapeamentoIdTable.idCanonico],
        idOrm = this[MapeamentoIdTable.idOrm],
        idOdm = this[MapeamentoIdTable.idOdm]
    )

    private fun ResultRow.toCanonico() = ClienteCanonico(
        idCanonico = this[ClienteCanonicoTable.idCanonico],
        primeiroNome = this[ClienteCanonicoTable.primeiroNome],
        sobrenome = this[ClienteCanonicoTable.sobrenome],
        documento = this[ClienteCanonicoTable.documento],
        endereco = this[ClienteCanonicoTable.endereco],
        statusFinanceiro = this[ClienteCanonicoTable.statusFinanceiro],
        elegivelEntrega = this[ClienteCanonicoTable.elegivelEntrega]
    )
}
