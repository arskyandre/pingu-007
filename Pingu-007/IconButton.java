import java.awt.*;
import java.awt.image.BufferedImage;

/** botao com icone de imagem */
public class IconButton extends MenuButton {

    private IconIndex iconIndex;

    public IconButton(int x, int y, int size, IconIndex ind) {
        super("", x, y, size, size);
        this.iconIndex = ind;
    }

    @Override
    protected void adjustHeight() {
        // fixed square, no text wrapping
    }

    public void setIcon(IconIndex index) {
        this.iconIndex = index;
    }

    @Override
    public void draw(Graphics2D g2) {
        if (hovered) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setColor(Color.WHITE);
        } else {
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