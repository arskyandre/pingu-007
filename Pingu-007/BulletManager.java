
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class BulletManager {

    private ArrayList<Bullet> Bullets = new ArrayList<>();

    public void shoot(double startX, double startY, double dirX, double dirY, BulletOwner owner) {
        Bullets.add(new Bullet(startX, startY, dirX, dirY, owner));
    }

    public void update(CameraManager camera, int telaLargura, int telaAltura, Player player, ArrayList<Enemy> enemies) {
        Bullets.removeIf(b -> !b.isActive());
        for (Bullet b : Bullets) {
            b.update(camera, telaLargura, telaAltura);
            if (!b.isActive()) {
                continue;
            }

            // Hit Registration
            if (b.getOwner() == BulletOwner.PLAYER) {
                for (Enemy e : enemies) {
                    if (b.getCollider().intersects(b.getX(), b.getY(), e.getHurtbox(), e.getX(), e.getY())) {
                        e.receberDano(b.getDano());
                        b.desativar(); // Destroi a bala
                        break; // Sai do loop para a mesma bala não acertar dois inimigos
                    }
                }
            } else if (b.getOwner() == BulletOwner.ENEMY) {
                if (b.getCollider().intersects(b.getX(), b.getY(), player.getHurtbox(), player.getX(), player.getY())) {
                    player.receberDano(b.getDano());
                    b.desativar();
                }
            }
        }
    }

    public void draw(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        g2.setColor(Color.WHITE);
        for (Bullet b : Bullets) {
            // Só desenha se estiver dentro da visão da câmera
            if (camera.onScreen(b.getX(), b.getY(), b.getLargura(), b.getAltura(), telaLargura, telaAltura)) {
                g2.fill(new Ellipse2D.Double(b.getX() - 4, b.getY() - 4, 8, 8));
            }
        }
    }

    public ArrayList<Bullet> getBullets() {
        return Bullets;
    }
}
