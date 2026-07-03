
import java.awt.event.*;
import javax.swing.SwingUtilities;

public class InputManager extends KeyAdapter implements MouseMotionListener, MouseListener {

    private final boolean[] teclas = new boolean[256];
    private final boolean[] teclasPrev = new boolean[256];
    private final boolean[] botoes = new boolean[10];
    private final boolean[] botoesPrev = new boolean[10];
    private final boolean[] cliquesRapidos = new boolean[10];
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
        for (int i = 0; i < cliquesRapidos.length; i++) {
            cliquesRapidos[i] = false;
        }
    }

    // Métodos para outras classes consultarem o estado
    public boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < teclas.length) {
            return teclas[keyCode];
        }
        return false;
    }

    public boolean isKeyJustPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < teclas.length) {
            return teclas[keyCode] && !teclasPrev[keyCode];
        }
        return false;
    }

    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < botoes.length) {
            return (botoes[button] && !botoesPrev[button]) || cliquesRapidos[button];
        }
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int btn = e.getButton();
        if (btn == 0) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                btn = 1;
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                btn = 2;
            } else if (SwingUtilities.isRightMouseButton(e)) {
                btn = 3;
            }
        }
        if (btn >= 0 && btn < botoes.length) {
            botoes[btn] = true;
            cliquesRapidos[btn] = true;
            System.out.println("DEBUG INPUT: Botão do rato pressionado -> " + btn);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int btn = e.getButton();

        if (btn == 0) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                btn = 1;
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                btn = 2;
            } else if (SwingUtilities.isRightMouseButton(e)) {
                btn = 3;
            }
        }

        if (btn >= 0 && btn < botoes.length) {
            botoes[btn] = false;
        }
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
        if (button >= 0 && button < botoes.length) {
            return botoes[button];
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
