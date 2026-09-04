import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class CreditsScreen {
    // Edite livremente estes textos para colocar os creditos definitivos.
    private static final String[][] CREDITOS = {
            { "DIRECAO E DESIGN", "Japonets" },
            { "PROGRAMACAO", "Leonardo Lima Silva", "Kaua Victor Menezes Ferraz", "André Arsky", "Alexander Enzo Açano" },
            { "ARTE E ANIMACAO", "Alexander Enzo Açano" },
            { "MUSICA E SOM", "Roubados" },
            { "AGRADECIMENTOS ESPECIAIS", "Codex e Claude",
                    "Corinthians - Paiola - Como fazer pudim de chocolate 2026" }
    };

    private static final int ESCURECER_FRAMES = 100;
    private static final double VELOCIDADE_CREDITOS = 0.85;
    private static final int ESPACO_ENTRE_BLOCOS = 118;
    private static final int TAMANHO_IMAGEM = 112;

    // Coloque a imagem dentro da pasta Pingu-007 e edite estes dois caminhos.
    private static final String CAMINHO_IMAGEM_ESQUERDA = "images/portrait/pingu_portrait_close.jpg";
    private static final String CAMINHO_IMAGEM_DIREITA = "images/portrait/Corinthians_simbolo.png";

    private int timer;
    private Font fonte;
    private BufferedImage imagemEsquerda;
    private BufferedImage imagemDireita;

    public CreditsScreen() {
        try {
            fonte = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
        } catch (Exception e) {
            fonte = new Font("Monospaced", Font.BOLD, 16);
        }
        imagemEsquerda = carregarImagemCreditos(CAMINHO_IMAGEM_ESQUERDA);
        imagemDireita = carregarImagemCreditos(CAMINHO_IMAGEM_DIREITA);
    }

    private BufferedImage carregarImagemCreditos(String caminho) {
        try {
            return LoadSave.GetSpriteAtlas(caminho);
        } catch (RuntimeException e) {
            System.err.println("Imagem dos creditos nao encontrada: " + caminho);
            return GameCore.missing_image;
        }
    }

    public void reset() { timer = 0; }
    public void update() { timer++; }

    public boolean querVoltarAoMenu(InputManager input) {
        return timer > ESCURECER_FRAMES
                && (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)
                || input.isKeyJustPressed(java.awt.event.KeyEvent.VK_ENTER)
                || input.isButtonJustPressed(InputManager.GamepadButton.A));
    }

    public void render(Graphics2D g, int width, int height) {
        float alpha = Math.min(1f, timer / (float) ESCURECER_FRAMES);
        g.setColor(new Color(0f, 0f, 0f, alpha));
        g.fillRect(0, 0, width, height);
        if (timer < ESCURECER_FRAMES) return;

        int tempoRolagem = timer - ESCURECER_FRAMES;
        double cabecaDaFila = -45 + tempoRolagem * VELOCIDADE_CREDITOS;

        desenharBlocoComFade(g, "PINGU 007", null, width, height,
                cabecaDaFila, 28f, new Color(220, 245, 255));

        double yCredito = cabecaDaFila - ESPACO_ENTRE_BLOCOS;
        for (int i = 0; i < CREDITOS.length; i++) {
            String[] credito = CREDITOS[i];
            if (i > 0) {
                // Usa a altura do bloco que vai entrar, inclusive seus nomes extras.
                yCredito -= calcularEspacoDoBloco(credito);
            }
            desenharBlocoComFade(g, credito, width, height, yCredito,
                    11f, new Color(120, 190, 220));
        }

        yCredito -= ESPACO_ENTRE_BLOCOS;
        desenharBlocoComFade(g, "OBRIGADO POR JOGAR!", null, width, height,
                yCredito, 14f, Color.WHITE);
    }

    private int calcularEspacoDoBloco(String[] credito) {
        int quantidadeNomes = Math.max(1, credito.length - 1);
        return ESPACO_ENTRE_BLOCOS + Math.max(0, quantidadeNomes - 1) * 30;
    }

    private void desenharBlocoComFade(Graphics2D g, String[] credito,
            int width, int height, double y, float tamanhoTitulo, Color corTitulo) {
        desenharBlocoComFade(g, credito[0], credito, 1,
                width, height, y, tamanhoTitulo, corTitulo);
    }

    private void desenharBlocoComFade(Graphics2D g, String titulo, String[] nomes,
            int width, int height, double y, float tamanhoTitulo, Color corTitulo) {
        desenharBlocoComFade(g, titulo, nomes, 0,
                width, height, y, tamanhoTitulo, corTitulo);
    }

    private void desenharBlocoComFade(Graphics2D g, String titulo, String[] nomes, int primeiroNome,
            int width, int height, double y, float tamanhoTitulo, Color corTitulo) {
        int quantidadeNomes = nomes == null ? 0 : nomes.length - primeiroNome;
        double alturaBloco = quantidadeNomes * 30.0;
        if (y + alturaBloco < -60 || y > height + 60) return;

        double margemFade = Math.max(55.0, height * 0.12);
        double alphaEntrada = Math.min(1.0, (y + 60.0) / margemFade);
        double alphaSaida = Math.min(1.0, (height + 60.0 - y) / margemFade);
        float alpha = (float) Math.max(0.0, Math.min(alphaEntrada, alphaSaida));
        if (alpha <= 0f) return;

        Composite anterior = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        desenharCentralizado(g, titulo, width, (int) y, tamanhoTitulo, corTitulo);
        for (int i = 0; i < quantidadeNomes; i++) {
            desenharCentralizado(g, nomes[primeiroNome + i], width, (int) y + 30 + i * 30, 14f, Color.WHITE);
        }
        if ("PROGRAMACAO".equals(titulo)) {
            desenharImagensLaterais(g, width, y, alturaBloco);
        }
        g.setComposite(anterior);
    }

    private void desenharImagensLaterais(Graphics2D g, int width, double y, double alturaBloco) {
        int tamanho = Math.min(TAMANHO_IMAGEM, Math.max(72, width / 10));
        int centroX = width / 2;
        int meiaLarguraTextos = Math.min(255, (int) (width * 0.28));
        int espaco = 24;
        int xEsquerda = Math.max(18, centroX - meiaLarguraTextos - espaco - tamanho);
        int xDireita = Math.min(width - tamanho - 18, centroX + meiaLarguraTextos + espaco);
        int centroBlocoY = (int) Math.round(y + alturaBloco / 2.0);
        int imagemY = centroBlocoY - tamanho / 2;

        desenharImagemQuadrada(g, imagemEsquerda, xEsquerda, imagemY, tamanho);
        desenharImagemQuadrada(g, imagemDireita, xDireita, imagemY, tamanho);
    }

    private void desenharImagemQuadrada(Graphics2D g, BufferedImage imagem, int x, int y, int tamanho) {
        if (imagem == null) return;

        int ladoFonte = Math.min(imagem.getWidth(), imagem.getHeight());
        int fonteX = (imagem.getWidth() - ladoFonte) / 2;
        int fonteY = (imagem.getHeight() - ladoFonte) / 2;

        g.setColor(new Color(120, 190, 220));
        g.fillRect(x - 3, y - 3, tamanho + 6, tamanho + 6);
        g.drawImage(imagem,
                x, y, x + tamanho, y + tamanho,
                fonteX, fonteY, fonteX + ladoFonte, fonteY + ladoFonte,
                null);
    }

    private void desenharCentralizado(Graphics2D g, String texto, int width, int y, float tamanho, Color cor) {
        g.setFont(fonte.deriveFont(Font.PLAIN, tamanho));
        g.setColor(cor);
        g.drawString(texto, (width - g.getFontMetrics().stringWidth(texto)) / 2, y);
    }
}
