#!/usr/bin/env bash
# Demonstração ponta a ponta com Redis, MongoDB e os três módulos em execução.
set -euo pipefail

for python in python3 python; do
  if command -v "$python" >/dev/null 2>&1 && "$python" -c 'import sys; sys.exit(sys.version_info.major != 3)' 2>/dev/null; then
    break
  fi
  python=""
done
if [[ -z "$python" ]]; then
  echo "Python 3 é necessário para executar a demonstração." >&2
  exit 1
fi

export PYTHONIOENCODING=utf-8
exec "$python" - <<'PY'
import json
import os
import sys
import time
from urllib.error import URLError
from urllib.request import Request, urlopen
from uuid import uuid4

orm = os.environ.get("ORM_URL", "http://localhost:8081").rstrip("/") + "/clientes"
odm = os.environ.get("ODM_URL", "http://localhost:8080").rstrip("/") + "/destinatarios"


def requisicao(url, metodo="GET", dados=None):
    corpo = json.dumps(dados).encode("utf-8") if dados is not None else None
    cabecalhos = {"Content-Type": "application/json"} if corpo is not None else {}
    pedido = Request(url, data=corpo, headers=cabecalhos, method=metodo)
    try:
        with urlopen(pedido, timeout=5) as resposta:
            return json.load(resposta)
    except (URLError, TimeoutError, ValueError) as erro:
        raise RuntimeError(f"{metodo} {url}: {erro}") from erro


def aguardar(descricao, consultar, corresponde):
    limite = time.monotonic() + 15
    while time.monotonic() < limite:
        for item in consultar():
            if corresponde(item):
                return item
        time.sleep(0.25)
    raise RuntimeError(f"Tempo esgotado aguardando {descricao}. Confira os logs do integrador.")


def mostrar(etapa, registro):
    print(f"{etapa}: {json.dumps(registro, ensure_ascii=False)}", flush=True)


def demonstrar():
    endereco = f"Rua Demo {uuid4().hex}"
    cliente = requisicao(orm, "POST", {
        "nomeCompleto": "Ana Souza",
        "cpf": "12345678900",
        "enderecoEntrega": endereco,
        "statusCredito": "APROVADO",
    })
    cliente_url = f"{orm}/{cliente['id']}"
    mostrar("1. Cliente criado no ORM", cliente)

    destino = aguardar(
        "a criação do destinatário",
        lambda: requisicao(odm),
        lambda item: item["logradouroDestino"] == endereco
        and item["nomeExibicao"] == "Souza, Ana"
        and item["liberadoParaEnvio"] is True,
    )
    destino_url = f"{odm}/{destino['id']}"
    mostrar("2. Destinatário sincronizado no ODM", destino)

    novo_endereco = f"{endereco} atualizado"
    requisicao(cliente_url, "PUT", {
        "enderecoEntrega": novo_endereco,
        "statusCredito": "BLOQUEADO",
    })
    destino = aguardar(
        "o novo endereço e o bloqueio de envio no ODM",
        lambda: [requisicao(destino_url)],
        lambda item: item["logradouroDestino"] == novo_endereco
        and item["liberadoParaEnvio"] is False,
    )
    mostrar("3. Atualização ORM → ODM", destino)

    requisicao(destino_url, "PUT", {"liberadoParaEnvio": True})
    cliente = aguardar(
        "a aprovação do crédito no ORM",
        lambda: [requisicao(cliente_url)],
        lambda item: item["statusCredito"] == "APROVADO"
        and item["enderecoEntrega"] == novo_endereco,
    )
    mostrar("4. Atualização ODM → ORM", cliente)


try:
    demonstrar()
except (RuntimeError, KeyError) as erro:
    sys.exit(f"Falha na demonstração: {erro}")
PY
