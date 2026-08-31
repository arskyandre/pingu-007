import java.awt.event.KeyEvent;
import java.util.EnumMap;

public final class ContextTransitionGuard {
    private final double triggerThreshold;
    private boolean suppressControllerConfirm;
    private boolean suppressKeyboardConfirm;

    public ContextTransitionGuard(double triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    public void apply(InputFrame previousFrame, InputContext nextContext,
            RawInputSnapshot raw, InputDevice device,
            EnumMap<InputAction, ActionState> actions) {
        if (previousFrame != null
                && previousFrame.context() != nextContext
                && nextContext == InputContext.PLAYING) {
            suppressControllerConfirm = raw.gamepad().isDown(GamepadButton.A, triggerThreshold);
            suppressKeyboardConfirm = raw.keyDown(KeyEvent.VK_SPACE);
        }

        if (!raw.gamepad().isDown(GamepadButton.A, triggerThreshold)) {
            suppressControllerConfirm = false;
        }
        if (!raw.keyDown(KeyEvent.VK_SPACE)) {
            suppressKeyboardConfirm = false;
        }

        if (nextContext != InputContext.PLAYING) {
            return;
        }
        if (device == InputDevice.CONTROLLER && suppressControllerConfirm) {
            ActionState leftBumper = new ActionState(
                    raw.gamepadDown(GamepadButton.LB, triggerThreshold),
                    raw.gamepadPressed(GamepadButton.LB, triggerThreshold),
                    raw.gamepadReleased(GamepadButton.LB, triggerThreshold));
            if (leftBumper.down() || leftBumper.pressed() || leftBumper.released()) {
                actions.put(InputAction.DASH, leftBumper);
            } else {
                actions.remove(InputAction.DASH);
            }
        } else if (device == InputDevice.KEYBOARD_MOUSE && suppressKeyboardConfirm) {
            actions.remove(InputAction.DASH);
        }
    }
}
