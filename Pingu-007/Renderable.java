
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public interface Renderable {

    double getProfundidade();

    void draw(Graphics2D g2, double delta);

    default Rectangle2D getOcclusionBounds() {
        return null;
    }

    default Rectangle2D getOpaqueOcclusionBounds() {
        return null;
    }
}
