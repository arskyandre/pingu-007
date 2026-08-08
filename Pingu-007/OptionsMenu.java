import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class OptionsMenu {

    private final SoundManager soundManager;
    private GameState returnTo;

    // ---- Layout constants: the ONLY numbers you should ever need to tweak ----
    private static final int SLIDER_W = 320;
    private static final int SLIDER_H = 10;
    private static final int BTN_SIZE = 36;
    private static final int ROW_GAP = 34;
    private static final int LABEL_TO_SLIDER_GAP = 22;
    private static final int SLIDER_TO_BTN_GAP = 20;
    private static final int BTN_TO_PCT_GAP = 16;
    private static final int BUTTON_ROW_GAP = 16;
    private static final int CORNER_MARGIN = 32;
    private static final int TITLE_Y_FRACTION = 8;
    private static final int CONTENT_START_FRACTION = 3;

    private static final int MIN_FPS = 30;
    private static final int MAX_FPS = 240;

    private enum ActiveSlider {
        MUSIC, SFX, FPS_CAP
    }

    private ActiveSlider activeSlider = ActiveSlider.MUSIC;
    private boolean fpsSliderInitialized = false;

    private float previousMusicVolume;
    private float previousSfxVolume;
    private boolean musicMuted = false;
    private boolean sfxMuted = false;

    private final MenuSlider musicSlider;
    private final MenuSlider sfxSlider;
    private final MenuSlider fpsCapSlider;
    private final IconButton toggleMuteBGM;
    private final IconButton toggleMuteSFX;
    private final MenuButton backBtn;
    private final MenuButton keyBindBtn;
    private final IconButton fullScreenButton;
    private final IconButton showFpsButton;
    private final IconButton enableAAButton;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public OptionsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        this.previousMusicVolume = soundManager.getMusicVolume();
        this.previousSfxVolume = soundManager.getSfxVolume();

        musicSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getMusicVolume());
        sfxSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getSfxVolume());
        fpsCapSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, 0.5f);

        toggleMuteBGM = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleMuteSFX = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        fullScreenButton = new IconButton(0, 0, BTN_SIZE, IconIndex.FULLSCREEN, false);
        showFpsButton = new IconButton(0, 0, BTN_SIZE, IconIndex.RED_X, false);
        enableAAButton = new IconButton(0, 0, BTN_SIZE, IconIndex.GREEN_CHECK, false);

        backBtn = new MenuButton("VOLTAR", 0, 0, 160, 46);
        keyBindBtn = new MenuButton("CONSULTAR TECLAS", 0, 0, 200, 46);

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

    // ---------------- Layout ----------------

    private static class LayoutCursor {
        int y;
        final int gap;

        LayoutCursor(int startY, int gap) {
            this.y = startY;
            this.gap = gap;
        }

        int nextRow(int rowHeight) {
            int rowY = y;
            y += rowHeight + gap;
            return rowY;
        }
    }

    public void repositionElements(int width, int height, GameCore GC) {
        int centerX = width / 2;
        LayoutCursor cursor = new LayoutCursor(height / CONTENT_START_FRACTION, ROW_GAP);
        int sliderRowHeight = LABEL_TO_SLIDER_GAP + SLIDER_H;

        int musicY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(musicSlider, toggleMuteBGM, centerX, musicY);

        int sfxY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(sfxSlider, toggleMuteSFX, centerX, sfxY);

        int fpsCapY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(fpsCapSlider, null, centerX, fpsCapY);

        int fpsToggleY = cursor.nextRow(BTN_SIZE);
        showFpsButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, fpsToggleY);

        int AATogleY = cursor.nextRow(BTN_SIZE);
        enableAAButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, AATogleY);

        int buttonsY = cursor.nextRow(46);
        layoutButtonRowCentered(buttonsY, centerX, keyBindBtn, backBtn);

        fullScreenButton.setPosition(width - CORNER_MARGIN - BTN_SIZE, height - CORNER_MARGIN - BTN_SIZE);
    }

    private void positionSliderRow(MenuSlider slider, IconButton muteButton, int centerX, int y) {
        slider.setPosition(centerX - SLIDER_W / 2, y);
        if (muteButton != null) {
            muteButton.setPosition(
                    slider.getRightX() + SLIDER_TO_BTN_GAP,
                    slider.getCenterY() - BTN_SIZE / 2);
        }
    }

    private void layoutButtonRowCentered(int y, int centerX, MenuButton... buttons) {
        int totalWidth = 0;
        for (MenuButton b : buttons) {
            totalWidth += b.getRect().width;
        }
        totalWidth += BUTTON_ROW_GAP * (buttons.length - 1);

        int x = centerX - totalWidth / 2;
        for (MenuButton b : buttons) {
            b.setPosition(x, y);
            x += b.getRect().width + BUTTON_ROW_GAP;
        }
    }

    public GameState update(InputManager input, int width, int height, GameCore GC) {
        repositionElements(width, height, GC);

        if (!fpsSliderInitialized) {
            fpsCapSlider.setValue(sliderValueFromFps(GC.getTargetFps()));
            fpsSliderInitialized = true;
        }

        if (GC.isAntiAliasingEnabled()) {
            enableAAButton.setIcon(IconIndex.GREEN_CHECK);
        } else
            enableAAButton.setIcon(IconIndex.RED_X);

        if (GC.isShowFpsCounter()) {
            showFpsButton.setIcon(IconIndex.GREEN_CHECK);
        } else {
            showFpsButton.setIcon(IconIndex.RED_X);
        }

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE) || input.isButtonJustPressed(InputManager.GamepadButton.B)) {
            return returnTo;
        }

        updateMusicSlider(input);
        updateSfxSlider(input);
        updateFpsCapSlider(input, GC);
        updateArrowKeyNavigation(input, GC);

        if (toggleMuteBGM.update(input) == IconButton.CLICKED) {
            activeSlider = ActiveSlider.MUSIC;
            toggleMusicMute();
        }

        if (toggleMuteSFX.update(input) == IconButton.CLICKED) {
            activeSlider = ActiveSlider.SFX;
            toggleSfxMute();
        }

        if (fullScreenButton.update(input) == IconButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            GC.toggleFullscreen();
        }

        if (showFpsButton.update(input) == IconButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            GC.toggleFpsCounter();
        }

        if (enableAAButton.update(input) == IconButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            GC.toggleAntiAliasing();
        }

        if (backBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return returnTo;
        }

        if (keyBindBtn.update(input) == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.KEYBINDINGS;
        }

        return GameState.OPTIONS;
    }

    private void updateMusicSlider(InputManager input) {
        int state = musicSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            activeSlider = ActiveSlider.MUSIC;
            if (state == MenuSlider.CLICKED)
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
    }

    private void updateSfxSlider(InputManager input) {
        int state = sfxSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            activeSlider = ActiveSlider.SFX;
            if (state == MenuSlider.CLICKED)
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
    }

    private void updateFpsCapSlider(InputManager input, GameCore GC) {
        int state = fpsCapSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            activeSlider = ActiveSlider.FPS_CAP;
            if (state == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            GC.setTargetFps(fpsFromSliderValue(fpsCapSlider.getValue()));
        }
    }

    private int fpsFromSliderValue(float value) {
        return Math.round(MIN_FPS + value * (MAX_FPS - MIN_FPS));
    }

    private float sliderValueFromFps(int fps) {
        return Math.clamp((fps - MIN_FPS) / (float) (MAX_FPS - MIN_FPS), 0f, 1f);
    }

    private void updateArrowKeyNavigation(InputManager input, GameCore GC) {
        if (input.isKeyJustPressed(KeyEvent.VK_UP)) {
            moveActiveSlider(-1);
        }
        if (input.isKeyJustPressed(KeyEvent.VK_DOWN)) {
            moveActiveSlider(1);
        }

        boolean left = input.isKeyJustPressed(KeyEvent.VK_LEFT);
        boolean right = input.isKeyJustPressed(KeyEvent.VK_RIGHT);
        if (!left && !right)
            return;

        float delta = left ? -0.05f : 0.05f;

        switch (activeSlider) {
            case MUSIC -> {
                float v = Math.clamp(soundManager.getMusicVolume() + delta, 0f, 1f);
                soundManager.setMusicVolume(v);
                musicSlider.setValue(v);
            }
            case SFX -> {
                float v = Math.clamp(soundManager.getSfxVolume() + delta, 0f, 1f);
                soundManager.setSfxVolume(v);
                sfxSlider.setValue(v);
            }
            case FPS_CAP -> {
                float v = Math.clamp(fpsCapSlider.getValue() + delta, 0f, 1f);
                fpsCapSlider.setValue(v);
                GC.setTargetFps(fpsFromSliderValue(v));
            }
        }
    }

    private void moveActiveSlider(int direction) {
        ActiveSlider[] values = ActiveSlider.values();
        int idx = Math.max(0, Math.min(activeSlider.ordinal() + direction, values.length - 1));
        activeSlider = values[idx];
    }

    private void toggleMusicMute() {
        if (!musicMuted) {
            previousMusicVolume = soundManager.getMusicVolume();
            soundManager.setMusicVolume(0f);
            musicSlider.setValue(0f);
            toggleMuteBGM.setIcon(IconIndex.MUTED);
            musicMuted = true;
        } else {
            soundManager.setMusicVolume(previousMusicVolume);
            musicSlider.setValue(previousMusicVolume);
            toggleMuteBGM.setIcon(IconIndex.UNMUTED);
            musicMuted = false;
        }
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
    }

    private void toggleSfxMute() {
        if (!sfxMuted) {
            previousSfxVolume = soundManager.getSfxVolume();
            soundManager.setSfxVolume(0f);
            sfxSlider.setValue(0f);
            toggleMuteSFX.setIcon(IconIndex.MUTED);
            sfxMuted = true;
        } else {
            soundManager.setSfxVolume(previousSfxVolume);
            sfxSlider.setValue(previousSfxVolume);
            toggleMuteSFX.setIcon(IconIndex.UNMUTED);
            sfxMuted = false;
        }
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(0, 0, width, height);

        g2.setFont(pixelFont);
        String title = "OPÇÕES";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, height / TITLE_Y_FRACTION + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / TITLE_Y_FRACTION);

        drawSliderRow(g2, "VOLUME DA MÚSICA", musicSlider, activeSlider == ActiveSlider.MUSIC, width, true, true);
        drawSliderRow(g2, "VOLUME DOS EFEITOS", sfxSlider, activeSlider == ActiveSlider.SFX, width, true, true);
        drawSliderRow(g2, "LIMITE DE FPS: " + fpsFromSliderValue(fpsCapSlider.getValue()),
                fpsCapSlider, activeSlider == ActiveSlider.FPS_CAP, width, false, false);

        drawLabelLeftOf(g2, "MOSTRAR FPS", showFpsButton.getRect());
        drawLabelLeftOf(g2, "Habilitar Anti-Aliasing", enableAAButton.getRect());
        enableAAButton.draw(g2);
        toggleMuteBGM.draw(g2);
        toggleMuteSFX.draw(g2);
        showFpsButton.draw(g2);
        keyBindBtn.draw(g2);
        backBtn.draw(g2);
        fullScreenButton.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawSliderRow(Graphics2D g2, String label, MenuSlider slider, boolean highlighted,
            int width, boolean hasButton, boolean drawPercentage) {
        Rectangle r = slider.getRect();

        g2.setFont(pixelFontSmall);
        g2.setColor(highlighted ? Color.WHITE : new Color(180, 180, 180));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (width - fm.stringWidth(label)) / 2, r.y - LABEL_TO_SLIDER_GAP + fm.getAscent());

        slider.draw(g2);
        if (drawPercentage) {
            g2.setFont(pixelFontTiny);
            g2.setColor(highlighted ? Color.WHITE : new Color(160, 160, 160));
            String pct = (int) (slider.getValue() * 100) + "%";
            int pctX = r.x + r.width + SLIDER_TO_BTN_GAP + (hasButton ? BTN_SIZE + BTN_TO_PCT_GAP : BTN_TO_PCT_GAP);
            g2.drawString(pct, pctX, r.y + r.height);
        }
    }

    private void drawLabelLeftOf(Graphics2D g2, String text, Rectangle anchorRect) {
        g2.setFont(pixelFontSmall);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int x = anchorRect.x - tw - SLIDER_TO_BTN_GAP;
        int y = anchorRect.y + (anchorRect.height + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(text, x + 2, y + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }
}