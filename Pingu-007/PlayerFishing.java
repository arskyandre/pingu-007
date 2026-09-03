import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

final class PlayerFishing {

    private static final int CAST_COOLDOWN = 20;

    private final FishingBobber bobber = new FishingBobber();
    private boolean hasFishingRod;
    private boolean fasterFishing;
    private int cooldown;

    void setFishingManager(FishingManager fishingManager) {
        bobber.setFishingManager(fishingManager);
    }

    void update(InputManager input, CameraManager camera, ArrayList<Enemy> enemies,
            int[][] levelData, Player player, boolean controllerActive,
            Vetor2D rightStick, boolean mouseAimActive) {
        if (!hasFishingRod) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
        }

        boolean castOrPull = controllerActive
                ? input.isButtonJustPressed(InputManager.GamepadButton.LT)
                : input.isMouseButtonJustPressed(MouseEvent.BUTTON3);

        if (castOrPull && cooldown == 0) {
            if (GameCore.getDebug()) {
                System.out.println(">>> CLIQUE DIREITO PROCESSADO COM SUCESSO! <<<");
            }

            if (bobber.isAtivo()) {
                bobber.pull();
            } else if (controllerActive) {
                castWithController(player, rightStick);
            } else if (mouseAimActive) {
                double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
                bobber.cast(player, player.soundManager, mouseXWorld, mouseYWorld);
            }
            cooldown = CAST_COOLDOWN;
        }

        bobber.update(enemies, levelData, player);
    }

    private void castWithController(Player player, Vetor2D rightStick) {
        double directionX = Math.cos(player.getAimAngle());
        double directionY = Math.sin(player.getAimAngle());
        if (rightStick.x != 0 || rightStick.y != 0) {
            directionX = rightStick.x;
            directionY = rightStick.y;
        }

        double aimDistance = Math.max(player.getLargura(), player.getAltura()) * 10.0;
        double targetX = player.getX() + player.getLargura() / 2.0 + directionX * aimDistance;
        double targetY = player.getY() + player.getAltura() / 2.0 + directionY * aimDistance;
        bobber.cast(player, player.soundManager, targetX, targetY);
    }

    void draw(Graphics2D graphics) {
        if (hasFishingRod && bobber.isAtivo()) {
            bobber.draw(graphics);
        }
    }

    void resetTransientState() {
        cooldown = 0;
        bobber.reset();
    }

    void resetProgress() {
        resetTransientState();
        hasFishingRod = false;
        fasterFishing = false;
    }

    boolean hasFishingRod() {
        return hasFishingRod;
    }

    void setFishingRod(boolean hasFishingRod) {
        this.hasFishingRod = hasFishingRod;
    }

    boolean hasFasterFishing() {
        return fasterFishing;
    }

    void setFasterFishing(boolean fasterFishing) {
        this.fasterFishing = fasterFishing;
    }
}
