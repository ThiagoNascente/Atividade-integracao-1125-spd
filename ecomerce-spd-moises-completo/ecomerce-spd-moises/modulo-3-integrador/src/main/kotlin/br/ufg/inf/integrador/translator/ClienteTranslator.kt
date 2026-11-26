package br.ufg.inf.integrador.translator

import br.ufg.inf.integrador.model.ClienteCanonico
import br.ufg.inf.integrador.model.ClienteOrmPayload
import br.ufg.inf.integrador.model.DestinatarioOdmPayload

/**
 * Message Translator: funções puras (sem I/O) que convertem entre
 * ClienteOrm <-> ClienteCanonico <-> DestinatarioOdm. Fáceis de testar.
 */
object ClienteTranslator {

    const val APROVADO = "APROVADO"
    const val BLOQUEADO = "BLOQUEADO"

    // ---------- ORM -> Canônico ----------

    fun ormParaCanonico(orm: ClienteOrmPayload, idCanonico: String): ClienteCanonico {
        val (primeiro, sobrenome) = separarNome(orm.nomeCompleto ?: "")
        val status = orm.statusCredito ?: BLOQUEADO
        return ClienteCanonico(
            idCanonico = idCanonico,
            primeiroNome = primeiro,
            sobrenome = sobrenome,
            documento = orm.cpf ?: "",
            endereco = orm.enderecoEntrega ?: "",
            statusFinanceiro = status,
            elegivelEntrega = status == APROVADO
        )
    }

    /** O ORM é a fonte da verdade para nome, CPF, endereço e crédito: sobrescreve o canônico. */
    fun aplicarOrm(existente: ClienteCanonico, orm: ClienteOrmPayload): ClienteCanonico =
        ormParaCanonico(orm, existente.idCanonico)

    // ---------- Canônico -> ODM ----------

    fun canonicoParaOdm(c: ClienteCanonico): DestinatarioOdmPayload =
        DestinatarioOdmPayload(
            nomeExibicao = formatarNomeExibicao(c.primeiroNome, c.sobrenome),
            logradouroDestino = c.endereco,
            liberadoParaEnvio = c.elegivelEntrega
        )

    // ---------- ODM -> Canônico (fluxo reverso) ----------

    /** O ODM só conhece nome, endereço e liberação; o CPF do canônico é preservado. */
    fun aplicarOdm(existente: ClienteCanonico, odm: DestinatarioOdmPayload): ClienteCanonico {
        val (primeiro, sobrenome) =
            odm.nomeExibicao?.let { lerNomeExibicao(it) } ?: (existente.primeiroNome to existente.sobrenome)
        val liberado = odm.liberadoParaEnvio ?: existente.elegivelEntrega
        return existente.copy(
            primeiroNome = primeiro,
            sobrenome = sobrenome,
            endereco = odm.logradouroDestino ?: existente.endereco,
            statusFinanceiro = if (liberado) APROVADO else BLOQUEADO,
            elegivelEntrega = liberado
        )
    }

    // ---------- Canônico -> ORM (atualização) ----------

    fun canonicoParaOrmUpdate(c: ClienteCanonico): ClienteOrmPayload =
        ClienteOrmPayload(
            nomeCompleto = "${c.primeiroNome} ${c.sobrenome}".trim(),
            enderecoEntrega = c.endereco,
            statusCredito = c.statusFinanceiro
        )

    // ---------- Regras de nome ----------

    /** "Ana Maria Souza" -> ("Ana", "Maria Souza") */
    fun separarNome(nomeCompleto: String): Pair<String, String> {
        val partes = nomeCompleto.trim().split(Regex("\\s+"), 2)
        return partes[0] to (if (partes.size > 1) partes[1] else "")
    }

    /** ("Ana", "Souza") -> "Souza, Ana"; sem sobrenome mantém só o primeiro nome. */
    fun formatarNomeExibicao(primeiro: String, sobrenome: String): String =
        if (sobrenome.isBlank()) primeiro else "$sobrenome, $primeiro"

    /** "Souza, Ana" -> ("Ana", "Souza") */
    fun lerNomeExibicao(nomeExibicao: String): Pair<String, String> {
        val i = nomeExibicao.indexOf(',')
        return if (i < 0) {
            nomeExibicao.trim() to ""
        } else {
            nomeExibicao.substring(i + 1).trim() to nomeExibicao.substring(0, i).trim()
        }
    }
}
