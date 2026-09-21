# Pingu 007

Pingu 007 é um jogo de ação 2D desenvolvido em Java como trabalho da disciplina de Programação Orientada a Objetos.

O jogo foi inspirado no episódio de pesadelo da série **Pingu**. Nele, o jogador controla um Pingu agente secreto que precisa atravessar uma base polar, enfrentar inimigos e derrotar a Morsa para concluir a missão.

<img src="screenshots/episodio.png" alt="Episódio" width="420">

*Episódio de inspiração para o jogo*

## Gameplay

O personagem pode se movimentar usando `W`, `A`, `S` e `D`, realizar um dash com `Espaço`, atirar utilizando o botão esquerdo do mouse, recarregar a arma com `R` e interagir com objetos pressionando `E`.

<img src="screenshots/gamep.png" alt="Gameplay" width="700">

Durante a fase é possível encontrar alguns itens que auxiliam o jogador, como munição, kits de cura e chaves utilizadas para abrir o acesso à sala do chefe.

## Arenas

O mapa também possui algumas arenas. Ao entrar em uma delas, a saída é bloqueada e só é liberada após todos os inimigos serem derrotados.

<img src="screenshots/arena_enter.webp" alt="Arenas" width="700">

## Pesca

Depois de ganhar do pescador a **vara de pesca**, é possível pescar nos buracos de água para conseguir cura de vida, munição, ou chaves para abrir o portão do Boss.

<img src="screenshots/pesca.png" alt="Pesca" width="700">

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

<img src="screenshots/morsa_rugido.webp" alt="Boss" width="700">


<!-- <img src="screenshots/boss_fight.png" alt="Luta contra a Morsa" width="700"> -->

## Cenário

O mapa é composto por diferentes tipos de terreno. Além da neve comum, existem áreas de gelo que alteram a movimentação do personagem e buracos que causam dano caso o jogador passe sobre eles.

<img src="screenshots/map.png" alt="Mapa" width="700">

## *NPCs*

Além de inimigos, o jogo possui diversos *NPCs* com os quais o jogador pode interagir para comprar itens, descobrir segredos e receber recompensas.

<img src="screenshots/npcs.png" alt="Mapa" width="700">

## Como executar

O jogo exige um JDK 21 ou superior. O Gradle Wrapper baixa as dependências
(libGDX 1.14.2 e gdx-miniaudio 0.8) e mantém o diretório de trabalho em
`Pingu-007`, onde ficam os mapas, imagens e sons.

Na raiz do projeto:

```bash
./gradlew clean run       # Linux/macOS
gradlew.bat clean run     # Windows
```

Também é possível executar `compila_e_roda_linux.sh` ou
`compila_e_roda.bat` dentro de `Pingu-007`.


## Autores

| Nome    | RA |
| ------- | -- |
| Alexander Enzo Açano | RA |
| André Arsky Silva Araujo | RA |
| Kauã Victor Menezes Ferraz | RA |
| Leonardo Lima Silva | RA |
