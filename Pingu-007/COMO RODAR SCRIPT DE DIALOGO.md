# Como rodar o script de diálogo

O arquivo `dialogue_sequence_gui.py` monta um WAV a partir de uma sequência de
sons de diálogo. Ele deve permanecer na pasta `Pingu-007`, ao lado da pasta
`sound`, pois os caminhos dos áudios são relativos a esse local.

## Requisitos

- Windows com Python 3.10 ou mais recente.
- FFmpeg disponível no `PATH` para usar pitch diferente de zero.
- O FFmpeg precisa ser uma versão completa com o filtro `rubberband`.

Não é necessário instalar pacotes com `pip`.

Para conferir os requisitos, abra o PowerShell e execute:

```powershell
python --version
ffmpeg -version
ffmpeg -filters | findstr rubberband
```

O último comando deve mostrar uma linha contendo `rubberband`. Caso `ffmpeg`
não seja reconhecido, instale uma versão completa do FFmpeg e adicione a pasta
que contém `ffmpeg.exe` ao `PATH` do Windows.

## Abrindo o programa

Abra o PowerShell na pasta `Pingu-007` e execute:

```powershell
python dialogue_sequence_gui.py
```

Se a instalação do Python não tiver o Tkinter completo, o programa abre
automaticamente usando a interface nativa do Windows.

## Formato da sequência

Cada entrada deve ser separada por vírgula. A sequência pode ficar toda na
mesma linha ou ocupar várias linhas.

Formas aceitas:

```text
a, ME, yo, null, SoundManager.SFX.KATAKANA_GU
```

- `a`, `A`, `me` e `ME` são atalhos sem diferença entre maiúsculas e minúsculas.
- O nome completo `SoundManager.SFX.KATAKANA_*` também é aceito.
- `null` cria um intervalo sem iniciar outro som.
- Comentários Java com `//` e `/* ... */` são ignorados.
- Uma vírgula continua obrigatória entre duas entradas, mesmo quando há um
  comentário entre elas.

Exemplo com comentários:

```java
null,
// Ei
e, i,

// Pingu
pi, n, gu,

null, null, null,
// vejo
be, jo
```

## Controles de áudio

- **Start interval (ms):** distância entre o início de cada som. O jogo usa
  `100 ms` atualmente.
- **Pitch (semitones):** aceita valores entre `-12` e `+12`, incluindo decimais.
  O pitch muda sem alterar a velocidade ou a duração final da sequência.
- **Radio effect + hiss:** deixa o áudio mono e bem limitado à faixa de voz,
  reduz bastante os graves, destaca os médios e adiciona um chiado audível.

Com pitch igual a `0`, o FFmpeg não é necessário.

## Exportando o WAV

1. Cole ou digite a sequência.
2. Ajuste intervalo, pitch e efeito de rádio.
3. Clique em **Export WAV...**.
4. Escolha o nome e a pasta no diálogo de salvamento.

O resultado é salvo como WAV PCM estéreo, 16 bits e 48 kHz.

## Problemas comuns

### Erro dizendo que o FFmpeg não foi encontrado

Confirme que `ffmpeg -version` funciona no mesmo PowerShell usado para abrir o
programa. Também é possível exportar temporariamente com pitch `0`.

### Erro sobre o filtro rubberband

A instalação do FFmpeg é uma versão reduzida. Substitua-a por uma versão
completa compilada com suporte a `librubberband`.

### Arquivo de som não encontrado

Confirme que `dialogue_sequence_gui.py` continua dentro de `Pingu-007` e que a
pasta `Pingu-007/sound` não foi movida.
