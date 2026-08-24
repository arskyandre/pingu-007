
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;

public class Renderer {

    public enum BorderState {
        IN, OUT, IDLE
    }

    public static final BufferedImage crosshair = LoadSave.GetSpriteAtlas("images/hud/crosshair.png");
    private static boolean renderShadows = true;

    public BorderState borderState = BorderState.IDLE;
    public boolean modoDebug = false;
    private double mouseCircleAlpha = 1.0;
    private static final double MOUSE_CIRCLE_FADE_DURATION = 0.5; // segundos

    // Cache de Strokes
    private static final BasicStroke DEBUG_STROKE_SOLID = new BasicStroke(2f);
    private static final BasicStroke DEBUG_STROKE_DASHED = new BasicStroke(1f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);

    // Cores em cache para o Bounding Box
    private static final Color DEBUG_BOUNDS_FILL = new Color(255, 255, 0, 30);
    private static final Color DEBUG_BOUNDS_LINE = new Color(255, 255, 0, 180);
    private static final Color DEBUG_WHITE_TRANS = new Color(255, 255, 255, 70);

    private int cinematicBorderHeight;
    private final double borderFadeDuration = 0.7;
    private double borderProgress = 0;

    private final ArrayList<Renderable> renderQueue = new ArrayList<>(200);
    private final java.util.Comparator<Renderable> depthComparator = (o1, o2) -> Double.compare(o1.getProfundidade(),
            o2.getProfundidade());

    private final Ellipse2D.Double mouseShape = new Ellipse2D.Double(0, 0, 20, 20);
    public boolean useAntiAliasing = true;

    public static boolean isRenderShadows() {
        return renderShadows;
    }

    public static void setRenderShadows(boolean enabled) {
        renderShadows = enabled;
    }

