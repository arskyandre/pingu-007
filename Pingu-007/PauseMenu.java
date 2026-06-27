import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;

public class PauseMenu {

    private enum Button {
        RESUME, OPTIONS, MAIN_MENU
    }

    private SoundManager soundManager;
    private Button hovered = null;
    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;

    private Font pixelFont;
    private Font pixelFontSmall;

    public PauseMenu(SoundManager sound) {
        soundManager = sound;
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 24f);
            pixelFontSmall = base.deriveFont(Font.PLAIN, 11f);
        } catch (Exception e) {
            System.err.println("fonte nao encontrada nos arquivos.");
            pixelFont = new Font("Monospaced", Font.BOLD, 24);
            pixelFontSmall = new Font("Monospaced", Font.BOLD, 11);
        }
    }

    public GameState update(InputManager input, int width, int height) {
        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)) {
            return GameState.PLAYING;
        }
        int mx = input.getMouseX();
        int my = input.getMouseY();
        hovered = null;
        Button[] buttons = Button.values();
        for (int i = 0; i < buttons.length; i++) {
            Rectangle r = getButtonRect(i, width, height);
            if (r.contains(mx, my)) {
                hovered = buttons[i];
                if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {

                    soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
                    return switch (buttons[i]) {
                        case RESUME -> GameState.PLAYING;
                        case OPTIONS -> GameState.OPTIONS;
                        case MAIN_MENU -> GameState.MAIN_MENU;
                    };
                }
            }
        }
        return GameState.PAUSED;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, height);

        int panelW = 320, panelH = 360;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        g2.setColor(new Color(10, 10, 10, 220));
        g2.fillRect(px, py, panelW, panelH);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(px, py, panelW, panelH);

        g2.setFont(pixelFont);
        g2.setColor(Color.WHITE);
        String title = "PAUSED";
        int tw = g2.getFontMetrics().stringWidth(title);

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, py + 55 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, py + 55);

        String[] labels = { "RESUME", "OPTIONS", "MAIN MENU" };
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
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(labels[i]);
            g2.drawString(labels[i],
                    r.x + (r.width - lw) / 2,
                    r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private Rectangle getButtonRect(int index, int width, int height) {
        int x = (width - BTN_W) / 2;
        int y = height / 2 - 20 + index * (BTN_H + BTN_GAP);
        return new Rectangle(x, y, BTN_W, BTN_H);
    }
}