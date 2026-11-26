# Integração ORM ↔ ODM

Exemplo de integração entre um e-commerce e um sistema de logística. A API de clientes usa Exposed e SQLite, a API de destinatários usa MongoDB e um integrador Apache Camel traduz os eventos recebidos pelo Redis Pub/Sub.

## Executar

Use JDK 21 e Docker com Compose. No PowerShell, a partir da raiz deste repositório, execute os comandos abaixo.

```powershell
cd .\ecomerce-spd-moises-completo\ecomerce-spd-moises
.\gradlew.bat build
docker compose up -d
```

Na mesma pasta, execute cada linha em um terminal separado.

```powershell
.\gradlew.bat :modulo-3-integrador:run
.\gradlew.bat :modulo-2-odm:run
.\gradlew.bat :modulo-1-orm:run
```

No Linux ou macOS, use `bash ./gradlew` no lugar de `.\gradlew.bat`. Quando o integrador estiver inscrito no canal `crud-channel` e as APIs estiverem prontas, execute `bash scripts/demo.sh` em outro terminal com Bash e Python 3. O roteiro usa dados fictícios, verifica as duas direções da integração e imprime os IDs encontrados.

## Documentação e evidências

- [README do projeto](ecomerce-spd-moises-completo/ecomerce-spd-moises/README.md), com arquitetura, modelagem, testes e resultados de execução.
- [Documentação geral no Notion](https://app.notion.com/p/Integra-o-ORM-para-ODM-424a35ffbe7d816e8c62d8273a44d530), com um roteiro em PowerShell e evidências da integração.

## Créditos

- Moises — etapa inicial
- Thiago — relatório e auditoria
