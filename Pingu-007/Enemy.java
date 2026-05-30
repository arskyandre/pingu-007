
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public abstract class Enemy extends Entity {

    protected double width;
    protected double height;
    protected int[][] lvlData;
    protected int danoContato = 10;
    protected Color cor = Color.MAGENTA;

    public Enemy(double startX, double startY, double width, double height, int[][] lvlData) {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.lvlData = lvlData;
    }

    // O método update agora é abstrato. Cada tipo de inimigo (Zumbi, Dasher, Boss) cria a sua própria IA
    public abstract void update(Player player);

    public void draw(Graphics2D g2) {
        g2.setColor(cor);
        g2.fill(new Rectangle2D.Double(x, y, width, height));
    }

    public double getLargura() {
        return width;
    }

    public double getAltura() {
        return height;
    }

    public void separarempilhamento(Enemy outro) {
        double dx = x - outro.x;
        double dy = y - outro.y;
        double distancia = Math.sqrt(dx * dx + dy * dy);
        double minDistancia = width;

        if (distancia < minDistancia && distancia > 0) {
            double sob = minDistancia - distancia;
            x += (dx / distancia) * sob * 0.5;
            y += (dy / distancia) * sob * 0.5;
        }
    }
}
