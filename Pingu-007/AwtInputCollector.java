import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.SwingUtilities;

public final class AwtInputCollector extends KeyAdapter
        implements MouseMotionListener, MouseListener, FocusListener {
    private final ConcurrentLinkedQueue<RawInputEvent> events = new ConcurrentLinkedQueue<>();
    private volatile PointerPosition latestPointerPosition = PointerPosition.ORIGIN;

    @Override
    public void keyPressed(KeyEvent event) {
        events.add(RawInputEvent.key(RawInputEvent.Type.KEY_PRESSED, event.getKeyCode()));
    }

    @Override
    public void keyReleased(KeyEvent event) {
        events.add(RawInputEvent.key(RawInputEvent.Type.KEY_RELEASED, event.getKeyCode()));
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        enqueueMousePosition(event);
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        enqueueMousePosition(event);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        updateLatestPointerPosition(event);
        events.add(RawInputEvent.mouse(RawInputEvent.Type.MOUSE_PRESSED,
                normalizedButton(event), event.getX(), event.getY()));
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        updateLatestPointerPosition(event);
        events.add(RawInputEvent.mouse(RawInputEvent.Type.MOUSE_RELEASED,
                normalizedButton(event), event.getX(), event.getY()));
    }

    @Override
    public void focusGained(FocusEvent event) {
        events.add(RawInputEvent.focus(RawInputEvent.Type.FOCUS_GAINED));
    }

    @Override
    public void focusLost(FocusEvent event) {
        events.add(RawInputEvent.focus(RawInputEvent.Type.FOCUS_LOST));
    }

    public List<RawInputEvent> drainEvents() {
        ArrayList<RawInputEvent> drained = new ArrayList<>();
        RawInputEvent event;
        while ((event = events.poll()) != null) {
            drained.add(event);
        }
        return drained;
    }

    public PointerPosition latestPointerPosition() {
        return latestPointerPosition;
    }

    private void enqueueMousePosition(MouseEvent event) {
        updateLatestPointerPosition(event);
        events.add(RawInputEvent.mouse(RawInputEvent.Type.MOUSE_MOVED,
                0, event.getX(), event.getY()));
    }

    private void updateLatestPointerPosition(MouseEvent event) {
        latestPointerPosition = new PointerPosition(event.getX(), event.getY());
    }

    private int normalizedButton(MouseEvent event) {
        int button = event.getButton();
        if (button != MouseEvent.NOBUTTON) {
            return button;
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            return MouseEvent.BUTTON1;
        }
        if (SwingUtilities.isMiddleMouseButton(event)) {
            return MouseEvent.BUTTON2;
        }
        if (SwingUtilities.isRightMouseButton(event)) {
            return MouseEvent.BUTTON3;
        }
        return MouseEvent.NOBUTTON;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }
}
