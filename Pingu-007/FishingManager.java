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

    public FishingManager(Player player, SoundManager sound) {
        soundManager = sound;
        this.player = player;
        this.fishingButton = new IconButton(0, 0, BUTTON_SIZE, IconIndex.FISHING, true);
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

        if (triggered) {
            startFishing();
        }
    }

    /**
     * Finds the row of a valid fishing hole (normal or key) at the given column.
     * Returns -1 if no fishing hole is found — void/abyss holes are ignored.
     */
    private int resolveFishingHoleRow(int mouseRow, int col, int[][] lvlData) {
        if (isFishingHoleAt(mouseRow, col, lvlData))
            return mouseRow;
        if (isFishingHoleAt(mouseRow + 1, col, lvlData))
            return mouseRow + 1;
        return -1;
    }

    /**
     * Returns true only for designated fishing holes (normal or key),
     * NOT for generic void/abyss tiles.
     */
    private boolean isFishingHoleAt(int row, int col, int[][] lvlData) {
        if (lvlData == null || row < 0 || row >= lvlData.length
                || col < 0 || col >= lvlData[row].length) {
            return false;
        }
        int tileID = lvlData[row][col];
        return TileProperties.isFishingHole(tileID) || TileProperties.isKeyFishingHole(tileID);
    }

    /**
     * Returns the HoleType for a given tileID.
     */
    private HoleType getFishingHoleType(int tileID) {
        if (TileProperties.isKeyFishingHole(tileID))
            return HoleType.KEY;
        if (TileProperties.isFishingHole(tileID))
            return HoleType.NORMAL;
        return HoleType.NONE;
    }

    private void repositionButton(CameraManager camera, int screenWidth, int screenHeight) {
        double screenX = (targetWorldX - camera.getX()) * camera.getZoom();
        double screenY = (targetWorldY - camera.getY()) * camera.getZoom();

        int bx = (int) (screenX - BUTTON_SIZE / 2.0);
        int by = (int) (screenY - BUTTON_SIZE - 12);

        bx = Math.max(4, Math.min(bx, screenWidth - BUTTON_SIZE - 4));
        by = Math.max(4, Math.min(by, screenHeight - BUTTON_SIZE - 4));

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

    /**
     * Recompensa baseada no tipo de buraco:
     * NORMAL -> cura o player
     * KEY -> dropa uma chave
     */
    private void onFishCaught() {
        switch (currentHoleType) {
            case NORMAL -> {
                System.out.println("Fish caught! Healing player.");
                player.curar(15);
            }
            case KEY -> {
                System.out.println("Key fish caught! Giving player a key.");
                player.curar(1);
            }
            default -> {
                System.out.println("Fish caught! (unknown hole type)");
            }
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