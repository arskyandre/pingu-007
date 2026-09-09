import java.util.Objects;

/** Immutable declaration for a conventional text-button menu entry. */
public final class ButtonMenuDefinition {

    private final String label;
    private final int width;
    private final int height;
    private final MenuAction action;

    public ButtonMenuDefinition(String label, int width, int height, MenuAction action) {
        this.label = Objects.requireNonNull(label, "label");
        this.width = width;
        this.height = height;
        this.action = Objects.requireNonNull(action, "action");
    }

    public MenuEntry createEntry() {
        return MenuEntry.button(label, width, height, action);
    }

    public String label() {
        return label;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
