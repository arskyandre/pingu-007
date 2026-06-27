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
    private boolean backHovered = false;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public OptionsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
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

    public GameState update(InputManager input, int width, int height) {
        int mx = input.getMouseX();
        int my = input.getMouseY();
        boolean clicking = input.isMouseButtonPressed(MouseEvent.BUTTON1);
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            return returnTo;
        }
        Rectangle musicSlider = getMusicSliderRect(width, height);
        Rectangle sfxSlider = getSFXSliderRect(width, height);

        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {
            if (musicSlider.contains(mx, my))
                draggingMusic = true;
            if (sfxSlider.contains(mx, my))
                draggingSFX = true;
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

        Rectangle backBtn = getBackButtonRect(width, height);
        backHovered = backBtn.contains(mx, my);
        if (backHovered && input.isMouseButtonJustPressed(MouseEvent.BUTTON1)) {
            return returnTo;
        }

        return GameState.OPTIONS;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // background
        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(0, 0, width, height);

        // title
        g2.setFont(pixelFont);
        g2.setColor(Color.WHITE);
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

        Rectangle r = getBackButtonRect(width, height);
        if (backHovered) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(new Color(200, 200, 200, 120));
        }
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(r.x, r.y, r.width, r.height);
        g2.setFont(pixelFontSmall);
        g2.setColor(backHovered ? Color.WHITE : new Color(200, 200, 200));
        FontMetrics fm = g2.getFontMetrics();
        String back = "BACK";
        g2.drawString(back,
                r.x + (r.width - fm.stringWidth(back)) / 2,
                r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawSlider(Graphics2D g2, String label, float value, Rectangle r, int width, int labelY) {
        g2.setFont(pixelFontSmall);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (width - fm.stringWidth(label)) / 2, labelY);

        // track
        g2.setColor(new Color(60, 60, 60));
        g2.fillRect(r.x, r.y, r.width, r.height);

        // fill
        g2.setColor(Color.WHITE);
        g2.fillRect(r.x, r.y, (int) (r.width * value), r.height);

        // handle — sharp square instead of oval
        int hx = r.x + (int) (r.width * value) - 5;
        g2.setColor(Color.WHITE);
        g2.fillRect(hx, r.y - 6, 10, r.height + 12);

        // border around handle
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(hx, r.y - 6, 10, r.height + 12);

        // percentage
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

    private Rectangle getBackButtonRect(int width, int height) {
        return new Rectangle((width - 160) / 2, height * 3 / 4, 160, 46);
    }
}