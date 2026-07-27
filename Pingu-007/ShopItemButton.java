import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ShopItemButton extends MenuButton {

    private final ShopItem item;
    private final BufferedImage coinIcon;
    private boolean selecionado = false;
    private List<String> nomeLines;

    private static final int ICON_LEFT_MARGIN = 12;
    private static final int ICON_TEXT_GAP = 14;
    private static final int PRICE_ICON_SIZE = 16;
    private static final int PRICE_RIGHT_MARGIN = 16;
    // espaço reservado pro preço + icone de moeda, independente do valor exato
    private static final int PRICE_RESERVED_WIDTH = 90;
    private static final int VERTICAL_PADDING = 16;

    public ShopItemButton(ShopItem item, BufferedImage coinIcon, int x, int y, int width, int height) {
        super(item.nome, x, y, width, height);
        this.item = item;
        this.coinIcon = coinIcon;
        // Nao pode ser feito dentro de adjustHeight(): esse metodo e chamado
        // pelo construtor de MenuButton, ANTES dos campos desta subclasse
        // (como "item") serem atribuidos. Por isso o calculo real acontece
        // aqui, apos os campos ja estarem prontos.
        ajustarAlturaPeloNome(height);
    }

    @Override
    protected void adjustHeight() {
        // no-op de propósito — ver comentário no construtor acima.
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        ajustarAlturaPeloNome(height);
    }

    private void ajustarAlturaPeloNome(int alturaMinima) {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dummy.createGraphics();
        g2.setFont(pixelFont.deriveFont(13f));
        FontMetrics fm = g2.getFontMetrics();
        g2.dispose();

        int iconSizeBase = alturaMinima - 16;
        int textAreaX = ICON_LEFT_MARGIN + iconSizeBase + ICON_TEXT_GAP;
        int maxTextWidth = rect.width - textAreaX - PRICE_RESERVED_WIDTH;
        if (maxTextWidth < 20) {
            maxTextWidth = 20;
        }

        nomeLines = wrapText(fm, item.nome, maxTextWidth);

        int lineHeight = fm.getAscent() + fm.getDescent() + 2;
        int neededHeight = nomeLines.size() * lineHeight + VERTICAL_PADDING;

        rect.height = Math.max(alturaMinima, neededHeight);
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

    public ShopItem getItem() {
        return item;
    }

    public void setSelecionado(boolean valor) {
        this.selecionado = valor;
    }

    private void drawDashedLine(Graphics2D g2, int x1, int y, int x2) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] { 2, 4 }, 0));
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawLine(x1, y, x2, y);
        g2.setStroke(old);
    }

    private void drawTextWithShadow(Graphics2D g2, String texto, int x, int y, Color cor) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(texto, x + 2, y + 2);
        g2.setColor(cor);
        g2.drawString(texto, x, y);
    }

    @Override
    public void draw(Graphics2D g2) {
        boolean destacado = selecionado || hovered;

        if (destacado) {
            g2.setColor(new Color(255, 220, 130, 35));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(new Color(255, 215, 80));
            g2.setStroke(new BasicStroke(2.5f));
        } else {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(new Color(255, 255, 255, 70));
            g2.setStroke(new BasicStroke(1.5f));
        }
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        int iconSize = rect.height - 16;
        int iconX = rect.x + ICON_LEFT_MARGIN;
        int iconY = rect.y + (rect.height - iconSize) / 2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (item.icone != null) {
            g2.drawImage(item.icone, iconX, iconY, iconSize, iconSize, null);
        } else {
            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(iconX, iconY, iconSize, iconSize);
        }

        int nameX = iconX + iconSize + ICON_TEXT_GAP;
        g2.setFont(pixelFont.deriveFont(13f));
        FontMetrics fmNome = g2.getFontMetrics();
        int lineHeight = fmNome.getAscent() + fmNome.getDescent() + 2;
        int totalTextHeight = nomeLines.size() * lineHeight;
        int textStartY = rect.y + (rect.height - totalTextHeight) / 2 + fmNome.getAscent();

        Color nomeColor = destacado ? Color.WHITE : new Color(220, 220, 220);
        for (int i = 0; i < nomeLines.size(); i++) {
            drawTextWithShadow(g2, nomeLines.get(i), nameX, textStartY + i * lineHeight, nomeColor);
        }

        String precoTexto = String.valueOf(item.preco);
        int precoTextW = fmNome.stringWidth(precoTexto);
        int precoX = rect.x + rect.width - PRICE_RIGHT_MARGIN - precoTextW;
        int iconPriceX = precoX - PRICE_ICON_SIZE - 4;
        int iconPriceY = rect.y + (rect.height - PRICE_ICON_SIZE) / 2;
        int precoBaselineY = rect.y + rect.height / 2 + fmNome.getAscent() / 2 - 2;

        if (coinIcon != null) {
            g2.drawImage(coinIcon, iconPriceX, iconPriceY, PRICE_ICON_SIZE, PRICE_ICON_SIZE, null);
        }
        drawTextWithShadow(g2, precoTexto, precoX, precoBaselineY, new Color(255, 215, 80));

        // linha pontilhada guia so faz sentido com o nome em uma unica linha
        if (nomeLines.size() == 1) {
            int lineStartX = nameX + fmNome.stringWidth(nomeLines.get(0)) + 10;
            int lineEndX = iconPriceX - 10;
            if (lineEndX > lineStartX) {
                drawDashedLine(g2, lineStartX, rect.y + rect.height / 2, lineEndX);
            }
        }
    }
}