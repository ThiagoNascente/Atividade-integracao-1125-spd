#!/usr/bin/env bash
# Demonstração ponta a ponta. Pré-requisitos: docker compose up -d e os 3 módulos rodando
# (integrador, ODM e ORM — veja o README, seção "Execução").
set -euo pipefail

ORM=${ORM_URL:-http://localhost:8081}
ODM=${ODM_URL:-http://localhost:8080}
JSON='Content-Type: application/json'

echo "=== 1) CREATE no Módulo 1 (ORM/SQLite) ==="
curl -s -X POST "$ORM/clientes" -H "$JSON" \
  -d '{"nomeCompleto":"Ana Souza","cpf":"12345678900","enderecoEntrega":"Rua 10, Goiânia","statusCredito":"APROVADO"}'
echo; sleep 2

echo "=== 2) Destinatário criado no Módulo 2 (ODM/MongoDB) ==="
curl -s "$ODM/destinatarios"; echo

echo "=== 3) UPDATE no ORM: novo endereço + crédito BLOQUEADO ==="
curl -s -X PUT "$ORM/clientes/1" -H "$JSON" \
  -d '{"enderecoEntrega":"Av. Goiás, 500","statusCredito":"BLOQUEADO"}'
echo; sleep 2
curl -s "$ODM/destinatarios"; echo

echo "=== 4) Fluxo reverso: libera o envio direto no ODM (liberadoParaEnvio=true) ==="
ID_ODM=$(curl -s "$ODM/destinatarios" | sed -E 's/.*"id":"([a-f0-9]{24})".*/\1/')
curl -s -X PUT "$ODM/destinatarios/$ID_ODM" -H "$JSON" -d '{"liberadoParaEnvio":true}'
echo; sleep 2
echo "Cliente no ORM (statusCredito deve voltar a APROVADO):"
curl -s "$ORM/clientes/1"; echo
