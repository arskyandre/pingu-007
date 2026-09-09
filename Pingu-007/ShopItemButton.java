import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/** Shop-row control with availability-aware painting and typed pointer results. */
public class ShopItemButton extends MenuButton {

    private final ShopItem item;
    private final BufferedImage coinIcon;
    private List<String> nameLines;

    private static final int ICON_LEFT_MARGIN = 12;
    private static final int ICON_TEXT_GAP = 14;
    private static final int PRICE_ICON_SIZE = 16;
    private static final int PRICE_RIGHT_MARGIN = 16;

    private static final int PRICE_RESERVED_WIDTH = 90;
    private static final int VERTICAL_PADDING = 16;

    public ShopItemButton(ShopItem item, BufferedImage coinIcon, int x, int y, int width, int height) {
        super(item.nome, x, y, width, height);
        this.item = item;
        this.coinIcon = coinIcon;
        adjustHeightFromName(height);
    }

    @Override
    protected void adjustHeight() {
        // Shop rows use their icon/text-aware height calculation below.
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        adjustHeightFromName(height);
    }

    private void adjustHeightFromName(int minimumHeight) {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dummy.createGraphics();
        try {
            graphics.setFont(MenuFonts.buttonFont(13f));
            FontMetrics metrics = graphics.getFontMetrics();

            int iconSizeBase = minimumHeight - 16;
            int textAreaX = ICON_LEFT_MARGIN + iconSizeBase + ICON_TEXT_GAP;
            int maxTextWidth = rect.width - textAreaX - PRICE_RESERVED_WIDTH;
            if (maxTextWidth < 20) {
                maxTextWidth = 20;
            }

            nameLines = MenuPainter.wrapWords(metrics, item.nome, maxTextWidth);
            int lineHeight = metrics.getAscent() + metrics.getDescent() + 2;
            int neededHeight = nameLines.size() * lineHeight + VERTICAL_PADDING;
            rect.height = Math.max(minimumHeight, neededHeight);
        } finally {
            graphics.dispose();
        }
    }

    public ShopItem getItem() {
        return item;
    }

    @Override
    public MenuInteraction updatePointer(InputManager input) {
        if (!item.disponivel) {
            clearPointerHover();
            setSelected(false);
            return MenuInteraction.IDLE;
        }
        return super.updatePointer(input);
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            boolean highlighted = isHighlighted();
            if (!item.disponivel) {
                copy.setColor(new Color(0, 0, 0, 128));
                copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                copy.setColor(new Color(140, 140, 80));
                copy.setStroke(new BasicStroke(2.5f));
            } else if (highlighted) {
                copy.setColor(new Color(255, 220, 130, 35));
                copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                copy.setColor(new Color(255, 215, 80));
                copy.setStroke(new BasicStroke(2.5f));
            } else {
                copy.setColor(new Color(0, 0, 0, 120));
                copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                copy.setColor(new Color(255, 255, 255, 70));
                copy.setStroke(new BasicStroke(1.5f));
            }
            copy.drawRect(rect.x, rect.y, rect.width, rect.height);

            int iconSize = rect.height - 16;
            int iconX = rect.x + ICON_LEFT_MARGIN;
            int iconY = rect.y + (rect.height - iconSize) / 2;
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            copy.setColor(new Color(0, 0, 0, 72));
            copy.fillRoundRect(iconX, iconY, iconSize, iconSize, 4, 4);
            if (item.icone != null) {
                copy.drawImage(item.icone, iconX, iconY, iconSize, iconSize, null);
            }

            int nameX = iconX + iconSize + ICON_TEXT_GAP;
            copy.setFont(MenuFonts.buttonFont(13f));
            FontMetrics nameMetrics = copy.getFontMetrics();
            int lineHeight = nameMetrics.getAscent() + nameMetrics.getDescent() + 2;
            int totalTextHeight = nameLines.size() * lineHeight;
            int textStartY = rect.y + (rect.height - totalTextHeight) / 2 + nameMetrics.getAscent();

            Color nameColor = highlighted ? Color.WHITE : new Color(220, 220, 220);
            for (int index = 0; index < nameLines.size(); index++) {
                MenuPainter.drawTextWithShadow(copy, nameLines.get(index), nameX,
                        textStartY + index * lineHeight, nameColor);
            }

            String priceText = String.valueOf(item.preco);
            int priceTextWidth = nameMetrics.stringWidth(priceText);
            int priceX = rect.x + rect.width - PRICE_RIGHT_MARGIN - priceTextWidth;
            int priceIconX = priceX - PRICE_ICON_SIZE - 4;
            int priceIconY = rect.y + (rect.height - PRICE_ICON_SIZE) / 2;
            int priceBaselineY = rect.y + rect.height / 2 + nameMetrics.getAscent() / 2 - 2;

            if (coinIcon != null) {
                copy.drawImage(coinIcon, priceIconX, priceIconY, PRICE_ICON_SIZE, PRICE_ICON_SIZE, null);
            }
            MenuPainter.drawTextWithShadow(copy, priceText, priceX, priceBaselineY,
                    new Color(255, 215, 80));

            if (nameLines.size() == 1) {
                int lineStartX = nameX + nameMetrics.stringWidth(nameLines.get(0)) + 10;
                int lineEndX = priceIconX - 10;
                if (lineEndX > lineStartX) {
                    MenuPainter.drawDashedLine(copy, lineStartX, rect.y + rect.height / 2, lineEndX,
                            new Color(255, 255, 255, 90), 1f, new float[] { 2, 4 }, 0);
                }
            }
        } finally {
            copy.dispose();
        }
    }
}
