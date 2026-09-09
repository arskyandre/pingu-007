import java.util.Objects;

/** Dependencies that a menu needs during one update. */
public final class MenuContext {

    private final InputManager input;
    private final SoundManager soundManager;
    private final MenuViewport viewport;

    public MenuContext(InputManager input, SoundManager soundManager, MenuViewport viewport) {
        this.input = Objects.requireNonNull(input, "input");
        this.soundManager = Objects.requireNonNull(soundManager, "soundManager");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    public InputManager input() {
        return input;
    }

    public InputManager getInput() {
        return input;
    }

    public SoundManager soundManager() {
        return soundManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public MenuViewport viewport() {
        return viewport;
    }

    public MenuViewport getViewport() {
        return viewport;
    }
}
