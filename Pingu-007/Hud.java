import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class Hud {
    public void draw(Graphics2D g2, int telaLargura, int telaAltura, CameraManager camera, Player p, EnemyManager em) {
        int playervida = p.getVida();
        int playervidamax = p.getVidaMax();

        g2.setColor(Color.BLACK);
        g2.fill(new RoundRectangle2D.Double(16, 16, 48, 8, 4, 4));
        g2.setColor(Color.RED);
        g2.fill(new RoundRectangle2D.Double(16 + 1, 16 + 2, 48 * (1.0 * playervida / playervidamax) - 2, 4, 2, 2));

        healthbar_inimigos(g2, telaLargura, telaAltura, camera, em);

    }

    // Desenha barra de vida para inimigos que nao estao com a vida cheia
    void healthbar_inimigos(Graphics2D g2, int telaLargura, int telaAltura, CameraManager camera, EnemyManager em) {
        double camX = camera.getX();
        double camY = camera.getY();
        double camzoom = camera.getZoom();
        ArrayList<Enemy> enemies = em.getEnemies();
        for (Enemy enemy : enemies) {
            int envida = enemy.getVida();
            int envidamax = enemy.getVidaMax();
            if (envida == envidamax) {
                //continue;
            }
            double enX = enemy.getX();
            double enY = enemy.getY();

            g2.setColor(Color.BLACK);
            g2.fill(new RoundRectangle2D.Double(enX - camX, enY - 32 - camY, 48, 8, 4, 4));
            g2.setColor(Color.RED);
            g2.fill(new RoundRectangle2D.Double(enX - camX + 1, enY + 2 - 32 - camY, 48 * (1.0 * envida / envidamax) - 2, 4,2, 2));
        }
    }
}
