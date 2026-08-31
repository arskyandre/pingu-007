import java.awt.event.MouseEvent;

public final class PointerActivityTracker {
    public static final double MOVEMENT_THRESHOLD = 24.0;

    private boolean locked;
    private int referenceX;
    private int referenceY;

    public void lockPointer(int mouseX, int mouseY) {
        if (locked) {
            return;
        }
        locked = true;
        referenceX = mouseX;
        referenceY = mouseY;
    }

    public void onPointerMoved(int mouseX, int mouseY) {
        if (!locked) {
            return;
        }
        if (Math.hypot(mouseX - referenceX, mouseY - referenceY) >= MOVEMENT_THRESHOLD) {
            locked = false;
        }
    }

    public void onPointerPressed(int button) {
        if (locked && (button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3)) {
            locked = false;
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        locked = false;
    }
}
