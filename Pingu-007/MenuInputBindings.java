import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps a screen's accepted physical inputs to logical menu intents. */
public final class MenuInputBindings {

    private final Map<MenuInputIntent, List<Integer>> keys;
    private final Map<MenuInputIntent, List<InputManager.GamepadButton>> buttons;

    private MenuInputBindings(Builder builder) {
        keys = copyMap(builder.keys);
        buttons = copyMap(builder.buttons);
    }

    private static <T> Map<MenuInputIntent, List<T>> copyMap(
            Map<MenuInputIntent, List<T>> source) {
        EnumMap<MenuInputIntent, List<T>> copy = new EnumMap<>(MenuInputIntent.class);
        for (MenuInputIntent intent : MenuInputIntent.values()) {
            copy.put(intent, List.copyOf(source.getOrDefault(intent, List.of())));
        }
        return Map.copyOf(copy);
    }

    public boolean isJustPressed(MenuInputIntent intent, InputManager input) {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(input, "input");

        for (int keyCode : keys.getOrDefault(intent, List.of())) {
            if (input.isKeyJustPressed(keyCode)) {
                return true;
            }
        }
        for (InputManager.GamepadButton button : buttons.getOrDefault(intent, List.of())) {
            if (input.isButtonJustPressed(button)) {
                return true;
            }
        }
        return false;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MenuInputBindings gamepadMenu() {
        return builder()
                .button(MenuInputIntent.UP, InputManager.GamepadButton.DPAD_UP)
                .button(MenuInputIntent.DOWN, InputManager.GamepadButton.DPAD_DOWN)
                .button(MenuInputIntent.CONFIRM, InputManager.GamepadButton.A)
                .build();
    }

    public static MenuInputBindings pauseMenu() {
        return builder()
                .button(MenuInputIntent.UP, InputManager.GamepadButton.DPAD_UP)
                .button(MenuInputIntent.DOWN, InputManager.GamepadButton.DPAD_DOWN)
                .button(MenuInputIntent.CONFIRM, InputManager.GamepadButton.A)
                .key(MenuInputIntent.BACK, KeyEvent.VK_ESCAPE)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.B)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.START)
                .build();
    }

    public static MenuInputBindings optionsMenu() {
        return builder()
                .key(MenuInputIntent.UP, KeyEvent.VK_UP)
                .button(MenuInputIntent.UP, InputManager.GamepadButton.DPAD_UP)
                .key(MenuInputIntent.DOWN, KeyEvent.VK_DOWN)
                .button(MenuInputIntent.DOWN, InputManager.GamepadButton.DPAD_DOWN)
                .key(MenuInputIntent.LEFT, KeyEvent.VK_LEFT)
                .button(MenuInputIntent.LEFT, InputManager.GamepadButton.DPAD_LEFT)
                .key(MenuInputIntent.RIGHT, KeyEvent.VK_RIGHT)
                .button(MenuInputIntent.RIGHT, InputManager.GamepadButton.DPAD_RIGHT)
                .button(MenuInputIntent.CONFIRM, InputManager.GamepadButton.A)
                .key(MenuInputIntent.BACK, KeyEvent.VK_ESCAPE)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.B)
                .build();
    }

    public static MenuInputBindings keyBindingsMenu() {
        return builder()
                .button(MenuInputIntent.UP, InputManager.GamepadButton.DPAD_UP)
                .button(MenuInputIntent.DOWN, InputManager.GamepadButton.DPAD_DOWN)
                .button(MenuInputIntent.LEFT, InputManager.GamepadButton.DPAD_LEFT)
                .button(MenuInputIntent.RIGHT, InputManager.GamepadButton.DPAD_RIGHT)
                .button(MenuInputIntent.CONFIRM, InputManager.GamepadButton.A)
                .key(MenuInputIntent.BACK, KeyEvent.VK_ESCAPE)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.B)
                .build();
    }

    public static MenuInputBindings shopMenu() {
        return builder()
                .key(MenuInputIntent.UP, KeyEvent.VK_W)
                .key(MenuInputIntent.UP, KeyEvent.VK_UP)
                .button(MenuInputIntent.UP, InputManager.GamepadButton.DPAD_UP)
                .key(MenuInputIntent.DOWN, KeyEvent.VK_S)
                .key(MenuInputIntent.DOWN, KeyEvent.VK_DOWN)
                .button(MenuInputIntent.DOWN, InputManager.GamepadButton.DPAD_DOWN)
                .key(MenuInputIntent.LEFT, KeyEvent.VK_A)
                .key(MenuInputIntent.LEFT, KeyEvent.VK_LEFT)
                .button(MenuInputIntent.LEFT, InputManager.GamepadButton.DPAD_LEFT)
                .key(MenuInputIntent.RIGHT, KeyEvent.VK_D)
                .key(MenuInputIntent.RIGHT, KeyEvent.VK_RIGHT)
                .button(MenuInputIntent.RIGHT, InputManager.GamepadButton.DPAD_RIGHT)
                .key(MenuInputIntent.CONFIRM, KeyEvent.VK_ENTER)
                .key(MenuInputIntent.CONFIRM, KeyEvent.VK_SPACE)
                .button(MenuInputIntent.CONFIRM, InputManager.GamepadButton.A)
                .key(MenuInputIntent.BACK, KeyEvent.VK_ESCAPE)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.B)
                .button(MenuInputIntent.BACK, InputManager.GamepadButton.START)
                .build();
    }

    public static final class Builder {
        private final EnumMap<MenuInputIntent, List<Integer>> keys = new EnumMap<>(MenuInputIntent.class);
        private final EnumMap<MenuInputIntent, List<InputManager.GamepadButton>> buttons = new EnumMap<>(
                MenuInputIntent.class);

        public Builder key(MenuInputIntent intent, int keyCode) {
            keys.computeIfAbsent(Objects.requireNonNull(intent, "intent"), ignored -> new ArrayList<>())
                    .add(keyCode);
            return this;
        }

        public Builder button(MenuInputIntent intent, InputManager.GamepadButton button) {
            buttons.computeIfAbsent(Objects.requireNonNull(intent, "intent"), ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(button, "button"));
            return this;
        }

        public MenuInputBindings build() {
            return new MenuInputBindings(this);
        }
    }
}
