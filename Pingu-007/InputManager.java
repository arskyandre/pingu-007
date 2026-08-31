import java.awt.event.MouseEvent;
import java.util.BitSet;

public final class InputManager {
    private static final int MAX_KEY_CODE = 256;
    private static final int MAX_MOUSE_BUTTON = 10;
    private static final double TRIGGER_THRESHOLD = 0.5;

    private final AwtInputCollector awtCollector;
    private final GamepadInputSource gamepadSource;
    private final PointerActivityTracker pointerTracker;
    private final ControllerPriorityPolicy controllerPriorityPolicy;
    private final InputBindings bindings;
    private final ContextTransitionGuard contextTransitionGuard;
    private final BitSet keysDown = new BitSet(MAX_KEY_CODE);
    private final BitSet mouseButtonsDown = new BitSet(MAX_MOUSE_BUTTON);
    private final BitSet suppressedKeysUntilRelease = new BitSet(MAX_KEY_CODE);
    private final BitSet suppressedMouseUntilRelease = new BitSet(MAX_MOUSE_BUTTON);

    private boolean focused = true;
    private boolean controllerNeutralRequired;
    private int mouseX;
    private int mouseY;
    private double leftDeadZone = 0.2;
    private double rightDeadZone = 0.4;
    private GamepadSnapshot previousRawGamepad;
    private GamepadSnapshot previousEffectiveGamepad;
    private InputFrame currentFrame;

    public InputManager() {
        this(new JamepadGamepadInputSource());
    }

    public InputManager(GamepadInputSource gamepadSource) {
        this.awtCollector = new AwtInputCollector();
        this.gamepadSource = gamepadSource;
        this.pointerTracker = new PointerActivityTracker();
        this.controllerPriorityPolicy = new ControllerPriorityPolicy();
        this.bindings = new InputBindings(TRIGGER_THRESHOLD);
        this.contextTransitionGuard = new ContextTransitionGuard(TRIGGER_THRESHOLD);
        this.previousRawGamepad = safePoll();
        this.previousEffectiveGamepad = previousRawGamepad;
        beginFrame(InputContext.MAIN_MENU);
    }

    public InputFrame beginFrame(InputContext context) {
        BitSet keysPressed = new BitSet(MAX_KEY_CODE);
        BitSet keysReleased = new BitSet(MAX_KEY_CODE);
        BitSet mousePressed = new BitSet(MAX_MOUSE_BUTTON);
        BitSet mouseReleased = new BitSet(MAX_MOUSE_BUTTON);

        for (RawInputEvent event : awtCollector.drainEvents()) {
            processEvent(event, keysPressed, keysReleased, mousePressed, mouseReleased);
        }

        GamepadSnapshot rawGamepad = safePoll();
        ControllerConnectionEvent connectionEvent = connectionEvent(previousRawGamepad, rawGamepad);
        if (connectionEvent == ControllerConnectionEvent.CONNECTED && isEngaged(rawGamepad)) {
            controllerNeutralRequired = true;
        } else if (connectionEvent == ControllerConnectionEvent.DISCONNECTED) {
            controllerNeutralRequired = false;
        }

        GamepadSnapshot effectiveGamepad = effectiveGamepad(rawGamepad);
        RawInputSnapshot raw = new RawInputSnapshot(
                keysDown, keysPressed, keysReleased,
                mouseButtonsDown, mousePressed, mouseReleased,
                mouseX, mouseY, focused,
                effectiveGamepad, previousEffectiveGamepad,
                connectionEvent);

        Vetor2D leftStick = filteredLeftStick(effectiveGamepad);
        Vetor2D rightStick = filteredRightStick(effectiveGamepad);
        InputDevice activeDevice = controllerPriorityPolicy.selectDevice(
                raw, leftStick, rightStick, TRIGGER_THRESHOLD);
        boolean pointerActive = focused && !pointerTracker.isLocked()
                && activeDevice == InputDevice.KEYBOARD_MOUSE;
        PointerSnapshot pointer = new PointerSnapshot(raw, pointerActive);
        java.util.EnumMap<InputAction, ActionState> actions =
                bindings.resolve(context, raw, activeDevice, pointerActive);
        contextTransitionGuard.apply(currentFrame, context, raw, activeDevice, actions);
        Vetor2D moveAxis = activeDevice == InputDevice.CONTROLLER
                ? leftStick : keyboardMovement(actions, context);
        Vetor2D aimAxis = activeDevice == InputDevice.CONTROLLER
                ? rightStick : new Vetor2D(0, 0);
        currentFrame = new InputFrame(raw, context, activeDevice, pointer,
                moveAxis, aimAxis, actions);
        previousRawGamepad = rawGamepad;
        previousEffectiveGamepad = effectiveGamepad;
        return currentFrame;
    }

