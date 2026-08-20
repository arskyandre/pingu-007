import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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

  private BufferedImage rostoFechado;
  private BufferedImage rostoAberto;
  private BufferedImage[] retratosAtual;
  private BufferedImage rostoTemporario;

  private boolean mostrarBocaAberta = false;
  private SoundManager soundManager;
  private SoundManager.SFX[] sonsAtual;

  public interface EscolhaListener {
    void onEscolha(int indiceEscolhido);
  }

  // isEscolha: a fala atual é uma pergunta de escolha (ainda digitando)
  // modoEscolha: a digitação terminou e as opções estão ativas/interativas
  private boolean isEscolha = false;
  private boolean modoEscolha = false;
  private boolean avancoBloqueado = false;
  private boolean encerrarQuandoTerminarDigitacao = false;
  private String[] opcoesEscolha;
  private int escolhaSelecionada = 0;
  private EscolhaListener escolhaListener;
  private Runnable aoTerminarDialogo; // callback opcional, roda quando o diálogo normal termina
  private long modoEscolhaAtivadoEm = 0;
  private static final long DELAY_INPUT_ESCOLHA_MS = 400;

  private final ConcurrentLinkedQueue<Runnable> acoesPendentes = new ConcurrentLinkedQueue<>();
  private JWindow janelaEntradaChat;
  private JTextField campoEntradaChat;
  private Canvas canvasChat;
  private MenuButton terminarConversaButton;
  private volatile boolean entradaChatVisivel = false;
  private volatile Consumer<String> aoEnviarMensagemChat;
  private volatile Runnable aoTerminarConversaChat;
  private int chatInputX;
  private int chatInputY;
  private int chatInputWidth;
  private static final int CHAT_INPUT_HEIGHT = 30;
  private static final int CHAT_BUTTON_WIDTH = 170;
  private static final int CHAT_BUTTON_HEIGHT = 32;

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

  /** Creates the lightweight Swing text input after GameCore has created its JFrame. */
  public void configurarEntradaChat(JFrame frame, Canvas canvas) {
    canvasChat = canvas;
    terminarConversaButton = new MenuButton("Terminar Conversa", 0, 0,
        CHAT_BUTTON_WIDTH, CHAT_BUTTON_HEIGHT, true);

    Runnable criarJanela = () -> {
      janelaEntradaChat = new JWindow(frame);
      campoEntradaChat = new JTextField();
      campoEntradaChat.setFont(new Font("SansSerif", Font.PLAIN, 16));
      campoEntradaChat.addActionListener(event -> {
        String texto = campoEntradaChat.getText().trim();
        if (texto.isEmpty() || !entradaChatVisivel) {
          System.out.println("[CHAT] Ignored empty or inactive input.");
          return;
        }
        System.out.println("[CHAT] Enter pressed: " + texto);
        campoEntradaChat.setText("");
        esconderEntradaChat();
        Consumer<String> listener = aoEnviarMensagemChat;
        if (listener != null) {
          System.out.println("[CHAT] Queueing submitted message for game loop.");
          listener.accept(texto);
        } else {
          System.err.println("[CHAT] No message listener is registered.");
        }
      });
      janelaEntradaChat.add(campoEntradaChat);
      janelaEntradaChat.setSize(500, CHAT_INPUT_HEIGHT);
      janelaEntradaChat.setAlwaysOnTop(true);
    };

    try {
      if (SwingUtilities.isEventDispatchThread()) {
        criarJanela.run();
      } else {
        SwingUtilities.invokeAndWait(criarJanela);
      }
    } catch (Exception e) {
      throw new RuntimeException("Nao foi possivel criar a entrada de conversa.", e);
    }
  }

  public void iniciarEntradaChat(Consumer<String> aoEnviar, Runnable aoTerminar) {
    System.out.println("[CHAT] Showing text input.");
    aoEnviarMensagemChat = aoEnviar;
    aoTerminarConversaChat = aoTerminar;
    entradaChatVisivel = true;
    posicionarEntradaChat();
    SwingUtilities.invokeLater(() -> {
      if (janelaEntradaChat == null || campoEntradaChat == null) {
        return;
      }
      campoEntradaChat.setText("");
      janelaEntradaChat.setVisible(true);
      campoEntradaChat.requestFocusInWindow();
    });
  }

  public void esconderEntradaChat() {
    System.out.println("[CHAT] Hiding text input.");
    entradaChatVisivel = false;
    SwingUtilities.invokeLater(() -> {
      if (janelaEntradaChat != null) {
        janelaEntradaChat.setVisible(false);
      }
    });
  }

  public boolean isEntradaChatVisivel() {
    return entradaChatVisivel;
  }

  public boolean possuiAcoesPendentes() {
    return !acoesPendentes.isEmpty();
  }

  /** Queues work from Swing/HTTP threads so dialogue state changes happen in the game loop. */
  public void postarAcaoNoGameLoop(Runnable acao) {
    if (acao != null) {
      System.out.println("[CHAT] Action added to game-loop queue.");
      acoesPendentes.add(acao);
    }
  }

  private void executarAcoesPendentes() {
    Runnable acao;
    while ((acao = acoesPendentes.poll()) != null) {
      System.out.println("[CHAT] Executing queued game-loop action.");
      acao.run();
    }
  }

  private void posicionarEntradaChat() {
    if (canvasChat == null || terminarConversaButton == null) {
      return;
    }

    int canvasWidth = canvasChat.getWidth();
    int canvasHeight = canvasChat.getHeight();
    chatInputX = 70;
    chatInputY = Math.max(10, canvasHeight - 54);
    chatInputWidth = Math.max(260, canvasWidth - 300);
    terminarConversaButton.setPosition(
        chatInputX + chatInputWidth - CHAT_BUTTON_WIDTH,
        Math.max(4, chatInputY - CHAT_BUTTON_HEIGHT - 6));

    if (janelaEntradaChat != null && canvasChat.isShowing()) {
      try {
        Point screenLocation = canvasChat.getLocationOnScreen();
        int width = Math.max(260, chatInputWidth);
        SwingUtilities.invokeLater(() -> {
          janelaEntradaChat.setBounds(screenLocation.x + chatInputX,
              screenLocation.y + chatInputY, width, CHAT_INPUT_HEIGHT);
        });
      } catch (IllegalComponentStateException ignored) {
        // The window is not showing yet; the next game frame will reposition it.
      }
    }
  }

  public void iniciarDialogo(String[] texto) {
    this.falas = texto;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.avancoBloqueado = false;
    this.encerrarQuandoTerminarDigitacao = false;
    this.isEscolha = false;
    this.modoEscolha = false;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = null;
    this.retratosAtual = null;
    this.rostoTemporario = null;
    soundManager.stopDialogue();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, BufferedImage[] imgs) {
    this.falas = texto;
    retratosAtual = imgs;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.avancoBloqueado = false;
    this.encerrarQuandoTerminarDigitacao = false;
    this.isEscolha = false;
    this.modoEscolha = false;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = null;
    soundManager.stopDialogue();
    aplicarRetratoFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, SoundManager.SFX[] sons) {
    this.falas = texto;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.avancoBloqueado = false;
    this.encerrarQuandoTerminarDigitacao = false;
    this.isEscolha = false;
    this.modoEscolha = false;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = sons;
    this.retratosAtual = null;
    this.rostoTemporario = null;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarDialogo(String[] texto, SoundManager.SFX[] sons, BufferedImage[] imgs) {
    this.falas = texto;
    retratosAtual = imgs;
    this.falaAtualIndex = 0;
    this.caractereIndex = 0;
    this.textoExibido = "";
    this.ativo = true;
    this.avancoBloqueado = false;
    this.encerrarQuandoTerminarDigitacao = false;
    this.isEscolha = false;
    this.modoEscolha = false;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.sonsAtual = sons;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
    aplicarRetratoFalaAtual();
  }

  private void tocarSomFalaAtual() {
    SoundManager.SFX som = null;
    if (sonsAtual != null && falaAtualIndex < sonsAtual.length) {
      som = sonsAtual[falaAtualIndex];
    }
    soundManager.playDialogue(som);
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

  private void atualizarEscolha(InputManager input) {
    if (System.currentTimeMillis() - modoEscolhaAtivadoEm < DELAY_INPUT_ESCOLHA_MS) {
      return;
    }

    if (input.isKeyJustPressed(KeyEvent.VK_UP) || input.isKeyJustPressed(KeyEvent.VK_W)
        || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_UP)) {
      if (escolhaSelecionada > 0)
        escolhaSelecionada--;
    }
    if (input.isKeyJustPressed(KeyEvent.VK_DOWN) || input.isKeyJustPressed(KeyEvent.VK_S)
        || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_DOWN)) {
      if (escolhaSelecionada < opcoesEscolha.length - 1)
        escolhaSelecionada++;
    }

    boolean confirmar = input.isKeyJustPressed(KeyEvent.VK_SPACE) || input.isKeyJustPressed(KeyEvent.VK_ENTER)
        || input.isButtonJustPressed(InputManager.GamepadButton.A);
    if (confirmar) {
      soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
      int escolhido = escolhaSelecionada;
      EscolhaListener listener = escolhaListener;

      modoEscolha = false;
      isEscolha = false;
      ativo = false;
      opcoesEscolha = null;
      escolhaListener = null;
      soundManager.stopDialogue();
      onDialogoTerminado();

      if (listener != null) {
        listener.onEscolha(escolhido);
      }
    }
  }

  public void atualizar(InputManager input) {
    executarAcoesPendentes();

    if (entradaChatVisivel) {
      posicionarEntradaChat();
      if (terminarConversaButton.update(input) == MenuButton.CLICKED) {
        System.out.println("[CHAT] Terminar Conversa clicked.");
        entradaChatVisivel = false;
        esconderEntradaChat();
        Runnable listener = aoTerminarConversaChat;
        aoTerminarConversaChat = null;
        aoEnviarMensagemChat = null;
        if (listener != null) {
          listener.run();
        }
      }
    }

    if (!ativo)
      return;

    if (modoEscolha) {
      atualizarEscolha(input);
      return;
    }

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

      if (encerrarQuandoTerminarDigitacao && !isEscolha) {
        ativo = false;
        avancoBloqueado = false;
        encerrarQuandoTerminarDigitacao = false;
        soundManager.stopDialogue();
        onDialogoTerminado();
        return;
      }

      // texto da pergunta terminou de digitar -> ativa as opções automaticamente
      if (isEscolha) {
        modoEscolha = true;
        modoEscolhaAtivadoEm = System.currentTimeMillis();
        return;
      }

      if (avancoBloqueado) {
        return;
      }
    }

    boolean teclaApertada = input.isKeyJustPressed(KeyEvent.VK_SPACE) || input.isKeyJustPressed(KeyEvent.VK_ENTER)
        || input.isButtonJustPressed(InputManager.GamepadButton.A);

    if (teclaApertada) {
      avancarFala();
    }
  }

  private void avancarFala() {
    if (caractereIndex < falas[falaAtualIndex].length()) {
      textoExibido = falas[falaAtualIndex];
      caractereIndex = falas[falaAtualIndex].length();
      soundManager.stopDialogue();
    } else if (!isEscolha) {
      // diálogos de escolha nunca avançam falaAtualIndex — a ativação do
      // modoEscolha acontece automaticamente em atualizar() assim que o
      // texto terminar de digitar
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
    if (!ativo) {
      if (entradaChatVisivel) {
        terminarConversaButton.draw(g2);
      }
      return;
    }

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

    if (modoEscolha) {
      desenharCaixaEscolha(g2, telaLargura, telaAltura, x, y, largura);
    }

    if (entradaChatVisivel) {
      terminarConversaButton.draw(g2);
    }
  }

  private void desenharCaixaEscolha(Graphics2D g2, int telaLargura, int telaAltura,
      int dialogoX, int dialogoY, int dialogoLargura) {
    g2.setFont(pixelFont);
    FontMetrics fmOpt = g2.getFontMetrics();

    int lineHeight = fmOpt.getAscent() + fmOpt.getDescent() + 8;
    int paddingX = 16;
    int paddingY = 10;

    int maxOptWidth = 0;
    for (String opcao : opcoesEscolha) {
      int w = fmOpt.stringWidth("> " + opcao);
      if (w > maxOptWidth) {
        maxOptWidth = w;
      }
    }

    int boxWidth = maxOptWidth + paddingX * 2;
    int boxHeight = opcoesEscolha.length * lineHeight + paddingY * 2;

    int boxX = dialogoX + dialogoLargura - boxWidth;
    int boxY = dialogoY - boxHeight - 16;

    g2.setColor(new Color(20, 20, 20, 220));
    g2.fill(new Rectangle2D.Double(boxX, boxY, boxWidth, boxHeight));

    g2.setColor(new Color(255, 255, 255, 180));
    g2.setStroke(new BasicStroke(2));
    g2.draw(new Rectangle2D.Double(boxX, boxY, boxWidth, boxHeight));

    int textX = boxX + paddingX;
    int textY = boxY + paddingY + fmOpt.getAscent();

    for (int i = 0; i < opcoesEscolha.length; i++) {
      String texto = (i == escolhaSelecionada ? "> " : "  ") + opcoesEscolha[i];
      g2.setColor(Color.BLACK);
      g2.drawString(texto, textX + 1, textY + i * lineHeight + 1);
      g2.setColor(i == escolhaSelecionada ? Color.YELLOW : Color.WHITE);
      g2.drawString(texto, textX, textY + i * lineHeight);
    }
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, EscolhaListener listener) {
    iniciarEscolha(pergunta, opcoes, 0, listener);
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, int escolhaInicial, EscolhaListener listener) {
    this.falas = new String[] { pergunta };
    this.sonsAtual = null;
    this.retratosAtual = null;
    this.falaAtualIndex = 0;
    this.textoExibido = "";
    this.caractereIndex = 0;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.rostoTemporario = null;
    this.opcoesEscolha = opcoes;
    this.escolhaSelecionada = Math.max(0, Math.min(escolhaInicial, opcoes.length - 1));
    this.escolhaListener = listener;
    this.isEscolha = true;
    this.modoEscolha = false;
    this.ativo = true;
    soundManager.stopDialogue();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, BufferedImage retrato, EscolhaListener listener) {
    iniciarEscolha(pergunta, opcoes, retrato, 0, listener);
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, BufferedImage retrato, int escolhaInicial,
      EscolhaListener listener) {
    this.falas = new String[] { pergunta };
    this.sonsAtual = null;
    this.retratosAtual = null;
    this.falaAtualIndex = 0;
    this.textoExibido = "";
    this.caractereIndex = 0;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.rostoTemporario = retrato;
    this.opcoesEscolha = opcoes;
    this.escolhaSelecionada = Math.max(0, Math.min(escolhaInicial, opcoes.length - 1));
    this.escolhaListener = listener;
    this.isEscolha = true;
    this.modoEscolha = false;
    this.ativo = true;
    soundManager.stopDialogue();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, SoundManager.SFX[] sons, EscolhaListener listener) {
    iniciarEscolha(pergunta, opcoes, sons, 0, listener);
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, SoundManager.SFX[] sons, int escolhaInicial,
      EscolhaListener listener) {
    this.falas = new String[] { pergunta };
    this.sonsAtual = sons;
    this.retratosAtual = null;
    this.falaAtualIndex = 0;
    this.textoExibido = "";
    this.caractereIndex = 0;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.rostoTemporario = null;
    this.opcoesEscolha = opcoes;
    this.escolhaSelecionada = Math.max(0, Math.min(escolhaInicial, opcoes.length - 1));
    this.escolhaListener = listener;
    this.isEscolha = true;
    this.modoEscolha = false;
    this.ativo = true;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, SoundManager.SFX[] sons, BufferedImage retrato,
      EscolhaListener listener) {
    iniciarEscolha(pergunta, opcoes, sons, retrato, 0, listener);
  }

  public void iniciarEscolha(String pergunta, String[] opcoes, SoundManager.SFX[] sons, BufferedImage retrato,
      int escolhaInicial, EscolhaListener listener) {
    this.falas = new String[] { pergunta };
    this.sonsAtual = sons;
    this.retratosAtual = null;
    this.falaAtualIndex = 0;
    this.textoExibido = "";
    this.caractereIndex = 0;
    this.ultimoFrameTempo = System.currentTimeMillis();
    this.rostoTemporario = retrato;
    this.opcoesEscolha = opcoes;
    this.escolhaSelecionada = Math.max(0, Math.min(escolhaInicial, opcoes.length - 1));
    this.escolhaListener = listener;
    this.isEscolha = true;
    this.modoEscolha = false;
    this.ativo = true;
    tocarSomFalaAtual();
    aplicarPrefixoInstantaneo();
  }

  private void onDialogoTerminado() {
    rostoTemporario = null;
    retratosAtual = null;
    isEscolha = false;
    if (aoTerminarDialogo != null) {
      Runnable callback = aoTerminarDialogo;
      aoTerminarDialogo = null;
      callback.run();
    }
  }

  public void setAoTerminarDialogo(Runnable callback) {
    this.aoTerminarDialogo = callback;
  }

  /** Starts a dialogue that cannot be skipped by keyboard or gamepad input. */
  public void iniciarDialogoBloqueado(String[] texto, BufferedImage[] imgs) {
    iniciarDialogo(texto, imgs);
    avancoBloqueado = true;
  }

  /** Allows a blocked dialogue to close automatically after its text finishes typing. */
  public void encerrarDialogoBloqueadoAoTerminarDigitacao() {
    if (!avancoBloqueado && !ativo) {
      return;
    }
    encerrarQuandoTerminarDigitacao = true;
  }

  public boolean isAtivo() {
    return ativo;
  }

}
