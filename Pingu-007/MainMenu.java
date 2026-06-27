import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MainMenu {

    private enum Button {
        PLAY, OPTIONS, QUIT
    }

    private Button hovered = null;
    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;

    private BufferedImage background;
    private Font pixelFont;
    private Font pixelFontSmall;

    public MainMenu() {
        try {
            background = ImageIO.read(new File("menu_background.png"));
        } catch (Exception e) {
            System.err.println("menu_background.png not found");
        }
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 32f);
            pixelFontSmall = base.deriveFont(Font.PLAIN, 14f);
        } catch (Exception e) {
            System.err.println("PressStart2P.ttf not found, falling back to Monospaced");
            pixelFont = new Font("Monospaced", Font.BOLD, 32);
            pixelFontSmall = new Font("Monospaced", Font.BOLD, 14);
        }
    }

    public GameState update(InputManager input, int width, int height) {
        int mx = input.getMouseX();
        int my = input.getMouseY();
        hovered = null;
        Button[] buttons = Button.values();
        for (int i = 0; i < buttons.length; i++) {
            Rectangle r = getButtonRect(i, width, height);
            if (r.contains(mx, my)) {
                hovered = buttons[i];
                if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {
                    return switch (buttons[i]) {
                        case PLAY -> GameState.PLAYING;
                        case OPTIONS -> GameState.OPTIONS;
                        case QUIT -> GameState.QUIT;
                    };
                }
            }
        }
        return GameState.MAIN_MENU;
    }

    public void render(Graphics2D g2, int width, int height) {
        
        if (background != null) {
            g2.drawImage(background, 0, 0, width, height, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, width, height);
        }

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, width, height);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setFont(pixelFont);
        g2.setColor(Color.WHITE);
        String title = "PINGU 007";
        int tw = g2.getFontMetrics().stringWidth(title);
        
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 3, height / 4 + 3);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / 4);

        
        String[] labels = { "PLAY", "OPTIONS", "QUIT" };
        Button[] buttons = Button.values();
        for (int i = 0; i < buttons.length; i++) {
            Rectangle r = getButtonRect(i, width, height);
            boolean isHovered = hovered == buttons[i];

            
            if (isHovered) {
                
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(r.x, r.y, r.width, r.height);
                
                g2.setColor(Color.WHITE);
            } else {
                
                g2.setColor(new Color(200, 200, 200, 120));
            }
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(r.x, r.y, r.width, r.height);

            
            g2.setFont(pixelFontSmall);
            g2.setColor(isHovered ? Color.WHITE : new Color(200, 200, 200));
            int lw = g2.getFontMetrics().stringWidth(labels[i]);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(labels[i],
                    r.x + (r.width - lw) / 2,
                    r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }

        
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private Rectangle getButtonRect(int index, int width, int height) {
        int x = (width - BTN_W) / 2;
        int y = height / 2 + index * (BTN_H + BTN_GAP);
        return new Rectangle(x, y, BTN_W, BTN_H);
    }
}