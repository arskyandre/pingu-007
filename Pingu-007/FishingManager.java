import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * Gerencia o minigame de pesca, bloqueia os inputs do player
 * enquanto estiver no minigame mas sem pausar o jogo(inimigos ainda atacam)
 */
public class FishingManager {

    private enum State {
        IDLE, WAITING, BITING, SUCCESS, MISSED
    }

    // type of the current fishing hole being targeted
    private enum HoleType {
        NONE, NORMAL, KEY
    }

    private ItemManager itemManager;
    private SoundManager soundManager;
    private State state = State.IDLE;
    private HoleType currentHoleType = HoleType.NONE;

    private final Player player;
    private final IconButton fishingButton;

    private boolean targetValid = false;
    private double targetWorldX, targetWorldY;

    private int waitTimer = 0;
    private int biteTimer = 0;
    private int feedbackTimer = 0;

    private static final int WAIT_MIN = 180;
    private static final int WAIT_MAX = 600;
    private static final int BITE_WINDOW = 45;
    private static final int FEEDBACK_DURATION = 60;
    private static final double RANGE = GameCore.tiles_size * 2;
    private static final int BUTTON_SIZE = 40;
    private static final double BASE_ZOOM = 1.25;

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
        if (!player.hasFishingRod()) {
            targetValid = false;
            return;
        }
        if (state == State.IDLE) {
            updateTarget(input, camera, lvlData, screenWidth, screenHeight);
            return;
        }

        repositionButton(camera, screenWidth, screenHeight);
        boolean triggered = fishingButton.update(input) == MenuButton.CLICKED
                || input.isKeyJustPressed(KeyEvent.VK_E);

