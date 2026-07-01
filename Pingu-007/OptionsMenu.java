import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class OptionsMenu {

    private final SoundManager soundManager;
    private GameState returnTo;

    private static final int SLIDER_W = 320;
    private static final int SLIDER_H = 10;
    private static final int BTN_SIZE = 36;
    private static final int BTN_GAP = 10;

    private int lastClickedSlider = 0;

    private float previousMusicVolume;
    private float previousSfxVolume;
    private boolean musicMuted = false;
    private boolean sfxMuted = false;

    private final MenuSlider musicSlider;
    private final MenuSlider sfxSlider;
    private final IconButton toggleMuteBGM;
    private final IconButton toggleMuteSFX;
    private final MenuButton backBtn;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public OptionsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        this.previousMusicVolume = soundManager.getMusicVolume();
        this.previousSfxVolume = soundManager.getSfxVolume();

        musicSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getMusicVolume());
        sfxSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getSfxVolume());
        toggleMuteBGM = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleMuteSFX = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
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

    private void repositionElements(int width, int height) {
        int sliderX = (width - SLIDER_W) / 2;

        musicSlider.setPosition(sliderX, height / 2 - 10);
        sfxSlider.setPosition(sliderX, height / 2 + 80);

        toggleMuteBGM.setPosition(
                musicSlider.getRightX() + BTN_GAP,
                musicSlider.getCenterY() - BTN_SIZE / 2);
        toggleMuteSFX.setPosition(
                sfxSlider.getRightX() + BTN_GAP,
                sfxSlider.getCenterY() - BTN_SIZE / 2);

        backBtn.setPosition((width - 160) / 2, height * 3 / 4);
    }

    public GameState update(InputManager input, int width, int height) {
        repositionElements(width, height);

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE))
            return returnTo;

        int musicState = musicSlider.update(input);
        if (musicState == MenuSlider.DRAGGING || musicState == MenuSlider.CLICKED) {
            if (musicState == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.setMusicVolume(musicSlider.getValue());
        }

        if (musicSlider.getValue() <= 0f && !musicMuted) {
            musicMuted = true;
            toggleMuteBGM.setIcon(IconIndex.MUTED);
        } else if (musicSlider.getValue() > 0f && musicMuted) {
            musicMuted = false;
            toggleMuteBGM.setIcon(IconIndex.UNMUTED);
        }

        int sfxState = sfxSlider.update(input);
        if (sfxState == MenuSlider.DRAGGING || sfxState == MenuSlider.CLICKED) {
            if (sfxState == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.setSfxVolume(sfxSlider.getValue());
        }
        if (sfxSlider.getValue() <= 0f && !sfxMuted) {
            sfxMuted = true;
            toggleMuteSFX.setIcon(IconIndex.MUTED);
        } else if (sfxSlider.getValue() > 0f && sfxMuted) {
            sfxMuted = false;
            toggleMuteSFX.setIcon(IconIndex.UNMUTED);
        }

        if (lastClickedSlider == 0) {
            if (input.isKeyJustPressed(KeyEvent.VK_LEFT)) {
                float v = Math.clamp(soundManager.getMusicVolume() - 0.05f, 0f, 1f);
                soundManager.setMusicVolume(v);
                musicSlider.setValue(v);
            }
            if (input.isKeyJustPressed(KeyEvent.VK_RIGHT)) {
                float v = Math.clamp(soundManager.getMusicVolume() + 0.05f, 0f, 1f);
                soundManager.setMusicVolume(v);
                musicSlider.setValue(v);
            }
        } else {
            if (input.isKeyJustPressed(KeyEvent.VK_LEFT)) {
                float v = Math.clamp(soundManager.getSfxVolume() - 0.05f, 0f, 1f);
                soundManager.setSfxVolume(v);
                sfxSlider.setValue(v);
            }
            if (input.isKeyJustPressed(KeyEvent.VK_RIGHT)) {
                float v = Math.clamp(soundManager.getSfxVolume() + 0.05f, 0f, 1f);
                soundManager.setSfxVolume(v);
                sfxSlider.setValue(v);
            }
        }

        if (toggleMuteBGM.update(input) == IconButton.CLICKED) {
            if (!musicMuted) {
                previousMusicVolume = soundManager.getMusicVolume();
                soundManager.setMusicVolume(0f);
                musicSlider.setValue(0f);
                toggleMuteBGM.setIcon(IconIndex.MUTED);
                musicMuted = true;
            } else {
                float restore = previousMusicVolume;
                soundManager.setMusicVolume(restore);
                musicSlider.setValue(restore);
                toggleMuteBGM.setIcon(IconIndex.UNMUTED);
                musicMuted = false;
            }
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        }

        if (toggleMuteSFX.update(input) == IconButton.CLICKED) {
            if (!sfxMuted) {
                previousSfxVolume = soundManager.getSfxVolume();
                soundManager.setSfxVolume(0f);
                sfxSlider.setValue(0f);
                toggleMuteSFX.setIcon(IconIndex.MUTED);
                sfxMuted = true;
            } else {
                float restore = previousSfxVolume;
                soundManager.setSfxVolume(restore);
                sfxSlider.setValue(restore);
                toggleMuteSFX.setIcon(IconIndex.UNMUTED);
                sfxMuted = false;
            }
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        }

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

        // title
        g2.setFont(pixelFont);
        String title = "OPÇÕES";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, height / 4 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / 4);

        // music label + slider
        drawLabel(g2, "VOLUME DA MÚSICA", musicSlider.getRect(), width, height / 2 - 30);
        drawPct(g2, soundManager.getMusicVolume(), musicSlider.getRect());
        musicSlider.draw(g2);

        // sfx label + slider
        drawLabel(g2, "VOLUME DOS EFEITOS", sfxSlider.getRect(), width, height / 2 + 60);
        drawPct(g2, soundManager.getSfxVolume(), sfxSlider.getRect());
        sfxSlider.draw(g2);

        toggleMuteBGM.draw(g2);
        toggleMuteSFX.draw(g2);
        backBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawLabel(Graphics2D g2, String label, Rectangle sliderRect, int width, int labelY) {
        g2.setFont(pixelFontSmall);
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (width - fm.stringWidth(label)) / 2, labelY);
    }

    private void drawPct(Graphics2D g2, float value, Rectangle sliderRect) {
        g2.setFont(pixelFontTiny);
        g2.setColor(new Color(200, 200, 200));
        String pct = (int) (value * 100) + "%";
        g2.drawString(pct,
                sliderRect.x + sliderRect.width + BTN_SIZE + BTN_GAP * 2 + 4,
                sliderRect.y + sliderRect.height);
    }
}