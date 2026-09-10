import java.util.List;
import java.util.Objects;

/** Shared pointer/controller update sequence for menu entries. */
public final class MenuController {

    private final List<MenuEntry> entries;
    private final MenuNavigator navigator;
    private final MenuInputBindings bindings;
    private MenuAction backAction;
    private boolean lockMouseOnNavigation = true;
    private boolean playActivationSound = true;
    private boolean playBackSound = false;
    private boolean confirmBeforePointerActivation = false;
    private boolean respectMouseLock = true;
    private boolean pointerBeforeNavigation = false;
    private boolean lockMouseOnConfirm = false;

    private MenuController(List<MenuEntry> entries, MenuNavigator navigator, MenuInputBindings bindings) {
        this.entries = Objects.requireNonNull(entries, "entries");
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
    }

    public static MenuController linear(List<MenuEntry> entries, MenuInputBindings bindings, boolean wrap) {
        return new MenuController(entries, new MenuNavigator(entries, new LinearMenuNavigation(wrap), 0), bindings);
    }

    public static MenuController spatial(List<MenuEntry> entries, MenuInputBindings bindings) {
        return new MenuController(entries, new MenuNavigator(entries, new SpatialMenuNavigation(), 0), bindings);
    }

    public MenuController withMouseLockOnNavigation(boolean enabled) {
        lockMouseOnNavigation = enabled;
        return this;
    }

    public MenuController withMouseLockArbitration(boolean enabled) {
        respectMouseLock = enabled;
        return this;
    }

    public MenuController withActivationSound(boolean enabled) {
        playActivationSound = enabled;
        return this;
    }

    public MenuController withBackAction(MenuAction action, boolean playSound) {
        backAction = Objects.requireNonNull(action, "action");
        playBackSound = playSound;
        return this;
    }

    public MenuController withConfirmBeforePointerActivation(boolean enabled) {
        confirmBeforePointerActivation = enabled;
        return this;
    }

    public MenuController withPointerBeforeNavigation(boolean enabled) {
        pointerBeforeNavigation = enabled;
        return this;
    }

    public MenuController withMouseLockOnConfirm(boolean enabled) {
        lockMouseOnConfirm = enabled;
        return this;
    }

    public MenuNavigator navigator() {
        return navigator;
    }

    public List<MenuEntry> entries() {
        return entries;
    }

    public GameState update(MenuContext context, GameState currentState) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(currentState, "currentState");

        InputManager input = context.input();
        navigator.ensureFocusedEntry(1);

        if (backAction != null && bindings.isJustPressed(MenuInputIntent.BACK, input)) {
            if (playBackSound) {
                playClick(context);
            }
            return execute(backAction, currentState);
        }

        if (lockMouseOnConfirm && bindings.isJustPressed(MenuInputIntent.CONFIRM, input)) {
            input.iniciarBloqueioMouse();
        }

        PointerUpdate pointerUpdate = PointerUpdate.none(currentState);
        boolean pointerActivationHandled = false;
        if (pointerBeforeNavigation) {
            pointerUpdate = updatePointer(context, currentState);
            if (pointerUpdate.activationEntry != null) {
                pointerActivationHandled = true;
                GameState pointerActivationResult = activate(pointerUpdate.activationEntry, context, currentState);
                if (pointerActivationResult != currentState) {
                    return pointerActivationResult;
                }
            }
            if (pointerUpdate.result != currentState) {
                return pointerUpdate.result;
            }
        }

        GameState navigationResult = handleNavigation(context, currentState);
        if (navigationResult != currentState) {
            return navigationResult;
        }

        if (!pointerBeforeNavigation) {
            pointerUpdate = updatePointer(context, currentState);
        }

        if (confirmBeforePointerActivation
                && bindings.isJustPressed(MenuInputIntent.CONFIRM, input)) {
            return activateFocused(context, currentState);
        }

        if (!pointerActivationHandled && pointerUpdate.activationEntry != null) {
            return activate(pointerUpdate.activationEntry, context, currentState);
        }
        if (pointerUpdate.result != currentState) {
            return pointerUpdate.result;
        }

