public final class MenuInputController {
    private final InputManager inputManager;
    private final InputFrame frame;

    public MenuInputController(InputManager inputManager) {
        this.inputManager = inputManager;
        this.frame = inputManager.frame();
    }

    public boolean navigate(InputAction action) {
        if (!frame.wasPressed(action)) {
            return false;
        }
        inputManager.lockPointer();
        return true;
    }

    public boolean wasPressed(InputAction action) {
        return frame.wasPressed(action);
    }

    public PointerSnapshot pointer() {
        if (inputManager.isMouseBloqueado() || usesController()) {
            return new PointerSnapshot(frame.raw(), false);
        }
        return frame.pointer();
    }

    public boolean usesController() {
        return frame.activeDevice() == InputDevice.CONTROLLER;
    }
}
