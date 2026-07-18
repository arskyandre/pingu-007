import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Gerencia apenas o minigame de pesca (espera -> fisgada -> resultado).
 * Quem decide QUANDO iniciar é o FishingBobber, ao detectar que assentou
 * sobre um buraco de pesca.
 */
public class FishingManager {

    private enum State {
        IDLE, WAITING, BITING, SUCCESS, MISSED
    }

    // tipo do buraco de pesca sendo trabalhado no momento
    public enum HoleType {
        NONE, NORMAL, KEY
    }

    private ItemManager itemManager;
    private SoundManager soundManager;
    private State state = State.IDLE;
    private HoleType currentHoleType = HoleType.NONE;

    private final Player player;
    private final IconButton fishingButton;

    private double targetWorldX, targetWorldY;

    private int waitTimer = 0;
    private int biteTimer = 0;
    private int feedbackTimer = 0;

    // ── Mash de puxão durante o BITING ──
    private double pullProgress = 0.0;

    private static final double NORMAL_PRESS_GAIN = 0.20;
    private static final double NORMAL_DECAY_PER_FRAME = 0.015;

    private static final double HARD_PRESS_GAIN = 0.09;
    private static final double HARD_DECAY_PER_FRAME = 0.020;

    private static final int WAIT_MIN = 180;
    private static final int WAIT_MAX = 600;
    private static final int BITE_WINDOW = 180;
    private static final int FEEDBACK_DURATION = 60;
    private static final int BUTTON_SIZE = 40;
    private static final double BASE_ZOOM = 1.25;

    private static final int START_ANIM_DURATION = 45;
    private int startAnimTimer = 0;
    private static final int BITE_START_ANIM_DURATION = 45;
    private int biteStartAnimTimer = 0;

    private static boolean playerHasKey = false;

    public FishingManager(Player player, SoundManager sound, ItemManager item) {
        itemManager = item;
        soundManager = sound;
        this.player = player;
        this.fishingButton = new IconButton(0, 0, BUTTON_SIZE, IconIndex.FISHING, true);
    }

    public static boolean isPlayerHasKey() {
        return playerHasKey;
    }

    public static void setPlayerHasKey(boolean b) {
        playerHasKey = b;
    }

    public boolean isActive() {
        return state != State.IDLE;
    }

    public void update(InputManager input, CameraManager camera, int[][] lvlData, int screenWidth,
            int screenHeight) {
        if (state == State.IDLE) {
            return;
        }

        if (startAnimTimer > 0) {
            startAnimTimer--;
        }
        if (biteStartAnimTimer > 0) {
            biteStartAnimTimer--;
        }

        repositionButton(camera, screenWidth, screenHeight);
        boolean triggered = fishingButton.update(input) == MenuButton.CLICKED
                || input.isKeyJustPressed(KeyEvent.VK_E);

        switch (state) {
            case WAITING ->
                updateWaiting(input);
            case BITING -> {
                updateBite(triggered);

            }
            case SUCCESS, MISSED ->
                updateFeedback();
            default -> {
            }
        }
    }

    public static boolean isFishingHoleAt(int row, int col, int[][] lvlData) {
        if (lvlData == null || row < 0 || row >= lvlData.length
                || col < 0 || col >= lvlData[row].length) {
            return false;
        }
        int tileID = lvlData[row][col];
        return TileProperties.isFishingHole(tileID) || TileProperties.isKeyFishingHole(tileID);
    }

    public static HoleType getFishingHoleType(int tileID) {
        if (TileProperties.isKeyFishingHole(tileID))
            return HoleType.KEY;
        if (TileProperties.isFishingHole(tileID))
            return HoleType.NORMAL;
        return HoleType.NONE;
    }

    private void repositionButton(CameraManager camera, int screenWidth, int screenHeight) {
        int scaledSize = (int) (BUTTON_SIZE * (camera.getZoom() / BASE_ZOOM));
        fishingButton.setSize(scaledSize, scaledSize);

        double screenX = (targetWorldX - camera.getX()) * camera.getZoom();
        double screenY = (targetWorldY - camera.getY()) * camera.getZoom();

        int bx = (int) (screenX - scaledSize / 2.0);
        int by = (int) (screenY - scaledSize - 12);

        bx = Math.max(4, Math.min(bx, screenWidth - scaledSize - 4));
        by = Math.max(4, Math.min(by, screenHeight - scaledSize - 4));

        fishingButton.setPosition(bx, by);
    }

    public void syncToCamera(CameraManager camera, int screenWidth, int screenHeight) {
        if (state == State.IDLE) {
            return;
        }
        repositionButton(camera, screenWidth, screenHeight);
    }

