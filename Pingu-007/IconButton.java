import java.awt.*;
import java.awt.image.BufferedImage;

/** botao com icone de imagem */
public class IconButton extends MenuButton {

    private IconIndex iconIndex;
    private boolean dimBackground;

    public IconButton(int x, int y, int size, IconIndex ind, boolean dim_background) {
        super("", x, y, size, size);
        dimBackground = dim_background;
        this.iconIndex = ind;
    }

    @Override
    protected void adjustHeight() {
    }

    public void setIcon(IconIndex index) {
        this.iconIndex = index;
    }

    public IconIndex getIcon() {
        return iconIndex;
    }

    @Override
    public void draw(Graphics2D g2) {
        if (hovered) {
            if (dimBackground) {
                g2.setColor(new Color(90, 90, 90, 128));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            } else {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
            g2.setColor(Color.WHITE);
        } else {
            if (dimBackground) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
            g2.setColor(new Color(200, 200, 200, 120));
        }
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        if (iconIndex != null) {
            BufferedImage icon = iconIndex.getSprite();
            if (icon != null) {
                int padding = 6;
                int iconSize = rect.width - padding * 2;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(icon, rect.x + padding, rect.y + padding, iconSize, iconSize, null);
            }
        }
    }
}