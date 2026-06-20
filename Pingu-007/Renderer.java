
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

public class Renderer {

    public boolean modoDebug = false;

    Boolean preDash = false;

    public void renderizar(Graphics2D g2, CameraManager camera, Player quadrado, InputManager input, int telaLargura,
            int telaAltura, LevelManager lm, BulletManager bulletmanager, LootManager lootmanager, EnemyManager enemyManager, ArenaManager arenaManager, Hud HUD) {

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, telaLargura, telaAltura);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform originalTransform = g2.getTransform();
        g2.scale(camera.getZoom(), camera.getZoom());
        g2.translate(-camera.getX(), -camera.getY());

        lm.drawBackground(g2, camera, telaLargura, telaAltura);
        lm.drawGround(g2, camera, telaLargura, telaAltura);
        lootmanager.draw(g2, camera, telaLargura, telaAltura);
        ArrayList<Entity> renderQueue = new ArrayList<>();
        renderQueue.add(quadrado);
        renderQueue.addAll(enemyManager.getEnemies());

        renderQueue.sort((Entity e1, Entity e2) -> {
            double base1 = e1.getY() + (e1.getBodyCollider() != null ? (e1.getBodyCollider().getOffsetY() + e1.getBodyCollider().getHeight()) : 48);
            double base2 = e2.getY() + (e2.getBodyCollider() != null ? (e2.getBodyCollider().getOffsetY() + e2.getBodyCollider().getHeight()) : 48);
            return Double.compare(base1, base2);
        });

        for (Entity e : renderQueue) {
            switch (e) {
                case Enemy enemy -> {
                    if (!enemy.isDead()) {
                        enemy.draw(g2);
                        enemy.animate(g2);
                    }
                }
                case Player p -> {
                    renderDashEffect(g2, p);
                    p.animate(g2);
                }
                default -> {
                }
            }
        }

        bulletmanager.draw(g2, camera, telaLargura, telaAltura);
        lm.drawForeground(g2, camera, telaLargura, telaAltura);
        if (modoDebug) {
            renderDebug(g2, camera, quadrado, input); // Hitboxes and Mouse Lines

            // Draw Tiled Objects
            MapDATA mapData = lm.getMapData();
            if (mapData != null && mapData.objects != null) {
                for (TiledObject obj : mapData.objects) {
                    if (obj.isPoint || obj.tipo.equals("spawner")) {
                        // Point Spawner
                        g2.setColor(Color.MAGENTA);
                        g2.fillOval((int) obj.x - 5, (int) obj.y - 5, 10, 10);
                        g2.drawString("Spawn: " + obj.inimigo, (int) obj.x + 10, (int) obj.y);
                    } else if (obj.isPolygon) {
                        // Polygon Trigger
                        Polygon poly = obj.getPolygon();
                        g2.setColor(new Color(0, 255, 255, 75)); // Cyan transparent
                        g2.fillPolygon(poly);
                        g2.setColor(Color.CYAN);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawPolygon(poly);
                        g2.drawString(obj.tipo, (int) obj.x, (int) obj.y);
                    } else {
                        // Standard Rectangle
                        g2.setColor(new Color(255, 255, 0, 75)); // Yellow transparent
                        g2.fillRect((int) obj.x, (int) obj.y, (int) obj.width, (int) obj.height);
                        g2.setColor(Color.YELLOW);
                        g2.setStroke(new BasicStroke(2));
                        g2.drawRect((int) obj.x, (int) obj.y, (int) obj.width, (int) obj.height);
                        g2.drawString(obj.tipo, (int) obj.x, (int) obj.y);
                    }
                }
            }
        }
        g2.setTransform(originalTransform);

        renderMouse(g2, input);
        HUD.draw(g2, telaLargura, telaAltura, camera, quadrado, enemyManager);
    }

    private void drawDebugColliders(Graphics2D g2, Entity e) {
        if (e.getHurtbox() != null) {
            e.getHurtbox().drawDebug(g2, e.getX(), e.getY(), Color.YELLOW);
        }
        if (e.getHitbox() != null) {
            e.getHitbox().drawDebug(g2, e.getX(), e.getY(), Color.RED);
        }
        if (e.getBodyCollider() != null) {
            e.getBodyCollider().drawDebug(g2, e.getX(), e.getY(), Color.BLUE);
        }
    }

    private void renderDashEffect(Graphics2D g2, Player p) {
        if (preDash && p.isEmDash()) {
            double centerX = p.getX() + p.getLargura() / 2.0;
            double centerY = p.getY() + p.getAltura() / 2.0;
            double dashDirX = p.getDashDirX();
            double dashDirY = p.getDashDirY();

            double tipX = centerX + dashDirX * 40;
            double tipY = centerY + dashDirY * 40;
            double perpX = -dashDirY;
            double perpY = dashDirX;
            double baseX = centerX - dashDirX * 20;
            double baseY = centerY - dashDirY * 20;

            Polygon tri = new Polygon();
            tri.addPoint((int) tipX, (int) tipY);
            tri.addPoint((int) (baseX + perpX * 20), (int) (baseY + perpY * 20));
            tri.addPoint((int) (baseX - perpX * 20), (int) (baseY - perpY * 20));

            g2.setColor(Color.PINK);
            g2.fillPolygon(tri);
        }
    }

    private void renderDebug(Graphics2D g2, CameraManager camera,
            Player quadrado, InputManager input) {
        double centerX = quadrado.getX() + quadrado.getLargura() / 2.0;
        double centerY = quadrado.getY() + quadrado.getAltura() / 2.0;
        double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
        double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
        g2.setColor(Color.WHITE);
        g2.drawLine((int) centerX, (int) centerY,
                (int) mouseXWorld, (int) mouseYWorld);
    }

    private void renderMouse(Graphics2D g2, InputManager input) {
        g2.setColor(Color.RED);
        g2.fill(new Ellipse2D.Double(
                input.getMouseX() - 10, input.getMouseY() - 10, 20, 20));
    }
}
