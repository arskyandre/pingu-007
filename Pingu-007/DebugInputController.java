public final class DebugInputController {
    private final InputFrame frame;

    public DebugInputController(InputFrame frame) {
        this.frame = frame;
    }

    public boolean isDown(InputAction action) {
        return frame.isDown(action);
    }

    public boolean wasPressed(InputAction action) {
        return frame.wasPressed(action);
    }

    public double pointerWorldX(CameraManager camera) {
        return frame.pointer().x() / camera.getZoom() + camera.getX();
    }

    public double pointerWorldY(CameraManager camera) {
        return frame.pointer().y() / camera.getZoom() + camera.getY();
    }
}
