import java.util.Objects;

/** Settings behavior and synchronization kept independent from OptionsMenu rendering. */
public final class OptionsModel {

    public enum ShadowMode {
        OFF,
        SHARP,
        SOFT
    }

    public static final int MIN_FPS = 30;
    public static final int MAX_FPS = 240;

    private final OptionsSettings settings;

    private float previousMusicVolume;
    private float previousSfxVolume;
    private boolean musicMuted;
    private boolean sfxMuted;
    private int previousFpsLimit = 120;
    private boolean fpsUnlimited;

    public OptionsModel(OptionsSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        previousMusicVolume = clampVolume(settings.getMusicVolume());
        previousSfxVolume = clampVolume(settings.getSfxVolume());
        musicMuted = previousMusicVolume <= 0f;
        sfxMuted = previousSfxVolume <= 0f;
        synchronizeFps();
    }

    public void onEnter() {
        synchronizeVolumes();
        synchronizeFps();
    }

    public void synchronize() {
        synchronizeVolumes();
        synchronizeFps();
    }

    private void synchronizeVolumes() {
        float musicVolume = clampVolume(settings.getMusicVolume());
        float sfxVolume = clampVolume(settings.getSfxVolume());
        if (musicVolume <= 0f) {
            musicMuted = true;
        } else {
            musicMuted = false;
        }
        if (sfxVolume <= 0f) {
            sfxMuted = true;
        } else {
            sfxMuted = false;
        }
    }

    private void synchronizeFps() {
        int targetFps = settings.getTargetFps();
        fpsUnlimited = targetFps == 0;
        if (!fpsUnlimited) {
            previousFpsLimit = clampFps(targetFps);
        }
    }

    public float musicVolume() {
        return clampVolume(settings.getMusicVolume());
    }

    public float sfxVolume() {
        return clampVolume(settings.getSfxVolume());
    }

    public boolean musicMuted() {
        return musicMuted;
    }

    public boolean sfxMuted() {
        return sfxMuted;
    }

    public void setMusicVolume(float volume) {
        float clamped = clampVolume(volume);
        settings.setMusicVolume(clamped);
        musicMuted = clamped <= 0f;
    }

    public void setSfxVolume(float volume) {
        float clamped = clampVolume(volume);
        settings.setSfxVolume(clamped);
        sfxMuted = clamped <= 0f;
    }

    public void adjustMusicVolume(float delta) {
        setMusicVolume(musicVolume() + delta);
    }

    public void adjustSfxVolume(float delta) {
        setSfxVolume(sfxVolume() + delta);
    }

    public void toggleMusicMute() {
        if (!musicMuted) {
            previousMusicVolume = musicVolume();
            setMusicVolume(0f);
        } else {
            setMusicVolume(previousMusicVolume);
            musicMuted = false;
        }
    }

    public void toggleSfxMute() {
        if (!sfxMuted) {
            previousSfxVolume = sfxVolume();
            setSfxVolume(0f);
        } else {
            setSfxVolume(previousSfxVolume);
            sfxMuted = false;
        }
    }

    public int fpsFromSliderValue(float value) {
        return Math.round(MIN_FPS + clampVolume(value) * (MAX_FPS - MIN_FPS));
    }

    public float sliderValueFromFps(int fps) {
        return clampVolume((clampFps(fps) - MIN_FPS) / (float) (MAX_FPS - MIN_FPS));
    }

    public float fpsSliderValue() {
        return sliderValueFromFps(fpsUnlimited ? previousFpsLimit : settings.getTargetFps());
    }

    public int currentCappedFps() {
        return fpsUnlimited ? previousFpsLimit : clampFps(settings.getTargetFps());
    }

    public boolean fpsUnlimited() {
        return fpsUnlimited;
    }

    public void setFpsFromSliderValue(float value) {
        defineFpsLimit(fpsFromSliderValue(value));
    }

    public void adjustFpsByDirection(int direction) {
        int step = direction < 0 ? -5 : 5;
        defineFpsLimit(currentCappedFps() + step);
    }

    private void defineFpsLimit(int fps) {
        previousFpsLimit = clampFps(fps);
        fpsUnlimited = false;
        settings.setTargetFps(previousFpsLimit);
    }

    public void toggleUnlimitedFps(float currentSliderValue) {
        if (!fpsUnlimited) {
            previousFpsLimit = clampFps(fpsFromSliderValue(currentSliderValue));
            fpsUnlimited = true;
            settings.setTargetFps(0);
        } else {
            fpsUnlimited = false;
            settings.setTargetFps(previousFpsLimit);
        }
    }

    public String fpsLabel() {
        return fpsUnlimited
                ? "LIMITE DE FPS: ILIMITADO"
                : "LIMITE DE FPS: " + fpsFromSliderValue(fpsSliderValue());
    }

    public ShadowMode shadowMode() {
        if (!settings.isRenderShadows()) {
            return ShadowMode.OFF;
        }
        return settings.isSoftShadows() ? ShadowMode.SOFT : ShadowMode.SHARP;
    }

    public void setShadowMode(ShadowMode mode) {
        switch (mode) {
            case OFF -> settings.setRenderShadows(false);
            case SHARP -> {
                settings.setRenderShadows(true);
                settings.setSoftShadows(false);
            }
            case SOFT -> {
                settings.setRenderShadows(true);
                settings.setSoftShadows(true);
            }
        }
    }

    public boolean isAntiAliasingEnabled() {
        return settings.isAntiAliasingEnabled();
    }

    public void toggleAntiAliasing() {
        settings.toggleAntiAliasing();
    }

    public boolean isShowFpsCounter() {
        return settings.isShowFpsCounter();
    }

    public void toggleFpsCounter() {
        settings.toggleFpsCounter();
    }

    public void toggleFullscreen() {
        settings.toggleFullscreen();
    }

    private static float clampVolume(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    private static int clampFps(int fps) {
        return Math.clamp(fps, MIN_FPS, MAX_FPS);
    }
}