        if (bindings.isJustPressed(MenuInputIntent.CONFIRM, input)) {
            return activateFocused(context, currentState);
        }
        return currentState;
    }

    private GameState handleNavigation(MenuContext context, GameState currentState) {
        InputManager input = context.input();
        MenuInputIntent vertical = firstPressed(input, MenuInputIntent.UP, MenuInputIntent.DOWN);
        if (vertical != null) {
            navigator.move(vertical);
            if (lockMouseOnNavigation) {
                input.iniciarBloqueioMouse();
            }
            return currentState;
        }

        MenuInputIntent horizontal = firstPressed(input, MenuInputIntent.LEFT, MenuInputIntent.RIGHT);
        if (horizontal == null) {
            return currentState;
        }

        MenuEntry focused = navigator.focusedEntry();
        MenuAction adjustment = focused == null ? null : focused.getAdjustment(horizontal);
        GameState result = currentState;
        if (adjustment != null) {
            result = execute(adjustment, currentState);
        } else {
            navigator.move(horizontal);
        }
        if (lockMouseOnNavigation) {
            input.iniciarBloqueioMouse();
        }
        return result;
    }

    private PointerUpdate updatePointer(MenuContext context, GameState currentState) {
        InputManager input = context.input();
        if (respectMouseLock && input.isMouseBloqueado()) {
            for (MenuEntry entry : entries) {
                entry.clearPointerHover();
            }
            applyFocusVisuals(context, false);
            return PointerUpdate.none(currentState);
        }

        MenuEntry hoveredEntry = null;
        MenuEntry activationEntry = null;
        GameState pointerResult = currentState;
        for (MenuEntry entry : entries) {
            if (!entry.isFocusable()) {
                entry.clearPointerHover();
                continue;
            }
            for (MenuControl control : entry.getControls()) {
                MenuInteraction interaction = control.updatePointer(input);
                if (control.isHovered() && entry.shouldFocusFromPointer(control, interaction)) {
                    hoveredEntry = entry;
                }
                if (interaction == MenuInteraction.CLICKED) {
                    if (control == entry.getActivationControl()) {
                        activationEntry = entry;
                    } else if (entry.getPointerAction() != null) {
                        playClick(context);
                        pointerResult = execute(entry.getPointerAction().apply(context, control, interaction), pointerResult);
                    }
                } else if ((interaction == MenuInteraction.DRAGGING || interaction == MenuInteraction.PRESSED)
                        && control != entry.getActivationControl() && entry.getPointerAction() != null) {
                    pointerResult = execute(entry.getPointerAction().apply(context, control, interaction), pointerResult);
                }
            }
        }

        if (hoveredEntry != null) {
            navigator.setFocusedIndex(entries.indexOf(hoveredEntry));
        }
        applyFocusVisuals(context, hoveredEntry != null);
        return new PointerUpdate(activationEntry, pointerResult);
    }

    private void applyFocusVisuals(MenuContext context, boolean pointerIsOverEntry) {
        if (pointerIsOverEntry) {
            navigator.clearFocusVisuals();
        } else if (context.input().isMouseBloqueado() || context.input().isControllerActive()) {
            navigator.syncFocusVisuals();
        } else {
            navigator.clearFocusVisuals();
        }
    }

    private GameState activateFocused(MenuContext context, GameState currentState) {
        MenuEntry focused = navigator.focusedEntry();
        return focused == null ? currentState : activate(focused, context, currentState);
    }

    private GameState activate(MenuEntry entry, MenuContext context, GameState currentState) {
        if (entry.getActivationAction() == null) {
            return currentState;
        }
        if (playActivationSound) {
            playClick(context);
        }
        return execute(entry.getActivationAction(), currentState);
    }

    private static GameState execute(MenuAction action, GameState fallback) {
        return execute(action.execute(), fallback);
    }

    private static GameState execute(GameState result, GameState fallback) {
        return result == null ? fallback : result;
    }

    private static void playClick(MenuContext context) {
        context.soundManager().playSFX(SoundManager.SFX.HUD_CLICK);
    }

    private MenuInputIntent firstPressed(InputManager input, MenuInputIntent first, MenuInputIntent second) {
        if (bindings.isJustPressed(first, input)) {
            return first;
        }
        if (bindings.isJustPressed(second, input)) {
            return second;
        }
        return null;
    }

    private static final class PointerUpdate {
        final MenuEntry activationEntry;
        final GameState result;

        PointerUpdate(MenuEntry activationEntry, GameState result) {
            this.activationEntry = activationEntry;
            this.result = result;
        }

        static PointerUpdate none(GameState state) {
            return new PointerUpdate(null, state);
        }
    }
}
