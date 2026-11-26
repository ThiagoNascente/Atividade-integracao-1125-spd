package br.ufg.inf.orm

import br.ufg.inf.events.EventJson
import br.ufg.inf.orm.model.StatusCredito
import br.ufg.inf.orm.repository.ClienteOrmRepository
import com.google.gson.JsonParseException
import io.javalin.Javalin
import io.javalin.http.Context

/** Corpo aceito em POST/PUT. Todos os campos são opcionais (PUT é parcial). */
data class ClienteRequest(
    val nomeCompleto: String? = null,
    val cpf: String? = null,
    val enderecoEntrega: String? = null,
    val statusCredito: StatusCredito? = null
)

/** API REST do Módulo 1 (porta 8081 por padrão). */
object OrmServer {

    fun create(repo: ClienteOrmRepository): Javalin {
        val app = Javalin.create()

        app.get("/clientes") { ctx -> ctx.jsonOut(repo.listar()) }

        app.get("/clientes/{id}") { ctx ->
            val cliente = repo.buscar(ctx.idParam())
            if (cliente == null) ctx.jsonOut(mapOf("erro" to "cliente não encontrado"), 404) else ctx.jsonOut(cliente)
        }

        app.post("/clientes") { ctx ->
            val req = ctx.requestBody()
            val criado = repo.criar(
                nomeCompleto = requireNotNull(req.nomeCompleto) { "nomeCompleto é obrigatório" },
                cpf = requireNotNull(req.cpf) { "cpf é obrigatório" },
                enderecoEntrega = requireNotNull(req.enderecoEntrega) { "enderecoEntrega é obrigatório" },
                statusCredito = req.statusCredito ?: StatusCredito.APROVADO,
                publicar = ctx.deveRepublicar()
            )
            ctx.jsonOut(criado, 201)
        }

        app.put("/clientes/{id}") { ctx ->
            val req = ctx.requestBody()
            val atualizado = repo.atualizar(
                id = ctx.idParam(),
                nomeCompleto = req.nomeCompleto,
                cpf = req.cpf,
                enderecoEntrega = req.enderecoEntrega,
                statusCredito = req.statusCredito,
                publicar = ctx.deveRepublicar()
            )
            if (atualizado == null) ctx.jsonOut(mapOf("erro" to "cliente não encontrado"), 404) else ctx.jsonOut(atualizado)
        }

        app.delete("/clientes/{id}") { ctx ->
            val ok = repo.remover(ctx.idParam(), publicar = ctx.deveRepublicar())
            if (ok) ctx.status(204) else ctx.jsonOut(mapOf("erro" to "cliente não encontrado"), 404)
        }

        app.exception(IllegalArgumentException::class.java) { e, ctx ->
            ctx.jsonOut(mapOf("erro" to (e.message ?: "requisição inválida")), 400)
        }
        app.exception(JsonParseException::class.java) { e, ctx ->
            ctx.jsonOut(mapOf("erro" to "JSON inválido: ${e.message}"), 400)
        }
        return app
    }

    // ---- helpers ----

    private fun Context.idParam(): Long =
        pathParam("id").toLongOrNull() ?: throw IllegalArgumentException("id deve ser numérico")

    private fun Context.requestBody(): ClienteRequest =
        EventJson.gson.fromJson(body(), ClienteRequest::class.java)
            ?: throw IllegalArgumentException("corpo da requisição vazio")

    /** Escritas feitas pelo integrador não devem gerar novo evento (anti-loop). */
    private fun Context.deveRepublicar(): Boolean =
        header(EventJson.ORIGIN_HEADER) != EventJson.ORIGIN_INTEGRADOR

    private fun Context.jsonOut(corpo: Any, codigo: Int = 200) {
        status(codigo)
        contentType("application/json")
        result(EventJson.gson.toJson(corpo))
    }
}
