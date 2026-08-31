import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EnumMap;

public final class InputBindings {
    private final double triggerThreshold;

    public InputBindings(double triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }

    public EnumMap<InputAction, ActionState> resolve(InputContext context,
            RawInputSnapshot raw, InputDevice device, boolean pointerActive) {
        EnumMap<InputAction, ActionState> actions = new EnumMap<>(InputAction.class);
        put(actions, InputAction.TOGGLE_FULLSCREEN, key(raw, KeyEvent.VK_F11));

        if (context == InputContext.BLOCKED || !raw.focused()) {
            return actions;
        }

        if (device == InputDevice.CONTROLLER) {
            resolveController(context, raw, actions);
        } else {
            resolveKeyboardMouse(context, raw, pointerActive, actions);
        }
        return actions;
    }

    private void resolveController(InputContext context, RawInputSnapshot raw,
            EnumMap<InputAction, ActionState> actions) {
        switch (context) {
            case PLAYING -> {
                put(actions, InputAction.PAUSE, pad(raw, GamepadButton.START));
                put(actions, InputAction.INTERACT, pad(raw, GamepadButton.Y));
                put(actions, InputAction.FIRE, pad(raw, GamepadButton.RT));
                put(actions, InputAction.RELOAD, pad(raw, GamepadButton.X));
                put(actions, InputAction.DASH,
                        any(pad(raw, GamepadButton.A), pad(raw, GamepadButton.LB)));
                put(actions, InputAction.WEAPON_TOGGLE, pad(raw, GamepadButton.RB));
                put(actions, InputAction.CAST_OR_PULL, pad(raw, GamepadButton.LT));
            }
            case FISHING ->
                put(actions, InputAction.CAST_OR_PULL, pad(raw, GamepadButton.Y));
            case DIALOGUE -> {
                put(actions, InputAction.MENU_UP, pad(raw, GamepadButton.DPAD_UP));
                put(actions, InputAction.MENU_DOWN, pad(raw, GamepadButton.DPAD_DOWN));
                put(actions, InputAction.CONFIRM, pad(raw, GamepadButton.A));
            }
            case MAIN_MENU, GAME_OVER -> resolveSimpleControllerMenu(raw, actions, false);
            case PAUSED -> {
                resolveSimpleControllerMenu(raw, actions, false);
                put(actions, InputAction.CANCEL,
                        any(pad(raw, GamepadButton.B), pad(raw, GamepadButton.START)));
            }
            case SHOP -> {
                resolveSimpleControllerMenu(raw, actions, true);
                put(actions, InputAction.CANCEL,
                        any(pad(raw, GamepadButton.B), pad(raw, GamepadButton.START)));
            }
            case OPTIONS -> {
                resolveSimpleControllerMenu(raw, actions, true);
                put(actions, InputAction.CANCEL, pad(raw, GamepadButton.B));
            }
            case KEYBINDINGS -> {
                put(actions, InputAction.CONFIRM, pad(raw, GamepadButton.A));
                put(actions, InputAction.CANCEL, pad(raw, GamepadButton.B));
                put(actions, InputAction.MENU_ACTIVITY,
                        any(pad(raw, GamepadButton.A),
                                pad(raw, GamepadButton.DPAD_UP), pad(raw, GamepadButton.DPAD_DOWN),
                                pad(raw, GamepadButton.DPAD_LEFT), pad(raw, GamepadButton.DPAD_RIGHT)));
            }
            default -> {
            }
        }
    }

    private void resolveSimpleControllerMenu(RawInputSnapshot raw,
            EnumMap<InputAction, ActionState> actions, boolean horizontal) {
        put(actions, InputAction.MENU_UP, pad(raw, GamepadButton.DPAD_UP));
        put(actions, InputAction.MENU_DOWN, pad(raw, GamepadButton.DPAD_DOWN));
        if (horizontal) {
            put(actions, InputAction.MENU_LEFT, pad(raw, GamepadButton.DPAD_LEFT));
            put(actions, InputAction.MENU_RIGHT, pad(raw, GamepadButton.DPAD_RIGHT));
        }
        put(actions, InputAction.CONFIRM, pad(raw, GamepadButton.A));
    }

