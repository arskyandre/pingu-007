import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** Text button with the existing wrapping, sizing, and pixel-style painting. */
public class MenuButton implements MenuControl {

    protected final String label;
    protected final Rectangle rect;
    protected boolean hovered;
    protected boolean held;
    private boolean focused;
    private boolean selected;

    public MenuButton(String label, int x, int y, int width, int height) {
        this.label = label;
        this.rect = new Rectangle(x, y, width, height);
        adjustHeight();
    }

    public void setSize(int width, int height) {
        rect.setSize(width, height);
    }

    protected void adjustHeight() {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dummy.createGraphics();
        try {
            graphics.setFont(MenuFonts.buttonFont(11f));
            FontMetrics metrics = graphics.getFontMetrics();
            List<String> lines = buildLines(metrics);
            int lineHeight = metrics.getAscent() + metrics.getDescent() + 2;
            int paddingV = 16;
            int neededHeight = lines.size() * lineHeight + paddingV;
            if (neededHeight > rect.height) {
                rect.height = neededHeight;
            }
        } finally {
            graphics.dispose();
        }
    }

    protected List<String> buildLines(FontMetrics metrics) {
        return MenuPainter.wrapWords(metrics, label, rect.width - 10);
    }

    @Override
    public MenuInteraction updatePointer(InputManager input) {
        hovered = rect.contains(input.getMouseX(), input.getMouseY());
        if (hovered && input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {
            held = true;
            return MenuInteraction.CLICKED;
        }
        if (hovered && input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
            held = true;
            return MenuInteraction.PRESSED;
        }
        held = false;
        return hovered ? MenuInteraction.HOVERED : MenuInteraction.IDLE;
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            boolean highlighted = isHighlighted();
            if (highlighted) {
                copy.setColor(new Color(255, 255, 255, 40));
                copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                copy.setColor(Color.WHITE);
            } else {
                copy.setColor(new Color(200, 200, 200, 120));
            }
            copy.setStroke(new BasicStroke(2));
            copy.drawRect(rect.x, rect.y, rect.width, rect.height);

            copy.setFont(MenuFonts.buttonFont(11f));
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            copy.setColor(highlighted ? Color.WHITE : new Color(200, 200, 200));

            FontMetrics metrics = copy.getFontMetrics();
            List<String> lines = buildLines(metrics);
            int lineHeight = metrics.getAscent() + metrics.getDescent() + 2;
            int totalHeight = lines.size() * lineHeight;
            int startY = rect.y + (rect.height - totalHeight) / 2 + metrics.getAscent();

            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                copy.drawString(line,
                        rect.x + (rect.width - metrics.stringWidth(line)) / 2,
                        startY + index * lineHeight);
            }
        } finally {
            copy.dispose();
        }
    }

    @Override
    public Rectangle getBoundsCopy() {
        return new Rectangle(rect);
    }

    @Override
    public void setPosition(int x, int y) {
        rect.setLocation(x, y);
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean isHovered() {
        return hovered;
    }

    @Override
    public void clearPointerHover() {
        hovered = false;
        held = false;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    protected boolean isHighlighted() {
        return hovered || focused || selected;
    }

    public boolean isHighlightedForRendering() {
        return isHighlighted();
    }

    public String getLabel() {
        return label;
    }
}
