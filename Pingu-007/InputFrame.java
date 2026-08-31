import java.util.EnumMap;
import java.util.Map;

public final class InputFrame {
    private final RawInputSnapshot raw;
    private final InputContext context;
    private final InputDevice activeDevice;
    private final PointerSnapshot pointer;
    private final Vetor2D moveAxis;
    private final Vetor2D aimAxis;
    private final Map<InputAction, ActionState> actions;

    public InputFrame(RawInputSnapshot raw, InputContext context,
            InputDevice activeDevice, PointerSnapshot pointer,
            Vetor2D moveAxis, Vetor2D aimAxis,
            Map<InputAction, ActionState> actions) {
        this.raw = raw;
        this.context = context;
        this.activeDevice = activeDevice;
        this.pointer = pointer;
        this.moveAxis = new Vetor2D(moveAxis.x, moveAxis.y);
        this.aimAxis = new Vetor2D(aimAxis.x, aimAxis.y);
        this.actions = new EnumMap<>(actions);
    }

    public boolean isDown(InputAction action) {
        return state(action).down();
    }

    public boolean wasPressed(InputAction action) {
        return state(action).pressed();
    }

    public boolean wasReleased(InputAction action) {
        return state(action).released();
    }

    public InputContext context() {
        return context;
    }

    public InputDevice activeDevice() {
        return activeDevice;
    }

    public boolean isControllerActive() {
        return activeDevice == InputDevice.CONTROLLER;
    }

    public boolean isControllerConnected() {
        return raw.gamepad().connected();
    }

    public PointerSnapshot pointer() {
        return pointer;
    }

    public Vetor2D moveAxis() {
        return new Vetor2D(moveAxis.x, moveAxis.y);
    }

    public Vetor2D aimAxis() {
        return new Vetor2D(aimAxis.x, aimAxis.y);
    }

    public ControllerConnectionEvent connectionEvent() {
        return raw.connectionEvent();
    }

    RawInputSnapshot raw() {
        return raw;
    }

    private ActionState state(InputAction action) {
        return actions.getOrDefault(action, ActionState.IDLE);
    }
}