    private void resolveKeyboardMouse(InputContext context, RawInputSnapshot raw,
            boolean pointerActive, EnumMap<InputAction, ActionState> actions) {
        switch (context) {
            case PLAYING -> {
                put(actions, InputAction.MOVE_UP, key(raw, KeyEvent.VK_W));
                put(actions, InputAction.MOVE_DOWN, key(raw, KeyEvent.VK_S));
                put(actions, InputAction.MOVE_LEFT, key(raw, KeyEvent.VK_A));
                put(actions, InputAction.MOVE_RIGHT, key(raw, KeyEvent.VK_D));
                put(actions, InputAction.PAUSE, key(raw, KeyEvent.VK_ESCAPE));
                put(actions, InputAction.INTERACT, key(raw, KeyEvent.VK_E));
                put(actions, InputAction.FIRE,
                        pointerActive ? mouse(raw, MouseEvent.BUTTON1) : ActionState.IDLE);
                put(actions, InputAction.RELOAD, key(raw, KeyEvent.VK_R));
                put(actions, InputAction.DASH, key(raw, KeyEvent.VK_SPACE));
                put(actions, InputAction.WEAPON_TOGGLE, key(raw, KeyEvent.VK_G));
                put(actions, InputAction.WEAPON_PISTOL, key(raw, KeyEvent.VK_1));
                put(actions, InputAction.WEAPON_SHOTGUN, key(raw, KeyEvent.VK_2));
                put(actions, InputAction.CAST_OR_PULL,
                        pointerActive ? mouse(raw, MouseEvent.BUTTON3) : ActionState.IDLE);
                resolveDebug(raw, actions);
            }
            case FISHING ->
                put(actions, InputAction.CAST_OR_PULL,
                        any(key(raw, KeyEvent.VK_E),
                                pointerActive ? mouse(raw, MouseEvent.BUTTON3) : ActionState.IDLE));
            case DIALOGUE -> {
                put(actions, InputAction.MENU_UP,
                        any(key(raw, KeyEvent.VK_UP), key(raw, KeyEvent.VK_W)));
                put(actions, InputAction.MENU_DOWN,
                        any(key(raw, KeyEvent.VK_DOWN), key(raw, KeyEvent.VK_S)));
                put(actions, InputAction.CONFIRM,
                        any(key(raw, KeyEvent.VK_SPACE), key(raw, KeyEvent.VK_ENTER)));
            }
            case MAIN_MENU, GAME_OVER -> {
                // These screens intentionally keep their current mouse-only keyboard policy.
            }
            case PAUSED -> put(actions, InputAction.CANCEL, key(raw, KeyEvent.VK_ESCAPE));
            case SHOP -> {
                put(actions, InputAction.MENU_UP,
                        any(key(raw, KeyEvent.VK_W), key(raw, KeyEvent.VK_UP)));
                put(actions, InputAction.MENU_DOWN,
                        any(key(raw, KeyEvent.VK_S), key(raw, KeyEvent.VK_DOWN)));
                put(actions, InputAction.MENU_LEFT,
                        any(key(raw, KeyEvent.VK_A), key(raw, KeyEvent.VK_LEFT)));
                put(actions, InputAction.MENU_RIGHT,
                        any(key(raw, KeyEvent.VK_D), key(raw, KeyEvent.VK_RIGHT)));
                put(actions, InputAction.CONFIRM,
                        any(key(raw, KeyEvent.VK_ENTER), key(raw, KeyEvent.VK_SPACE)));
                put(actions, InputAction.CANCEL, key(raw, KeyEvent.VK_ESCAPE));
            }
            case OPTIONS -> {
                put(actions, InputAction.MENU_UP, key(raw, KeyEvent.VK_UP));
                put(actions, InputAction.MENU_DOWN, key(raw, KeyEvent.VK_DOWN));
                put(actions, InputAction.MENU_LEFT, key(raw, KeyEvent.VK_LEFT));
                put(actions, InputAction.MENU_RIGHT, key(raw, KeyEvent.VK_RIGHT));
                put(actions, InputAction.CANCEL, key(raw, KeyEvent.VK_ESCAPE));
            }
            case KEYBINDINGS -> put(actions, InputAction.CANCEL, key(raw, KeyEvent.VK_ESCAPE));
            default -> {
            }
        }
    }

