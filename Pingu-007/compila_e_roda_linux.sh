#!/usr/bin/env bash

set -u

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

echo "Compilando..."
if ! javac -cp ".:*" ./*.java; then
    echo "Erro: a compilacao falhou." >&2
    exit 1
fi

echo "Iniciando o jogo..."
java -cp ".:*" GameCore
game_exit_code=$?

rm -f -- ./*.class

if (( game_exit_code != 0 )); then
    echo "O jogo terminou com o codigo de erro ${game_exit_code}." >&2
fi

exit "${game_exit_code}"
