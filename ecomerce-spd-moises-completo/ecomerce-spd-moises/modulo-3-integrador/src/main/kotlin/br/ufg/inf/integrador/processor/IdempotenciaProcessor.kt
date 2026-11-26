package br.ufg.inf.integrador.processor

import br.ufg.inf.events.CrudOperation
import br.ufg.inf.events.EventJson
import br.ufg.inf.events.OperationType
import br.ufg.inf.integrador.model.ClienteOrmPayload
import br.ufg.inf.integrador.repository.MapeamentoRepository
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

/**
 * Idempotent Receiver: se um CREATE chegar de novo (reentrega, reprocessamento
 * da fila de retry), o `MapeamentoId` já existe e a mensagem é descartada —
 * sem isso criaríamos um segundo documento no MongoDB.
 */
class IdempotenciaProcessor(private val repo: MapeamentoRepository) : Processor {

    private val log = LoggerFactory.getLogger(IdempotenciaProcessor::class.java)

    override fun process(exchange: Exchange) {
        val op = exchange.getProperty(Props.ENVELOPE, CrudOperation::class.java)
        if (op.operation != OperationType.CREATE) return

        val orm = EventJson.gson.fromJson(op.data, ClienteOrmPayload::class.java)
        val idOrm = orm.id ?: return
        val existente = repo.buscarPorIdOrm(idOrm)
        if (existente != null) {
            log.info("[CAMEL] CREATE duplicado ignorado: idOrm={} já mapeado para idOdm={}", idOrm, existente.idOdm)
            exchange.setProperty(Props.SKIP, true)
        }
    }
}
