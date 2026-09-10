/** Labeled icon toggle entry used by Options. */
public final class ToggleOptionEntry extends MenuEntry {

    private final String label;
    private final IconButton button;

    public ToggleOptionEntry(String label, IconButton button, MenuAction action) {
        super(java.util.List.of(button), button, action);
        this.label = label;
        this.button = button;
    }

    public String label() {
        return label;
    }

    public IconButton button() {
        return button;
    }
}
