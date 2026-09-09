/** Production adapter from the options port to the existing game services. */
public final class GameOptionsSettings implements OptionsSettings {

    private final GameCore gameCore;
    private final SoundManager soundManager;

    public GameOptionsSettings(GameCore gameCore, SoundManager soundManager) {
        this.gameCore = gameCore;
        this.soundManager = soundManager;
    }

    @Override
    public float getMusicVolume() {
        return soundManager.getMusicVolume();
    }

    @Override
    public void setMusicVolume(float volume) {
        soundManager.setMusicVolume(volume);
    }

    @Override
    public float getSfxVolume() {
        return soundManager.getSfxVolume();
    }

    @Override
    public void setSfxVolume(float volume) {
        soundManager.setSfxVolume(volume);
    }

    @Override
    public int getTargetFps() {
        return gameCore.getTargetFps();
    }

    @Override
    public void setTargetFps(int fps) {
        gameCore.setTargetFps(fps);
    }

    @Override
    public boolean isAntiAliasingEnabled() {
        return gameCore.isAntiAliasingEnabled();
    }

    @Override
    public void toggleAntiAliasing() {
        gameCore.toggleAntiAliasing();
    }

    @Override
    public boolean isShowFpsCounter() {
        return gameCore.isShowFpsCounter();
    }

    @Override
    public void toggleFpsCounter() {
        gameCore.toggleFpsCounter();
    }

    @Override
    public boolean isRenderShadows() {
        return Renderer.isRenderShadows();
    }

    @Override
    public void setRenderShadows(boolean enabled) {
        Renderer.setRenderShadows(enabled);
    }

    @Override
    public boolean isSoftShadows() {
        return ProjectedShadow.isSoftShadows();
    }

    @Override
    public void setSoftShadows(boolean enabled) {
        ProjectedShadow.setSoftShadows(enabled);
    }

    @Override
    public void toggleFullscreen() {
        gameCore.toggleFullscreen();
    }
}