    private void resolveDebug(RawInputSnapshot raw, EnumMap<InputAction, ActionState> actions) {
        put(actions, InputAction.DEBUG_P, key(raw, KeyEvent.VK_P));
        put(actions, InputAction.DEBUG_F, key(raw, KeyEvent.VK_F));
        put(actions, InputAction.DEBUG_I, key(raw, KeyEvent.VK_I));
        put(actions, InputAction.DEBUG_O, key(raw, KeyEvent.VK_O));
        put(actions, InputAction.DEBUG_L, key(raw, KeyEvent.VK_L));
        put(actions, InputAction.DEBUG_K, key(raw, KeyEvent.VK_K));
        put(actions, InputAction.DEBUG_H, key(raw, KeyEvent.VK_H));
        put(actions, InputAction.DEBUG_M, key(raw, KeyEvent.VK_M));
        put(actions, InputAction.DEBUG_B, key(raw, KeyEvent.VK_B));
        put(actions, InputAction.DEBUG_V, key(raw, KeyEvent.VK_V));
        put(actions, InputAction.DEBUG_F10, key(raw, KeyEvent.VK_F10));
        put(actions, InputAction.DEBUG_F1, key(raw, KeyEvent.VK_F1));
        put(actions, InputAction.DEBUG_F2, key(raw, KeyEvent.VK_F2));
        put(actions, InputAction.DEBUG_F3, key(raw, KeyEvent.VK_F3));
        put(actions, InputAction.DEBUG_F4, key(raw, KeyEvent.VK_F4));
        put(actions, InputAction.DEBUG_F5, key(raw, KeyEvent.VK_F5));
        put(actions, InputAction.DEBUG_F6, key(raw, KeyEvent.VK_F6));
        put(actions, InputAction.DEBUG_N, key(raw, KeyEvent.VK_N));
        put(actions, InputAction.DEBUG_J, key(raw, KeyEvent.VK_J));
        put(actions, InputAction.DEBUG_T, key(raw, KeyEvent.VK_T));
        put(actions, InputAction.DEBUG_6, key(raw, KeyEvent.VK_6));
        put(actions, InputAction.DEBUG_7, key(raw, KeyEvent.VK_7));
    }

    private ActionState key(RawInputSnapshot raw, int keyCode) {
        return new ActionState(raw.keyDown(keyCode), raw.keyPressed(keyCode), raw.keyReleased(keyCode));
    }

    private ActionState mouse(RawInputSnapshot raw, int button) {
        return new ActionState(raw.mouseDown(button), raw.mousePressed(button), raw.mouseReleased(button));
    }

    private ActionState pad(RawInputSnapshot raw, GamepadButton button) {
        return new ActionState(raw.gamepadDown(button, triggerThreshold),
                raw.gamepadPressed(button, triggerThreshold),
                raw.gamepadReleased(button, triggerThreshold));
    }

    private static ActionState any(ActionState... states) {
        boolean down = false;
        boolean pressed = false;
        boolean released = false;
        for (ActionState state : states) {
            down |= state.down();
            pressed |= state.pressed();
            released |= state.released();
        }
        return new ActionState(down, pressed, released && !down);
    }

    private static void put(EnumMap<InputAction, ActionState> actions,
            InputAction action, ActionState state) {
        if (state.down() || state.pressed() || state.released()) {
            actions.put(action, state);
        }
    }
}
