import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class GamepadSnapshot {
    private static final GamepadSnapshot DISCONNECTED = new GamepadSnapshot(false,
            0, 0, 0, 0, 0, 0, EnumSet.noneOf(GamepadButton.class));

    private final boolean connected;
    private final double leftStickX;
    private final double leftStickY;
    private final double rightStickX;
    private final double rightStickY;
    private final double leftTrigger;
    private final double rightTrigger;
    private final Set<GamepadButton> buttonsDown;

    public GamepadSnapshot(boolean connected,
            double leftStickX, double leftStickY,
            double rightStickX, double rightStickY,
            double leftTrigger, double rightTrigger,
            Set<GamepadButton> buttonsDown) {
        this.connected = connected;
        this.leftStickX = leftStickX;
        this.leftStickY = leftStickY;
        this.rightStickX = rightStickX;
        this.rightStickY = rightStickY;
        this.leftTrigger = leftTrigger;
        this.rightTrigger = rightTrigger;
        EnumSet<GamepadButton> copy = buttonsDown.isEmpty()
                ? EnumSet.noneOf(GamepadButton.class)
                : EnumSet.copyOf(buttonsDown);
        this.buttonsDown = Collections.unmodifiableSet(copy);
    }

    public static GamepadSnapshot disconnected() {
        return DISCONNECTED;
    }

    public static GamepadSnapshot neutralConnected() {
        return new GamepadSnapshot(true, 0, 0, 0, 0, 0, 0,
                EnumSet.noneOf(GamepadButton.class));
    }

    public boolean connected() {
        return connected;
    }

    public double leftStickX() {
        return leftStickX;
    }

    public double leftStickY() {
        return leftStickY;
    }

    public double rightStickX() {
        return rightStickX;
    }

    public double rightStickY() {
        return rightStickY;
    }

    public boolean isDown(GamepadButton button, double triggerThreshold) {
        if (!connected) {
            return false;
        }
        return switch (button) {
            case LT -> leftTrigger >= triggerThreshold;
            case RT -> rightTrigger >= triggerThreshold;
            default -> buttonsDown.contains(button);
        };
    }

}
