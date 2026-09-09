import java.util.Objects;

/** Immutable dimensions used by menu layout and rendering. */
public final class MenuViewport {

    private final int width;
    private final int height;

    public MenuViewport(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int centerX() {
        return width / 2;
    }

    public int centerY() {
        return height / 2;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MenuViewport other)) {
            return false;
        }
        return width == other.width && height == other.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "MenuViewport[" + width + "x" + height + "]";
    }
}
