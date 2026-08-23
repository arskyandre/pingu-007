
import java.awt.Graphics2D;

public interface Renderable {

    double getProfundidade();

    void draw(Graphics2D g2, double delta);
}
