# Pingu 007

Pingu 007 é um jogo de ação 2D desenvolvido em Java como trabalho da disciplina de Programação Orientada a Objetos.

O jogo foi inspirado no episódio de pesadelo da série **Pingu**. Nele, o jogador controla um Pingu agente secreto que precisa atravessar uma base polar, enfrentar inimigos e derrotar a Morsa para concluir a missão.

<img src="screenshots/episodio.png" alt="Episódio" width="420">

*Episódio de inspiração para o jogo*

## Menu principal

Ao iniciar o jogo, o menu principal permite começar a jogar, acessar as configurações ou encerrar o programa.

<img src="screenshots/menu_principal.png" alt="Menu Principal" width="700">

## Gameplay

O personagem pode se movimentar usando `W`, `A`, `S` e `D`, realizar um dash com `Espaço`, atirar utilizando o botão esquerdo do mouse, recarregar a arma com `R` e interagir com objetos pressionando `E`.

<img src="screenshots/gameplay.png" alt="Gameplay" width="700">

Durante a fase é possível encontrar alguns itens que auxiliam o jogador, como munição, kits de cura e chaves utilizadas para abrir o acesso à sala do chefe.

## Arenas

O mapa também possui algumas arenas. Ao entrar em uma delas, a saída é bloqueada e só é liberada após todos os inimigos serem derrotados.

<img src="screenshots/arena.png" alt="Arena" width="700">

## Inimigos

O jogo possui diversos tipos de inimigos:

* Lobo
* Narval(dasher)
* Boneco de Neve
* Bombardeiro
* Atirador

<img src="screenshots/inimigos.png" alt="Inimigos" width="600">

Cada inimigo possui um comportamento diferente, exigindo estratégias distintas durante a fase.


Ao final do mapa acontece a batalha contra a Morsa.

<img src="screenshots/boss.png" alt="Boss" width="700">


<!-- <img src="screenshots/boss_fight.png" alt="Luta contra a Morsa" width="700"> -->

## Cenário

O mapa é composto por diferentes tipos de terreno. Além da neve comum, existem áreas de gelo, que alteram a movimentação do personagem, e buracos que causam dano caso o jogador passe sobre eles.

<img src="screenshots/map.png" alt="Mapa" width="700">


## Configurações

O menu de configurações pode ser acessado tanto pelo menu principal quanto durante a partida.

Nele é possível ajustar o volume da música e dos efeitos sonoros, além de consultar as teclas de ações do jogo.

<img src="screenshots/settings.png" alt="Configurações" width="700">

## Pausa

Durante o jogo, pressionando `Esc`, o jogo é pausado. Nesse menu é possível continuar o jogo, abrir as configurações ou retornar ao menu principal.

<img src="screenshots/pause.png" alt="Menu de Pausa" width="700">

## Como executar

O jogo exige o Java JDK instalado no computador. Para executar o jogo, deve-se compilar o código fonte e executá-lo, com os comandos:

Compilação:
```bash
javac *.java
```

Execução:
```bash
java GameCore
```

Os arquivos de imagens, sons, mapas e demais recursos devem permanecer na pasta raiz do projeto.


## Autores

| Nome    | RA |
| ------- | -- |
| Alexander Enzo Açano | RA |
| André Arsky Silva Araujo | RA |
| Kauã Victor Menezes Ferraz | RA |
| Leonardo Lima Silva | RA |

Esse texto de README teve sua escrita auxiliada por IA.
