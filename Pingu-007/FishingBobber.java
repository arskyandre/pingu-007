
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class FishingBobber {

    private double x, y;
    private double velX, velY;
    private boolean ativo = false;
    private Enemy hookedEnemy = null;
    private Player owner;

    private final double MAX_DIST = 350.0; // Distância máxima antes da linha quebrar
    private final double BASE_PULL_FORCE = 40.0; // Força base do puxão 

    public void cast(Player owner, double targetX, double targetY) {
        this.owner = owner;
        this.x = owner.getX() + owner.getLargura() / 2.0;
        this.y = owner.getY() + owner.getAltura() / 2.0;
        this.ativo = true;
        this.hookedEnemy = null;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.hypot(dx, dy);

        double force = 25.0;
        if (dist > 0) {
            this.velX = (dx / dist) * force;
            this.velY = (dy / dist) * force;
        }
    }

    public void pull() {
        if (!ativo) {
            return;
        }

        if (hookedEnemy != null && !hookedEnemy.isDead) {
            double originX = owner.getX() + owner.getLargura() / 2.0;
            double originY = owner.getY() + owner.getAltura() / 2.0;

            double dist = Math.hypot(hookedEnemy.getX() + hookedEnemy.getLargura() / 2.0 - originX,
                    hookedEnemy.getY() + hookedEnemy.getAltura() / 2.0 - originY);

            double distanceFactor = Math.min(Math.max(dist / MAX_DIST, 0.1), 1.0);
            double finalForce = BASE_PULL_FORCE * distanceFactor;

            hookedEnemy.serPuxado(originX, originY, finalForce);
        }
        reset();
    }

    public void reset() {
        this.ativo = false;
        if (hookedEnemy != null) {
            hookedEnemy.isHooked = false;
            hookedEnemy = null;
        }
    }

    public void update(ArrayList<Enemy> enemies, int[][] lvlData) {
        if (!ativo) {
            return;
        }

        double ownerCX = owner.getX() + owner.getLargura() / 2.0;
        double ownerCY = owner.getY() + owner.getAltura() / 2.0;

        if (Math.hypot(x - ownerCX, y - ownerCY) > MAX_DIST) {
            reset();
            return;
        }

        if (hookedEnemy != null) {
            if (hookedEnemy.isDead) {
                reset();
                return;
            }
            this.x = hookedEnemy.getX() + hookedEnemy.getLargura() / 2.0;
            this.y = hookedEnemy.getY() + hookedEnemy.getAltura() / 2.0;

            hookedEnemy.velX *= 0.2;
            hookedEnemy.velY *= 0.2;
        } else {
            x += velX;
            y += velY;

            velX *= 0.90;
            velY *= 0.90;

            for (Enemy e : enemies) {
                if (!e.isDead && !e.isInvulneravel && e.podeSerPuxado) {
                    if (e.getX() < this.x && e.getX() + e.getLargura() > this.x
                            && e.getY() < this.y && e.getY() + e.getAltura() > this.y) {
                        hookedEnemy = e;
                        e.isHooked = true;
                        break;
                    }
                }
            }
            if (lvlData != null) {
                int col = (int) (x / GameCore.tiles_size);
                int row = (int) (y / GameCore.tiles_size);
                if (row >= 0 && row < lvlData.length && col >= 0 && col < lvlData[0].length) {
                    if (TileProperties.isSolid(lvlData[row][col])) {
                        velX = 0;
                        velY = 0;
                    }
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!ativo) {
            return;
        }

        double ownerCX = owner.getX() + owner.getLargura() / 2.0;
        double ownerCY = owner.getY() + owner.getAltura() / 2.0;
        double dist = Math.hypot(x - ownerCX, y - ownerCY);

        // Feedback Visual da Tensão da Linha
        if (hookedEnemy != null && dist > MAX_DIST * 0.8) {
            // Se estiver acima de 80% do limite de quebra, a linha pisca em vermelho
            if ((System.currentTimeMillis() / 100) % 2 == 0) {
                g2.setColor(Color.RED);
            } else {
                g2.setColor(Color.WHITE);
            }
        } else {
            g2.setColor(new Color(200, 200, 200, 180)); // Cor normal da linha
        }

        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine((int) ownerCX, (int) ownerCY, (int) x, (int) y);

        // Desenha a boia/anzol
        g2.setColor(Color.RED);
        g2.fillOval((int) x - 4, (int) y - 4, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval((int) x - 2, (int) y - 2, 4, 4);
    }

    public boolean isAtivo() {
        return ativo;
    }
}
