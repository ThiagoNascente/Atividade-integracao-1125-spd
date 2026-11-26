package br.ufg.inf.integrador.processor

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.OperationType
import br.ufg.inf.integrador.model.DestinatarioOdmPayload
import br.ufg.inf.integrador.repository.MapeamentoRepository
import br.ufg.inf.integrador.translator.ClienteTranslator
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

/**
 * Message Translator (ODM -> ORM) — fluxo reverso simplificado.
 * Só propaga UPDATE de destinatários que o integrador já conhece (existe mapeamento).
 * Ex.: liberadoParaEnvio=false no MongoDB => statusCredito=BLOQUEADO no SQLite.
 */
class OdmParaOrmProcessor(private val repo: MapeamentoRepository) : Processor {

    private val log = LoggerFactory.getLogger(OdmParaOrmProcessor::class.java)

    override fun process(exchange: Exchange) {
        val op = exchange.getProperty(Props.ENVELOPE, CrudOperation::class.java)
        if (op.operation != OperationType.UPDATE) {
            log.info("[CAMEL] Fluxo reverso só trata UPDATE; {} do ODM ignorado", op.operation)
            exchange.setProperty(Props.SKIP, true)
            return
        }

        val odm = EventJson.gson.fromJson(op.data, DestinatarioOdmPayload::class.java)
        val idOdm = odm.id
        val mapeamento = idOdm?.let { repo.buscarPorIdOdm(it) }
        if (mapeamento == null) {
            log.info("[CAMEL] Destinatário {} não tem mapeamento (não veio do ORM); ignorado", idOdm)
            exchange.setProperty(Props.SKIP, true)
            return
        }
        val atual = repo.buscarCanonico(mapeamento.idCanonico)
            ?: error("Canônico ${mapeamento.idCanonico} não encontrado")

        val novo = ClienteTranslator.aplicarOdm(atual, odm)
        val ormUpdate = ClienteTranslator.canonicoParaOrmUpdate(novo)

        exchange.setProperty(Props.CANONICO, novo)
        exchange.setProperty(Props.ID_ORM, mapeamento.idOrm)
        exchange.setProperty(Props.ID_ODM, mapeamento.idOdm)
        exchange.setProperty(Props.HTTP_METHOD, "PUT")
        exchange.setProperty(Props.HTTP_PATH, "/clientes/${mapeamento.idOrm}")
        exchange.setProperty(Props.ACAO, Acoes.SALVAR)
        exchange.getMessage().setBody(EventJson.gson.toJson(ormUpdate))
        log.info("[CAMEL] Traduzido ODM {} -> PUT ORM idOrm={} {}", idOdm, mapeamento.idOrm, ormUpdate)
    }
}
