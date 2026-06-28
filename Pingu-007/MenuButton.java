import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;

public class MenuButton {

    private final String label;
    private final Rectangle rect;
    private boolean hovered = false;

    private static Font pixelFont;

    static {
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 11f);
        } catch (Exception e) {
            pixelFont = new Font("Monospaced", Font.BOLD, 11);
        }
    }

    public MenuButton(String label, int x, int y, int width, int height) {
        this.label = label;
        this.rect = new Rectangle(x, y, width, height);
    }

    public static final int IDLE = 0;
    public static final int HOVERED = 1;
    public static final int CLICKED = 2;

    public int update(InputManager input) {
        hovered = rect.contains(input.getMouseX(), input.getMouseY());
        if (!hovered)
            return IDLE;
        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1))
            return CLICKED;
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
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(hovered ? Color.WHITE : new Color(200, 200, 200));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label,
                rect.x + (rect.width - fm.stringWidth(label)) / 2,
                rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2);
    }

    public boolean isHovered() {
        return hovered;
    }

    public Rectangle getRect() {
        return rect;
    }

    // reposition if needed (e.g. centering dynamically)
    public void setPosition(int x, int y) {
        rect.setLocation(x, y);
    }
}