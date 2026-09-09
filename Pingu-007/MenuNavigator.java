import java.util.List;
import java.util.Objects;

/** Owns the current focus and keeps control focus visuals synchronized. */
public final class MenuNavigator {

    private final List<MenuEntry> entries;
    private final MenuNavigationStrategy strategy;
    private int focusedIndex;

    public MenuNavigator(List<MenuEntry> entries, MenuNavigationStrategy strategy, int initialIndex) {
        this.entries = Objects.requireNonNull(entries, "entries");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.focusedIndex = entries.isEmpty() ? -1 : Math.clamp(initialIndex, 0, entries.size() - 1);
        clearFocusVisuals();
    }

    public boolean move(MenuInputIntent direction) {
        if (entries.isEmpty()) {
            return false;
        }
        int next = strategy.findNext(entries, focusedIndex, direction);
        if (next < 0 || next >= entries.size() || next == focusedIndex) {
            return false;
        }
        focusedIndex = next;
        syncFocusVisuals();
        return true;
    }

    public void ensureFocusedEntry(int direction) {
        if (entries.isEmpty() || isFocusedEntryEnabled()) {
            return;
        }
        int next = strategy.findNext(entries, focusedIndex, direction >= 0
                ? MenuInputIntent.DOWN : MenuInputIntent.UP);
        if (next != focusedIndex && next >= 0 && next < entries.size()) {
            focusedIndex = next;
            syncFocusVisuals();
        }
    }

    public void setFocusedIndex(int index) {
        if (entries.isEmpty()) {
            focusedIndex = -1;
            syncFocusVisuals();
            return;
        }
        if (index < 0 || index >= entries.size()) {
            throw new IndexOutOfBoundsException("Invalid menu focus index: " + index);
        }
        focusedIndex = index;
        syncFocusVisuals();
    }

    public int focusedIndex() {
        return focusedIndex;
    }

    public MenuEntry focusedEntry() {
        return focusedIndex >= 0 && focusedIndex < entries.size() ? entries.get(focusedIndex) : null;
    }

    public boolean isFocused(MenuEntry entry) {
        return focusedEntry() == entry;
    }

    public void syncFocusVisuals() {
        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).setFocused(index == focusedIndex);
        }
    }

    public void clearFocusVisuals() {
        for (MenuEntry entry : entries) {
            entry.setFocused(false);
        }
    }

    private boolean isFocusedEntryEnabled() {
        return focusedEntry() != null && focusedEntry().isFocusable();
    }
}
