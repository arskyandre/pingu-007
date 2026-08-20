# Conversa com o Vendedor usando Ollama

Esta versão adiciona uma conversa opcional com o NPC Vendedor usando o modelo local `gemma3:4b` através do Ollama.

## Requisitos

- Java JDK instalado;
- Ollama instalado e em execução;
- modelo `gemma3:4b` baixado.

Baixe o modelo, caso ainda não esteja instalado:

```powershell
ollama pull gemma3:4b
```

Antes de iniciar o jogo, deixe o Ollama disponível localmente:

```powershell
ollama run gemma3:4b
```

## Como usar no jogo

1. Inicie o Pingu 007 normalmente.
2. Interaja com o Vendedor pressionando `E`.
3. Escolha **Vamos conversar!**.
4. Digite uma mensagem no campo de texto e pressione `Enter`.
5. Use **Terminar Conversa** para encerrar o chat e voltar ao menu do Vendedor.

As mensagens da conversa são mantidas enquanto o chat estiver aberto. O contexto é apagado ao terminar a conversa ou iniciar uma nova.

## Personalização

O prompt de personalidade, história e regras do Vendedor está em:

```text
Pingu-007/OllamaClient.java
```

Edite a constante `SYSTEM_PROMPT` para adicionar detalhes do mundo e da história do jogo.

## Execução

Na pasta `Pingu-007`, execute:

```powershell
.\compila_e_roda.bat
```

O jogo usa a API local do Ollama em `http://localhost:11434/api/chat` e não envia as mensagens para um serviço externo.
