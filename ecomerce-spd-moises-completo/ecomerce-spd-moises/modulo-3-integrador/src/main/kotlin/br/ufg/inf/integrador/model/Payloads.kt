package br.ufg.inf.integrador.model

/**
 * Contratos JSON trocados com os módulos 1 e 2. O integrador NÃO depende dos
 * módulos em tempo de compilação — só do formato JSON (baixo acoplamento).
 * Campos nulos são omitidos na serialização (útil para updates parciais).
 */
data class ClienteOrmPayload(
    val id: Long? = null,
    val nomeCompleto: String? = null,
    val cpf: String? = null,
    val enderecoEntrega: String? = null,
    val statusCredito: String? = null
)

data class DestinatarioOdmPayload(
    val id: String? = null,
    val nomeExibicao: String? = null,
    val logradouroDestino: String? = null,
    val liberadoParaEnvio: Boolean? = null
)
