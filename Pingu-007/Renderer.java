
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

import java.awt.image.BufferedImage;

public class Renderer {

    public enum BorderState {
        IN, OUT, IDLE
    }

    public static final BufferedImage crosshair = LoadSave.GetSpriteAtlas("images/hud/crosshair.png");

    public BorderState borderState = BorderState.IDLE;
    public boolean modoDebug = false;
    private double mouseCircleAlpha = 1.0;
    private static final double MOUSE_CIRCLE_FADE_DURATION = 0.5; // segundos

    private int cinematicBorderHeight;
    private final double borderFadeDuration = 0.7;
    private double borderProgress = 0;
    // Boolean preDash = false;

    private final ArrayList<Object> renderQueue = new ArrayList<>(200);
    private final java.util.Comparator<Object> depthComparator = (o1, o2) -> Double.compare(getRenderBaseY(o1),
            getRenderBaseY(o2));
    // private final Polygon dashPoly = new Polygon();
    private final Ellipse2D.Double mouseShape = new Ellipse2D.Double(0, 0, 20, 20);
    public boolean useAntiAliasing = true;

    public void setBorderProgress(double prog) {
        borderProgress = prog;
    }

    public int getOffset() {
        double eased;

        if (borderState == BorderState.OUT) {
            eased = borderProgress * borderProgress;
        } else {
            eased = 1.0 - (1.0 - borderProgress) * (1.0 - borderProgress);
        }

        int cinematicBorder = (int) (cinematicBorderHeight * eased);
        return cinematicBorder;
    }

