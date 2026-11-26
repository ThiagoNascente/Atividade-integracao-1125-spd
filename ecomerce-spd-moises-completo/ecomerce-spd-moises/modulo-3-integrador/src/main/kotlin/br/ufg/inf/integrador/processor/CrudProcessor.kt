package br.ufg.inf.integrador.processor

import br.ufg.inf.events.EventJson
import br.ufg.inf.events.Source
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

/**
 * Primeiro passo da rota: faz o parse do envelope `CrudOperation` e decide
 * para qual sub-rota a mensagem segue (Message Router, via propriedade `rota`).
 * Mensagens inválidas ou de outras entidades são marcadas como IGNORAR —
 * repetir o processamento de lixo não adiantaria, então não geram retry.
 */
class CrudProcessor : Processor {

    private val log = LoggerFactory.getLogger(CrudProcessor::class.java)

    override fun process(exchange: Exchange) {
        val json = exchange.getMessage().getBody(String::class.java)
        exchange.setProperty(Props.ENVELOPE_JSON, json)

        val op = json?.let { EventJson.parse(it) }
        if (op == null) {
            log.warn("[CAMEL] Mensagem inválida descartada: {}", json)
            exchange.setProperty(Props.ROTA, Rotas.IGNORAR)
            return
        }
        exchange.setProperty(Props.ENVELOPE, op)

        val rota = when {
            op.entity != "Cliente" -> Rotas.IGNORAR
            op.source == Source.ORM -> Rotas.ORM_TO_ODM
            op.source == Source.ODM -> Rotas.ODM_TO_ORM
            else -> Rotas.IGNORAR
        }
        log.info("[CAMEL] Evento {} {} origem={} -> rota {}", op.entity, op.operation, op.source, rota)
        exchange.setProperty(Props.ROTA, rota)
    }
}