    public void startFishing(HoleType holeType, double worldX, double worldY) {
        if (state != State.IDLE) {
            return;
        }
        this.currentHoleType = holeType;
        this.targetWorldX = worldX;
        this.targetWorldY = worldY;
        this.pullProgress = 0.0;
        this.startAnimTimer = START_ANIM_DURATION;

        soundManager.playSFX(SoundManager.SFX.FISHING_START);
        state = State.WAITING;
        waitTimer = WAIT_MIN + (int) (Math.random() * (WAIT_MAX - WAIT_MIN));
        player.setBlockInputs(true);
        System.out.println("Started fishing, waiting for a bite...");
    }

    public void cancelFishing() {
        if (state == State.IDLE) {
            return;
        }
        state = State.IDLE;
        currentHoleType = HoleType.NONE;
        waitTimer = 0;
        biteTimer = 0;
        feedbackTimer = 0;
        pullProgress = 0.0;
        startAnimTimer = 0;
        player.setBlockInputs(false);
        System.out.println("Fishing cancelled.");
    }

    private void updateWaiting(InputManager input) {
        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON3) || input.isKeyJustPressed(KeyEvent.VK_E)) {
            cancelFishing();
            return;
        }
        waitTimer--;
        if (waitTimer <= 0) {
            if (firstFlag) {
                ToastNotifications
                        .RequestNotification("Aperte E repetidamente para ajudar o Pingu a puxar o peixe!");
            }
            state = State.BITING;
            biteTimer = BITE_WINDOW;
            biteStartAnimTimer = BITE_START_ANIM_DURATION;
        }
    }

    /**
     * Retorna true se esse buraco esta na dificuldade "dificil" —
     * ou seja, e um buraco KEY e o player ainda nao possui a chave dele.
     * Assim que a chave e obtida (onFishCaught), esse mesmo buraco
     * volta a dificuldade normal em futuras pescarias.
     */
    private boolean isHardBite() {
        return currentHoleType == HoleType.KEY && !playerHasKey;
    }

    private void updateBite(boolean pressedNow) {
        biteTimer--;

        boolean hard = isHardBite();
        double gain = hard ? HARD_PRESS_GAIN : NORMAL_PRESS_GAIN;
        double decay = hard ? HARD_DECAY_PER_FRAME : NORMAL_DECAY_PER_FRAME;

        if (pressedNow) {
            pullProgress = Math.min(1.0, pullProgress + gain);
        } else
            pullProgress = Math.max(0.0, pullProgress - decay);

        if (pullProgress >= 1.0) {
            fishCaught();
            return;
        }

        if (biteTimer <= 0) {
            fishEscaped();
        }
    }

    private void fishCaught() {
        state = State.SUCCESS;
        feedbackTimer = FEEDBACK_DURATION;
        onFishCaught();
    }

    private void fishEscaped() {
        state = State.MISSED;
        feedbackTimer = FEEDBACK_DURATION;
        System.out.println("The fish got away...");
    }

    private void updateFeedback() {
        feedbackTimer--;
        if (feedbackTimer <= 0) {
            finishFishing();
        }
    }

    private void finishFishing() {
        state = State.IDLE;
        currentHoleType = HoleType.NONE;
        pullProgress = 0.0;
        player.setBlockInputs(false);
    }

    /** Quando o player pesca */
    private void onFishCaught() {
        if (firstFlag) {
            firstFlag = false;
            ToastNotifications.RequestNotification("Parabéns! O Pingu pescou algo!");
        }
        soundManager.playSFX(SoundManager.SFX.NOOT_NOOT);
        if (currentHoleType == HoleType.NORMAL || playerHasKey) {
            double rand = Math.random();
            if (currentHoleType == HoleType.KEY) {
                itemManager.spawn(new MoedaItem(targetWorldX, targetWorldY + 24, 15));
                System.out.println("peixe pego, dropando moeda");
            } else if (rand > 0.66) {
                itemManager.spawn(new AmmoPackItem(targetWorldX, targetWorldY + 24));
                System.out.println("peixe pego, dropando municao");
            } else if (rand < 0.05) {
                itemManager.spawn(new MoedaItem(targetWorldX, targetWorldY + 24, 15));
                System.out.println("peixe pego, dropando moeda");
            } else {
                itemManager.spawn(new HealthPackItem(targetWorldX, targetWorldY + 24));
                System.out.println("peixe pego, dropando cura");
            }
        } else {
            System.out.printf("encontrou a chave! spawnando em %f, %f\n", 114.3 * GameCore.tiles_size,
                    57.7 * GameCore.tiles_size);
            itemManager.spawn(new MoedaItem(targetWorldX, targetWorldY + 24, 30));
            itemManager.spawn(new KeyItem(targetWorldX, targetWorldY + 32));
            setPlayerHasKey(true);
        }
    }

    private boolean firstFlag = true;

    public void setfirstFlag(boolean set) {
        firstFlag = set;

    }

    public boolean getfirstFlag() {
        return firstFlag;
    }

    public void render(Graphics2D g2, CameraManager camera, int screenWidth, int screenHeight, double delta) {
        if (state == State.IDLE) {
            return;
        }

        double screenX = (targetWorldX - camera.getX()) * camera.getZoom();
        double screenY = (targetWorldY - camera.getY()) * camera.getZoom();

        if (startAnimTimer > 0) {
            double startFrac = (START_ANIM_DURATION - startAnimTimer) / (double) START_ANIM_DURATION;
            drawWaterRings(g2, screenX, screenY, startFrac, 3, 46, 6,
                    new Color(180, 230, 255), 200, 2.5f, camera.getZoom());
        }
        if (biteStartAnimTimer > 0) {
            double biteFrac = (BITE_START_ANIM_DURATION - biteStartAnimTimer) / (double) BITE_START_ANIM_DURATION;
            drawWaterRings(g2, screenX, screenY, biteFrac, 3, 46, 6,
                    new Color(180, 230, 255), 200, 2.5f, camera.getZoom());
        }
        switch (state) {
            case BITING -> {
                drawBite(g2, screenX, screenY);
            }
            case SUCCESS ->
                drawFeedback(g2, screenX, screenY, true, new Color(120, 220, 120), delta);
            case MISSED ->
                drawFeedback(g2, screenX, screenY, false, new Color(220, 90, 90), delta);
            default -> {
            }
        }
    }

    private void drawBite(Graphics2D g2, double screenX, double screenY) {
        Rectangle rect = fishingButton.getRect();
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;

        boolean hard = isHardBite();

        float progress = biteTimer / (float) BITE_WINDOW;
        double pulse = 0.5 + 0.5 * Math.sin(biteTimer * 0.6);
        int radius = (int) (rect.width * 0.75 + pulse * 8);

        Color ringColor = new Color(255, (int) (60 + 160 * progress), 40, 230);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(ringColor);
        g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // Barra de progresso do puxão
        int barW = 80;
        int barH = 12;
        int barX = centerX - barW / 2;
        int barY = rect.y - 30;

        g2.setColor(new Color(20, 20, 20, 200));
        g2.fillRect(barX, barY, barW, barH);

        int fillW = (int) (barW * pullProgress);
        Color fillColor = hard ? new Color(255, 100, 40) : new Color(100, 220, 255);
        g2.setColor(fillColor);
        g2.fillRect(barX, barY, fillW, barH);

        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barW, barH);

        // Texto indicativo
        g2.setFont(MenuButton.pixelFont.deriveFont(hard ? 10f : 12f));
        FontMetrics fm = g2.getFontMetrics();
        String texto = "[E]";
        int tx = centerX - fm.stringWidth(texto) / 2;
        int ty = barY - 6;

        g2.setColor(Color.BLACK);
        g2.drawString(texto, tx + 1, ty + 1);
        g2.setColor(Color.ORANGE);
        g2.drawString(texto, tx, ty);
    }

    private void drawWaterRings(Graphics2D g2, double centerX, double centerY, double frac,
            int ringCount, double maxRadius, double baseRadius, Color ringBase, int maxAlpha, float strokeWidth,
            double zoom) {
        frac = Math.max(0.0, Math.min(1.0, frac));
        double scale = 2.0 / 3.0 * zoom / BASE_ZOOM;
        maxRadius *= scale;
        baseRadius *= scale;
        strokeWidth *= scale;
        for (int i = 0; i < ringCount; i++) {
            double stagger = i * 0.18;
            double ringFrac = (frac - stagger) / (1.0 - stagger);
            if (ringFrac <= 0) {
                continue;
            }
            ringFrac = Math.min(1.0, ringFrac);

            double radius = baseRadius + ringFrac * maxRadius;
            int alpha = (int) (Math.max(0, 1.0 - ringFrac) * maxAlpha);
            if (alpha <= 0) {
                continue;
            }

            g2.setColor(new Color(ringBase.getRed(), ringBase.getGreen(), ringBase.getBlue(), alpha));
            g2.setStroke(new BasicStroke(strokeWidth));
            double h = radius;
            g2.draw(new Ellipse2D.Double(centerX - radius, centerY - h / 2, radius * 2, h));
        }
    }

    private void drawFeedback(Graphics2D g2, double screenX, double screenY, boolean pescou, Color color,
            double delta) {
    }
}