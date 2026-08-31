public final class PointerSnapshot {
    private final RawInputSnapshot raw;
    private final int x;
    private final int y;
    private final boolean active;

    public PointerSnapshot(RawInputSnapshot raw, boolean active) {
        this(raw, raw.mouseX(), raw.mouseY(), active);
    }

    private PointerSnapshot(RawInputSnapshot raw, int x, int y, boolean active) {
        this.raw = raw;
        this.x = x;
        this.y = y;
        this.active = active;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDown(int button) {
        return active && raw.mouseDown(button);
    }

    public boolean wasPressed(int button) {
        return active && raw.mousePressed(button);
    }

    public boolean wasReleased(int button) {
        return active && raw.mouseReleased(button);
    }

    public PointerSnapshot withPosition(PointerPosition position) {
        return new PointerSnapshot(raw, position.x(), position.y(), active);
    }
}
