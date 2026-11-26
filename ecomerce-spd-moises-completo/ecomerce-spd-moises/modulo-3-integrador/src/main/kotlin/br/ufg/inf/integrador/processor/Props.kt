package br.ufg.inf.integrador.processor

/** Nomes das propriedades do Exchange Camel (estado interno da mensagem, NÃO enviado por HTTP). */
object Props {
    const val ENVELOPE = "envelope"            // CrudOperation já parseado
    const val ENVELOPE_JSON = "envelopeJson"   // JSON original (para a fila de retry)
    const val ROTA = "rota"                    // ORM_TO_ODM | ODM_TO_ORM | IGNORAR
    const val SKIP = "skip"                    // true => nada a fazer (duplicata / não aplicável)
    const val CANONICO = "canonico"            // ClienteCanonico resultante da tradução
    const val ID_ORM = "idOrm"
    const val ID_ODM = "idOdm"
    const val HTTP_METHOD = "httpMethod"
    const val HTTP_PATH = "httpPath"
    const val ACAO = "acaoPersistencia"        // CRIAR | SALVAR | REMOVER
}

object Rotas {
    const val ORM_TO_ODM = "ORM_TO_ODM"
    const val ODM_TO_ORM = "ODM_TO_ORM"
    const val IGNORAR = "IGNORAR"
}

object Acoes {
    const val CRIAR = "CRIAR"
    const val SALVAR = "SALVAR"
    const val REMOVER = "REMOVER"
}
