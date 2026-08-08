import java.awt.event.*;
import javax.swing.SwingUtilities;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public class InputManager extends KeyAdapter implements MouseMotionListener, MouseListener {

    public enum GamepadButton {
        A,
        B,
        X,
        Y,
        LB,
        RB,
        LT,
        RT,
        START,
        BACK,
        GUIDE,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        LEFT_STICK,
        RIGHT_STICK
    }

    private static final int GAMEPAD_INDEX = 0;
    private static final double TRIGGER_THRESHOLD = 0.5;

    private final boolean[] teclas = new boolean[256];
    private final boolean[] teclasPrev = new boolean[256];
    private final boolean[] botoes = new boolean[10];
    private final boolean[] botoesPrev = new boolean[10];
    private final boolean[] cliquesRapidos = new boolean[10];

    private final ControllerManager controllerManager;
    private ControllerState estadoControle;
    private ControllerState estadoControlePrev;

    private double deadzone = 0.15;
    private int mouseX = 0;
    private int mouseY = 0;

    public InputManager() {
        controllerManager = new ControllerManager();
        controllerManager.initSDLGamepad();
        estadoControle = controllerManager.getState(GAMEPAD_INDEX);
        estadoControlePrev = estadoControle;
    }

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

        boolean estavaConectado = estadoControle != null && estadoControle.isConnected;

        estadoControlePrev = estadoControle;

        controllerManager.update();
        estadoControle = controllerManager.getState(GAMEPAD_INDEX);

        if (isButtonJustPressed(GamepadButton.X))
            System.out.println("TESTE: Apertou X");

        boolean estaConectado = estadoControle != null && estadoControle.isConnected;

        if (!estavaConectado && estaConectado) {
            System.out.println("Controle conectado.");
        }

        if (estavaConectado && !estaConectado) {
            System.out.println("Controle desconectado.");
        }
    }

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
            System.out.println("DEBUG INPUT: Botão do mouse pressionado -> " + btn);
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

    public boolean isButtonPressed(GamepadButton button) {
        if (!isControllerConnected()) {
            return false;
        }

        return getButtonState(estadoControle, button);
    }

    public boolean isButtonJustPressed(GamepadButton button) {
        if (!isControllerConnected()) {
            return false;
        }

        return getButtonState(estadoControle, button)
                && !getButtonState(estadoControlePrev, button);
    }

    private boolean getButtonState(ControllerState estado, GamepadButton button) {
        if (estado == null || !estado.isConnected) {
            return false;
        }

        return switch (button) {
            case A -> estado.a;
            case B -> estado.b;
            case X -> estado.x;
            case Y -> estado.y;
            case LB -> estado.lb;
            case RB -> estado.rb;
            case LT -> estado.leftTrigger >= TRIGGER_THRESHOLD;
            case RT -> estado.rightTrigger >= TRIGGER_THRESHOLD;
            case START -> estado.start;
            case BACK -> estado.back;
            case GUIDE -> estado.guide;
            case DPAD_UP -> estado.dpadUp;
            case DPAD_DOWN -> estado.dpadDown;
            case DPAD_LEFT -> estado.dpadLeft;
            case DPAD_RIGHT -> estado.dpadRight;
            case LEFT_STICK -> estado.leftStickClick;
            case RIGHT_STICK -> estado.rightStickClick;
        };
    }

    public Vetor2D getLeftStick() {
        if (!isControllerConnected()) {
            return new Vetor2D(0, 0);
        }

        return aplicarDeadzoneRadial(estadoControle.leftStickX, -estadoControle.leftStickY);
    }

    public Vetor2D getRightStick() {
        if (!isControllerConnected()) {
            return new Vetor2D(0, 0);
        }

        return aplicarDeadzoneRadial(estadoControle.rightStickX, -estadoControle.rightStickY);
    }

    private Vetor2D aplicarDeadzoneRadial(double x, double y) {
        double magnitude = Math.sqrt(x * x + y * y);

        if (magnitude <= deadzone) {
            return new Vetor2D(0, 0);
        }

        double magnitudeLimitada = Math.min(magnitude, 1.0);
        double magnitudeRemapeada = (magnitudeLimitada - deadzone) / (1.0 - deadzone);
        double escala = magnitudeRemapeada / magnitude;

        return new Vetor2D(x * escala, y * escala);
    }

    public void setDeadzone(double deadzone) {
        if (deadzone < 0.0 || deadzone >= 1.0) {
            throw new IllegalArgumentException("A deadzone deve estar no intervalo [0.0, 1.0).");
        }

        this.deadzone = deadzone;
    }

    public double getDeadzone() {
        return deadzone;
    }

    public boolean isControllerConnected() {
        return estadoControle != null && estadoControle.isConnected;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public void shutdown() {
        controllerManager.quitSDLGamepad();
    }
}