    public AwtInputCollector getAwtCollector() {
        return awtCollector;
    }

    public InputFrame frame() {
        return currentFrame;
    }

    public PointerSnapshot pointer() {
        return currentFrame.pointer();
    }

    public PointerSnapshot presentationPointer() {
        return currentFrame.pointer().withPosition(awtCollector.latestPointerPosition());
    }

    public void lockPointer() {
        pointerTracker.lockPointer(mouseX, mouseY);
    }

    public boolean isMouseBloqueado() {
        return pointerTracker.isLocked();
    }

    public void resetMouseBloqueio() {
        pointerTracker.unlock();
    }

    public double getMouseBloqueioThreshold() {
        return PointerActivityTracker.MOVEMENT_THRESHOLD;
    }

    public void setDeadzoneEsquerda(double deadzone) {
        validateDeadZone(deadzone);
        leftDeadZone = deadzone;
    }

    public void setDeadzoneDireita(double deadzone) {
        validateDeadZone(deadzone);
        rightDeadZone = deadzone;
    }

    public double getDeadzoneEsquerda() {
        return leftDeadZone;
    }

    public double getDeadzoneDireita() {
        return rightDeadZone;
    }

    public void shutdown() {
        gamepadSource.close();
    }

    private void processEvent(RawInputEvent event, BitSet keysPressed, BitSet keysReleased,
            BitSet mousePressed, BitSet mouseReleased) {
        switch (event.type()) {
            case KEY_PRESSED -> processKeyPressed(event.code(), keysPressed);
            case KEY_RELEASED -> processKeyReleased(event.code(), keysReleased);
            case MOUSE_PRESSED -> {
                mouseX = event.x();
                mouseY = event.y();
                pointerTracker.onPointerPressed(event.code());
                processMousePressed(event.code(), mousePressed);
            }
            case MOUSE_RELEASED -> {
                mouseX = event.x();
                mouseY = event.y();
                processMouseReleased(event.code(), mouseReleased);
            }
            case MOUSE_MOVED -> {
                mouseX = event.x();
                mouseY = event.y();
                pointerTracker.onPointerMoved(mouseX, mouseY);
            }
            case FOCUS_GAINED -> focused = true;
            case FOCUS_LOST -> processFocusLost(keysReleased, mouseReleased);
        }
    }

    private void processKeyPressed(int keyCode, BitSet keysPressed) {
        if (!validKey(keyCode)) {
            return;
        }
        if (!focused || suppressedKeysUntilRelease.get(keyCode)) {
            suppressedKeysUntilRelease.set(keyCode);
            return;
        }
        if (!keysDown.get(keyCode)) {
            keysDown.set(keyCode);
            keysPressed.set(keyCode);
        }
    }

    private void processKeyReleased(int keyCode, BitSet keysReleased) {
        if (!validKey(keyCode)) {
            return;
        }
        suppressedKeysUntilRelease.clear(keyCode);
        if (keysDown.get(keyCode)) {
            keysDown.clear(keyCode);
            keysReleased.set(keyCode);
        }
    }

    private void processMousePressed(int button, BitSet mousePressed) {
        if (!validMouseButton(button)) {
            return;
        }
        if (!focused || suppressedMouseUntilRelease.get(button)) {
            suppressedMouseUntilRelease.set(button);
            return;
        }
        if (!mouseButtonsDown.get(button)) {
            mouseButtonsDown.set(button);
            mousePressed.set(button);
        }
    }

    private void processMouseReleased(int button, BitSet mouseReleased) {
        if (!validMouseButton(button)) {
            return;
        }
        suppressedMouseUntilRelease.clear(button);
        if (mouseButtonsDown.get(button)) {
            mouseButtonsDown.clear(button);
            mouseReleased.set(button);
        }
    }

