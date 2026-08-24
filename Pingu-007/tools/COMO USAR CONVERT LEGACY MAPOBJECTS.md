# Como usar o conversor de MapObjects antigos

O arquivo `convert_legacy_mapobjects.py` converte cenários antigos de mapas do
Tiled (`.json` ou `.tmj`) em objetos na camada `Paredes`.

Ele reconhece árvores, iglus, pedras e cercas desenhados nas antigas camadas de
tiles. Para cada conjunto reconhecido, o programa:

1. remove os tiles antigos;
2. adiciona o tileset de objeto necessário, caso ainda não exista;
3. cria o objeto equivalente na camada `Paredes`;
4. atualiza o `nextobjectid` do mapa.

O mapa precisa conter a camada de objetos `Paredes` e as camadas antigas usadas
pelo conversor, como `tTree`, `bTree`, `tIglu`, `bIglu`, `tStone`, `bStone` e
`fence`.

## Conferir sem alterar o mapa

Na pasta `Pingu-007`, execute:

```powershell
python tools/convert_legacy_mapobjects.py caminho/do/mapa.tmj
```

Esse é o modo de simulação (*dry run*). Ele mostra quantos objetos seriam
convertidos e possíveis avisos, mas não grava nenhuma alteração.

## Criar uma cópia convertida

```powershell
python tools/convert_legacy_mapobjects.py caminho/do/mapa.tmj --output caminho/do/mapa_convertido.tmj
```

O arquivo original permanece intacto e o resultado é salvo no caminho indicado
por `--output`.

## Alterar o próprio arquivo

```powershell
python tools/convert_legacy_mapobjects.py caminho/do/mapa.tmj --in-place
```

Antes de substituir o mapa, o programa cria uma cópia com a extensão `.bak`.
Por segurança, ele interrompe a operação se esse backup já existir.

Revise os avisos exibidos: partes incompletas ficam inalteradas, sobreposições
recuperadas são informadas e o modelo antigo de iglu com porta à direita não é
convertido porque não possui um MapObject equivalente.
