import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class Player extends Entity {

    public enum GunType {
        PISTOL,
        SHOTGUN
    }

    private static final int DASH_COOLDOWN = 60;
    private static final int DASH_DURATION = 28;
    private static final double DASH_FORCE = 15.0;
    private static final double DASH_FRICTION = 0.90;
    private static final double DASH_CONTROL = 0.50;
    private static final int SIX_SEVEN_COOLDOWN = 60;
    private static final int DAMAGE_IFRAMES_DURATION = 60;
    private static final int DASH_IFRAMES_GRACE = 15;

    private final double largura;
    private final double altura;
    private final PlayerCombat combat;
    private final PlayerFishing fishing;
    private final PlayerProgress progress;
    private final PlayerRenderer renderer;

    private Direction direction = Direction.DOWN;
    private double aimAngle;
    private boolean blockInputs;

    private double velocityOverrideX;
    private double velocityOverrideY;
    private int velocityOverrideUpdatesRemaining;

    private boolean podeDash = true;
    private boolean emDash;
    private int dashCooldownTimer;
    private int dashDurationTimer;
    private double dashDirX;
    private double dashDirY;
    private double lastMovementDirectionX;
    private double lastMovementDirectionY;

    private boolean hasPoderSixSeven;
    private int sixSevenCooldownTimer;
    private int iFramesTimer;
    private boolean damageReceived;

    private int[][] levelData;

    public Player(double startX, double startY, double largura, double altura, BulletManager bulletManager,
            SoundManager soundManager, ArenaManager arenaManager) {
        super(arenaManager, soundManager);
        this.x = startX;
        this.y = startY;
        this.aceleracao = 1.0;
        this.atritoAtual = 0.85;
        this.velocidadeMax = 30;
        this.largura = largura;
        this.altura = altura;
        this.bodyCollider = new Collider(0, altura / 2.0, largura, altura / 2.0);
        this.hurtbox = new Collider(0, 0, largura, altura);
        this.vidaMaxima = 50;
        this.vida = this.vidaMaxima;

        combat = new PlayerCombat(bulletManager, soundManager);
        fishing = new PlayerFishing();
        progress = new PlayerProgress();
        renderer = new PlayerRenderer();
    }

    @Override
    public void receberDano(int dano) {
        if (no_clip) {
            return;
        }
        if ((iFramesTimer == 0 && !emDash) || isCaindo) {
            soundManager.playSFX(SoundManager.SFX.PLAYER_DAMAGE);
            int damage = progress.hasHelmet() && !isCaindo
                    ? (int) Math.round(dano * (2.0 / 3.0))
                    : dano;
            super.receberDano(damage);
            iFramesTimer = DAMAGE_IFRAMES_DURATION;
            damageReceived = true;
            System.out.println("Player tomou dano! Vida: " + vida);
        }
    }

    public void update(InputManager input, CameraManager camera, ArrayList<Enemy> enemies) {
        updateActionTimers();
        renderer.update();
        dmgCheck();
        combat.updateCooldownsBeforeInput();

        if (!blockInputs) {
            processInput(input, camera, enemies);
        }

        combat.updateAfterInput();
        updatePlayerMovement();
    }

    private void updateActionTimers() {
        if (!podeDash) {
            dashCooldownTimer--;
            if (dashCooldownTimer <= 0) {
                dashCooldownTimer = 0;
                podeDash = true;
            }
        }

        if (emDash) {
            dashDurationTimer--;
            if (dashDurationTimer <= 0) {
                finishDash();
            }
        }

        if (iFramesTimer > 0) {
            iFramesTimer--;
        }
        if (sixSevenCooldownTimer > 0) {
            sixSevenCooldownTimer--;
        }
    }

    private void processInput(InputManager input, CameraManager camera, ArrayList<Enemy> enemies) {
        Vetor2D leftStick = input.getLeftStick();
        Vetor2D rightStick = input.getRightStick();
        boolean controllerActive = input.isControllerActive();
        boolean mouseAimActive = camera.isMouseMiraAtiva();
        double controlMultiplier = no_clip ? 4.0 : emDash ? DASH_CONTROL : 1.0;

        if (controllerActive) {
            processControllerInput(input, leftStick, rightStick, controlMultiplier);
        } else {
            processKeyboardAndMouseInput(input, camera, mouseAimActive, controlMultiplier);
        }

        fishing.update(input, camera, enemies, levelData, this,
                controllerActive, rightStick, mouseAimActive);
    }

    private void processControllerInput(InputManager input, Vetor2D leftStick,
            Vetor2D rightStick, double controlMultiplier) {
        Vetor2D movement = leftStick.partiallyNormalized();
        registerLastMovementDirection(movement);
        applyMovementAcceleration(movement, controlMultiplier);

        double centerX = getCenterX();
        double centerY = getCenterY();
        double shotDirectionX = Math.cos(aimAngle);
        double shotDirectionY = Math.sin(aimAngle);

        if (!isZero(rightStick)) {
            shotDirectionX = rightStick.x;
            shotDirectionY = rightStick.y;
            updatePlayerDirection(centerX + shotDirectionX, centerY + shotDirectionY);
        }

        if (input.isButtonPressed(InputManager.GamepadButton.RT)) {
            combat.tryShoot(centerX, centerY, shotDirectionX, shotDirectionY);
        }
        if (input.isButtonJustPressed(InputManager.GamepadButton.RB)) {
            combat.toggleGun();
        }
        if (input.isButtonPressed(InputManager.GamepadButton.X)) {
            combat.requestReload();
        }

        combat.scheduleAutomaticReloadIfNeeded();

        boolean dashPressed = input.isButtonPressed(InputManager.GamepadButton.A)
                || input.isButtonPressed(InputManager.GamepadButton.LB);
        if (!hasVelocityOverride() && dashPressed && podeDash && !emDash && !isZero(leftStick)) {
            applyDirectionalDash(movement);
        }
    }

    private void processKeyboardAndMouseInput(InputManager input, CameraManager camera,
            boolean mouseAimActive, double controlMultiplier) {
        Vetor2D movement = getKeyboardMovement(input).partiallyNormalized();
        registerLastMovementDirection(movement);
        applyMovementAcceleration(movement, controlMultiplier);

        double mouseXWorld = 0;
        double mouseYWorld = 0;
        if (mouseAimActive) {
            mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            if (input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
                combat.tryShoot(getCenterX(), getCenterY(),
                        mouseXWorld - getCenterX(), mouseYWorld - getCenterY());
            }
        }

        processDebugInput(input);
        processSixSevenInput(input);
        processKeyboardGunSelection(input);

        if (input.isKeyPressed(KeyEvent.VK_R)) {
            combat.requestReload();
        }
        combat.scheduleAutomaticReloadIfNeeded();

        if (!hasVelocityOverride() && input.isKeyPressed(KeyEvent.VK_SPACE)
                && podeDash && !emDash && !isZero(movement)) {
            applyDirectionalDash(movement);
        }

        if (mouseAimActive) {
            updatePlayerDirection(mouseXWorld, mouseYWorld);
        }
    }

    private Vetor2D getKeyboardMovement(InputManager input) {
        double movementX = 0;
        double movementY = 0;
        if (input.isKeyPressed(KeyEvent.VK_D)) {
            movementX++;
        }
        if (input.isKeyPressed(KeyEvent.VK_A)) {
            movementX--;
        }
        if (input.isKeyPressed(KeyEvent.VK_S)) {
            movementY++;
        }
        if (input.isKeyPressed(KeyEvent.VK_W)) {
            movementY--;
        }
        return new Vetor2D(movementX, movementY);
    }

    private void applyMovementAcceleration(Vetor2D movement, double controlMultiplier) {
        if (hasVelocityOverride()) {
            return;
        }
        velX += aceleracao * controlMultiplier * movement.x;
        velY += aceleracao * controlMultiplier * movement.y;
    }

    private void processDebugInput(InputManager input) {
        if (GameCore.getDebug() && input.isKeyJustPressed(KeyEvent.VK_J)) {
            setFasterFishing(true);
            ToastNotifications.RequestNotification("FasterFishing = true");
        }
        if (GameCore.getDebug() && input.isKeyJustPressed(KeyEvent.VK_T)) {
            setX(250.5 * GameCore.tiles_size);
            setY(60.0 * GameCore.tiles_size);
        }
        if (GameCore.getDebug() && input.isKeyPressed(KeyEvent.VK_S)
                && input.isKeyJustPressed(KeyEvent.VK_H)) {
            setHasShotgun(true);
            ToastNotifications.RequestNotification("DEBUG 67: habilitou shotgun", 2.0);
        }
    }

    private void processSixSevenInput(InputManager input) {
        if (!isSixSevenPressed(input) || sixSevenCooldownTimer > 0) {
            return;
        }
        if (consumeSixSeven()) {
            soundManager.playSFX(SoundManager.SFX.SIX_SEVEN);
        } else {
            soundManager.playSFX(SoundManager.SFX.SEM_AURA);
        }
        sixSevenCooldownTimer = SIX_SEVEN_COOLDOWN;
    }

    private boolean isSixSevenPressed(InputManager input) {
        return input.isKeyPressed(KeyEvent.VK_6) && input.isKeyJustPressed(KeyEvent.VK_7);
    }

    private boolean consumeSixSeven() {
        if (!hasPoderSixSeven) {
            return false;
        }
        hasPoderSixSeven = false;
        return true;
    }

    private void processKeyboardGunSelection(InputManager input) {
        boolean pressedG = input.isKeyJustPressed(KeyEvent.VK_G);
        boolean pressed1 = input.isKeyJustPressed(KeyEvent.VK_1);
        boolean pressed2 = input.isKeyJustPressed(KeyEvent.VK_2);

        if (pressed1) {
            combat.selectGun(GunType.PISTOL);
        } else if (pressed2) {
            combat.selectGun(GunType.SHOTGUN);
        } else if (pressedG) {
            combat.toggleGun();
        }
    }

    private double getCenterX() {
        return x + largura / 2.0;
    }

    private double getCenterY() {
        return y + altura / 2.0;
    }

    private boolean isZero(Vetor2D vector) {
        return vector.x == 0.0 && vector.y == 0.0;
    }

    private void updatePlayerDirection(double targetX, double targetY) {
        double dx = targetX - getCenterX();
        double dy = targetY - getCenterY();
        double angleDegrees = Math.toDegrees(Math.atan2(dy, dx));
        aimAngle = Math.atan2(dy, dx);
        if (angleDegrees < 0) {
            angleDegrees += 360;
        }

        if (angleDegrees >= 45 && angleDegrees < 135) {
            direction = Direction.DOWN;
        } else if (angleDegrees >= 135 && angleDegrees < 225) {
            direction = Direction.LEFT;
        } else if (angleDegrees >= 225 && angleDegrees < 315) {
            direction = Direction.UP;
        } else {
            direction = Direction.RIGHT;
        }
    }

    public void updatePlayerMovement() {
        boolean velocityOverrideActive = hasVelocityOverride();
        double savedMaxSpeed = velocidadeMax;

        if (velocityOverrideActive) {
            velX = velocityOverrideX;
            velY = velocityOverrideY;
            velocidadeMax = Math.max(savedMaxSpeed, Math.hypot(velX, velY));
        } else {
            applyPlayerPhysics();
        }

        moveAndCollideWithMap(levelData, arenaManager.getObjetosDeCenario());
        velocidadeMax = savedMaxSpeed;

        if (velocityOverrideActive) {
            velocityOverrideUpdatesRemaining = Math.max(0, velocityOverrideUpdatesRemaining - 1);
        }

        if (!no_clip) {
            clampToMapBounds();
        }

        if (isMoving() && !emDash) {
            updateFootsteps(soundManager, levelData);
        }
    }

    private void applyPlayerPhysics() {
        double savedFriction = atritoAtual;
        if (emDash) {
            atritoAtual = DASH_FRICTION;
        }
        aplicarFisicaBasica();
        atritoAtual = savedFriction;
    }

    private void clampToMapBounds() {
        int mapWidth = levelData[0].length * GameCore.tiles_size;
        int mapHeight = levelData.length * GameCore.tiles_size;

        if (x < 0) {
            x = 0;
            velX = 0;
        }
        if (y < 0) {
            y = 0;
            velY = 0;
        }
        if (x + largura > mapWidth) {
            x = mapWidth - largura;
            velX = 0;
        }
        if (y + altura > mapHeight) {
            y = mapHeight - altura;
            velY = 0;
        }
    }

    private void applyDirectionalDash(Vetor2D movementDirection) {
        double absX = Math.abs(movementDirection.x);
        double absY = Math.abs(movementDirection.y);
        if (absX == 0.0 && absY == 0.0) {
            return;
        }

        velX += movementDirection.x * DASH_FORCE;
        velY += movementDirection.y * DASH_FORCE;
        selectDashAnimationDirection(movementDirection, absX, absY);

        podeDash = false;
        emDash = true;
        isAirborne = true;
        dashCooldownTimer = DASH_COOLDOWN;
        dashDurationTimer = DASH_DURATION;
    }

    private void selectDashAnimationDirection(Vetor2D movementDirection, double absX, double absY) {
        if (Math.abs(absX - absY) < 0.0001) {
            if (Math.abs(lastMovementDirectionX) >= Math.abs(lastMovementDirectionY)) {
                dashDirX = Math.signum(lastMovementDirectionX != 0.0
                        ? lastMovementDirectionX
                        : movementDirection.x);
                dashDirY = 0;
            } else {
                dashDirX = 0;
                dashDirY = Math.signum(lastMovementDirectionY != 0.0
                        ? lastMovementDirectionY
                        : movementDirection.y);
            }
        } else if (absX > absY) {
            dashDirX = Math.signum(movementDirection.x);
            dashDirY = 0;
        } else {
            dashDirX = 0;
            dashDirY = Math.signum(movementDirection.y);
        }
    }

    private void registerLastMovementDirection(Vetor2D movementDirection) {
        if (movementDirection == null || isZero(movementDirection)) {
            return;
        }
        Vetor2D normalized = movementDirection.normalized();
        lastMovementDirectionX = normalized.x;
        lastMovementDirectionY = normalized.y;
    }

    private void finishDash() {
        emDash = false;
        dashDurationTimer = 0;
        isAirborne = false;
        iFramesTimer = DASH_IFRAMES_GRACE;
    }

    public void setEmDash(boolean emDash) {
        if (emDash) {
            this.emDash = true;
        } else if (this.emDash) {
            finishDash();
        }
    }

    public boolean consumirDanoFlag() {
        boolean value = damageReceived;
        damageReceived = false;
        return value;
    }

    @Override
    public void draw(Graphics2D graphics, double delta) {
        renderer.draw(graphics, delta, this, combat, fishing);
    }

    public void respawn(double checkpointX, double checkpointY, int checkpointHealth,
            int checkpointAmmo, int checkpointMagazine, int checkpointKeys) {
        x = checkpointX;
        y = checkpointY;
        vida = checkpointHealth;
        progress.restoreKeys(checkpointKeys);
        combat.restoreAmmo(checkpointAmmo, checkpointMagazine);
        isDead = false;
        resetTransientState();
        iFramesTimer = DAMAGE_IFRAMES_DURATION;
    }

    public void resetarProgresso() {
        progress.reset();
        combat.resetProgress();
        fishing.resetProgress();
        hasPoderSixSeven = false;
        sixSevenCooldownTimer = 0;
        vida = vidaMaxima;
        isDead = false;
        resetTransientState();
    }

    private void resetTransientState() {
        velX = 0;
        velY = 0;
        velocityOverrideX = 0;
        velocityOverrideY = 0;
        velocityOverrideUpdatesRemaining = 0;
        podeDash = true;
        emDash = false;
        dashCooldownTimer = 0;
        dashDurationTimer = 0;
        dashDirX = 0;
        dashDirY = 0;
        lastMovementDirectionX = 0;
        lastMovementDirectionY = 0;
        isAirborne = false;
        isPuxado = false;
        isSlippery = false;
        isCaindo = false;
        timerQueda = 0;
        timerLedgeSnap = 0;
        timerDano = 0;
        iFramesTimer = 0;
        atritoAtual = 0.85;
        damageReceived = false;
        combat.resetTransientState();
        fishing.resetTransientState();
        renderer.resetTransientState();
    }

    public static void setDesbloqueouRecompensa(boolean unlocked) {
        PlayerProgress.setRewardUnlocked(unlocked);
    }

    public static boolean getDesbloqueouRecompensa() {
        return PlayerProgress.isRewardUnlocked();
    }

    public void setPlayerShadowLength(double length) {
        renderer.setShadowLength(length);
    }

    public void setPlayerShadowOpacity(float opacity) {
        renderer.setShadowOpacity(opacity);
    }

    public void setTemporarySpriteOverride(int spriteIndex, int durationUpdates) {
        renderer.setTemporarySpriteOverride(spriteIndex, durationUpdates);
    }

    public void setFishingManager(FishingManager fishingManager) {
        fishing.setFishingManager(fishingManager);
    }

    public void setHasShotgun(boolean hasShotgun) {
        combat.setHasShotgun(hasShotgun);
    }

    public boolean getHasShotgun() {
        return combat.hasShotgun();
    }

    public void setFasterReload(boolean fasterReload) {
        combat.setFasterReload(fasterReload);
    }

    public boolean getFasterReload() {
        return combat.hasFasterReload();
    }

    public void setExtendedMag(boolean extendedMag) {
        combat.setExtendedMag(extendedMag);
    }

    public boolean getExtendedMag() {
        return combat.hasExtendedMag();
    }

    public void setTemCapacete(boolean helmet) {
        progress.setHelmet(helmet);
    }

    public boolean getTemCapacete() {
        return progress.hasHelmet();
    }

    public void setGunType(GunType gunType) {
        combat.setGunType(gunType);
    }

    public GunType getGunType() {
        return combat.getGunType();
    }

    public int getTotalEnemyCount() {
        return progress.getTotalEnemyCount();
    }

    public int getCurrentEnemyCount() {
        return progress.getCurrentEnemyCount();
    }

    public void setCurrentEnemyCount(int count) {
        progress.setCurrentEnemyCount(count);
    }

    public void setEnemyCount(int count) {
        progress.setTotalEnemyCount(count);
    }

    public void addEnemyCount(int count) {
        progress.addEnemyCount(count);
    }

    public int getShooterEnemyCount() {
        return progress.getShooterEnemyCount();
    }

    public void addShooterEnemyCount(int count) {
        progress.addShooterEnemyCount(count);
    }

    public int getLoboEnemyCount() {
        return progress.getLoboEnemyCount();
    }

    public void setLoboEnemyCount(int count) {
        progress.setLoboEnemyCount(count);
    }

    public void addLoboEnemyCount(int count) {
        progress.addLoboEnemyCount(count);
    }

    public int getJumperEnemyCount() {
        return progress.getJumperEnemyCount();
    }

    public void addJumperEnemyCount(int count) {
        progress.addJumperEnemyCount(count);
    }

    public int getDasherEnemyCount() {
        return progress.getDasherEnemyCount();
    }

    public void addDasherEnemyCount(int count) {
        progress.addDasherEnemyCount(count);
    }

    public int getBomberEnemyCount() {
        return progress.getBomberEnemyCount();
    }

    public void addBomberEnemyCount(int count) {
        progress.addBomberEnemyCount(count);
    }

    public void addMunicao(int amount) {
        combat.addAmmo(amount);
        System.out.println("coletou " + amount + "municao, total: " + combat.getReserveAmmo());
    }

    public void addMoedas(int amount) {
        progress.addCoins(amount);
        System.out.println("coletou " + amount + "moedas, total: " + progress.getCoins());
    }

    public void addIscas(int amount) {
        progress.addBait(amount);
        System.out.println("coletou " + amount + "iscas, total: " + progress.getBait());
    }

    public void curar(int amount) {
        vida = Math.min(vidaMaxima, vida + amount);
        System.out.println("coletou cura +" + amount + ", vida: " + vida);
        soundManager.playSFX(SoundManager.SFX.PLAYER_HEAL);
    }

    public void addChave(int amount) {
        progress.addKey(amount);
        if (progress.getKeys() == 1) {
            ToastNotifications.RequestNotification(
                    "Você encontrou a primeira chave para abrir o portão da Morsa!");
        }
        System.out.println("coletou chave total: " + progress.getKeys());
    }

    public int getChaves() {
        return progress.getKeys();
    }

    public int getMoedas() {
        return progress.getCoins();
    }

    public int getIscas() {
        return progress.getBait();
    }

    public void setMoedas(int value) {
        progress.setCoins(value);
    }

    public void equiparArma(String weaponType) {
        progress.equipWeapon();
        System.out.println("equipou arma: " + weaponType + " total: " + progress.getWeapons());
    }

    public int getPente() {
        return combat.getMagazine();
    }

    public int getPenteMax() {
        return combat.getMaxMagazine();
    }

    public int getMunicao() {
        return combat.getReserveAmmo();
    }

    public boolean isReloading() {
        return combat.isReloading();
    }

    public boolean hasFishingRod() {
        return fishing.hasFishingRod();
    }

    public void setFishingRod(boolean hasFishingRod) {
        fishing.setFishingRod(hasFishingRod);
    }

    public boolean getFasterFishing() {
        return fishing.hasFasterFishing();
    }

    public void setFasterFishing(boolean fasterFishing) {
        fishing.setFasterFishing(fasterFishing);
    }

    public boolean getHasPoderSixSeven() {
        return hasPoderSixSeven;
    }

    public void setHasPoderSixSeven(boolean hasPower) {
        hasPoderSixSeven = hasPower;
    }

    public void concederPoderSixSeven() {
        hasPoderSixSeven = true;
        soundManager.playSFX(SoundManager.SFX.SCREAM);
    }

    public void solicitarCheckpoint() {
        progress.requestCheckpoint();
    }

    public void limparSolicitacaoCheckpoint() {
        progress.clearCheckpointRequest();
    }

    public boolean isCheckpointSolicitado() {
        return progress.isCheckpointRequested();
    }

    public int getTotalChavesColetadas() {
        return progress.getTotalCollectedKeys();
    }

    public void loadLvlData(int[][] levelData) {
        this.levelData = levelData;
    }

    public double getLargura() {
        return largura;
    }

    public double getAltura() {
        return altura;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isEmDash() {
        return emDash;
    }

    public double getDashDirX() {
        return dashDirX;
    }

    public double getDashDirY() {
        return dashDirY;
    }

    public void setShootCooldownTimer(int value) {
        combat.setShootCooldownTimer(value);
    }

    public void setBlockInputs(boolean blockInputs) {
        this.blockInputs = blockInputs;
    }

    public boolean isBlockInputs() {
        return blockInputs;
    }

    public void setVelocity(double velocityX, double velocityY, int durationUpdates) {
        if (!Double.isFinite(velocityX) || !Double.isFinite(velocityY)) {
            throw new IllegalArgumentException("Velocity must contain finite numbers");
        }
        velX = velocityX;
        velY = velocityY;
        velocityOverrideX = velocityX;
        velocityOverrideY = velocityY;
        velocityOverrideUpdatesRemaining = Math.max(0, durationUpdates);
    }

    private boolean hasVelocityOverride() {
        return velocityOverrideUpdatesRemaining > 0;
    }

    public boolean isMoving() {
        return Math.hypot(velX, velY) > 0.2;
    }

    double getAimAngle() {
        return aimAngle;
    }

    int getDashDurationRemaining() {
        return dashDurationTimer;
    }

    int getDamageAnimationTimer() {
        return timerDano;
    }
}
