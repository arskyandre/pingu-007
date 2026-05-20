import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Ellipse2D;

public class BulletManager {
    private ArrayList<Bullet> Bullets = new ArrayList<>();

    public void shoot(double startX, double startY, double dirX, double dirY, BulletOwner owner) {

        Bullets.add(new Bullet(startX, startY, dirX, dirY, BulletOwner.PLAYER));
    }

    public void update(double telaLargura, double telaAltura) {
        Bullets.removeIf(b -> !b.isActive());
        for (Bullet b : Bullets) {
            b.update(telaLargura, telaAltura);
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        for (Bullet b : Bullets) {
            g2.fill(new Ellipse2D.Double(b.getX() - 4, b.getY() - 4, 8, 8));
        }
    }

    public ArrayList<Bullet> getBullets() {
        return Bullets;
    }
}