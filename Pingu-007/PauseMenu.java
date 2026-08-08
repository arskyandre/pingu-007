import java.awt.*;
import java.io.File;

public class PauseMenu {

    private final MenuButton resumeBtn;
    private final MenuButton optionsBtn;
    private final MenuButton mainMenuBtn;

    private final SoundManager soundManager;

    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;

    private Font pixelFont;

    public PauseMenu(SoundManager sound) {
        soundManager = sound;
        resumeBtn = new MenuButton("RESUMIR", 0, 0, BTN_W, BTN_H);
        optionsBtn = new MenuButton("OPÇÕES", 0, 0, BTN_W, BTN_H);
        mainMenuBtn = new MenuButton("VOLTAR AO MENU PRINCIPAL", 0, 0, BTN_W, BTN_H);

        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 24f);
        } catch (Exception e) {
            System.err.println("fonte nao encontrada nos arquivos.");
            pixelFont = new Font("Monospaced", Font.BOLD, 24);
        }
    }

    private void repositionButtons(int width, int height) {
        int x = (width - BTN_W) / 2;
        int y = height / 2 - 20;
        resumeBtn.setPosition(x, y);
        optionsBtn.setPosition(x, y + BTN_H + BTN_GAP);
        mainMenuBtn.setPosition(x, y + (BTN_H + BTN_GAP) * 2);
    }

    public GameState update(InputManager input, int width, int height) {
        repositionButtons(width, height);

        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_ESCAPE)
                || input.isButtonJustPressed(InputManager.GamepadButton.B)
                || input.isButtonJustPressed(InputManager.GamepadButton.START))
            return GameState.PLAYING;

        if (resumeBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.PLAYING;
        }
        if (optionsBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.OPTIONS;
        }
        if (mainMenuBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.MAIN_MENU;
        }

        return GameState.PAUSED;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // dim game behind
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, height);

        // panel
        int panelW = 320, panelH = 360;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        g2.setColor(new Color(10, 10, 10, 220));
        g2.fillRect(px, py, panelW, panelH);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(px, py, panelW, panelH);

        // title
        g2.setFont(pixelFont);
        String title = "PAUSADO";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, py + 55 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, py + 55);

        // buttons
        resumeBtn.draw(g2);
        optionsBtn.draw(g2);
        mainMenuBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }
}