    public static Color interpolateColor(
            Color startColor,
            Color endColor,
            double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));

        int red = (int) Math.round(
                startColor.getRed()
                        + (endColor.getRed() - startColor.getRed()) * t);

        int green = (int) Math.round(
                startColor.getGreen()
                        + (endColor.getGreen() - startColor.getGreen()) * t);

        int blue = (int) Math.round(
                startColor.getBlue()
                        + (endColor.getBlue() - startColor.getBlue()) * t);

        int alpha = (int) Math.round(
                startColor.getAlpha()
                        + (endColor.getAlpha() - startColor.getAlpha()) * t);

        return new Color(red, green, blue, alpha);
    }

    private static final Color DAY_OVERLAY = new Color(255, 255, 255, 0);
    private static final Color AFTERNOON_OVERLAY = new Color(255, 205, 135, 18);
    private static final Color DUSK_OVERLAY = new Color(185, 75, 105, 80);
    private static final Color NIGHT_OVERLAY = new Color(20, 32, 78, 120);
    private static final Color PRE_DAWN_OVERLAY = new Color(90, 48, 70, 105);

    private Color getDayNightOverlayColor(double dayProgress) {
        double hour = dayProgress * 24.0;

        if (hour < 4.0) {
            return NIGHT_OVERLAY;
        }
        if (hour < 5.5) {
            double localProgress = (hour - 4.0) / (5.5 - 4.0);

            return interpolateColor(
                    NIGHT_OVERLAY,
                    PRE_DAWN_OVERLAY,
                    localProgress);
        }

        if (hour < 9.0) {
            double localProgress = (hour - 5.5) / (9.0 - 5.5);

            return interpolateColor(
                    PRE_DAWN_OVERLAY,
                    DAY_OVERLAY,
                    localProgress);
        }

        if (hour < 16.0) {
            double localProgress = (hour - 9.0) / (16.0 - 6.5);

            return interpolateColor(
                    DAY_OVERLAY,
                    AFTERNOON_OVERLAY,
                    localProgress);
        }

        if (hour < 19.0) {
            double localProgress = (hour - 16.0) / (19.0 - 16.0);

            return interpolateColor(
                    AFTERNOON_OVERLAY,
                    DUSK_OVERLAY,
                    localProgress);
        }

        if (hour < 21.5) {
            double localProgress = (hour - 19.0) / (21.5 - 19.0);

            return interpolateColor(
                    DUSK_OVERLAY,
                    NIGHT_OVERLAY,
                    localProgress);
        }

        return NIGHT_OVERLAY;
    }

    private void drawDayNightOverlay(Graphics2D g2, double dayProgress, int telaLargura, int telaAltura) {

        if (modoDebug) {
            int totalMinutes = (int) (dayProgress * 24.0 * 60.0) % 1440;

            int hour = totalMinutes / 60;
            int minute = totalMinutes % 60;
            debugDrawHorario(g2, hour, minute, telaLargura, telaAltura);
        }
        Color overlayColor = getDayNightOverlayColor(dayProgress);

        if (overlayColor.getAlpha() <= 0) {
            return;
        }

        Composite originalComposite = g2.getComposite();

        Color originalColor = g2.getColor();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(overlayColor);

        g2.fillRect(
                0,
                0,
                telaLargura,
                telaAltura);

        g2.setComposite(originalComposite);
        g2.setColor(originalColor);
    }

    public void debugDrawHorario(Graphics2D g2, int hour, int minute, int telaLargura, int telaAltura) {

        Color originalColor = g2.getColor();
        g2.setColor(Color.BLACK);
        Font old = g2.getFont();
        g2.setFont(GameCore.pixelFont.deriveFont(Font.BOLD, 16f));
        String time = "DEBUG: Horário: " + Integer.toString(hour) + ":" + Integer.toString(minute);
        g2.drawString(time, (int) ((telaLargura - g2.getFontMetrics().getStringBounds(time, g2).getWidth()) / 2),
                (int) (24 + g2.getFontMetrics().getStringBounds(time, g2).getHeight()));
        g2.setFont(old);
        g2.setColor(originalColor);
    }

    public void renderizar(Graphics2D g2, CameraManager camera, Player quadrado, InputManager input, int telaLargura,
            int telaAltura, LevelManager lm, BulletManager bulletmanager, ItemManager itemManager,
            EnemyManager enemyManager, ArenaManager arenaManager, Hud HUD, DialogueManager dialogueManager,
            FishingManager fishingManager, NPCManager npcManager, CutsceneManager cutsceneManager, double dayProgress,
            double delta,
            boolean animateBorder, boolean mouseCircle) {

        // Mantem o tamanho da borda proporcional a altura da tela
        cinematicBorderHeight = telaAltura / 8;
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, telaLargura, telaAltura);

        // Anti-Aliasing e Interpolação
        if (useAntiAliasing) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        AffineTransform originalTransform = g2.getTransform();
        g2.scale(camera.getZoom(), camera.getZoom());
        g2.translate(-camera.getX(), -camera.getY());

        lm.drawBackground(g2, camera, telaLargura, telaAltura);
        if (cutsceneManager.isWallRevealAtiva()) {
            lm.drawGround(g2, camera, telaLargura, telaAltura,
                    cutsceneManager.getWallFadeRect(), cutsceneManager.getWallFadeAlpha(),
                    cutsceneManager.getWallShakeX(), cutsceneManager.getWallShakeY());
        } else {
            lm.drawGround(g2, camera, telaLargura, telaAltura);
        }

        if (arenaManager != null) {
            arenaManager.drawOverlays(g2);
        }

        renderQueue.clear();
        renderQueue.add(quadrado);
        renderQueue.addAll(enemyManager.getEnemies());
        renderQueue.addAll(itemManager.getItems());
        renderQueue.addAll(npcManager.getNpcs());

        renderQueue.sort(depthComparator);

        for (Object entidade : renderQueue) {
            switch (entidade) {
                case Enemy enemy -> {
                    if (!enemy.isDead()) {
                        enemy.draw(g2);
                        enemy.animate(g2, delta);
                    }
                }
                case Player p -> {
                    // renderDashEffect(g2, p);
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
                        npc.draw(g2, delta);
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
        int cinematicBorder = getOffset();
        drawDayNightOverlay(g2, dayProgress, telaLargura, telaAltura);
        HUD.draw(g2, telaLargura, telaAltura, camera, quadrado, enemyManager, delta, (int) cinematicBorder);
        fishingManager.render(g2, camera, telaLargura, telaAltura, delta);
        double mouseCircleTarget = mouseCircle ? 1.0 : 0.0;
        double mouseCircleFadeSpeed = 1.0 / MOUSE_CIRCLE_FADE_DURATION;

        if (mouseCircleAlpha < mouseCircleTarget) {
            mouseCircleAlpha = Math.min(mouseCircleTarget, mouseCircleAlpha + mouseCircleFadeSpeed * delta);
        } else if (mouseCircleAlpha > mouseCircleTarget) {
            mouseCircleAlpha = Math.max(mouseCircleTarget, mouseCircleAlpha - mouseCircleFadeSpeed * delta);
        }

        if (mouseCircleAlpha > 0.0) {
            renderMouse(g2, input, mouseCircleAlpha, telaAltura);
        }
        if (GameCore.getGameState() == GameState.CUTSCENE)
            cutsceneManager.draw(g2, telaLargura, telaAltura, delta);
        if (cinematicBorder > 0) {
            camera.setLetterboxAtivo(true);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, telaLargura, (int) cinematicBorder);
            g2.fillRect(0, telaAltura - (int) cinematicBorder, telaLargura, (int) cinematicBorder);
        } else
            camera.setLetterboxAtivo(false);

        ToastNotifications.draw(g2, telaLargura, telaAltura);
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

    public void setAntiAliasing(boolean aa) {
        this.useAntiAliasing = aa;
        System.out.println("Anti-Aliasing: " + (aa ? "LIGADO" : "DESLIGADO"));
    }

    public void toggleAntiAliasing() {
        setAntiAliasing(!this.useAntiAliasing);
    }

    private void renderDashEffect(Graphics2D g2, Player p) {
        // TODO: talvez implementar afterimages no pingu e inimigos quando derem dash
        /*
         * if (preDash && p.isEmDash()) {
         * double centerX = p.getX() + p.getLargura() / 2.0;
         * double centerY = p.getY() + p.getAltura() / 2.0;
         * double dashDirX = p.getDashDirX();
         * double dashDirY = p.getDashDirY();
         * 
         * double tipX = centerX + dashDirX * 40;
         * double tipY = centerY + dashDirY * 40;
         * double perpX = -dashDirY;
         * double perpY = dashDirX;
         * double baseX = centerX - dashDirX * 20;
         * double baseY = centerY - dashDirY * 20;
         * 
         * Polygon tri = new Polygon();
         * tri.addPoint((int) tipX, (int) tipY);
         * tri.addPoint((int) (baseX + perpX * 20), (int) (baseY + perpY * 20));
         * tri.addPoint((int) (baseX - perpX * 20), (int) (baseY - perpY * 20));
         * 
         * g2.setColor(Color.PINK);
         * g2.fillPolygon(tri);
         * }
         */
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

    private void renderMouse(
            Graphics2D g2,
            InputManager input,
            double alpha,
            int telaAltura) {
        Composite originalComposite = g2.getComposite();
        RenderingHints originalHints = (RenderingHints) g2.getRenderingHints().clone();

        g2.setComposite(
                AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        (float) alpha));

        // Raw pixel scaling: no smoothing.
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        double scale = telaAltura / 672.0;
        double dampenedScale = 1.0 + (scale - 1.0) * 0.5;

        int size = (int) Math.round(32 * dampenedScale);

        int mouseX = input.getMouseX() - size / 2;
        int mouseY = input.getMouseY() - size / 2;

        g2.drawImage(
                crosshair,
                mouseX,
                mouseY,
                size,
                size,
                null);

        g2.setRenderingHints(originalHints);
        g2.setComposite(originalComposite);
    }
}
