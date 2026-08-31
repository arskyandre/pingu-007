public final class ControllerPriorityPolicy {
    public InputDevice selectDevice(RawInputSnapshot raw, Vetor2D leftStick, Vetor2D rightStick,
            double triggerThreshold) {
        if (!raw.focused() || !raw.gamepad().connected()) {
            return InputDevice.KEYBOARD_MOUSE;
        }
        if (leftStick.x != 0.0 || leftStick.y != 0.0
                || rightStick.x != 0.0 || rightStick.y != 0.0) {
            return InputDevice.CONTROLLER;
        }
        for (GamepadButton button : GamepadButton.values()) {
            if (raw.gamepadDown(button, triggerThreshold)) {
                return InputDevice.CONTROLLER;
            }
        }
        return InputDevice.KEYBOARD_MOUSE;
    }
}
