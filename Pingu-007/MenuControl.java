import java.awt.Graphics2D;
import java.awt.Rectangle;

/** A visual menu control with encapsulated pointer and focus state. */
public interface MenuControl {

    Rectangle getBoundsCopy();

    void setPosition(int x, int y);

    MenuInteraction updatePointer(InputManager input);

    void setFocused(boolean focused);

    boolean isFocused();

    void render(Graphics2D graphics);

    default boolean isHovered() {
        return false;
    }

    default void clearPointerHover() {
    }

    default void setSelected(boolean selected) {
    }

    default boolean isSelected() {
        return false;
    }
}
