import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Semantic menu entry composed from one or more visual controls. */
public class MenuEntry {

    @FunctionalInterface
    public interface ControlAction {
        GameState apply(MenuContext context, MenuControl control, MenuInteraction interaction);
    }

    private final List<MenuControl> controls;
    private final MenuControl activationControl;
    private final MenuAction activationAction;
    private MenuAction leftAction;
    private MenuAction rightAction;
    private BooleanSupplier enabled = () -> true;
    private ControlAction pointerAction;
    private MenuControl hoverFocusSuppressedControl;
    private boolean focused;
    private boolean selected;

    protected MenuEntry(List<? extends MenuControl> controls, MenuControl activationControl,
            MenuAction activationAction) {
        if (controls == null || controls.isEmpty()) {
            throw new IllegalArgumentException("A menu entry needs at least one control");
        }
        this.controls = List.copyOf(controls);
        if (activationControl != null && !this.controls.contains(activationControl)) {
            throw new IllegalArgumentException("The activation control must belong to the entry");
        }
        this.activationControl = activationControl;
        this.activationAction = activationAction;
    }

    public static MenuEntry button(String label, MenuAction action) {
        return button(label, 220, 46, action);
    }

    public static MenuEntry button(String label, int width, int height, MenuAction action) {
        return button(new MenuButton(label, 0, 0, width, height), action);
    }

    public static MenuEntry button(MenuButton button, MenuAction action) {
        return new MenuEntry(List.of(Objects.requireNonNull(button, "button")), button,
                Objects.requireNonNull(action, "action"));
    }

    public static MenuEntry control(MenuControl control, MenuAction action) {
        return new MenuEntry(List.of(Objects.requireNonNull(control, "control")), control,
                Objects.requireNonNull(action, "action"));
    }

    public static MenuEntry composite(List<? extends MenuControl> controls,
            MenuControl activationControl, MenuAction activationAction) {
        return new MenuEntry(controls, activationControl, activationAction);
    }

    public MenuEntry withAdjustment(MenuInputIntent direction, MenuAction action) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(action, "action");
        if (direction == MenuInputIntent.LEFT) {
            leftAction = action;
        } else if (direction == MenuInputIntent.RIGHT) {
            rightAction = action;
        } else {
            throw new IllegalArgumentException("Only left/right can adjust an entry");
        }
        return this;
    }

    public MenuEntry withPointerAction(ControlAction action) {
        pointerAction = Objects.requireNonNull(action, "action");
        return this;
    }

    public MenuEntry withoutPointerFocusOnHover(MenuControl control) {
        if (!controls.contains(Objects.requireNonNull(control, "control"))) {
            throw new IllegalArgumentException("The control must belong to the entry");
        }
        hoverFocusSuppressedControl = control;
        return this;
    }

    public MenuEntry enabledWhen(BooleanSupplier predicate) {
        enabled = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    public List<MenuControl> getControls() {
        return controls;
    }

    public MenuControl getPrimaryControl() {
        return controls.get(0);
    }

    public MenuControl getActivationControl() {
        return activationControl;
    }

    public MenuAction getActivationAction() {
        return activationAction;
    }

    public MenuAction getAdjustment(MenuInputIntent direction) {
        return direction == MenuInputIntent.LEFT ? leftAction
                : direction == MenuInputIntent.RIGHT ? rightAction : null;
    }

    public ControlAction getPointerAction() {
        return pointerAction;
    }

    public boolean isFocusable() {
        return enabled.getAsBoolean();
    }

    public boolean isPointerHovered() {
        for (MenuControl control : controls) {
            if (control.isHovered()) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldFocusFromPointer(MenuControl control, MenuInteraction interaction) {
        return interaction != MenuInteraction.HOVERED || control != hoverFocusSuppressedControl;
    }

    public void clearPointerHover() {
        for (MenuControl control : controls) {
            control.clearPointerHover();
        }
    }

    public Rectangle getFocusBoundsCopy() {
        return MenuLayouts.union(controls);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        for (MenuControl control : controls) {
            control.setFocused(focused);
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        for (MenuControl control : controls) {
            control.setSelected(selected);
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
