import java.util.List;

/** Ordered navigation with optional wrapping and disabled-entry skipping. */
public final class LinearMenuNavigation implements MenuNavigationStrategy {

    private final boolean wrap;

    public LinearMenuNavigation(boolean wrap) {
        this.wrap = wrap;
    }

    public boolean wraps() {
        return wrap;
    }

    @Override
    public int findNext(List<MenuEntry> entries, int currentIndex, MenuInputIntent direction) {
        if (entries.isEmpty() || (direction != MenuInputIntent.UP && direction != MenuInputIntent.DOWN)) {
            return currentIndex;
        }

        int step = direction == MenuInputIntent.DOWN ? 1 : -1;
        int index = currentIndex;
        for (int attempts = 0; attempts < entries.size(); attempts++) {
            index += step;
            if (index < 0 || index >= entries.size()) {
                if (!wrap) {
                    return currentIndex;
                }
                index = (index + entries.size()) % entries.size();
            }
            if (entries.get(index).isFocusable()) {
                return index;
            }
        }
        return currentIndex;
    }
}
