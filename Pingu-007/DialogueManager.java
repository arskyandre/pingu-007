import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DialogueManager {
  private String[] falas;
  private int falaAtualIndex = 0;
  private String textoExibido = "";
  private int caractereIndex = 0;
  private boolean ativo = false;
  private Font pixelFont;

  private long ultimoFrameTempo = 0;
  private final int delayLetrasMs = 40;

  private int shakeX = 0;
  private int shakeY = 0;
  private final Random random = new Random();

  private boolean teclaLiberada = true;

  private boolean tocarSomDeEscrita = true;
  private BufferedImage rostoFechado;
  private BufferedImage rostoAberto;
  private BufferedImage[] retratosAtual;
  private BufferedImage rostoTemporario;

  private boolean mostrarBocaAberta = false;
  private SoundManager soundManager;
  private SoundManager.SFX[][] sonsAtual;

  public DialogueManager(SoundManager s) {
    soundManager = s;
    try {
      Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
      pixelFont = base.deriveFont(Font.PLAIN, 14f);
    } catch (Exception e) {
      System.err.println("nao encontrou font/PressStart2P-Regular.ttf.");
      pixelFont = new Font("Courier New", Font.BOLD, 18);
    }
    rostoFechado = GameCore.pingu_portrait;
    rostoAberto = GameCore.pingu_portrait;
  }

  public void iniciarDialogo(String[] texto, boolean tocarSomDeEscrita) {
    this.falas = texto;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = null;
    this.retratosAtual = null;
    this.rostoTemporario = null;
    soundManager.stopDialogue();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, BufferedImage[] imgs, boolean tocarSomDeEscrita) {
    this.falas = texto;
    retratosAtual = imgs;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = null;
    soundManager.stopDialogue();
    aplicarRetratoFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, SoundManager.SFX[][] sons, boolean tocarSomDeEscrita) {
    this.falas = texto;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = sons;
    this.retratosAtual = null;
    this.rostoTemporario = null;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, SoundManager.SFX[][] sons, BufferedImage[] imgs,
      boolean tocarSomDeEscrita) {
    this.falas = texto;
    retratosAtual = imgs;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = sons;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
    aplicarRetratoFalaAtual();
  }

  private void tocarSomFalaAtual() {
    if (sonsAtual != null && falaAtualIndex < sonsAtual.length) {
      soundManager.playDialogue(sonsAtual[falaAtualIndex]);
    }
  }

  private void aplicarRetratoFalaAtual() {
    if (retratosAtual != null && falaAtualIndex < retratosAtual.length && retratosAtual[falaAtualIndex] != null) {
      rostoTemporario = retratosAtual[falaAtualIndex];
    }
  }

  /**
   * Se a fala atual comeca com um prefixo tipo "NOME: ", esse prefixo
   * (nome + ":") e escrito instantaneamente, pulando a animacao de
   * digitacao apenas para essa parte.
   */
  private void aplicarPrefixoInstantaneo() {
    int prefixLen = tamanhoPrefixoNome(falas[falaAtualIndex]);
    if (prefixLen > 0) {
      textoExibido = falas[falaAtualIndex].substring(0, prefixLen);
      caractereIndex = prefixLen;
    }
  }

  /**
   * Detecta a primeira ocorrencia de ":" na string. Se tudo antes dela for
   * maiusculo (ignorando espacos), retorna o indice logo apos o ":" (e o
   * espaco seguinte, se houver) — ou seja, o tamanho do prefixo "NOME: ".
   * Retorna 0 se nao houver prefixo valido.
   */
  private int tamanhoPrefixoNome(String texto) {
    int colonIndex = texto.indexOf(':');
    if (colonIndex <= 0) {
      return 0;
    }

    String possivelNome = texto.substring(0, colonIndex);
    for (int i = 0; i < possivelNome.length(); i++) {
      char c = possivelNome.charAt(i);
      if (Character.isLetter(c) && !Character.isUpperCase(c)) {
        return 0;
      }
    }

    int fim = colonIndex + 1;
    if (fim < texto.length() && texto.charAt(fim) == ' ') {
      fim++;
    }
    return fim;
  }

  public void atualizar(InputManager input) {
    if (!ativo)
      return;

    long agora = System.currentTimeMillis();

    if (caractereIndex < falas[falaAtualIndex].length()) {
      if (agora - ultimoFrameTempo >= delayLetrasMs) {
        textoExibido += falas[falaAtualIndex].charAt(caractereIndex);
        if (falas[falaAtualIndex].charAt(caractereIndex) != ' '
            && falas[falaAtualIndex].charAt(caractereIndex) != '-') {
          soundManager.playRandomDialogueSound();
        }
        caractereIndex++;
        ultimoFrameTempo = agora;

        if (caractereIndex % 2 == 0) {
          mostrarBocaAberta = !mostrarBocaAberta;
        }

        shakeX = random.nextInt(5) - 2;
        shakeY = random.nextInt(5) - 2;
      }

    } else {

      shakeX = 0;
      shakeY = 0;
      mostrarBocaAberta = false;
    }
    boolean teclaApertada = input.isKeyPressed(KeyEvent.VK_SPACE) || input.isKeyPressed(KeyEvent.VK_ENTER);

    if (teclaApertada && teclaLiberada) {
      teclaLiberada = false;
      avancarFala();
    }

    if (!teclaApertada) {
      teclaLiberada = true;
    }
  }

  private void avancarFala() {
    if (caractereIndex < falas[falaAtualIndex].length()) {
      textoExibido = falas[falaAtualIndex];
      caractereIndex = falas[falaAtualIndex].length();
      soundManager.stopDialogue();
    } else {
      falaAtualIndex++;
      if (falaAtualIndex < falas.length) {
        textoExibido = "";
        caractereIndex = 0;
        ultimoFrameTempo = System.currentTimeMillis();
        tocarSomFalaAtual();
        aplicarRetratoFalaAtual();
        aplicarPrefixoInstantaneo();
      } else {
        ativo = false;
        soundManager.stopDialogue();
        onDialogoTerminado();
      }
    }
  }

  private List<String> LinhasdeTexto(FontMetrics fm, String text, int maxWidth) {
    String[] words = text.split(" ");
    List<String> lines = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String word : words) {
      String test = current.isEmpty() ? word : current + " " + word;
      if (fm.stringWidth(test) <= maxWidth) {
        current = new StringBuilder(test);
      } else {
        if (!current.isEmpty())
          lines.add(current.toString());
        current = new StringBuilder(word);
      }
    }
    if (!current.isEmpty())
      lines.add(current.toString());
    return lines;
  }

  public void renderizar(Graphics2D g2, int telaLargura, int telaAltura) {
    if (!ativo)
      return;

    int x = 50 + shakeX;
    int y = telaAltura - 170 + shakeY;
    int largura = telaLargura - 100;
    int altura = 110;

    g2.setColor(Color.BLACK);
    g2.fillRect(x, y, largura, altura);

    g2.setStroke(new BasicStroke(3));
    g2.setColor(Color.MAGENTA);
    g2.drawRect(x, y, largura, altura);

    g2.setStroke(new BasicStroke(1));
    g2.setColor(Color.CYAN);
    g2.drawRect(x + 4, y + 4, largura - 8, altura - 8);

    int fotoX = x + 15;
    int fotoY = y + 15;
    int fotoTamanho = 80;
    if (rostoTemporario != null) {
      g2.drawImage(rostoTemporario, fotoX, fotoY, fotoTamanho, fotoTamanho, null);
    } else if (rostoFechado != null && rostoAberto != null) {
      BufferedImage frameAtual = mostrarBocaAberta ? rostoAberto : rostoFechado;
      g2.drawImage(frameAtual, fotoX, fotoY, fotoTamanho, fotoTamanho, null);
    } else {
      g2.setColor(Color.DARK_GRAY);
      g2.fillRect(fotoX, fotoY, fotoTamanho, fotoTamanho);
      g2.setColor(Color.GREEN);
      g2.drawRect(fotoX, fotoY, fotoTamanho, fotoTamanho);
      g2.setFont(new Font("Arial", Font.PLAIN, 10));
      g2.drawString("SEM FOTO", fotoX + 15, fotoY + 45);
    }

    g2.setColor(Color.WHITE);
    g2.setFont(pixelFont);

    int textX = x + 110;
    int maxTextWidth = largura - 130;

    FontMetrics fm = g2.getFontMetrics();
    List<String> lines = LinhasdeTexto(fm, textoExibido, maxTextWidth);

    int lineHeight = fm.getAscent() + fm.getDescent() + 4;
    int totalTextHeight = lines.size() * lineHeight;
    int startY = y + (altura - totalTextHeight) / 2 + fm.getAscent();

    for (int i = 0; i < lines.size(); i++) {
      g2.drawString(lines.get(i), textX, startY + i * lineHeight);
    }
  }

  private void onDialogoTerminado() {
    rostoTemporario = null;
    retratosAtual = null;
  }

  public boolean isAtivo() {
    return ativo;
  }

}