    private void processFocusLost(BitSet keysReleased, BitSet mouseReleased) {
        focused = false;
        suppressedKeysUntilRelease.or(keysDown);
        suppressedMouseUntilRelease.or(mouseButtonsDown);
        keysReleased.or(keysDown);
        mouseReleased.or(mouseButtonsDown);
        keysDown.clear();
        mouseButtonsDown.clear();
        controllerNeutralRequired = true;
    }

    private GamepadSnapshot effectiveGamepad(GamepadSnapshot rawGamepad) {
        if (!rawGamepad.connected()) {
            return GamepadSnapshot.disconnected();
        }
        if (!focused) {
            if (isEngaged(rawGamepad)) {
                controllerNeutralRequired = true;
            }
            return GamepadSnapshot.neutralConnected();
        }
        if (controllerNeutralRequired) {
            if (isEngaged(rawGamepad)) {
                return GamepadSnapshot.neutralConnected();
            }
            controllerNeutralRequired = false;
        }
        return rawGamepad;
    }

    private boolean isEngaged(GamepadSnapshot gamepad) {
        if (!gamepad.connected()) {
            return false;
        }
        Vetor2D left = filteredLeftStick(gamepad);
        Vetor2D right = filteredRightStick(gamepad);
        if (left.x != 0 || left.y != 0 || right.x != 0 || right.y != 0) {
            return true;
        }
        for (GamepadButton button : GamepadButton.values()) {
            if (gamepad.isDown(button, TRIGGER_THRESHOLD)) {
                return true;
            }
        }
        return false;
    }

    private Vetor2D keyboardMovement(java.util.EnumMap<InputAction, ActionState> actions,
            InputContext context) {
        if (context != InputContext.PLAYING) {
            return new Vetor2D(0, 0);
        }
        double x = 0;
        double y = 0;
        if (actions.getOrDefault(InputAction.MOVE_RIGHT, ActionState.IDLE).down()) x += 1;
        if (actions.getOrDefault(InputAction.MOVE_LEFT, ActionState.IDLE).down()) x -= 1;
        if (actions.getOrDefault(InputAction.MOVE_DOWN, ActionState.IDLE).down()) y += 1;
        if (actions.getOrDefault(InputAction.MOVE_UP, ActionState.IDLE).down()) y -= 1;
        return new Vetor2D(x, y).partiallyNormalized();
    }

    private Vetor2D filteredLeftStick(GamepadSnapshot gamepad) {
        return applyRadialDeadZone(gamepad.leftStickX(), -gamepad.leftStickY(), leftDeadZone);
    }

    private Vetor2D filteredRightStick(GamepadSnapshot gamepad) {
        return applyRadialDeadZone(gamepad.rightStickX(), -gamepad.rightStickY(), rightDeadZone);
    }

    private Vetor2D applyRadialDeadZone(double x, double y, double deadZone) {
        double magnitude = Math.sqrt(x * x + y * y);
        if (magnitude <= deadZone) {
            return new Vetor2D(0, 0);
        }
        double limitedMagnitude = Math.min(magnitude, 1.0);
        double remappedMagnitude = (limitedMagnitude - deadZone) / (1.0 - deadZone);
        double scale = remappedMagnitude / magnitude;
        return new Vetor2D(x * scale, y * scale);
    }

    private ControllerConnectionEvent connectionEvent(GamepadSnapshot previous, GamepadSnapshot current) {
        if (!previous.connected() && current.connected()) return ControllerConnectionEvent.CONNECTED;
        if (previous.connected() && !current.connected()) return ControllerConnectionEvent.DISCONNECTED;
        return ControllerConnectionEvent.NONE;
    }

    private GamepadSnapshot safePoll() {
        GamepadSnapshot snapshot = gamepadSource.poll();
        return snapshot == null ? GamepadSnapshot.disconnected() : snapshot;
    }

    private static void validateDeadZone(double deadzone) {
        if (deadzone < 0.0 || deadzone >= 1.0) {
            throw new IllegalArgumentException("A deadzone deve estar no intervalo [0.0, 1.0).");
        }
    }

    private static boolean validKey(int code) {
        return code >= 0 && code < MAX_KEY_CODE;
    }

    private static boolean validMouseButton(int button) {
        return button > MouseEvent.NOBUTTON && button < MAX_MOUSE_BUTTON;
    }
}