    public static void toggleRenderShadows() {
        renderShadows = !renderShadows;
    }

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
            Color ColorA,
            Color ColorB,
            double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));

        int red = (int) Math.round(
                ColorA.getRed()
                        + (ColorB.getRed() - ColorA.getRed()) * t);

        int green = (int) Math.round(
                ColorA.getGreen()
                        + (ColorB.getGreen() - ColorA.getGreen()) * t);

        int blue = (int) Math.round(
                ColorA.getBlue()
                        + (ColorB.getBlue() - ColorA.getBlue()) * t);

        int alpha = (int) Math.round(
                ColorA.getAlpha()
                        + (ColorB.getAlpha() - ColorA.getAlpha()) * t);

        return new Color(red, green, blue, alpha);
    }

    public static BufferedImage gaussianBlur(BufferedImage source, int radius, double sigma) {
        if (source == null) {
            return null;
        }
        if (radius <= 0) {
            return source;
        }
        if (sigma <= 0.0) {
            throw new IllegalArgumentException("Sigma must be greater than zero.");
        }

        int kernelSize = radius * 2 + 1;
        float[] kernelData = new float[kernelSize];
        double sigmaSquaredTimesTwo = 2.0 * sigma * sigma;
        double sum = 0.0;

        for (int i = -radius; i <= radius; i++) {
            double weight = Math.exp(-(i * i) / sigmaSquaredTimesTwo);
            kernelData[i + radius] = (float) weight;
            sum += weight;
        }
        for (int i = 0; i < kernelData.length; i++) {
            kernelData[i] /= (float) sum;
        }

        BufferedImage preparedSource = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D preparedGraphics = preparedSource.createGraphics();
        try {
            preparedGraphics.setComposite(AlphaComposite.Src);
            preparedGraphics.drawImage(source, 0, 0, null);
        } finally {
            preparedGraphics.dispose();
        }

        BufferedImage horizontalResult = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage finalResult = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB_PRE);

        ConvolveOp blurOp = new ConvolveOp(
                new Kernel(kernelSize, 1, kernelData),
                ConvolveOp.EDGE_NO_OP,
                null);
        blurOp.filter(preparedSource, horizontalResult);

        blurOp = new ConvolveOp(
                new Kernel(1, kernelSize, kernelData),
                ConvolveOp.EDGE_NO_OP,
                null);
        blurOp.filter(horizontalResult, finalResult);

        return finalResult;
    }

    private static final Color PRE_DAWN_OVERLAY = new Color(90, 48, 70, 105);
    private static final Color DAY_OVERLAY = new Color(255, 255, 255, 0);
    private static final Color AFTERNOON_OVERLAY = new Color(255, 205, 135, 18);
    private static final Color DUSK_OVERLAY = new Color(185, 75, 105, 80);
    private static final Color NIGHT_OVERLAY = new Color(20, 32, 78, 120);

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
            double localProgress = (hour - 9.0) / (16.0 - 9.0);

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

    public void renderizar(Graphics2D g2, CameraManager camera, Player player, InputManager input, int telaLargura,
            int telaAltura, LevelManager lm, BulletManager bulletmanager, ItemManager itemManager,
            EnemyManager enemyManager, ArenaManager arenaManager, Hud HUD, DialogueManager dialogueManager,
            FishingManager fishingManager, NPCManager npcManager, CutsceneManager cutsceneManager,
            boolean renderizarDayNightOverlay, double dayProgress,
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
        renderQueue.add(player);

        if (enemyManager != null && enemyManager.getEnemies() != null) {
            for (Enemy e : enemyManager.getEnemies()) {
                if (e != null && !e.isDead()) {
                    if (camera.onScreen(e.getX(), e.getY(), e.getLargura(), e.getAltura(), telaLargura, telaAltura)) {
                        renderQueue.add(e);
                    }
                }
            }
        }

        if (itemManager != null && itemManager.getItems() != null) {
            for (Item item : itemManager.getItems()) {
                if (item != null && item.isAtivo()) {
                    if (camera.onScreen(item.getX(), item.getY(), item.getLargura(), item.getAltura(), telaLargura,
                            telaAltura)) {
                        renderQueue.add(item);
                    }
                }
            }
        }

        if (npcManager != null && npcManager.getNpcs() != null) {
            for (NPC npc : npcManager.getNpcs()) {
                if (npc != null && npc.isActive()) {
                    if (camera.onScreen(npc.getX(), npc.getY(), npc.getLargura(), npc.getAltura(), telaLargura,
                            telaAltura)) {
                        renderQueue.add(npc);
                    }
                }
            }
        }

        if (arenaManager != null && arenaManager.getObjetosDeCenario() != null) {
            for (MapObject obj : arenaManager.getObjetosDeCenario()) {
                double w = obj.getLargura() > 0 ? obj.getLargura() : GameCore.tiles_size;
                double h = obj.getAltura() > 0 ? obj.getAltura() : GameCore.tiles_size;

                if (camera.onScreen(obj.getX(), obj.getY(), w, h, telaLargura, telaAltura)) {
                    renderQueue.add(obj);
                }
            }
        }
        renderQueue.sort(depthComparator);

        for (Renderable obj : renderQueue) {
            obj.draw(g2, delta);
        }

        bulletmanager.draw(g2, camera, telaLargura, telaAltura);
        lm.drawForeground(g2, camera, telaLargura, telaAltura);

        if (modoDebug) {
            renderDebug(g2, camera, player, input);

            drawDebugColliders(g2, player);
            if (enemyManager != null && enemyManager.getEnemies() != null) {
                for (Enemy e : enemyManager.getEnemies()) {
                    if (e != null && !e.isDead()) {
                        drawDebugColliders(g2, e);
                    }
                }
            }

            if (arenaManager != null) {
                if (arenaManager.getObjetosInstanciadosParaDebug() != null) {
                    for (DebugRenderable obj : arenaManager.getObjetosInstanciadosParaDebug()) {
                        desenharDebugDeObjeto(g2, obj.getDadosTiled(), obj.getHitboxAtual(), camera, telaLargura,
                                telaAltura);
                    }
                }
                if (arenaManager.getTriggersESpawnersParaDebug() != null) {
                    for (TiledObject raw : arenaManager.getTriggersESpawnersParaDebug()) {
                        Shape hitboxCru = raw.isPolygon
                                ? raw.getPolygonShape()
                                : new Rectangle2D.Double(raw.x, raw.y, raw.width, raw.height);
                        desenharDebugDeObjeto(g2, raw, hitboxCru, camera, telaLargura, telaAltura);
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
        if (renderizarDayNightOverlay) {
            drawDayNightOverlay(g2, dayProgress, telaLargura, telaAltura);
        }
        HUD.draw(g2, telaLargura, telaAltura, camera, player, enemyManager, delta, (int) cinematicBorder);
        fishingManager.render(g2, camera, telaLargura, telaAltura, delta);
        double mouseCircleTarget = (mouseCircle && camera.isMouseMiraAtiva()) ? 1.0 : 0.0;
        double mouseCircleFadeSpeed = 1.0 / MOUSE_CIRCLE_FADE_DURATION;

        if (mouseCircleAlpha < mouseCircleTarget) {
            mouseCircleAlpha = Math.min(mouseCircleTarget, mouseCircleAlpha + mouseCircleFadeSpeed * delta);
        } else if (mouseCircleAlpha > mouseCircleTarget) {
            mouseCircleAlpha = Math.max(mouseCircleTarget, mouseCircleAlpha - mouseCircleFadeSpeed * delta);
        }

        if (mouseCircleAlpha > 0.0) {
            renderMouse(g2, input, mouseCircleAlpha, telaAltura);
        }
        if (GameCore.getGameState() == GameState.CUTSCENE) {
            cutsceneManager.draw(g2, telaLargura, telaAltura, delta);
        }
        if (cinematicBorder > 0) {
            camera.setLetterboxAtivo(true);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, telaLargura, (int) cinematicBorder);
            g2.fillRect(0, telaAltura - (int) cinematicBorder, telaLargura, (int) cinematicBorder);
        } else {
            camera.setLetterboxAtivo(false);
        }

        ToastNotifications.draw(g2, telaLargura, telaAltura);

        if (modoDebug) {
            int totalMinutes = (int) (dayProgress * 24.0 * 60.0) % 1440;
            int hour = totalMinutes / 60;
            int minute = totalMinutes % 60;

            debugDrawHorario(g2, hour, minute, telaLargura, telaAltura);
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
        return 0; // Substituído pelo método nativo Renderable::getProfundidade
    }

    private void desenharDebugDeObjeto(Graphics2D g2, TiledObject data, Shape hitbox, CameraManager camera,
            int telaLargura, int telaAltura) {
        if (data == null) {
            return;
        }

        double sx = data.x;
        double sy = data.y;
        double sw = data.width > 0 ? data.width : 16;
        double sh = data.height > 0 ? data.height : 16;

        double checkX = sx;
        double checkY = sy;
        double checkW = sw;
        double checkH = sh;

        if (hitbox != null) {
            Rectangle2D bounds = hitbox.getBounds2D();
            checkX = bounds.getX();
            checkY = bounds.getY();
            checkW = bounds.getWidth();
            checkH = bounds.getHeight();
        }

        if (camera != null && !camera.onScreen(checkX, checkY, checkW, checkH, telaLargura, telaAltura)) {
            return;
        }

        String tipoSeguro = data.tipo != null ? data.tipo.toLowerCase() : "";

        // Spawners
        if (data.isPoint || tipoSeguro.contains("spawn")) {
            switch (tipoSeguro) {
                case "spawn_player" -> {
                    g2.setColor(new Color(50, 205, 50));
                    g2.fillOval((int) sx - 6, (int) sy - 6, 12, 12);
                    g2.drawString("Spawn Player", (int) sx + 10, (int) sy);
                }
                case "spawn_npc" -> {
                    g2.setColor(new Color(30, 144, 255));
                    g2.fillOval((int) sx - 5, (int) sy - 5, 10, 10);
                    String npcNomeStr = (data.npc_nome != null && !data.npc_nome.isEmpty()) ? data.npc_nome : "NPC";
                    g2.drawString("Spawn: " + npcNomeStr, (int) sx + 10, (int) sy);
                }
                default -> {
                    g2.setColor(new Color(255, 105, 180));
                    g2.fillOval((int) sx - 5, (int) sy - 5, 10, 10);
                    String inimigoStr = (data.inimigo != null && !data.inimigo.isEmpty()) ? data.inimigo : "Spawner";
                    g2.drawString("Spawn: " + inimigoStr, (int) sx + 10, (int) sy);
                }
            }
            return;
        }

        g2.setColor(DEBUG_BOUNDS_FILL);
        g2.fillRect((int) sx, (int) sy, (int) sw, (int) sh);

        g2.setColor(DEBUG_BOUNDS_LINE);
        g2.setStroke(DEBUG_STROKE_DASHED);
        g2.drawRect((int) sx, (int) sy, (int) sw, (int) sh);

        g2.setStroke(DEBUG_STROKE_SOLID);

        Color fillColor = null;
        Color outlineColor = null;

        if (tipoSeguro.contains("trigger")) {
            if (tipoSeguro.equalsIgnoreCase("level_trigger")) {
                fillColor = new Color(255, 165, 0, 80);
                outlineColor = Color.ORANGE;
            } else if (tipoSeguro.equalsIgnoreCase("arena_trigger")) {
                fillColor = new Color(138, 43, 226, 80);
                outlineColor = new Color(138, 43, 226);
            } else {
                fillColor = new Color(0, 250, 154, 80);
                outlineColor = new Color(0, 200, 120);
            }
        } else if (data.isPolygon && hitbox != null && !data.collision && !tipoSeguro.equals("colision")) {
            fillColor = new Color(0, 255, 255, 75);
            outlineColor = Color.CYAN;
        } else if (data.collision || tipoSeguro.equals("colision")) {
            fillColor = new Color(255, 0, 0, 90);
            outlineColor = Color.RED;
        } else if (!tipoSeguro.equals("map_object") && !tipoSeguro.isEmpty()) {
            if (tipoSeguro.equalsIgnoreCase("wall") || tipoSeguro.equalsIgnoreCase("door")) {
                fillColor = new Color(255, 0, 255, 90);
                outlineColor = Color.MAGENTA;
            } else {
                fillColor = new Color(255, 255, 0, 75);
                outlineColor = Color.YELLOW;
            }
        }

        // Hitbox ou Trigger
        if (hitbox != null) {
            if (fillColor != null && outlineColor != null) {
                g2.setColor(fillColor);
                g2.fill(hitbox);
                g2.setColor(outlineColor);
                g2.draw(hitbox);
            } else {
                g2.setColor(DEBUG_WHITE_TRANS);
                g2.fill(hitbox);
                g2.setColor(Color.WHITE);
                g2.draw(hitbox);
            }
        } else if (fillColor != null && outlineColor != null) {
            g2.setColor(fillColor);
            g2.fillRect((int) sx, (int) sy, (int) sw, (int) sh);
            g2.setColor(outlineColor);
            g2.drawRect((int) sx, (int) sy, (int) sw, (int) sh);
        }

        if (!tipoSeguro.equals("map_object") && !tipoSeguro.isEmpty()) {
            g2.setColor(outlineColor != null ? outlineColor : Color.YELLOW);
            g2.drawString(data.tipo, (int) sx, (int) sy - 5);
        }
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

        g2.scale(1.0, 1.0);

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
