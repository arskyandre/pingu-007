
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.*;

public class Enemy extends Entity {

    private final double width;
    private final double height;
    private final double speed = 2;
    private final int[][] lvlData;
    private final int danoContato = 10;

    public Enemy(double startX, double startY, double width, double height, int[][] lvlData) {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.lvlData = lvlData;
        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
    }

    public void update(Player player) {

        if (player.getX() > x) {
            velX = speed;
        }

        if (player.getX() < x) {
            velX = -speed;
        }

        if (player.getY() > y) {
            velY = speed;
        }

        if (player.getY() < y) {
            velY = -speed;
        }
        //COLISAO Com Tiles
        moveAndCollideWithMap(lvlData);

        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(Color.MAGENTA);
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
