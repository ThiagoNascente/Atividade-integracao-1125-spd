package br.ufg.inf.integrador.routes

import br.ufg.inf.events.EventJson
import br.ufg.inf.integrador.IntegradorConfig
import br.ufg.inf.integrador.processor.ClienteTranslatorProcessor
import br.ufg.inf.integrador.processor.CrudProcessor
import br.ufg.inf.integrador.processor.IdempotenciaProcessor
import br.ufg.inf.integrador.processor.OdmParaOrmProcessor
import br.ufg.inf.integrador.processor.PersistenciaCanonicoProcessor
import br.ufg.inf.integrador.processor.Props
import br.ufg.inf.integrador.processor.Rotas
import br.ufg.inf.integrador.repository.MapeamentoRepository
import br.ufg.inf.integrador.retry.RetryQueue
import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder

/**
 * Rotas Apache Camel do integrador.
 *
 *  direct:crud ──(Message Router)──► direct:orm-to-odm  (Módulo 1 -> Módulo 2)
 *                                └─► direct:odm-to-orm  (Módulo 2 -> Módulo 1)
 *  Falhas esgotadas ─────────────► direct:dead-letter   (fila de retry no Redis)
 */
class CrudTransformRoute(
    private val cfg: IntegradorConfig,
    private val repo: MapeamentoRepository,
    private val retryQueue: RetryQueue
) : RouteBuilder() {

    override fun configure() {

        // Resiliência: 3 reentregas com intervalo de 2s; esgotadas, vai para a Dead Letter.
        errorHandler(
            deadLetterChannel("direct:dead-letter")
                .maximumRedeliveries(cfg.maxRedeliveries)
                .redeliveryDelay(cfg.redeliveryDelayMs)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
        )

        // ---------------------------------------------------------------
        // Roteador principal
        // ---------------------------------------------------------------
        from("direct:crud")
            .routeId("crud-router")
            .process(CrudProcessor())
            .choice()
                .`when`(exchangeProperty(Props.ROTA).isEqualTo(Rotas.ORM_TO_ODM))
                    .to("direct:orm-to-odm")
                .`when`(exchangeProperty(Props.ROTA).isEqualTo(Rotas.ODM_TO_ORM))
                    .to("direct:odm-to-orm")
                .otherwise()
                    .log("Evento ignorado (entidade/origem não tratada)")
            .end()

        // ---------------------------------------------------------------
        // ORM -> ODM : CREATE / UPDATE / DELETE
        // ---------------------------------------------------------------
        from("direct:orm-to-odm")
            .routeId("orm-to-odm")
            .process(IdempotenciaProcessor(repo))
            .choice()
                .`when`(exchangeProperty(Props.SKIP).isEqualTo(true))
                    .log("Nada a fazer (duplicata)")
                .otherwise()
                    .process(ClienteTranslatorProcessor(repo))
                    .setHeader(Exchange.HTTP_METHOD, exchangeProperty(Props.HTTP_METHOD))
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json; charset=UTF-8"))
                    .setHeader(EventJson.ORIGIN_HEADER, constant(EventJson.ORIGIN_INTEGRADOR))
                    .toD(cfg.odmUrl + "\${exchangeProperty.httpPath}")
                    .process(PersistenciaCanonicoProcessor(repo))
                    .log("Sincronizado ORM -> ODM: \${exchangeProperty.httpMethod} \${exchangeProperty.httpPath}")
            .end()

        // ---------------------------------------------------------------
        // ODM -> ORM : UPDATE (fluxo reverso)
        // ---------------------------------------------------------------
        from("direct:odm-to-orm")
            .routeId("odm-to-orm")
            .process(OdmParaOrmProcessor(repo))
            .choice()
                .`when`(exchangeProperty(Props.SKIP).isEqualTo(true))
                    .log("Nada a fazer no fluxo reverso")
                .otherwise()
                    .setHeader(Exchange.HTTP_METHOD, exchangeProperty(Props.HTTP_METHOD))
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json; charset=UTF-8"))
                    .setHeader(EventJson.ORIGIN_HEADER, constant(EventJson.ORIGIN_INTEGRADOR))
                    .toD(cfg.ormUrl + "\${exchangeProperty.httpPath}")
                    .process(PersistenciaCanonicoProcessor(repo))
                    .log("Sincronizado ODM -> ORM: \${exchangeProperty.httpMethod} \${exchangeProperty.httpPath}")
            .end()

        // ---------------------------------------------------------------
        // Dead Letter: guarda o evento original na fila de retry (Redis)
        // ---------------------------------------------------------------
        from("direct:dead-letter")
            .routeId("dead-letter")
            .process(Processor { exchange ->
                val evento = exchange.getProperty(Props.ENVELOPE_JSON, String::class.java)
                    ?: exchange.getMessage().getBody(String::class.java)
                val erro = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable::class.java)?.message
                retryQueue.enqueue(evento, erro)
            })
    }
}
