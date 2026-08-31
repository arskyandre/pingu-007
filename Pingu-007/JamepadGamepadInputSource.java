import java.util.EnumSet;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public final class JamepadGamepadInputSource implements GamepadInputSource {
    private static final int GAMEPAD_INDEX = 0;

    private final ControllerManager controllerManager;

    public JamepadGamepadInputSource() {
        controllerManager = new ControllerManager();
        controllerManager.initSDLGamepad();
    }

    @Override
    public GamepadSnapshot poll() {
        controllerManager.update();
        ControllerState state = controllerManager.getState(GAMEPAD_INDEX);
        if (state == null || !state.isConnected) {
            return GamepadSnapshot.disconnected();
        }

        EnumSet<GamepadButton> buttons = EnumSet.noneOf(GamepadButton.class);
        add(buttons, GamepadButton.A, state.a);
        add(buttons, GamepadButton.B, state.b);
        add(buttons, GamepadButton.X, state.x);
        add(buttons, GamepadButton.Y, state.y);
        add(buttons, GamepadButton.LB, state.lb);
        add(buttons, GamepadButton.RB, state.rb);
        add(buttons, GamepadButton.START, state.start);
        add(buttons, GamepadButton.BACK, state.back);
        add(buttons, GamepadButton.GUIDE, state.guide);
        add(buttons, GamepadButton.DPAD_UP, state.dpadUp);
        add(buttons, GamepadButton.DPAD_DOWN, state.dpadDown);
        add(buttons, GamepadButton.DPAD_LEFT, state.dpadLeft);
        add(buttons, GamepadButton.DPAD_RIGHT, state.dpadRight);
        add(buttons, GamepadButton.LEFT_STICK, state.leftStickClick);
        add(buttons, GamepadButton.RIGHT_STICK, state.rightStickClick);

        return new GamepadSnapshot(true,
                state.leftStickX, state.leftStickY,
                state.rightStickX, state.rightStickY,
                state.leftTrigger, state.rightTrigger,
                buttons);
    }

    @Override
    public void close() {
        controllerManager.quitSDLGamepad();
    }

    private static void add(EnumSet<GamepadButton> buttons, GamepadButton button, boolean down) {
        if (down) {
            buttons.add(button);
        }
    }
}
