package br.ufg.inf.integrador.processor

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.OperationType
import br.ufg.inf.integrador.model.ClienteOrmPayload
import br.ufg.inf.integrador.repository.MapeamentoRepository
import br.ufg.inf.integrador.translator.ClienteTranslator
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Message Translator (ORM -> ODM): lê o evento do ORM, converte para o modelo
 * canônico e monta a requisição HTTP para o Módulo 2 (método, caminho e corpo).
 */
class ClienteTranslatorProcessor(private val repo: MapeamentoRepository) : Processor {

    private val log = LoggerFactory.getLogger(ClienteTranslatorProcessor::class.java)

    override fun process(exchange: Exchange) {
        val op = exchange.getProperty(Props.ENVELOPE, CrudOperation::class.java)
        val orm = EventJson.gson.fromJson(op.data, ClienteOrmPayload::class.java)
        val idOrm = requireNotNull(orm.id) { "Evento do ORM sem id: ${op.data}" }
        exchange.setProperty(Props.ID_ORM, idOrm)

        when (op.operation) {
            OperationType.CREATE -> {
                val canonico = ClienteTranslator.ormParaCanonico(orm, UUID.randomUUID().toString())
                val odm = ClienteTranslator.canonicoParaOdm(canonico)
                exchange.setProperty(Props.CANONICO, canonico)
                exchange.setProperty(Props.HTTP_METHOD, "POST")
                exchange.setProperty(Props.HTTP_PATH, "/destinatarios")
                exchange.setProperty(Props.ACAO, Acoes.CRIAR)
                exchange.getMessage().setBody(EventJson.gson.toJson(odm))
                log.info("[CAMEL] Traduzido ORM -> canônico {} -> ODM {}", canonico.idCanonico, odm)
            }

            OperationType.UPDATE -> {
                val mapeamento = repo.buscarPorIdOrm(idOrm)
                    ?: error("Sem mapeamento para idOrm=$idOrm (CREATE ainda não processado?)")
                val atual = repo.buscarCanonico(mapeamento.idCanonico)
                    ?: error("Canônico ${mapeamento.idCanonico} não encontrado")
                val novo = ClienteTranslator.aplicarOrm(atual, orm)
                val odm = ClienteTranslator.canonicoParaOdm(novo)
                exchange.setProperty(Props.CANONICO, novo)
                exchange.setProperty(Props.ID_ODM, mapeamento.idOdm)
                exchange.setProperty(Props.HTTP_METHOD, "PUT")
                exchange.setProperty(Props.HTTP_PATH, "/destinatarios/${mapeamento.idOdm}")
                exchange.setProperty(Props.ACAO, Acoes.SALVAR)
                exchange.getMessage().setBody(EventJson.gson.toJson(odm))
                log.info("[CAMEL] Traduzido UPDATE idOrm={} -> PUT idOdm={} {}", idOrm, mapeamento.idOdm, odm)
            }

            OperationType.DELETE -> {
                val mapeamento = repo.buscarPorIdOrm(idOrm)
                    ?: error("Sem mapeamento para idOrm=$idOrm (CREATE ainda não processado?)")
                exchange.setProperty(Props.ID_ODM, mapeamento.idOdm)
                exchange.setProperty(Props.HTTP_METHOD, "DELETE")
                exchange.setProperty(Props.HTTP_PATH, "/destinatarios/${mapeamento.idOdm}")
                exchange.setProperty(Props.ACAO, Acoes.REMOVER)
                val semCorpo: Any? = null
                exchange.getMessage().setBody(semCorpo)
                log.info("[CAMEL] Traduzido DELETE idOrm={} -> DELETE idOdm={}", idOrm, mapeamento.idOdm)
            }
        }
    }
}
