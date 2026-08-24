# Como usar o conversor de MapObjects antigos

O arquivo `convert_legacy_mapobjects.py` converte cenários antigos de mapas do
Tiled (`.json` ou `.tmj`) em objetos na camada `Paredes`.

Ele reconhece árvores, iglus, pedras e cercas desenhados nas antigas camadas de
tiles. Para cada conjunto reconhecido, o programa:

1. remove os tiles antigos;
2. adiciona o tileset de objeto necessário, caso ainda não exista;
3. cria o objeto equivalente na camada `Paredes`;
4. atualiza o `nextobjectid` do mapa.

O mapa precisa conter a camada de objetos `Paredes`. As camadas antigas de
tiles são opcionais: `tTree`, `bTree`, `tIglu`, `bIglu`, `tStone`, `bStone` e
`fence` podem estar ausentes quando o mapa não usa aquela família de objetos.

## Compatibilidade com camadas faltando

O conversor trata cada família de objetos de forma independente:

- árvores usam `tTree` e `bTree`;
- iglus usam `tIglu` e `bIglu`;
- pedras usam `tStone` e `bStone`;
- cercas usam `fence`.

Se as camadas necessárias para uma família não existirem, essa família será
ignorada sem interromper a conversão das demais. Por exemplo, um mapa sem
iglus pode não possuir `tIglu` e `bIglu`; árvores, pedras e cercas ainda serão
convertidas normalmente.

Nesse caso, o programa exibe um aviso semelhante a:

```text
WARNING: Skipped patterns requiring missing layer(s): bIglu, tIglu
```

Esse aviso é informativo e não indica falha. A camada `Paredes`, porém, continua
obrigatória porque recebe os novos MapObjects.

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
