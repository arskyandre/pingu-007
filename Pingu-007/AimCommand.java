public final class AimCommand {
    private final InputDevice device;
    private final Vetor2D controllerAxis;
    private final int pointerX;
    private final int pointerY;
    private final boolean pointerLocked;
    private final boolean mouseWorldTargetAvailable;
    private final double mouseWorldX;
    private final double mouseWorldY;

    public AimCommand(InputDevice device, Vetor2D controllerAxis,
            int pointerX, int pointerY, boolean pointerLocked,
            boolean mouseWorldTargetAvailable, double mouseWorldX, double mouseWorldY) {
        this.device = device;
        this.controllerAxis = new Vetor2D(controllerAxis.x, controllerAxis.y);
        this.pointerX = pointerX;
        this.pointerY = pointerY;
        this.pointerLocked = pointerLocked;
        this.mouseWorldTargetAvailable = mouseWorldTargetAvailable;
        this.mouseWorldX = mouseWorldX;
        this.mouseWorldY = mouseWorldY;
    }

    public InputDevice device() {
        return device;
    }

    public boolean usesController() {
        return device == InputDevice.CONTROLLER;
    }

    public Vetor2D controllerAxis() {
        return new Vetor2D(controllerAxis.x, controllerAxis.y);
    }

    public int pointerX() {
        return pointerX;
    }

    public int pointerY() {
        return pointerY;
    }

    public boolean pointerLocked() {
        return pointerLocked;
    }

    public boolean hasMouseWorldTarget() {
        return mouseWorldTargetAvailable;
    }

    public double mouseWorldX() {
        return mouseWorldX;
    }

    public double mouseWorldY() {
        return mouseWorldY;
    }
}
