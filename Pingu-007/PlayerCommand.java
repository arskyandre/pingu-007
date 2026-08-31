public final class PlayerCommand {
    private final Vetor2D movement;
    private final AimCommand aim;
    private final boolean fireHeld;
    private final boolean reloadHeld;
    private final boolean dashHeld;
    private final WeaponSelection weaponSelection;
    private final boolean castPressed;
    private final boolean debugFasterFishing;
    private final boolean debugTeleport;
    private final boolean debugUnlockShotgun;

    public PlayerCommand(Vetor2D movement, AimCommand aim,
            boolean fireHeld, boolean reloadHeld, boolean dashHeld,
            WeaponSelection weaponSelection, boolean castPressed,
            boolean debugFasterFishing, boolean debugTeleport, boolean debugUnlockShotgun) {
        this.movement = new Vetor2D(movement.x, movement.y);
        this.aim = aim;
        this.fireHeld = fireHeld;
        this.reloadHeld = reloadHeld;
        this.dashHeld = dashHeld;
        this.weaponSelection = weaponSelection;
        this.castPressed = castPressed;
        this.debugFasterFishing = debugFasterFishing;
        this.debugTeleport = debugTeleport;
        this.debugUnlockShotgun = debugUnlockShotgun;
    }

    public Vetor2D movement() {
        return new Vetor2D(movement.x, movement.y);
    }

    public AimCommand aim() {
        return aim;
    }

    public boolean fireHeld() {
        return fireHeld;
    }

    public boolean reloadHeld() {
        return reloadHeld;
    }

    public boolean dashHeld() {
        return dashHeld;
    }

    public WeaponSelection weaponSelection() {
        return weaponSelection;
    }

    public boolean castPressed() {
        return castPressed;
    }

    public boolean debugFasterFishing() {
        return debugFasterFishing;
    }

    public boolean debugTeleport() {
        return debugTeleport;
    }

    public boolean debugUnlockShotgun() {
        return debugUnlockShotgun;
    }
}
