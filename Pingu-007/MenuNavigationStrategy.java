import java.util.List;

/** Chooses the next focusable menu entry for a logical direction. */
public interface MenuNavigationStrategy {

    int findNext(List<MenuEntry> entries, int currentIndex, MenuInputIntent direction);
}
