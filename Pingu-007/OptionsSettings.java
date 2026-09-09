/** Narrow settings port used by OptionsModel. */
public interface OptionsSettings {

    float getMusicVolume();

    void setMusicVolume(float volume);

    float getSfxVolume();

    void setSfxVolume(float volume);

    int getTargetFps();

    void setTargetFps(int fps);

    boolean isAntiAliasingEnabled();

    void toggleAntiAliasing();

    boolean isShowFpsCounter();

    void toggleFpsCounter();

    boolean isRenderShadows();

    void setRenderShadows(boolean enabled);

    boolean isSoftShadows();

    void setSoftShadows(boolean enabled);

    void toggleFullscreen();
}
