package br.ufg.inf.odm

import br.ufg.inf.events.EventJson
import br.ufg.inf.odm.repository.DestinatarioRepository
import com.google.gson.JsonParseException
import io.javalin.Javalin
import io.javalin.http.Context

/** Corpo aceito em POST/PUT. Todos os campos são opcionais (PUT é parcial). */
data class DestinatarioRequest(
    val nomeExibicao: String? = null,
    val logradouroDestino: String? = null,
    val liberadoParaEnvio: Boolean? = null
)

/** API REST do Módulo 2 (porta 8080 por padrão). */
object OdmServer {

    fun create(repo: DestinatarioRepository): Javalin {
        val app = Javalin.create()

        app.get("/destinatarios") { ctx -> ctx.jsonOut(repo.listar()) }

        app.get("/destinatarios/{id}") { ctx ->
            val d = repo.buscar(ctx.pathParam("id"))
            if (d == null) ctx.jsonOut(mapOf("erro" to "destinatário não encontrado"), 404) else ctx.jsonOut(d)
        }

        app.post("/destinatarios") { ctx ->
            val req = ctx.requestBody()
            val criado = repo.criar(
                nomeExibicao = requireNotNull(req.nomeExibicao) { "nomeExibicao é obrigatório" },
                logradouroDestino = requireNotNull(req.logradouroDestino) { "logradouroDestino é obrigatório" },
                liberadoParaEnvio = req.liberadoParaEnvio ?: false,
                publicar = ctx.deveRepublicar()
            )
            ctx.jsonOut(criado, 201)
        }

        app.put("/destinatarios/{id}") { ctx ->
            val req = ctx.requestBody()
            val atualizado = repo.atualizar(
                id = ctx.pathParam("id"),
                nomeExibicao = req.nomeExibicao,
                logradouroDestino = req.logradouroDestino,
                liberadoParaEnvio = req.liberadoParaEnvio,
                publicar = ctx.deveRepublicar()
            )
            if (atualizado == null) ctx.jsonOut(mapOf("erro" to "destinatário não encontrado"), 404) else ctx.jsonOut(atualizado)
        }

        // DELETE idempotente: sempre 204 (o integrador pode reenviar após uma falha)
        app.delete("/destinatarios/{id}") { ctx ->
            repo.remover(ctx.pathParam("id"), publicar = ctx.deveRepublicar())
            ctx.status(204)
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

    private fun Context.requestBody(): DestinatarioRequest =
        EventJson.gson.fromJson(body(), DestinatarioRequest::class.java)
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
