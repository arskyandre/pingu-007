
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

public class Renderer {
    public enum BorderState {
        IN, OUT, IDLE
    }

    public BorderState borderState = BorderState.IDLE;
    public boolean modoDebug = false;

    private int cinematicBorderHeight;
    private double borderFadeDuration = 0.7;
    private double borderProgress = 0;
    Boolean preDash = false;

    public void renderizar(Graphics2D g2, CameraManager camera, Player quadrado, InputManager input, int telaLargura,
            int telaAltura, LevelManager lm, BulletManager bulletmanager, ItemManager itemManager,
            EnemyManager enemyManager, ArenaManager arenaManager, Hud HUD, DialogueManager dialogueManager,
            FishingManager fishingManager, NPCManager npcManager, double delta, boolean animateBorder) {

        // Mantem o tamanho da borda proporcional a altura da tela
        cinematicBorderHeight = telaAltura / 8;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, telaLargura, telaAltura);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform originalTransform = g2.getTransform();
        g2.scale(camera.getZoom(), camera.getZoom());
        g2.translate(-camera.getX(), -camera.getY());

        lm.drawBackground(g2, camera, telaLargura, telaAltura);
        lm.drawGround(g2, camera, telaLargura, telaAltura);

        if (arenaManager != null) {
            arenaManager.drawOverlays(g2);
        }

        ArrayList<Object> renderQueue = new ArrayList<>();
        renderQueue.add(quadrado);
        renderQueue.addAll(enemyManager.getEnemies());
        renderQueue.addAll(itemManager.getItems());
        renderQueue.addAll(npcManager.getNpcs());
        renderQueue.sort((Object o1, Object o2) -> {
            double base1 = getRenderBaseY(o1);
            double base2 = getRenderBaseY(o2);
            return Double.compare(base1, base2);
        });

        for (Object entidade : renderQueue) {
            switch (entidade) {
                case Enemy enemy -> {
                    if (!enemy.isDead()) {
                        enemy.draw(g2);
                        enemy.animate(g2, delta);
                    }
                }
                case Player p -> {
                    renderDashEffect(g2, p);
                    p.animate(g2, delta);
                }
                case Item item -> {
                    if (item.isAtivo() && camera.onScreen(item.getX(), item.getY(), item.getLargura(), item.getAltura(),
                            telaLargura, telaAltura)) {
                        item.draw(g2);
                    }
                }
                case NPC npc -> {
                    if (npc.isActive()) {
                        npc.draw(g2);
                    }
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
        HUD.draw(g2, telaLargura, telaAltura, camera, quadrado, enemyManager);

        // Animacao das bordas cinematicas
        if (animateBorder) {
            double progressSpeed = 1.0 / borderFadeDuration;

            if (borderState == BorderState.IN) {
                borderProgress += progressSpeed * delta;

                if (borderProgress >= 1.0) {
                    borderProgress = 1.0;
                    borderState = BorderState.IDLE;
                }
            } else if (borderState == BorderState.OUT) {
                borderProgress -= progressSpeed * delta;

                if (borderProgress <= 0.0) {
                    borderProgress = 0.0;
                    borderState = BorderState.IDLE;
                }
            }
        }
        double eased;

        if (borderState == BorderState.OUT) {
            eased = borderProgress * borderProgress;
        } else {
            eased = 1.0 - (1.0 - borderProgress) * (1.0 - borderProgress);
        }

        int cinematicBorder = (int) (cinematicBorderHeight * eased);

        HUD.player_hearts(g2, quadrado, (int) cinematicBorder);
        HUD.ammobar(g2, telaLargura, telaAltura, quadrado, (int) cinematicBorder);
        fishingManager.render(g2, camera, telaLargura, telaAltura);
        renderMouse(g2, input);

        if (cinematicBorder > 0) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, telaLargura, (int) cinematicBorder);
            g2.fillRect(0, telaAltura - (int) cinematicBorder, telaLargura, (int) cinematicBorder);
        }

        if (dialogueManager != null && dialogueManager.isAtivo()) {
            dialogueManager.renderizar(g2, telaLargura, telaAltura);
        }
    }

    public void setCinematicBorderAnimation(BorderState state) {
        if (state == BorderState.IN) {
            System.out.println("criando borda cinematica");
        } else if (state == BorderState.OUT) {
            System.out.println("destruindo borda cinematica");
        }

        borderState = state;
    }

    private double getRenderBaseY(Object entidade) {
        if (entidade instanceof Entity e) {
            return e.getY() + (e.getBodyCollider() != null
                    ? (e.getBodyCollider().getOffsetY() + e.getBodyCollider().getHeight())
                    : 48);
        }
        if (entidade instanceof Item item) {
            return item.getSortBaseY();
        }
        if (entidade instanceof NPC npc) {
            return npc.getY() + npc.altura;
        }
        return 0;
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
