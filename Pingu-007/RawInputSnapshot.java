import java.util.BitSet;

public final class RawInputSnapshot {
    private final BitSet keysDown;
    private final BitSet keysPressed;
    private final BitSet keysReleased;
    private final BitSet mouseDown;
    private final BitSet mousePressed;
    private final BitSet mouseReleased;
    private final int mouseX;
    private final int mouseY;
    private final boolean focused;
    private final GamepadSnapshot gamepad;
    private final GamepadSnapshot previousGamepad;
    private final ControllerConnectionEvent connectionEvent;

    public RawInputSnapshot(BitSet keysDown, BitSet keysPressed, BitSet keysReleased,
            BitSet mouseDown, BitSet mousePressed, BitSet mouseReleased,
            int mouseX, int mouseY, boolean focused,
            GamepadSnapshot gamepad, GamepadSnapshot previousGamepad,
            ControllerConnectionEvent connectionEvent) {
        this.keysDown = (BitSet) keysDown.clone();
        this.keysPressed = (BitSet) keysPressed.clone();
        this.keysReleased = (BitSet) keysReleased.clone();
        this.mouseDown = (BitSet) mouseDown.clone();
        this.mousePressed = (BitSet) mousePressed.clone();
        this.mouseReleased = (BitSet) mouseReleased.clone();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.focused = focused;
        this.gamepad = gamepad;
        this.previousGamepad = previousGamepad;
        this.connectionEvent = connectionEvent;
    }

    public boolean keyDown(int code) {
        return focused && code >= 0 && keysDown.get(code);
    }

    public boolean keyPressed(int code) {
        return focused && code >= 0 && keysPressed.get(code);
    }

    public boolean keyReleased(int code) {
        return code >= 0 && keysReleased.get(code);
    }

    public boolean mouseDown(int button) {
        return focused && button >= 0 && mouseDown.get(button);
    }

    public boolean mousePressed(int button) {
        return focused && button >= 0 && mousePressed.get(button);
    }

    public boolean mouseReleased(int button) {
        return button >= 0 && mouseReleased.get(button);
    }

    public boolean gamepadDown(GamepadButton button, double triggerThreshold) {
        return focused && gamepad.isDown(button, triggerThreshold);
    }

    public boolean gamepadPressed(GamepadButton button, double triggerThreshold) {
        return focused && gamepad.isDown(button, triggerThreshold)
                && !previousGamepad.isDown(button, triggerThreshold);
    }

    public boolean gamepadReleased(GamepadButton button, double triggerThreshold) {
        return previousGamepad.isDown(button, triggerThreshold)
                && !gamepad.isDown(button, triggerThreshold);
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public boolean focused() {
        return focused;
    }

    public GamepadSnapshot gamepad() {
        return gamepad;
    }

    public ControllerConnectionEvent connectionEvent() {
        return connectionEvent;
    }
}
