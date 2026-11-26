package br.ufg.inf.orm.model

import org.jetbrains.exposed.dao.id.LongIdTable

enum class StatusCredito { APROVADO, BLOQUEADO }

/** Objeto de domínio do Módulo 1 (Vendas / E-Commerce). */
data class ClienteOrm(
    val id: Long,
    val nomeCompleto: String,
    val cpf: String,
    val enderecoEntrega: String,
    val statusCredito: StatusCredito
)

/** Mapeamento objeto-relacional (tabela `clientes` no SQLite). */
object ClientesTable : LongIdTable("clientes") {
    val nomeCompleto = varchar("nome_completo", 200)
    val cpf = varchar("cpf", 14)
    val enderecoEntrega = varchar("endereco_entrega", 300)
    val statusCredito = enumerationByName("status_credito", 20, StatusCredito::class)
}
