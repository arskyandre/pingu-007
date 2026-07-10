import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * botao de menu com texto, auto ajustado
 */
public class MenuButton {

    protected final String label;
    protected final Rectangle rect;
    protected boolean hovered = false;
    protected boolean held = false;
    protected static Font pixelFont;

    static {
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 11f);
        } catch (Exception e) {
            pixelFont = new Font("Monospaced", Font.BOLD, 11);
        }
    }

    public static final int IDLE = 0;
    public static final int HOVERED = 1;
    public static final int CLICKED = 2;

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
        Graphics2D g2 = dummy.createGraphics();
        g2.setFont(pixelFont);
        FontMetrics fm = g2.getFontMetrics();
        g2.dispose();

        List<String> lines = buildLines(fm);
        int lineHeight = fm.getAscent() + fm.getDescent() + 2;
        int paddingV = 16;
        int neededHeight = lines.size() * lineHeight + paddingV;

        if (neededHeight > rect.height) {
            rect.height = neededHeight;
        }
    }

    protected List<String> buildLines(FontMetrics fm) {
        int maxWidth = rect.width - 10;
        String[] words = label.split(" ");
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

    public int update(InputManager input) {
        hovered = rect.contains(input.getMouseX(), input.getMouseY());
        if (hovered && input.isMouseButtonJustPressed(MouseEvent.BUTTON1))
            return CLICKED;
        else if (hovered && input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
            held = true;
        }
        held = false;
        if (!hovered) {
            return IDLE;
        }
        return HOVERED;
    }

    public void draw(Graphics2D g2) {
        if (hovered) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(new Color(200, 200, 200, 120));
        }
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        g2.setFont(pixelFont);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setColor(hovered ? Color.WHITE : new Color(200, 200, 200));

        FontMetrics fm = g2.getFontMetrics();
        List<String> lines = buildLines(fm);

        int lineHeight = fm.getAscent() + fm.getDescent() + 2;
        int totalHeight = lines.size() * lineHeight;
        int startY = rect.y + (rect.height - totalHeight) / 2 + fm.getAscent();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            g2.drawString(line,
                    rect.x + (rect.width - fm.stringWidth(line)) / 2,
                    startY + i * lineHeight);
        }
    }

    public boolean isHovered() {
        return hovered;
    }

    public Rectangle getRect() {
        return rect;
    }

    public void setPosition(int x, int y) {
        rect.setLocation(x, y);
    }
}