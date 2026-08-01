import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class KeyBindingsMenu {

    private final SoundManager soundManager;
    private GameState returnTo = GameState.OPTIONS;

    private static final int BTN_SIZE = 36;
    private static final int BTN_GAP = 10;

    private final MenuButton backBtn;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public KeyBindingsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;

        backBtn = new MenuButton("VOLTAR", 0, 0, 160, 46);

        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 24f);
            pixelFontSmall = base.deriveFont(Font.PLAIN, 11f);
            pixelFontTiny = base.deriveFont(Font.PLAIN, 9f);
        } catch (Exception e) {
            System.err.println("Font not found, falling back");
            pixelFont = new Font("Monospaced", Font.BOLD, 24);
            pixelFontSmall = new Font("Monospaced", Font.BOLD, 11);
            pixelFontTiny = new Font("Monospaced", Font.PLAIN, 9);
        }
    }

    public void setReturnState(GameState state) {
        this.returnTo = state;
    }

    public void repositionElements(int width, int height) {
        backBtn.setPosition((width - 160) / 2, height * 3 / 4 + 76);
    }

    public GameState update(InputManager input, int width, int height) {
        repositionElements(width, height);

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE))
            return returnTo;

        if (backBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return returnTo;
        }

        return GameState.KEYBINDINGS;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(0, 0, width, height);

        // title
        g2.setFont(pixelFont);
        String title = "TECLAS DE AÇÃO";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, height / 8 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / 8);
        g2.setFont(pixelFont);

        int textY = height / 8 + 64;
        g2.setFont(pixelFont.deriveFont(Font.PLAIN, 24f));
        title = "ANDAR: WASD";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);
        g2.setFont(pixelFont);

        textY += 32;
        title = "ATIRAR: BOTÃO ESQUERDO DO MOUSE";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);
        g2.setFont(pixelFont);

        textY += 32;
        title = "RECARREGAR: R";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);
        g2.setFont(pixelFont);

        textY += 32;
        title = "INTERAGIR/FORÇA(PESCA): E";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);

        textY += 32;
        title = "LANÇAR LINHA DE PESCA: BOTÃO DIREITO DO MOUSE";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);

        textY += 32;
        title = "ALTERNAR ARMAS: G";
        tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, textY + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, textY);

        backBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

}