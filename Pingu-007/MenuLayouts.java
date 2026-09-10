import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;

/** Stateless helpers for the exact arithmetic shared by menu layouts. */
public final class MenuLayouts {

    private MenuLayouts() {
    }

    public static void centeredVerticalStack(MenuViewport viewport, List<? extends MenuControl> controls,
            int firstY, int gap) {
        int y = firstY;
        for (MenuControl control : controls) {
            Rectangle bounds = control.getBoundsCopy();
            control.setPosition((viewport.width() - bounds.width) / 2, y);
            y += bounds.height + gap;
        }
    }

    public static void centeredHorizontalRow(MenuViewport viewport, int y, int gap,
            MenuControl... controls) {
        int totalWidth = 0;
        for (MenuControl control : controls) {
            totalWidth += control.getBoundsCopy().width;
        }
        totalWidth += gap * Math.max(0, controls.length - 1);

        int x = viewport.centerX() - totalWidth / 2;
        for (MenuControl control : controls) {
            Rectangle bounds = control.getBoundsCopy();
            control.setPosition(x, y);
            x += bounds.width + gap;
        }
    }

    public static void bottomRight(MenuViewport viewport, int margin, MenuControl control) {
        Rectangle bounds = control.getBoundsCopy();
        control.setPosition(viewport.width() - margin - bounds.width,
                viewport.height() - margin - bounds.height);
    }

    public static Rectangle union(MenuControl... controls) {
        Rectangle result = null;
        for (MenuControl control : controls) {
            result = result == null ? control.getBoundsCopy() : result.union(control.getBoundsCopy());
        }
        return result == null ? new Rectangle() : result;
    }

    public static Rectangle union(Collection<? extends MenuControl> controls) {
        Rectangle result = null;
        for (MenuControl control : controls) {
            result = result == null ? control.getBoundsCopy() : result.union(control.getBoundsCopy());
        }
        return result == null ? new Rectangle() : result;
    }

    public static final class LayoutCursor {
        private int y;
        private final int gap;

        public LayoutCursor(int startY, int gap) {
            y = startY;
            this.gap = gap;
        }

        public int nextRow(int rowHeight) {
            int rowY = y;
            y += rowHeight + gap;
            return rowY;
        }

        public int currentY() {
            return y;
        }
    }
}
