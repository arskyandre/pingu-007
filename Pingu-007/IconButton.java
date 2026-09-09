import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Icon-specific MenuButton rendering that retains the original hit behavior. */
public class IconButton extends MenuButton {

    private IconIndex iconIndex;
    private final boolean dimBackground;

    public IconButton(int x, int y, int size, IconIndex index, boolean dimBackground) {
        super("", x, y, size, size);
        this.dimBackground = dimBackground;
        this.iconIndex = index;
    }

    @Override
    protected void adjustHeight() {
        // Icon buttons keep the caller-provided square size.
    }

    public void setIcon(IconIndex index) {
        iconIndex = index;
    }

    public IconIndex getIcon() {
        return iconIndex;
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            boolean highlighted = isHighlighted();
            if (highlighted) {
                if (dimBackground) {
                    copy.setColor(new Color(90, 90, 90, 128));
                    copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                } else {
                    copy.setColor(new Color(255, 255, 255, 40));
                    copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                }
                copy.setColor(Color.WHITE);
            } else {
                if (dimBackground) {
                    copy.setColor(new Color(0, 0, 0, 150));
                    copy.fillRect(rect.x, rect.y, rect.width, rect.height);
                }
                copy.setColor(new Color(200, 200, 200, 120));
            }
            copy.setStroke(new BasicStroke(2));
            copy.drawRect(rect.x, rect.y, rect.width, rect.height);

            if (iconIndex != null) {
                BufferedImage icon = iconIndex.getSprite();
                if (icon != null) {
                    int padding = 6;
                    int iconSize = rect.width - padding * 2;
                    copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    copy.drawImage(icon, rect.x + padding, rect.y + padding, iconSize, iconSize, null);
                }
            }
        } finally {
            copy.dispose();
        }
    }
}
