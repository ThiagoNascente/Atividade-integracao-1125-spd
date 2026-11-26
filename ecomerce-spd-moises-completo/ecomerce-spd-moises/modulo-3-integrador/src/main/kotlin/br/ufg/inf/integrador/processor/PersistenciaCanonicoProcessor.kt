package br.ufg.inf.integrador.processor

import br.ufg.inf.integrador.model.ClienteCanonico
import br.ufg.inf.integrador.model.MapeamentoId
import br.ufg.inf.integrador.repository.MapeamentoRepository
import com.google.gson.JsonParser
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

/**
 * Último passo (executa SÓ se o HTTP do destino deu certo): grava o modelo
 * canônico e o mapeamento de IDs em `integrador.db`.
 */
class PersistenciaCanonicoProcessor(private val repo: MapeamentoRepository) : Processor {

    private val log = LoggerFactory.getLogger(PersistenciaCanonicoProcessor::class.java)

    override fun process(exchange: Exchange) {
        when (exchange.getProperty(Props.ACAO, String::class.java)) {
            Acoes.CRIAR -> {
                val canonico = exchange.getProperty(Props.CANONICO, ClienteCanonico::class.java)
                val idOrm = exchange.getProperty(Props.ID_ORM, Long::class.javaObjectType)
                val resposta = exchange.getMessage().getBody(String::class.java)
                val idOdm = JsonParser.parseString(resposta).asJsonObject.get("id").asString
                repo.salvarNovo(canonico, MapeamentoId(canonico.idCanonico, idOrm, idOdm))
                log.info("[CAMEL] Persistido canônico {} e mapeamento idOrm={} <-> idOdm={}", canonico.idCanonico, idOrm, idOdm)
            }

            Acoes.SALVAR -> {
                val canonico = exchange.getProperty(Props.CANONICO, ClienteCanonico::class.java)
                repo.salvarCanonico(canonico)
                log.info("[CAMEL] Canônico {} atualizado", canonico.idCanonico)
            }

            Acoes.REMOVER -> {
                val idOrm = exchange.getProperty(Props.ID_ORM, Long::class.javaObjectType)
                repo.removerPorIdOrm(idOrm)
                log.info("[CAMEL] Mapeamento e canônico do idOrm={} removidos", idOrm)
            }
        }
    }
}
