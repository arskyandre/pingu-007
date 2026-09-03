final class PlayerCombat {

    private static final int BASE_MAGAZINE_SIZE = 15;
    private static final int EXTENDED_MAGAZINE_SIZE = 30;
    private static final int INITIAL_RESERVE_AMMO = 45;
    private static final int PISTOL_SHOOT_COOLDOWN = 20;
    private static final int SHOTGUN_SHOOT_COOLDOWN = 65;
    private static final int CHANGE_GUN_COOLDOWN = 20;
    private static final int DEFAULT_RELOAD_COOLDOWN = 30;
    private static final int FASTER_RELOAD_COOLDOWN = 15;
    private static final int RELOAD_ON_ZERO_COOLDOWN = 35;
    private static final int SHOT_ANIMATION_DURATION = 6;
    private static final double SHOTGUN_SPREAD_DEGREES = 15.0;

    private final BulletManager bulletManager;
    private final SoundManager soundManager;

    private Player.GunType gunType = Player.GunType.PISTOL;
    private boolean hasShotgun;
    private boolean extendedMag;
    private boolean fasterReload;

    private int magazine = BASE_MAGAZINE_SIZE;
    private int maxMagazine = BASE_MAGAZINE_SIZE;
    private int reserveAmmo = INITIAL_RESERVE_AMMO;

    private int shootCooldownTimer;
    private int changeGunCooldownTimer;
    private int reloadCooldownTimer;
    private int reloadOnZeroCooldownTimer;
    private int shotAnimationTimer;
    private boolean reloadOnZeroScheduled;
    private boolean reloading;

    PlayerCombat(BulletManager bulletManager, SoundManager soundManager) {
        this.bulletManager = bulletManager;
        this.soundManager = soundManager;
    }

    void updateCooldownsBeforeInput() {
        if (shootCooldownTimer > 0) {
            shootCooldownTimer--;
        }
        if (changeGunCooldownTimer > 0) {
            changeGunCooldownTimer--;
        }
        if (reloadOnZeroCooldownTimer > 0) {
            reloadOnZeroCooldownTimer--;
            if (reloadOnZeroCooldownTimer == 0 && reloadOnZeroScheduled && !reloading && reserveAmmo > 0) {
                startReload();
                reloadOnZeroScheduled = false;
            }
        }
    }

    void updateAfterInput() {
        if (reloading) {
            reloadCooldownTimer--;
            if (reloadCooldownTimer <= 0) {
                completeReload();
            }
        }
        if (shotAnimationTimer > 0) {
            shotAnimationTimer--;
        }
    }

    boolean tryShoot(double centerX, double centerY, double directionX, double directionY) {
        if (reloading || shootCooldownTimer != 0 || magazine <= 0) {
            return false;
        }

        shotAnimationTimer = SHOT_ANIMATION_DURATION;
        switch (gunType) {
            case PISTOL -> shootPistol(centerX, centerY, directionX, directionY);
            case SHOTGUN -> shootShotgun(centerX, centerY, directionX, directionY);
        }
        magazine--;
        return true;
    }

    private void shootPistol(double centerX, double centerY, double directionX, double directionY) {
        bulletManager.shoot(centerX, centerY, directionX, directionY, BulletOwner.PLAYER);
        soundManager.playGunshot();
        shootCooldownTimer = PISTOL_SHOOT_COOLDOWN;
    }

    private void shootShotgun(double centerX, double centerY, double directionX, double directionY) {
        double baseAngle = Math.atan2(directionY, directionX);
        double leftAngle = baseAngle - Math.toRadians(SHOTGUN_SPREAD_DEGREES);
        double rightAngle = baseAngle + Math.toRadians(SHOTGUN_SPREAD_DEGREES);

        bulletManager.shoot(centerX, centerY, Math.cos(baseAngle), Math.sin(baseAngle),
                BulletOwner.PLAYER, true);
        bulletManager.shoot(centerX, centerY, Math.cos(leftAngle), Math.sin(leftAngle),
                BulletOwner.PLAYER, true);
        bulletManager.shoot(centerX, centerY, Math.cos(rightAngle), Math.sin(rightAngle),
                BulletOwner.PLAYER, true);

        soundManager.playSFX(SoundManager.SFX.EXPLOSION);
        shootCooldownTimer = SHOTGUN_SHOOT_COOLDOWN;
    }

    void requestReload() {
        if (!reloading && magazine < maxMagazine && reserveAmmo > 0) {
            startReload();
            reloadOnZeroScheduled = false;
        }
    }

    void scheduleAutomaticReloadIfNeeded() {
        if (magazine == 0 && !reloading && reserveAmmo > 0 && !reloadOnZeroScheduled) {
            reloadOnZeroScheduled = true;
            reloadOnZeroCooldownTimer = RELOAD_ON_ZERO_COOLDOWN;
        }
    }

    private void startReload() {
        reloading = true;
        reloadCooldownTimer = fasterReload ? FASTER_RELOAD_COOLDOWN : DEFAULT_RELOAD_COOLDOWN;
    }

    private void completeReload() {
        int missingRounds = maxMagazine - magazine;
        int roundsToLoad = Math.min(reserveAmmo, missingRounds);
        magazine += roundsToLoad;
        reserveAmmo -= roundsToLoad;
        reloading = false;
        reloadOnZeroScheduled = false;
    }

    void toggleGun() {
        if (!canChangeGun()) {
            return;
        }
        Player.GunType next = gunType == Player.GunType.SHOTGUN
                ? Player.GunType.PISTOL
                : Player.GunType.SHOTGUN;
        applyGunChange(next);
    }

    void selectGun(Player.GunType selectedGun) {
        if (!canChangeGun() || selectedGun == gunType) {
            return;
        }
        applyGunChange(selectedGun);
    }

    private boolean canChangeGun() {
        return hasShotgun && changeGunCooldownTimer <= 0;
    }

    private void applyGunChange(Player.GunType next) {
        gunType = next;
        String notification = next == Player.GunType.PISTOL
                ? "Mudou para Pistola"
                : "Mudou para Shotgun";
        String currentNotification = ToastNotifications.getNotifAtual();
        if ("Mudou para Shotgun".equals(currentNotification)
                || "Mudou para Pistola".equals(currentNotification)) {
            ToastNotifications.skipNotification();
        }
        if (!notification.equals(ToastNotifications.getNotifAtual())) {
            ToastNotifications.RequestNotification(notification, 1.0);
        }
        shootCooldownTimer = PISTOL_SHOOT_COOLDOWN;
        changeGunCooldownTimer = CHANGE_GUN_COOLDOWN;
    }

    void resetTransientState() {
        shootCooldownTimer = 0;
        changeGunCooldownTimer = 0;
        reloadCooldownTimer = 0;
        reloadOnZeroCooldownTimer = 0;
        shotAnimationTimer = 0;
        reloadOnZeroScheduled = false;
        reloading = false;
    }

    void resetProgress() {
        resetTransientState();
        gunType = Player.GunType.PISTOL;
        hasShotgun = false;
        extendedMag = false;
        fasterReload = false;
        magazine = BASE_MAGAZINE_SIZE;
        maxMagazine = BASE_MAGAZINE_SIZE;
        reserveAmmo = INITIAL_RESERVE_AMMO;
    }

    Player.GunType getGunType() {
        return gunType;
    }

    void setGunType(Player.GunType gunType) {
        this.gunType = gunType;
    }

    boolean hasShotgun() {
        return hasShotgun;
    }

    void setHasShotgun(boolean hasShotgun) {
        this.hasShotgun = hasShotgun;
    }

    boolean hasExtendedMag() {
        return extendedMag;
    }

    void setExtendedMag(boolean extendedMag) {
        this.extendedMag = extendedMag;
        maxMagazine = extendedMag ? EXTENDED_MAGAZINE_SIZE : BASE_MAGAZINE_SIZE;
    }

    boolean hasFasterReload() {
        return fasterReload;
    }

    void setFasterReload(boolean fasterReload) {
        this.fasterReload = fasterReload;
    }

    int getMagazine() {
        return magazine;
    }

    int getMaxMagazine() {
        return maxMagazine;
    }

    int getReserveAmmo() {
        return reserveAmmo;
    }

    void addAmmo(int amount) {
        reserveAmmo += amount;
    }

    void restoreAmmo(int reserveAmmo, int magazine) {
        this.reserveAmmo = reserveAmmo;
        this.magazine = magazine;
    }

    boolean isReloading() {
        return reloading;
    }

    boolean isShotAnimationActive() {
        return shotAnimationTimer > 0;
    }

    void setShootCooldownTimer(int value) {
        shootCooldownTimer = value;
    }
}
