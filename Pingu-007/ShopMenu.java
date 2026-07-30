import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ShopMenu {

    private final List<ShopItem> itens = new ArrayList<>();
    private final List<ShopItemButton> botoes = new ArrayList<>();
    private final SoundManager soundManager;
    private final IconButton setaQuantidadeEsquerda;
    private final IconButton setaQuantidadeDireita;
    private BufferedImage coinIcon;
    private Runnable aoFechar = null;

    private Player player;
    private int selecionado = 0;
    private int quantidadeSelecionada = 1;
    private boolean aberto = false;

    private String mensagemFeedback = null;
    private int feedbackTimer = 0;
    private static final int FEEDBACK_DURATION = 90;

    private static final int BUTTON_WIDTH = 360;
    private static final int BUTTON_HEIGHT = 64;
    private static final int BUTTON_GAP = 10;
    private static final int LIST_MARGIN_LEFT = 70;
    private static final int TOP_MARGIN = 70;
    private static final int HEADER_GAP = 26;
    private static final int PANEL_GAP = 60;
    private static final int QUANTITY_BUTTON_SIZE = 36;
    private static final int QUANTITY_NUMBER_HALF_GAP = 34;
    private static final int QUANTITY_TOP_GAP = 8;

    public ShopMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        setaQuantidadeEsquerda = new IconButton(0, 0, QUANTITY_BUTTON_SIZE, IconIndex.LEFT_ARROW, false);
        setaQuantidadeDireita = new IconButton(0, 0, QUANTITY_BUTTON_SIZE, IconIndex.RIGHT_ARROW, false);
        try {
            coinIcon = LoadSave.GetSpriteAtlas("images/hud/moedasprite.png").getSubimage(16, 0, 16, 16);
        } catch (Exception e) {
            System.err.println("ShopMenu: erro ao carregar moedasprite.png: " + e.getMessage());
        }
    }

    public void addItem(String nome, String descricao, BufferedImage icone, int preco,
            Runnable aoComprar, boolean disponivel, boolean compra_unica) {
        ShopItem item = new ShopItem(nome, descricao, icone, preco, aoComprar, disponivel, compra_unica);
        itens.add(item);
        botoes.add(new ShopItemButton(item, coinIcon, 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT));
    }

    public void limparItens() {
        itens.clear();
        botoes.clear();
        selecionado = 0;
        quantidadeSelecionada = 1;
    }

    public boolean isAberto() {
        return aberto;
    }

    public void setAoFechar(Runnable callback) {
        this.aoFechar = callback;
    }

    public void abrir(Player player) {
        this.player = player;
        this.selecionado = 0;
        this.quantidadeSelecionada = 1;
        this.mensagemFeedback = null;
        this.feedbackTimer = 0;
        this.aberto = true;
        GameCore.setGameState(GameState.SHOP);
    }

    public void fechar() {
        aberto = false;
        GameCore.setGameState(GameState.PLAYING);
        if (aoFechar != null) {
            Runnable callback = aoFechar;
            aoFechar = null;
            callback.run();
        }
    }

    private void repositionButtons() {
        int y = TOP_MARGIN + HEADER_GAP;
        for (int i = 0; i < botoes.size(); i++) {
            ShopItemButton botao = botoes.get(i);
            botao.setPosition(LIST_MARGIN_LEFT, y);
            y += botao.getRect().height + BUTTON_GAP;
        }
    }

    public void update(InputManager input, int telaLargura, int telaAltura) {
        if (!aberto) {
            return;
        }

        repositionButtons();

        if (feedbackTimer > 0) {
            feedbackTimer--;
        }

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            fechar();
            return;
        }

        if (botoes.isEmpty()) {
            return;
        }

        for (int i = 0; i < botoes.size(); i++) {
            int resultado = botoes.get(i).update(input);
            if (botoes.get(i).isHovered()) {
                if (selecionado != i) {
                    quantidadeSelecionada = 1;
                }
                selecionado = i;
            }
            if (resultado == MenuButton.CLICKED) {
                comprarItem(i);
            }
        }

        ShopItem itemSelecionado = itens.get(selecionado);
        ajustarQuantidadeAoLimite(itemSelecionado);
        if (!itemSelecionado.compra_unica) {
            repositionQuantityButtons(telaLargura, itemSelecionado);

            if (setaQuantidadeEsquerda.update(input) == MenuButton.CLICKED) {
                alterarQuantidade(-1, itemSelecionado);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
            if (setaQuantidadeDireita.update(input) == MenuButton.CLICKED) {
                alterarQuantidade(1, itemSelecionado);
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            }
        }
    }

    private void comprarItem(int index) {
        ShopItem item = itens.get(index);
        if (!item.disponivel) {
            mensagemFeedback = "Item já adquirido!";
            feedbackTimer = FEEDBACK_DURATION;
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return;
        }
        int quantidade = item.compra_unica ? 1 : quantidadeSelecionada;
        long total = (long) item.preco * quantidade;
        if (player.getMoedas() >= total) {
            player.addMoedas(-(int) total);
            if (item.aoComprar != null) {
                for (int i = 0; i < quantidade; i++) {
                    item.aoComprar.run();
                }
            }
            if (item.compra_unica) {
                item.disponivel = false;
            }
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.playSFX(SoundManager.SFX.NOOT_NOOT);
            mensagemFeedback = "Comprou: " + item.nome
                    + (quantidade > 1 ? " x" + quantidade : "") + "!";
            ajustarQuantidadeAoLimite(item);
        } else {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            mensagemFeedback = "Moedas insuficientes!";
        }
        feedbackTimer = FEEDBACK_DURATION;
    }

    public void render(Graphics2D g2, int telaLargura, int telaAltura) {
        if (!aberto) {
            return;
        }

        repositionButtons();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, telaLargura, telaAltura);

        drawListHeader(g2);
        drawItemList(g2);
        drawDetailPanel(g2, telaLargura, telaAltura);
        drawFeedback(g2, telaLargura, telaAltura);
        drawControlsHint(g2, telaLargura, telaAltura);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawTextWithShadow(Graphics2D g2, String texto, int x, int y, Color cor) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(texto, x + 2, y + 2);
        g2.setColor(cor);
        g2.drawString(texto, x, y);
    }

    private void drawListHeader(Graphics2D g2) {
        g2.setFont(MenuButton.pixelFont.deriveFont(11f));
        String itensLabel = "ITENS";
        String precoLabel = "PREÇO";
        FontMetrics fm = g2.getFontMetrics();

        int headerY = TOP_MARGIN;
        drawTextWithShadow(g2, itensLabel, LIST_MARGIN_LEFT, headerY, new Color(220, 220, 220, 200));

        int precoLabelX = LIST_MARGIN_LEFT + BUTTON_WIDTH - fm.stringWidth(precoLabel);
        drawTextWithShadow(g2, precoLabel, precoLabelX, headerY, new Color(220, 220, 220, 200));

        int lineY = headerY + 6;
        int lineStartX = LIST_MARGIN_LEFT + fm.stringWidth(itensLabel) + 10;
        int lineEndX = precoLabelX - 10;
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawLine(lineStartX, lineY, lineEndX, lineY);
    }

    private void drawItemList(Graphics2D g2) {
        for (int i = 0; i < botoes.size(); i++) {
            botoes.get(i).setSelecionado(i == selecionado);
            botoes.get(i).draw(g2);
        }
    }

    private void drawDetailPanel(Graphics2D g2, int telaLargura, int telaAltura) {
        int listRightEdge = LIST_MARGIN_LEFT + BUTTON_WIDTH;
        int panelX = listRightEdge + PANEL_GAP;
        int panelWidth = Math.max(200, telaLargura - panelX - 60);

        if (itens.isEmpty()) {
            g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 14f));
            String vazio = "Nenhum item disponível.";
            drawTextWithShadow(g2, vazio, panelX, TOP_MARGIN + HEADER_GAP + BUTTON_HEIGHT / 2, Color.LIGHT_GRAY);
            return;
        }

        ShopItem item = itens.get(selecionado);
        int y = TOP_MARGIN + HEADER_GAP;

        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 22f));
        FontMetrics fmVal = g2.getFontMetrics();
        int iconSize = 22;

        if (coinIcon != null) {
            g2.drawImage(coinIcon, panelX, y - iconSize + 4, iconSize, iconSize, null);
        }
        String precoTexto = String.valueOf(item.preco);
        int textX = panelX + iconSize + 8;
        drawTextWithShadow(g2, precoTexto, textX, y, Color.WHITE);

        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 11f));
        String labelPreco = "Preço";
        FontMetrics fmLabel = g2.getFontMetrics();
        int labelX = panelX + panelWidth - fmLabel.stringWidth(labelPreco);
        drawTextWithShadow(g2, labelPreco, labelX, y, new Color(210, 210, 210, 170));

        int dashStartX = textX + fmVal.stringWidth(precoTexto) + 14;
        int dashEndX = labelX - 14;
        if (dashEndX > dashStartX) {
            drawDashedLine(g2, dashStartX, y - fmVal.getAscent() / 2, dashEndX);
        }

        y += 50;

        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 26f));
        drawTextWithShadow(g2, item.nome, panelX, y, Color.WHITE);
        y += 40;

        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 12f));
        FontMetrics fmDesc = g2.getFontMetrics();
        List<String> linhas = wrapTextComQuebras(fmDesc, item.descricao, panelWidth);
        int lineHeight = fmDesc.getHeight() + 6;
        for (String linha : linhas) {
            drawTextWithShadow(g2, linha, panelX, y, new Color(220, 220, 220));
            y += lineHeight;
        }

        if (!item.compra_unica) {
            ajustarQuantidadeAoLimite(item);
            positionQuantityButtons(panelX, panelWidth, y + QUANTITY_TOP_GAP);
            drawQuantitySelector(g2, item);
        }

        drawTotal(g2, telaLargura, telaAltura, item);
    }

    private void repositionQuantityButtons(int telaLargura, ShopItem item) {
        int listRightEdge = LIST_MARGIN_LEFT + BUTTON_WIDTH;
        int panelX = listRightEdge + PANEL_GAP;
        int panelWidth = Math.max(200, telaLargura - panelX - 60);

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dummy.createGraphics();
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 12f));
        FontMetrics fmDesc = g2.getFontMetrics();
        int quantidadeLinhas = wrapTextComQuebras(fmDesc, item.descricao, panelWidth).size();
        int lineHeight = fmDesc.getHeight() + 6;
        g2.dispose();

        int descriptionY = TOP_MARGIN + HEADER_GAP + 50 + 40;
        int selectorY = descriptionY + quantidadeLinhas * lineHeight + QUANTITY_TOP_GAP;
        positionQuantityButtons(panelX, panelWidth, selectorY);
    }

    private void positionQuantityButtons(int panelX, int panelWidth, int y) {
        int centerX = panelX + panelWidth / 2;
        setaQuantidadeEsquerda.setPosition(
                centerX - QUANTITY_NUMBER_HALF_GAP - QUANTITY_BUTTON_SIZE, y);
        setaQuantidadeDireita.setPosition(centerX + QUANTITY_NUMBER_HALF_GAP, y);
    }

    private void drawQuantitySelector(Graphics2D g2, ShopItem item) {
        setaQuantidadeEsquerda.draw(g2);
        setaQuantidadeDireita.draw(g2);

        String texto = String.valueOf(quantidadeSelecionada);
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 15f));
        FontMetrics fm = g2.getFontMetrics();
        int centerX = (setaQuantidadeEsquerda.getRect().x + setaQuantidadeEsquerda.getRect().width
                + setaQuantidadeDireita.getRect().x) / 2;
        int x = centerX - fm.stringWidth(texto) / 2;
        int y = setaQuantidadeEsquerda.getRect().y
                + (setaQuantidadeEsquerda.getRect().height - fm.getHeight()) / 2
                + fm.getAscent();

        drawTextWithShadow(g2, texto, x, y,
                podeComprar(item) ? new Color(255, 215, 80) : new Color(220, 90, 90));
    }

    private void alterarQuantidade(int direcao, ShopItem item) {
        int limite = getLimiteQuantidade(item);
        if (limite <= 1) {
            quantidadeSelecionada = 1;
        } else if (direcao < 0) {
            quantidadeSelecionada = quantidadeSelecionada == 1
                    ? limite
                    : quantidadeSelecionada - 1;
        } else {
            quantidadeSelecionada = quantidadeSelecionada == limite
                    ? 1
                    : quantidadeSelecionada + 1;
        }
    }

    private void ajustarQuantidadeAoLimite(ShopItem item) {
        if (item.compra_unica) {
            quantidadeSelecionada = 1;
            return;
        }
        quantidadeSelecionada = Math.max(1,
                Math.min(quantidadeSelecionada, getLimiteQuantidade(item)));
    }

    private int getLimiteQuantidade(ShopItem item) {
        if (player == null || item.preco <= 0) {
            return 1;
        }
        long moedas = Math.max(0, player.getMoedas());
        return (int) Math.min(Integer.MAX_VALUE, moedas / item.preco + 1);
    }

    private long getTotal(ShopItem item) {
        int quantidade = item.compra_unica ? 1 : quantidadeSelecionada;
        return (long) item.preco * quantidade;
    }

    private boolean podeComprar(ShopItem item) {
        return player != null && player.getMoedas() >= getTotal(item);
    }

    private List<String> wrapTextComQuebras(FontMetrics fm, String texto, int maxWidth) {
        List<String> linhas = new ArrayList<>();
        String[] partes = texto.split("\\s*\\bENDL\\b\\s*");
        for (String parte : partes) {
            linhas.addAll(wrapText(fm, parte, maxWidth));
        }
        return linhas;
    }

    private void drawTotal(Graphics2D g2, int telaLargura, int telaAltura, ShopItem item) {
        String texto = "Total: " + getTotal(item) + " moedas";
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 15f));
        FontMetrics fm = g2.getFontMetrics();
        int x = telaLargura - 60 - fm.stringWidth(texto);
        int y = telaAltura - 60;

        drawTextWithShadow(g2, texto, x, y,
                podeComprar(item) ? new Color(255, 215, 80) : new Color(220, 90, 90));
    }

    private void drawFeedback(Graphics2D g2, int telaLargura, int telaAltura) {
        if (mensagemFeedback == null || feedbackTimer <= 0) {
            return;
        }
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 12f));
        FontMetrics fm = g2.getFontMetrics();
        int x = (telaLargura - fm.stringWidth(mensagemFeedback)) / 2;
        int y = telaAltura - 90;

        drawTextWithShadow(g2, mensagemFeedback, x, y, new Color(150, 230, 150));
    }

    private void drawControlsHint(Graphics2D g2, int telaLargura, int telaAltura) {
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 10f));
        String texto = "[ESC] Sair";
        FontMetrics fm = g2.getFontMetrics();
        int x = (telaLargura - fm.stringWidth(texto)) / 2;
        int y = telaAltura - 30;

        drawTextWithShadow(g2, texto, x, y, new Color(200, 200, 200));
    }

    private void drawDashedLine(Graphics2D g2, int x1, int y, int x2) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] { 2, 4 }, 0));
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawLine(x1, y, x2, y);
        g2.setStroke(old);
    }

    private List<String> wrapText(FontMetrics fm, String texto, int maxWidth) {
        String[] palavras = texto.split(" ");
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : palavras) {
            String teste = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (fm.stringWidth(teste) <= maxWidth) {
                atual = new StringBuilder(teste);
            } else {
                if (!atual.isEmpty())
                    linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            }
        }
        if (!atual.isEmpty())
            linhas.add(atual.toString());
        return linhas;
    }
}
