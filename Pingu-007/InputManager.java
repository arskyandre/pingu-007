
import java.awt.event.*;

public class InputManager extends KeyAdapter implements MouseMotionListener, MouseListener {

    private final boolean[] teclas = new boolean[256];
    private final boolean[] teclasPrev = new boolean[256];
    private final boolean[] botoes = new boolean[4];
    private final boolean[] botoesPrev = new boolean[4];
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

    public void update() {
        System.arraycopy(teclas, 0, teclasPrev, 0, teclas.length);
        System.arraycopy(botoes, 0, botoesPrev, 0, botoes.length);
    }

    // Métodos para outras classes consultarem o estado
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < teclas.length) {
            return teclas[keyCode];
        }
        return false;
    }

    public boolean isKeyJustPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < teclas.length)
            return teclas[keyCode] && !teclasPrev[keyCode];
        return false;
    }

    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < botoes.length)
            return botoes[button] && !botoesPrev[button];
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() < botoes.length)
            botoes[e.getButton()] = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() < botoes.length)
            botoes[e.getButton()] = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < botoes.length)
            return botoes[button];
        return false;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
}
