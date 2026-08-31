public final class PlayerInputAdapter {
    public PlayerCommand create(InputManager input, CameraManager camera) {
        InputFrame frame = input.frame();
        Vetor2D aimAxis = frame.aimAxis();
        boolean controllerAimActive = frame.activeDevice() == InputDevice.CONTROLLER
                && (aimAxis.x != 0.0 || aimAxis.y != 0.0);

        if (controllerAimActive) {
            input.lockPointer();
        }
        boolean pointerLocked = input.isMouseBloqueado();
        boolean mouseTargetAvailable = frame.activeDevice() == InputDevice.KEYBOARD_MOUSE
                && !pointerLocked;
        double mouseWorldX = (frame.pointer().x() / camera.getZoom()) + camera.getX();
        double mouseWorldY = (frame.pointer().y() / camera.getZoom()) + camera.getY();
        AimCommand aim = new AimCommand(frame.activeDevice(), aimAxis,
                frame.pointer().x(), frame.pointer().y(), pointerLocked,
                mouseTargetAvailable, mouseWorldX, mouseWorldY);

        WeaponSelection weaponSelection = resolveWeaponSelection(frame);
        return new PlayerCommand(frame.moveAxis(), aim,
                frame.isDown(InputAction.FIRE),
                frame.isDown(InputAction.RELOAD),
                frame.isDown(InputAction.DASH),
                weaponSelection,
                frame.wasPressed(InputAction.CAST_OR_PULL),
                frame.wasPressed(InputAction.DEBUG_J),
                frame.wasPressed(InputAction.DEBUG_T),
                frame.isDown(InputAction.DEBUG_6) && frame.wasPressed(InputAction.DEBUG_7));
    }

    private WeaponSelection resolveWeaponSelection(InputFrame frame) {
        if (frame.wasPressed(InputAction.WEAPON_PISTOL)) {
            return WeaponSelection.PISTOL;
        }
        if (frame.wasPressed(InputAction.WEAPON_SHOTGUN)) {
            return WeaponSelection.SHOTGUN;
        }
        if (frame.wasPressed(InputAction.WEAPON_TOGGLE)) {
            return WeaponSelection.TOGGLE;
        }
        return WeaponSelection.NONE;
    }
}