        switch (state) {
            case WAITING ->
                updateWaiting();
            case BITING ->
                updateBite(triggered);
            case SUCCESS, MISSED ->
                updateFeedback();
            default -> {
            }
        }
    }

    private void updateTarget(InputManager input, CameraManager camera, int[][] lvlData, int screenWidth,
            int screenHeight) {
        double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
        double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();

        int col = (int) (mouseXWorld / GameCore.tiles_size);
        int row = (int) (mouseYWorld / GameCore.tiles_size);

        targetValid = false;
        currentHoleType = HoleType.NONE;

        // resolve which row contains a valid fishing hole
        int holeRow = resolveFishingHoleRow(row, col, lvlData);

        if (holeRow != -1) {
            double centerX = col * GameCore.tiles_size + GameCore.tiles_size / 2.0;
            double centerY = holeRow * GameCore.tiles_size + GameCore.tiles_size / 2.0;
            double playerCenterX = player.getX() + player.getLargura() / 2.0;
            double playerCenterY = player.getY() + player.getAltura() / 2.0;

            if (Math.hypot(centerX - playerCenterX, centerY - playerCenterY) <= RANGE) {
                targetValid = true;
                targetWorldX = centerX;
                targetWorldY = centerY;
                currentHoleType = getFishingHoleType(lvlData[holeRow][col]);
            }
        }

        if (!targetValid) {
            return;
        }

        repositionButton(camera, screenWidth, screenHeight);
        boolean triggered = fishingButton.update(input) == MenuButton.CLICKED
                || input.isKeyJustPressed(KeyEvent.VK_E);

        if (triggered && player.getIscas() > 0) {
            startFishing();
            player.addIscas(-1);
        }
    }

    private int resolveFishingHoleRow(int mouseRow, int col, int[][] lvlData) {
        if (isFishingHoleAt(mouseRow, col, lvlData))
            return mouseRow;
        if (isFishingHoleAt(mouseRow + 1, col, lvlData))
            return mouseRow + 1;
        return -1;
    }

    private boolean isFishingHoleAt(int row, int col, int[][] lvlData) {
        if (lvlData == null || row < 0 || row >= lvlData.length
                || col < 0 || col >= lvlData[row].length) {
            return false;
        }
        int tileID = lvlData[row][col];
        return TileProperties.isFishingHole(tileID) || TileProperties.isKeyFishingHole(tileID);
    }

    private HoleType getFishingHoleType(int tileID) {
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
            if (targetValid) {
                repositionButton(camera, screenWidth, screenHeight);
            }
            return;
        }
        repositionButton(camera, screenWidth, screenHeight);
    }

    private void startFishing() {
        soundManager.playSFX(SoundManager.SFX.FISHING_START);
        state = State.WAITING;
        waitTimer = WAIT_MIN + (int) (Math.random() * (WAIT_MAX - WAIT_MIN));
        player.setBlockInputs(true);
        System.out.println("Started fishing, waiting for a bite...");
    }

    private void updateWaiting() {
        waitTimer--;
        if (waitTimer <= 0) {
            state = State.BITING;
            biteTimer = BITE_WINDOW;
        }
    }

    private void updateBite(boolean triggered) {
        biteTimer--;

        if (triggered) {
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
        targetValid = false;
        currentHoleType = HoleType.NONE;
        player.setBlockInputs(false);
    }

    /** Quando o player pesca */
    private void onFishCaught() {
        if (currentHoleType == HoleType.NORMAL || playerHasKey) {
            double rand = Math.random();
            if (rand > 0.66) {
                itemManager.spawn(new AmmoPackItem(targetWorldX, targetWorldY + 24));
                System.out.println("peixe pego, dropando municao");
            } else if (rand > 0.61) {
                itemManager.spawn(new MoedaItem(targetWorldX, targetWorldY + 24, 10));
                System.out.println("peixe pego, dropando moeda");
            } else {
                itemManager.spawn(new HealthPackItem(targetWorldX, targetWorldY + 24));
                System.out.println("peixe pego, dropando cura");
            }
        } else {
            System.out.printf("encontrou a chave! spawnando em %f, %f\n", 114.3 * GameCore.tiles_size,
                    57.7 * GameCore.tiles_size);
            itemManager.spawn(new KeyItem(114.3 * GameCore.tiles_size, 57.7 * GameCore.tiles_size));
            itemManager.spawn(new MoedaItem(113. * GameCore.tiles_size, 57.7 * GameCore.tiles_size, 30));
            setPlayerHasKey(true);
        }
    }

    public void render(Graphics2D g2, CameraManager camera, int screenWidth, int screenHeight) {
        if (state == State.IDLE) {
            if (targetValid) {
                fishingButton.draw(g2);
            }
            return;
        }

        double screenX = (targetWorldX - camera.getX()) * camera.getZoom();
        double screenY = (targetWorldY - camera.getY()) * camera.getZoom();

        switch (state) {
            case WAITING ->
                drawWaiting(g2, screenX, screenY);
            case BITING ->
                drawBite(g2, screenX, screenY);
            case SUCCESS ->
                drawFeedback(g2, screenX, screenY, "FISH CAUGHT!", new Color(120, 220, 120));
            case MISSED ->
                drawFeedback(g2, screenX, screenY, "GOT AWAY...", new Color(220, 90, 90));
            default -> {
            }
        }
        if (state == State.BITING)
            fishingButton.draw(g2);
    }

    private void drawWaiting(Graphics2D g2, double screenX, double screenY) {
        g2.setColor(new Color(255, 255, 255, 160));
        g2.fillOval((int) screenX - 4, (int) screenY - 30, 8, 8);
    }

    private void drawBite(Graphics2D g2, double screenX, double screenY) {
        Rectangle rect = fishingButton.getRect();
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;

        // 1.0 right as the bite starts -> 0.0 as the window closes
        float progress = biteTimer / (float) BITE_WINDOW;
        double pulse = 0.5 + 0.5 * Math.sin(biteTimer * 0.6);
        int radius = (int) (rect.width * 0.75 + pulse * 8);

        // Ring shifts from yellow toward red as time runs out
        Color ringColor = new Color(255, (int) (60 + 160 * progress), 40, 230);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(ringColor);
        g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        g2.setFont(MenuButton.pixelFont.deriveFont(18f));
        FontMetrics fm = g2.getFontMetrics();
        String texto = "!";
        int tx = centerX - fm.stringWidth(texto) / 2;
        int ty = rect.y - 10;

        g2.setColor(Color.BLACK);
        g2.drawString(texto, tx + 1, ty + 1);
        g2.setColor(Color.YELLOW);
        g2.drawString(texto, tx, ty);
    }

    private void drawFeedback(Graphics2D g2, double screenX, double screenY, String text, Color color) {
        g2.setFont(MenuButton.pixelFont.deriveFont(9f));
        FontMetrics fm = g2.getFontMetrics();
        int textX = (int) screenX - fm.stringWidth(text) / 2;
        int textY = (int) screenY - 30;

        g2.setColor(Color.BLACK);
        g2.drawString(text, textX + 1, textY + 1);
        g2.setColor(color);
        g2.drawString(text, textX, textY);
    }
}