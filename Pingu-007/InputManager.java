
import java.awt.event.*;

public class InputManager extends KeyAdapter implements MouseMotionListener {

    private final boolean[] teclas = new boolean[256];
    private int mouseX = 0;
    private int mouseY = 0;

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() < teclas.length) {
            teclas[e.getKeyCode()] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() < teclas.length) {
            teclas[e.getKeyCode()] = false;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    // Métodos para outras classes consultarem o estado
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < teclas.length) {
            return teclas[keyCode];
        }
        return false;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
}
