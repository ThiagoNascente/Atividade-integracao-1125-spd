package br.ufg.inf.integrador.model

/**
 * Modelo Canônico de Dados (Canonical Data Model): formato neutro, independente
 * dos esquemas do SQLite (ClienteOrm) e do MongoDB (DestinatarioOdm).
 */
data class ClienteCanonico(
    val idCanonico: String,          // UUID gerado pelo integrador
    val primeiroNome: String,
    val sobrenome: String,
    val documento: String,           // CPF — existe só no ORM e no canônico
    val endereco: String,
    val statusFinanceiro: String,    // "APROVADO" | "BLOQUEADO"
    val elegivelEntrega: Boolean
)

/** Liga o mesmo cliente nos três mundos: canônico (UUID), ORM (Long) e ODM (ObjectId hex). */
data class MapeamentoId(
    val idCanonico: String,
    val idOrm: Long,
    val idOdm: String
)
