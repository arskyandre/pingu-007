#!/usr/bin/env bash

set -eu

# Executa sempre a partir da pasta em que este script esta localizado.
cd -- "$(dirname -- "${BASH_SOURCE[0]}")" || {
    echo "Erro: nao foi possivel acessar a pasta do jogo." >&2
    exit 1
}

if ! command -v javac >/dev/null 2>&1; then
    echo "Erro: javac nao foi encontrado. Instale um JDK e adicione-o ao PATH." >&2
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "Erro: java nao foi encontrado. Instale um JDK e adicione-o ao PATH." >&2
    exit 1
fi

exec sh ../gradlew -p .. clean run
