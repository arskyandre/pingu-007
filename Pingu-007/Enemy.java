
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

    public void separarempilhamento(Entity outro) {
        if (this == outro || this.isDead || outro.isDead) {
            return;
        }

        double meuCenterX = this.x + bodyCollider.getOffsetX() + (bodyCollider.getWidth() / 2.0);
        double meuCenterY = this.y + bodyCollider.getOffsetY() + (bodyCollider.getHeight() / 2.0);

        double outroCenterX = outro.x + outro.bodyCollider.getOffsetX() + (outro.bodyCollider.getWidth() / 2.0);
        double outroCenterY = outro.y + outro.bodyCollider.getOffsetY() + (outro.bodyCollider.getHeight() / 2.0);

        double dx = meuCenterX - outroCenterX;
        double dy = meuCenterY - outroCenterY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double distMinima = (this.bodyCollider.getWidth() / 2.0) + (outro.bodyCollider.getWidth() / 2.0);

        if (dist < distMinima) {
            // Trava de segurança
            if (dist == 0.0) {
                dx = Math.random() - 0.5;
                dy = Math.random() - 0.5;
                dist = Math.sqrt(dx * dx + dy * dy);
            }
            double intensidade = (distMinima - dist) / distMinima;

            double forcaRepulsao = 3.0;

            double pushX = (dx / dist) * intensidade * forcaRepulsao;
            double pushY = (dy / dist) * intensidade * forcaRepulsao;
            this.velX += pushX;
            this.velY += pushY;
        }
    }
}
