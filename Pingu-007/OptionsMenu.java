import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;

public class OptionsMenu {

    private final SoundManager soundManager;
    private GameState returnTo;

    private static final int SLIDER_W = 250;
    private static final int SLIDER_H = 10;

    private boolean draggingMusic = false;
    private boolean draggingSFX = false;

    private final MenuButton backBtn;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public OptionsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        backBtn = new MenuButton("BACK", 0, 0, 160, 46);
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

    private void repositionButtons(int width, int height) {
        backBtn.setPosition((width - 160) / 2, height * 3 / 4);
    }

    public GameState update(InputManager input, int width, int height) {
        repositionButtons(width, height);

        int mx = input.getMouseX();
        int my = input.getMouseY();
        boolean clicking = input.isMouseButtonPressed(MouseEvent.BUTTON1);

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            return returnTo;
        }

        Rectangle musicSlider = getMusicSliderRect(width, height);
        Rectangle sfxSlider = getSFXSliderRect(width, height);

        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {
            if (musicSlider.contains(mx, my)) {
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
                draggingMusic = true;
            }
            if (sfxSlider.contains(mx, my)) {
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
                draggingSFX = true;
            }
        }
        if (!clicking) {
            draggingMusic = false;
            draggingSFX = false;
        }

        if (draggingMusic) {
            float v = (float) (mx - musicSlider.x) / musicSlider.width;
            soundManager.setMusicVolume(Math.clamp(v, 0f, 1f));
        }
        if (draggingSFX) {
            float v = (float) (mx - sfxSlider.x) / sfxSlider.width;
            soundManager.setSfxVolume(Math.clamp(v, 0f, 1f));
        }

        if (input.isKeyJustPressed(KeyEvent.VK_LEFT))
            soundManager.setMusicVolume(Math.clamp(soundManager.getMusicVolume() - 0.05f, 0f, 1f));
        if (input.isKeyJustPressed(KeyEvent.VK_RIGHT))
            soundManager.setMusicVolume(Math.clamp(soundManager.getMusicVolume() + 0.05f, 0f, 1f));

        if (backBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return returnTo;
        }

        return GameState.OPTIONS;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(0, 0, width, height);

        g2.setFont(pixelFont);
        String title = "OPTIONS";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, height / 4 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / 4);

        drawSlider(g2, "MUSIC VOLUME", soundManager.getMusicVolume(),
                getMusicSliderRect(width, height), width, height / 2 - 30);

        drawSlider(g2, "SFX VOLUME", soundManager.getSfxVolume(),
                getSFXSliderRect(width, height), width, height / 2 + 60);

        backBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawSlider(Graphics2D g2, String label, float value, Rectangle r, int width, int labelY) {
        g2.setFont(pixelFontSmall);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (width - fm.stringWidth(label)) / 2, labelY);

        g2.setColor(new Color(60, 60, 60));
        g2.fillRect(r.x, r.y, r.width, r.height);

        g2.setColor(Color.WHITE);
        g2.fillRect(r.x, r.y, (int) (r.width * value), r.height);

        int hx = r.x + (int) (r.width * value) - 5;
        g2.setColor(Color.WHITE);
        g2.fillRect(hx, r.y - 6, 10, r.height + 12);

        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(hx, r.y - 6, 10, r.height + 12);

        g2.setFont(pixelFontTiny);
        String pct = (int) (value * 100) + "%";
        g2.setColor(new Color(200, 200, 200));
        g2.drawString(pct, r.x + r.width + 14, r.y + r.height);
    }

    private Rectangle getMusicSliderRect(int width, int height) {
        return new Rectangle((width - SLIDER_W) / 2, height / 2 - 10, SLIDER_W, SLIDER_H);
    }

    private Rectangle getSFXSliderRect(int width, int height) {
        return new Rectangle((width - SLIDER_W) / 2, height / 2 + 80, SLIDER_W, SLIDER_H);
    }
}