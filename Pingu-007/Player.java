
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Player extends Entity {

    final private double largura, altura;
    private Direction direction = Direction.DOWN;

    public enum GunType {
        PISTOL,
        SHOTGUN
    };

    private GunType gunType = GunType.PISTOL;
    private boolean hasShotgun = false;
    private double dirX = 0;
    private double dirY = 0;

    private boolean blockInputs = false;

    private boolean podeDash = true;
    private boolean emDash = false;
    private final int dashCooldown = 60;
    private int dashCooldownTimer = 0;
    private final int dashDuracao = 28;
    private int dashDuracaoTimer = 0;
    private final double dashForca = 15;
    private final double atritoDash = 0.90;
    private final double controleDash = 0.50;
    private double dashDirX = 0;
    private double dashDirY = 0;
    private double ultimaDirecaoMovimentoX = 0;
    private double ultimaDirecaoMovimentoY = 0;

    private int totalEnemyCount = 0;
    private int currentEnemyCount = 0;
    private final BulletManager bulletmanager;
    private static boolean desbloqueouRecompensa = false;
    private boolean extendedMag = false;
    private boolean fasterReload = false;
    private int moedas = 0;
    private int iscas = 0;
    private int armas = 1;
    private int chaves = 0;
    private int pente = 15;
    private int maxpente = 15;
    private int municao = 45;
    private int shootCooldownTimer = 0;
    private final int changeGunCooldown = 30;
    private int changeGunCooldownTimer = 0;
    private final int pistolShootCooldown = 20;
    private final int shotgunShootCooldown = 80;
    private int reloadCooldownTimer = 0;
    private final int defaultReloadCooldown = 30;
    private final int fasterReloadCooldown = 15;
    private final int reloadOnZeroCooldown = 35;
    private int reloadOnZeroCooldownTimer = 0;
    private boolean danoRecebidoFlag = false;
    private boolean novaChaveFlag = false;
    private boolean reloading = false;
    private boolean hasFishingRod = false;
    private boolean fasterFishing = false;
    private FishingBobber fishingBobber;
    private int fishingCooldown = 0;

    private int footstepTimer = 0;
    private final int footstepInterval = 22;
    private boolean penteZeroTimerActive = false;

    private int iFramesTimer = 0;
    private final int iFramesDanoDuration = 60;
    private final int iFramesDashGrace = 15;

    private int chavesColetadasTotal = 0;
    private boolean checkpointSolicitado = false;

    private BufferedImage[] Sprites, arma;
    private double sunAngle = -Math.PI / 2.0;
    private double playerShadowLength = 42.0;
    private float playerShadowOpacity = 0.42f;
    private int animIndex = 0;
    private double animTick = 0;
    private int animSp = 0;
    private int spriteOverrideIndex = -1;
    private int spriteOverrideTimer = 0;
    private double angulo = 0;
    private int tiroTimer = 0;
    private int tiroTimerTime = 6;
    int t = 0;

    private int[][] lvlData;

    public Player(double startX, double startY, double largura, double altura, BulletManager bulmgr,
            SoundManager soundManager, ArenaManager arenaManager) {
        super(arenaManager, soundManager);

        this.x = startX;
        this.y = startY;
        this.aceleracao = 1.0;
        this.totalEnemyCount = 0;
        this.currentEnemyCount = 0;
        this.atritoAtual = 0.85;
        this.velocidadeMax = 30;
        this.largura = largura;
        this.altura = altura;
        this.bulletmanager = bulmgr;
        fishingBobber = new FishingBobber();
        this.bodyCollider = new Collider(0, altura / 2.0, largura, altura / 2.0);
        this.hurtbox = new Collider(0, 0, largura, altura);
        this.vidaMaxima = 50;
        this.vida = this.vidaMaxima;

        BufferedImage img = LoadSave.GetSpriteAtlas("images/pingu_sprite_sheet.png");

        Sprites = new BufferedImage[24];
        arma = new BufferedImage[4];
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 7; i++) {
                int index = j * 7 + i;
                BufferedImage frame = img.getSubimage(i * 16, j * 16, 16, 16);
                Sprites[index] = frame;
            }
        }
        Sprites[21] = img.getSubimage(0, 48, 16, 16);
        Sprites[22] = img.getSubimage(16, 48, 16, 16);
        Sprites[23] = img.getSubimage(32, 48, 16, 16);
        arma[0] = img.getSubimage(48, 48, 16, 16);
        arma[1] = img.getSubimage(64, 48, 16, 16);
        arma[2] = img.getSubimage(80, 48, 16, 16);
        arma[3] = img.getSubimage(96, 48, 16, 16);

    }

    private static final class PlayerVisualState {
        private final BufferedImage playerFrame;
        private final int playerX, playerY, playerWidth;
        private final BufferedImage gunFrame;
        private final int gunX, gunY, gunWidth, gunHeight;
        private final boolean gunBehindPlayer;

        private PlayerVisualState(BufferedImage playerFrame, int playerX, int playerY, int playerWidth,
                BufferedImage gunFrame, int gunX, int gunY, int gunWidth, int gunHeight,
                boolean gunBehindPlayer) {
            this.playerFrame = playerFrame;
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerWidth = playerWidth;
            this.gunFrame = gunFrame;
            this.gunX = gunX;
            this.gunY = gunY;
            this.gunWidth = gunWidth;
            this.gunHeight = gunHeight;
            this.gunBehindPlayer = gunBehindPlayer;
        }
    }

    private static final class ShadowSilhouette {
        private final BufferedImage image;
        private final double sourceFeetX, sourceFeetY;

        private ShadowSilhouette(BufferedImage image, double sourceFeetX, double sourceFeetY) {
            this.image = image;
            this.sourceFeetX = sourceFeetX;
            this.sourceFeetY = sourceFeetY;
        }
    }

    private PlayerVisualState createPlayerVisualState(int spriteFinal, int playerFlip, int playerDrawX) {
        int gunFlip = 1;
        int gunAnchorX = (int) x;
        double gunAngle = angulo;

        if (angulo > Math.PI / 2 || angulo < -Math.PI / 2) {
            gunFlip = -1;
            gunAnchorX = (int) x + 48;
            gunAngle = angulo > 0 ? Math.PI - angulo : -Math.PI - angulo;
        }

        int gunIndex = gunType == GunType.SHOTGUN ? 2 : 0;
        if (tiroTimer > 0) {
            gunIndex++;
        }

        BufferedImage gunFrame = HelpMethods.rotateImageByDegrees(arma[gunIndex], gunAngle);
        int gap = (gunFrame.getWidth() * 3 - 48) / 2;
        int gunY = (int) y - gap + 6;
        int gunOffset = gunType == GunType.PISTOL ? 20 : 12;
        int gunX = gunAnchorX - (gap - gunOffset) * gunFlip;
        int gunWidth = gunFrame.getWidth() * 3 * gunFlip;
        int gunHeight = gunFrame.getHeight() * 3;

        return new PlayerVisualState(
                Sprites[spriteFinal], playerDrawX, (int) y, 48 * playerFlip,
                gunFrame, gunX, gunY, gunWidth, gunHeight,
                gunType == GunType.PISTOL || direction == Direction.UP);
    }

    private ShadowSilhouette createPlayerShadowSilhouette(PlayerVisualState visual) {
        double playerLeft = Math.min(visual.playerX, visual.playerX + visual.playerWidth);
        double playerRight = Math.max(visual.playerX, visual.playerX + visual.playerWidth);
        double gunLeft = Math.min(visual.gunX, visual.gunX + visual.gunWidth);
        double gunRight = Math.max(visual.gunX, visual.gunX + visual.gunWidth);

        int worldLeft = (int) Math.floor(Math.min(playerLeft, gunLeft));
        int worldTop = (int) Math.floor(Math.min(visual.playerY, visual.gunY));
        int worldRight = (int) Math.ceil(Math.max(playerRight, gunRight));
        int worldBottom = (int) Math.ceil(Math.max(visual.playerY + 48.0, visual.gunY + visual.gunHeight));

        BufferedImage combined = new BufferedImage(
                Math.max(1, worldRight - worldLeft),
                Math.max(1, worldBottom - worldTop),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = combined.createGraphics();
        try {
            cg.setComposite(AlphaComposite.SrcOver);
            cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            cg.drawImage(visual.playerFrame, visual.playerX - worldLeft, visual.playerY - worldTop,
                    visual.playerWidth, 48, null);
            cg.drawImage(visual.gunFrame, visual.gunX - worldLeft, visual.gunY - worldTop,
                    visual.gunWidth, visual.gunHeight, null);
        } finally {
            cg.dispose();
        }

        double sourceFeetX = x + 24.0 - worldLeft;
        double sourceFeetY = y + 45.0 - worldTop;
        BufferedImage shadowMask = new BufferedImage(combined.getWidth(), combined.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        for (int py = 0; py < combined.getHeight(); py++) {
            double heightAboveFeet = Math.max(0.0, sourceFeetY - py);
            double normalizedHeight = heightAboveFeet / 45.0;
            float proximity = (float) Math.max(0.0, 1.0 - normalizedHeight);
            float rowStrength = 0.12f + 0.88f * (float) Math.pow(proximity, 0.75);

            for (int px = 0; px < combined.getWidth(); px++) {
                int originalAlpha = (combined.getRGB(px, py) >>> 24) & 0xFF;
                int finalAlpha = Math.round(originalAlpha * rowStrength);
                shadowMask.setRGB(px, py, finalAlpha << 24);
            }
        }

        return new ShadowSilhouette(shadowMask, sourceFeetX, sourceFeetY);
    }

    // "merge" da sombras
    private void drawPlayerShadow(
            Graphics2D g2,
            ShadowSilhouette silhouette,
            double playerX,
            double playerY) {
        if (silhouette == null || silhouette.image == null) {
            return;
        }

        BufferedImage shadowFrame = silhouette.image;
        int sourceWidth = shadowFrame.getWidth();
        int sourceHeight = shadowFrame.getHeight();
        double shadowAngle = sunAngle + Math.PI;
        double shadowDirX = Math.cos(shadowAngle);
        double shadowDirY = Math.sin(shadowAngle);
        double southFactor = (shadowDirY + 1.0) / 2.0;
        southFactor = Math.max(0.0, Math.min(1.0, southFactor));

        double lengthMultiplier = 0.55 + (1.60 - 0.55) * southFactor;
        double widthMultiplier = 0.80 + (1.20 - 0.80) * southFactor;
        double effectiveLength = playerShadowLength * lengthMultiplier;
        double maximumHeight = Math.max(45.0, silhouette.sourceFeetY);
        double maximumProjectedDistance = effectiveLength * Math.pow(maximumHeight / 45.0, 0.90);
        double worldFeetX = playerX + 24.0;
        double worldFeetY = playerY + 45.0;
        int blurPadding = 12;
        int safetyPadding = 12;

        int bufferWidth = (int) Math.ceil(
                sourceWidth * widthMultiplier
                        + Math.abs(shadowDirX) * maximumProjectedDistance
                        + blurPadding * 2
                        + safetyPadding * 2);
        int bufferHeight = (int) Math.ceil(
                sourceHeight
                        + Math.abs(shadowDirY) * maximumProjectedDistance
                        + blurPadding * 2
                        + safetyPadding * 2);

        BufferedImage shadowLayer = new BufferedImage(
                Math.max(1, bufferWidth),
                Math.max(1, bufferHeight),
                BufferedImage.TYPE_INT_ARGB);

        double localFeetX = blurPadding
                + safetyPadding
                + Math.max(0.0, -shadowDirX * maximumProjectedDistance)
                + silhouette.sourceFeetX * widthMultiplier;
        double localFeetY = blurPadding
                + safetyPadding
                + Math.max(0.0, -shadowDirY * maximumProjectedDistance)
                + 6.0;

        Graphics2D sg = shadowLayer.createGraphics();
        try {
            sg.setComposite(AlphaComposite.Src);
            sg.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (int sourceY = 0; sourceY < sourceHeight; sourceY++) {
                double heightAboveFeet = Math.max(0.0, silhouette.sourceFeetY - sourceY);
                double distanceFromFeet = heightAboveFeet / 45.0;
                double projectedDistance = effectiveLength * Math.pow(distanceFromFeet, 0.90);
                double rowCenterY = localFeetY + shadowDirY * projectedDistance;
                double rowPerspective = 0.72 + 0.28 * (1.0 - distanceFromFeet);
                rowPerspective = Math.max(0.45, rowPerspective);
                double horizontalScale = widthMultiplier * rowPerspective;

                int destinationHeight = Math.max(
                        2,
                        (int) Math.ceil(Math.abs(shadowDirY) * effectiveLength / 45.0) + 1);

                double projectedFeetX = localFeetX + shadowDirX * projectedDistance;
                int dx1 = (int) Math.round(projectedFeetX - silhouette.sourceFeetX * horizontalScale);
                int dx2 = (int) Math.round(
                        projectedFeetX + (sourceWidth - silhouette.sourceFeetX) * horizontalScale);
                if (dx2 <= dx1) {
                    dx2 = dx1 + 1;
                }
                int dy1 = (int) Math.round(rowCenterY - destinationHeight / 2.0);
                int dy2 = dy1 + destinationHeight;

                sg.drawImage(
                        shadowFrame,
                        dx1, dy1, dx2, dy2,
                        0, sourceY, sourceWidth, sourceY + 1,
                        null);
            }

        } finally {
            sg.dispose();
        }

        BufferedImage blurredShadow = Renderer.gaussianBlur(shadowLayer, 4, 2.0);
        int drawX = (int) Math.round(worldFeetX - localFeetX);
        int drawY = (int) Math.round(worldFeetY - localFeetY);
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, playerShadowOpacity));
        g2.drawImage(blurredShadow, drawX, drawY, null);
        g2.setComposite(oldComposite);
    }

    public void setSunAngle(double angleRadians) {
        this.sunAngle = angleRadians;
    }

    public void setPlayerShadowLength(double length) {
        this.playerShadowLength = Math.max(0.0, length);
    }

    public void setPlayerShadowOpacity(float opacity) {
        this.playerShadowOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    // TODO: Recompensa em moedas do vendedor por eliminar inimigos
    public static void setDesbloqueouRecompensa(boolean set) {
        desbloqueouRecompensa = set;
    }

    public static boolean getDesbloqueouRecompensa() {
        return desbloqueouRecompensa;
    }

    public void setHasShotgun(boolean set) {
        hasShotgun = set;
    }

    public void setFasterReload(boolean set) {
        fasterReload = set;
    }

    public boolean getFasterReload() {
        return fasterReload;
    }

    public void setExtendedMag(boolean set) {
        extendedMag = set;
        if (extendedMag) {
            maxpente = 30;
        } else {
            maxpente = 15;
        }
    }

    public boolean getExtendedMag() {
        return extendedMag;
    }

    public boolean getHasShotgun() {
        return hasShotgun;
    }

    public int getTotalEnemyCount() {
        return totalEnemyCount;
    }

    public int getCurrentEnemyCount() {
        return currentEnemyCount;
    }

    public void setCurrentEnemyCount(int count) {
        currentEnemyCount = count;
    }

    public void setEnemyCount(int count) {
        totalEnemyCount = count;
    }

    public void addEnemyCount(int count) {
        totalEnemyCount += count;
        currentEnemyCount += count;
    }

    public void setFishingManager(FishingManager fishingMgr) {
        fishingBobber.setFishingManager(fishingMgr);
    }

    private int muniCOoldownTimer = 0;
    private final int muniCooldown = 60;

    // debug
    /*
     * public void testemunicao(InputManager input, int telaLargura, int telaAltura,
     * ItemManager itemManager,
     * CameraManager camera) {
     * if (muniCOoldownTimer > 0) {
     * muniCOoldownTimer--;
     * }
     * 
     * if (input.isMouseButtonPressed(MouseEvent.BUTTON3)) {
     * if (muniCOoldownTimer == 0) {
     * double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
     * double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
     * 
     * itemManager.spawn(new AmmoPackItem(mouseXWorld, mouseYWorld, 32));
     * muniCOoldownTimer = muniCooldown;
     * System.out.println("Spawnou municao");
     * }
     * }
     * }
     */
    @Override
    public void receberDano(int dano) {
        if (no_clip) {
            return;
        }
        if ((iFramesTimer == 0 && !emDash) || isCaindo) {
            soundManager.playSFX(SoundManager.SFX.PLAYER_DAMAGE);
            super.receberDano(dano);
            iFramesTimer = iFramesDanoDuration;
            danoRecebidoFlag = true;
            System.out.println("Player tomou dano! Vida: " + vida);
        }
    }

    public void setEmDash(boolean set) {
        emDash = set;
        if (!set) {
            dashDuracaoTimer = 0;
            this.isAirborne = false;
            iFramesTimer = iFramesDashGrace;
        }

    }

    public boolean consumirDanoFlag() {
        boolean val = danoRecebidoFlag;
        danoRecebidoFlag = false;
        return val;
    }

    public boolean consumirNovaChave() {
        boolean val = novaChaveFlag;
        novaChaveFlag = false;
        return val;
    }

    public void updatePlayerMovement() {
        double atritoSalvo = this.atritoAtual;
        if (emDash) {
            this.atritoAtual = atritoDash;
        }
        aplicarFisicaBasica();
        this.atritoAtual = atritoSalvo;

        moveAndCollideWithMap(lvlData, arenaManager.getObjetosDeCenario());

        int mapaLargura = lvlData[0].length * GameCore.tiles_size;
        int mapaAltura = lvlData.length * GameCore.tiles_size;

        if (!no_clip) {
            if (x < 0) {
                x = 0;
                velX = 0;
            }
            if (y < 0) {
                y = 0;
                velY = 0;
            }
            if (x + largura > mapaLargura) {
                x = mapaLargura - largura;
                velX = 0;
            }
            if (y + altura > mapaAltura) {
                y = mapaAltura - altura;
                velY = 0;
            }
        }

        if (x < 0) {
            x = 0;
            velX = 0;
        }
        if (y < 0) {
            y = 0;
            velY = 0;
        }
        if (x + largura > mapaLargura) {
            x = mapaLargura - largura;
            velX = 0;
        }
        if (y + altura > mapaAltura) {
            y = mapaAltura - altura;
            velY = 0;
        }

        if (isMoving() && !emDash) {
            updateFootsteps(soundManager, lvlData);
        } else {
            footstepTimer = 0;
        }
    }

    public void setGunType(GunType type) {
        gunType = type;
    }

    public GunType getGunType() {
        return gunType;
    }

    public void update(InputManager input, int telaLargura, int telaAltura, CameraManager camera,
            ArrayList<Enemy> enemies) {
        if (!podeDash) {
            dashCooldownTimer--;
            if (dashCooldownTimer <= 0) {
                podeDash = true;
            }
        }

        if (emDash) {
            dashDuracaoTimer--;
            if (dashDuracaoTimer <= 0) {
                emDash = false;
                this.isAirborne = false;
                iFramesTimer = iFramesDashGrace;
            }
        }

        if (iFramesTimer > 0) {
            iFramesTimer--;
        }
        if (spriteOverrideTimer > 0) {
            spriteOverrideTimer--;
            if (spriteOverrideTimer <= 0) {
                spriteOverrideIndex = -1;
            }
        }
        dmgCheck();
        double controleAtual = emDash ? controleDash : 1.0;

        if (no_clip) {
            controleAtual = 4.0;
        }

        if (shootCooldownTimer > 0) {
            shootCooldownTimer--;
        }
        if (changeGunCooldownTimer > 0) {
            changeGunCooldownTimer--;
        }

        if (reloadOnZeroCooldownTimer > 0) {
            reloadOnZeroCooldownTimer--;
            if (reloadOnZeroCooldownTimer == 0 && penteZeroTimerActive && !reloading && municao > 0) {
                reloading = true;
                reloadCooldownTimer = (fasterReload) ? fasterReloadCooldown : defaultReloadCooldown;
                penteZeroTimerActive = false;
            }
        }

        if (!blockInputs) {
            Vetor2D analogicoEsquerdo = input.getLeftStick();
            Vetor2D analogicoDireito = input.getRightStick();
            boolean controleAtivo = input.isControllerActive();
            boolean mouseMiraAtiva = camera.isMouseMiraAtiva();

            if (controleAtivo) {
                Vetor2D movimento = analogicoEsquerdo.partiallyNormalized();
                registrarUltimaDirecaoMovimento(movimento);
                velX += aceleracao * controleAtual * movimento.x;
                velY += aceleracao * controleAtual * movimento.y;

                if (analogicoEsquerdo.x > 0) {
                    dirX = 1;
                } else if (analogicoEsquerdo.x < 0) {
                    dirX = -1;
                } else {
                    dirX = 0;
                }

                if (analogicoEsquerdo.y > 0) {
                    dirY = 1;
                } else if (analogicoEsquerdo.y < 0) {
                    dirY = -1;
                } else {
                    dirY = 0;
                }

                double centerX = x + largura / 2.0;
                double centerY = y + altura / 2.0;
                double dirTiroX = Math.cos(angulo);
                double dirTiroY = Math.sin(angulo);

                if (analogicoDireito.x != 0 || analogicoDireito.y != 0) {
                    dirTiroX = analogicoDireito.x;
                    dirTiroY = analogicoDireito.y;
                    updatePlayerDirection(centerX + dirTiroX, centerY + dirTiroY);
                }

                if (input.isButtonPressed(InputManager.GamepadButton.RT)) {
                    if (shootCooldownTimer == 0 && pente > 0) {
                        tiroTimer = tiroTimerTime;

                        switch (gunType) {
                            case PISTOL -> {
                                bulletmanager.shoot(centerX, centerY, dirTiroX, dirTiroY, BulletOwner.PLAYER);
                                soundManager.playGunshot();
                                shootCooldownTimer = pistolShootCooldown;
                            }
                            case SHOTGUN -> {
                                double anguloBase = Math.atan2(dirTiroY, dirTiroX);
                                double angulo1 = anguloBase;
                                double angulo2 = anguloBase - Math.toRadians(15);
                                double angulo3 = anguloBase + Math.toRadians(15);

                                bulletmanager.shoot(centerX, centerY, Math.cos(angulo1), Math.sin(angulo1),
                                        BulletOwner.PLAYER, true);
                                bulletmanager.shoot(centerX, centerY, Math.cos(angulo2), Math.sin(angulo2),
                                        BulletOwner.PLAYER, true);
                                bulletmanager.shoot(centerX, centerY, Math.cos(angulo3), Math.sin(angulo3),
                                        BulletOwner.PLAYER, true);

                                soundManager.playSFX(SoundManager.SFX.EXPLOSION);
                                shootCooldownTimer = shotgunShootCooldown;
                            }
                        }

                        pente--;
                    }
                }

                if (input.isButtonJustPressed(InputManager.GamepadButton.RB) && hasShotgun
                        && changeGunCooldownTimer == 0) {
                    if (getGunType() == Player.GunType.SHOTGUN) {
                        setGunType(Player.GunType.PISTOL);
                        if (ToastNotifications.getNotifAtual() != null
                                && (ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")
                                        || ToastNotifications.getNotifAtual().equals("Mudou para Pistola"))) {
                            ToastNotifications.skipNotification();
                        }
                        if (ToastNotifications.getNotifAtual() == null
                                || !ToastNotifications.getNotifAtual().equals("Mudou para Pistola")) {
                            ToastNotifications.RequestNotification("Mudou para Pistola", 1.0);
                        }
                    } else {
                        setGunType(Player.GunType.SHOTGUN);
                        if (ToastNotifications.getNotifAtual() != null
                                && (ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")
                                        || ToastNotifications.getNotifAtual().equals("Mudou para Pistola"))) {
                            ToastNotifications.skipNotification();
                        }
                        if (ToastNotifications.getNotifAtual() == null
                                || !ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")) {
                            ToastNotifications.RequestNotification("Mudou para Shotgun", 1.0);
                        }
                    }
                    shootCooldownTimer = pistolShootCooldown;
                    changeGunCooldownTimer = changeGunCooldown;
                }

                if (input.isButtonPressed(InputManager.GamepadButton.X) && !reloading && pente < maxpente
                        && municao > 0) {
                    reloading = true;
                    reloadCooldownTimer = (fasterReload) ? fasterReloadCooldown : defaultReloadCooldown;
                    penteZeroTimerActive = false;
                }

                if (pente == 0 && !reloading && municao > 0 && !penteZeroTimerActive) {
                    penteZeroTimerActive = true;
                    reloadOnZeroCooldownTimer = reloadOnZeroCooldown;
                }

                if ((input.isButtonPressed(InputManager.GamepadButton.A)
                        || input.isButtonPressed(InputManager.GamepadButton.LB)) && podeDash && !emDash) {
                    if (analogicoEsquerdo.x != 0 || analogicoEsquerdo.y != 0) {
                        aplicarDashDirecional(analogicoEsquerdo.partiallyNormalized());
                    }
                }

                updateFishing(input, camera, enemies);
            } else {
                boolean andaX = false;
                boolean andaY = false;
                Vetor2D movimento = new Vetor2D(0, 0);

                if (input.isKeyPressed(KeyEvent.VK_D)) {
                    movimento.x += 1;
                    dirX = 1;
                    andaX = true;
                }
                if (input.isKeyPressed(KeyEvent.VK_A)) {
                    movimento.x -= 1;
                    dirX = -1;
                    andaX = true;
                }
                if (!andaX || (input.isKeyPressed(KeyEvent.VK_D) && input.isKeyPressed(KeyEvent.VK_A))) {
                    dirX = 0;
                }

                if (input.isKeyPressed(KeyEvent.VK_S)) {
                    movimento.y += 1;
                    dirY = 1;
                    andaY = true;
                }
                if (input.isKeyPressed(KeyEvent.VK_W)) {
                    movimento.y -= 1;
                    dirY = -1;
                    andaY = true;
                }
                if (!andaY || (input.isKeyPressed(KeyEvent.VK_W) && input.isKeyPressed(KeyEvent.VK_S))) {
                    dirY = 0;
                }

                Vetor2D movimentoNormalizado = movimento.partiallyNormalized();
                registrarUltimaDirecaoMovimento(movimentoNormalizado);
                velX += aceleracao * controleAtual * movimentoNormalizado.x;
                velY += aceleracao * controleAtual * movimentoNormalizado.y;

                if (mouseMiraAtiva) {
                    double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                    double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();

                    if (input.isMouseButtonPressed(MouseEvent.BUTTON1)) {
                        if (shootCooldownTimer == 0 && pente > 0) {
                            double centerX = x + largura / 2.0;
                            double centerY = y + altura / 2.0;
                            double dirToMouseX = mouseXWorld - centerX;
                            double dirToMouseY = mouseYWorld - centerY;
                            tiroTimer = tiroTimerTime;

                            switch (gunType) {
                                case PISTOL -> {
                                    bulletmanager.shoot(centerX, centerY, dirToMouseX, dirToMouseY, BulletOwner.PLAYER);
                                    soundManager.playGunshot();
                                    shootCooldownTimer = pistolShootCooldown;
                                }
                                case SHOTGUN -> {
                                    double anguloBase = Math.atan2(dirToMouseY, dirToMouseX);
                                    double angulo1 = anguloBase;
                                    double angulo2 = anguloBase - Math.toRadians(15);
                                    double angulo3 = anguloBase + Math.toRadians(15);

                                    bulletmanager.shoot(centerX, centerY, Math.cos(angulo1), Math.sin(angulo1),
                                            BulletOwner.PLAYER, true);
                                    bulletmanager.shoot(centerX, centerY, Math.cos(angulo2), Math.sin(angulo2),
                                            BulletOwner.PLAYER, true);
                                    bulletmanager.shoot(centerX, centerY, Math.cos(angulo3), Math.sin(angulo3),
                                            BulletOwner.PLAYER, true);

                                    soundManager.playSFX(SoundManager.SFX.EXPLOSION);
                                    shootCooldownTimer = shotgunShootCooldown;
                                }
                            }

                            pente--;
                        }
                    }
                }

                if (GameCore.getDebug() && input.isKeyJustPressed(KeyEvent.VK_J)) {
                    setFasterFishing(true);
                    ToastNotifications.RequestNotification("FasterFishing = true");
                }
                if (GameCore.getDebug() && input.isKeyJustPressed(KeyEvent.VK_T)) {
                    setX((double) (250.5 * GameCore.tiles_size));
                    setY((double) (60 * GameCore.tiles_size));
                }

                if (GameCore.getDebug() && input.isKeyPressed(KeyEvent.VK_6) && input.isKeyJustPressed(KeyEvent.VK_7)) {
                    hasShotgun = true;
                    ToastNotifications.RequestNotification("DEBUG 67: habilitou shotgun", 2.0);
                }
                if (input.isKeyJustPressed(KeyEvent.VK_G) && hasShotgun && changeGunCooldownTimer == 0) {
                    if (getGunType() == Player.GunType.SHOTGUN) {
                        setGunType(Player.GunType.PISTOL);
                        if (ToastNotifications.getNotifAtual() != null
                                && (ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")
                                        || ToastNotifications.getNotifAtual().equals("Mudou para Pistola"))) {
                            ToastNotifications.skipNotification();
                        }
                        if (ToastNotifications.getNotifAtual() == null
                                || !ToastNotifications.getNotifAtual().equals("Mudou para Pistola")) {
                            ToastNotifications.RequestNotification("Mudou para Pistola", 1.0);
                        }
                    } else {
                        setGunType(Player.GunType.SHOTGUN);
                        if (ToastNotifications.getNotifAtual() != null
                                && (ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")
                                        || ToastNotifications.getNotifAtual().equals("Mudou para Pistola"))) {
                            ToastNotifications.skipNotification();
                        }
                        if (ToastNotifications.getNotifAtual() == null
                                || !ToastNotifications.getNotifAtual().equals("Mudou para Shotgun")) {
                            ToastNotifications.RequestNotification("Mudou para Shotgun", 1.0);
                        }
                    }
                    shootCooldownTimer = pistolShootCooldown;
                    changeGunCooldownTimer = changeGunCooldown;
                }
                if (input.isKeyPressed(KeyEvent.VK_R) && !reloading && pente < maxpente && municao > 0) {
                    reloading = true;
                    reloadCooldownTimer = (fasterReload) ? fasterReloadCooldown : defaultReloadCooldown;
                    penteZeroTimerActive = false;
                }
                if (pente == 0 && !reloading && municao > 0 && !penteZeroTimerActive) {
                    penteZeroTimerActive = true;
                    reloadOnZeroCooldownTimer = reloadOnZeroCooldown;
                }

                if (input.isKeyPressed(KeyEvent.VK_SPACE) && podeDash && !emDash) {
                    if (dirX != 0 || dirY != 0) {
                        Vetor2D direcaoMovimento = movimento.partiallyNormalized();
                        aplicarDashDirecional(direcaoMovimento);
                    }
                }

                if (mouseMiraAtiva) {
                    double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                    double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
                    updatePlayerDirection(mouseXWorld, mouseYWorld);
                }
                updateFishing(input, camera, enemies);
            }
        }
        if (reloading) {
            reloadCooldownTimer--;
            if (reloadCooldownTimer <= 0) {
                int diff = maxpente - pente;
                if (municao >= diff) {
                    pente = maxpente;
                    municao -= diff;
                } else {
                    pente += municao;
                    municao = 0;
                }
                reloading = false;
                penteZeroTimerActive = false;
            }
        }
        if (tiroTimer > 0) {
            tiroTimer--;
        }

        updatePlayerMovement();
    }

    private void updatePlayerDirection(double mouseX, double mouseY) {
        double centerX = x + largura / 2.0;
        double centerY = y + altura / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        angulo = Math.atan2(dy, dx);
        if (angle < 0) {
            angle += 360;
        }

        if (angle >= 45 && angle < 135) {
            direction = Direction.DOWN;
        } else if (angle >= 135 && angle < 225) {
            direction = Direction.LEFT;
        } else if (angle >= 225 && angle < 315) {
            direction = Direction.UP;
        } else {
            direction = Direction.RIGHT;
        }

        // System.out.println(angulo);
    }

    public void updateFishing(InputManager input, CameraManager camera, ArrayList<Enemy> enemies) {
        if (!hasFishingRod) {
            return;
        }

        if (fishingCooldown > 0) {
            fishingCooldown--;
        }

        Vetor2D analogicoEsquerdo = input.getLeftStick();
        Vetor2D analogicoDireito = input.getRightStick();
        boolean controleAtivo = input.isControllerActive();
        boolean mouseMiraAtiva = camera.isMouseMiraAtiva();
        boolean pescar = controleAtivo
                ? input.isButtonJustPressed(InputManager.GamepadButton.LT)
                : input.isMouseButtonJustPressed(MouseEvent.BUTTON3);

        if (pescar && fishingCooldown == 0) {
            System.out.println(">>> CLIQUE DIREITO PROCESSADO COM SUCESSO! <<<");

            if (fishingBobber.isAtivo()) {
                fishingBobber.pull();
            } else if (controleAtivo) {
                double direcaoX = Math.cos(angulo);
                double direcaoY = Math.sin(angulo);

                if (analogicoDireito.x != 0 || analogicoDireito.y != 0) {
                    direcaoX = analogicoDireito.x;
                    direcaoY = analogicoDireito.y;
                }

                double distanciaMira = Math.max(largura, altura) * 10.0;
                double alvoX = x + largura / 2.0 + direcaoX * distanciaMira;
                double alvoY = y + altura / 2.0 + direcaoY * distanciaMira;
                fishingBobber.cast(this, soundManager, alvoX, alvoY);
            } else if (mouseMiraAtiva) {
                double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
                fishingBobber.cast(this, soundManager, mouseXWorld, mouseYWorld);
            }
            fishingCooldown = 20;
        }
        fishingBobber.update(enemies, lvlData, this);
    }

    public void setTemporarySpriteOverride(int spriteIndex, int durationFrames) {
        this.spriteOverrideIndex = spriteIndex;
        this.spriteOverrideTimer = durationFrames;
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        int inv = 1;
        int xx = (int) x;

        if (dashDuracaoTimer > 0) {
            if (dashDirX < 0) {
                inv = -1;
                xx = (int) (x + GameCore.tiles_size);
            }
            if (dashDuracaoTimer > 26) {
                animIndex = 0;
            } else if (dashDuracaoTimer > 24) {
                animIndex = 1;
            } else if (dashDuracaoTimer > 22) {
                animIndex = 2;
            } else {
                animIndex = 3;
            }

            if (dashDirX != 0) {
                animSp = 10;
            } else if (dashDirY < 0) {
                animSp = 17;
            } else {
                animSp = 3;
            }
        } else {
            if (direction == Direction.LEFT) {
                inv = -1;
                xx = (int) (x + GameCore.tiles_size);
            }
            animTick += 60f * delta;
            if (animTick >= 12) {
                animTick = 0;
                t++;
                if (t >= 4) {
                    t = 0;
                }
                if (t % 2 == 0) {
                    animIndex = 0;
                } else {
                    if (t == 1) {
                        animIndex = 1;
                    }
                    if (t == 3) {
                        animIndex = 2;
                    }
                }
            }
            if (direction == Direction.DOWN) {
                animSp = 0;
            } else if (direction == Direction.UP) {
                animSp = 14;
            } else {
                animSp = 7;
            }
        }
        if (!isMoving() && !emDash) {
            animIndex = 0;
        }
        if (timerDano > 0) {
            animIndex = 0;
            if (direction == Direction.DOWN) {
                animSp = 21;
            } else if (direction == Direction.UP) {
                animSp = 23;
            } else {
                animSp = 22;
            }
        }
        boolean overrideAtivo = spriteOverrideTimer > 0 && spriteOverrideIndex >= 0
                && spriteOverrideIndex < Sprites.length;
        int spriteFinal = overrideAtivo ? spriteOverrideIndex : (animSp + animIndex);

        setSunAngle(GameCore.getSunAngle());
        PlayerVisualState visual = createPlayerVisualState(spriteFinal, inv, xx);
        ShadowSilhouette silhouette = createPlayerShadowSilhouette(visual);
        drawPlayerShadow(g2, silhouette, x, y);

        if (visual.gunBehindPlayer) {
            g2.drawImage(visual.gunFrame, visual.gunX, visual.gunY,
                    visual.gunWidth, visual.gunHeight, null);
        }
        g2.drawImage(visual.playerFrame, visual.playerX, visual.playerY,
                visual.playerWidth, 48, null);
        if (!visual.gunBehindPlayer) {
            g2.drawImage(visual.gunFrame, visual.gunX, visual.gunY,
                    visual.gunWidth, visual.gunHeight, null);
        }

        if (hasFishingRod && fishingBobber != null && fishingBobber.isAtivo()) {
            fishingBobber.draw(g2);
        }
    }

    public void addMunicao(int qtd) {
        municao += qtd;
        System.out.println("coletou " + String.valueOf(qtd) + "municao, total: " + String.valueOf(municao));
    }

    public void addMoedas(int qtd) {
        moedas += qtd;
        System.out.println("coletou " + String.valueOf(qtd) + "moedas, total: " + String.valueOf(municao));
    }

    public void addIscas(int qtd) {
        iscas += qtd;
        System.out.println("coletou " + String.valueOf(qtd) + "iscas, total: " + String.valueOf(iscas));

    }

    public void curar(int qtd) {
        vida = Math.min(vidaMaxima, vida + qtd);
        System.out.println("coletou cura +" + qtd + ", vida: " + vida);
        soundManager.playSFX(SoundManager.SFX.PLAYER_HEAL);
    }

    public void addChave(int qtd) {
        chaves += qtd;
        if (qtd > 0) {
            chavesColetadasTotal += qtd;
        }
        if (chaves == 1) {
            ToastNotifications.RequestNotification("Você encontrou a primeira chave para abrir o portão da Morsa!");
        }
        System.out.println("coletou chave total: " + chaves);
    }

    public void solicitarCheckpoint() {
        this.checkpointSolicitado = true;
    }

    public void limparSolicitacaoCheckpoint() {
        this.checkpointSolicitado = false;
    }

    public void respawn(double cx, double cy, int cvida, int cmunicao, int cpente, int cchaves) {
        this.x = cx;
        this.y = cy;
        this.spriteOverrideIndex = -1;
        this.spriteOverrideTimer = 0;
        this.vida = cvida;
        this.municao = cmunicao;
        this.pente = cpente;
        this.chaves = cchaves;

        this.isDead = false;
        this.velX = 0;
        this.velY = 0;
        this.emDash = false;
        this.isAirborne = false;
        this.isCaindo = false;
        this.danoRecebidoFlag = false;
        this.iFramesTimer = 60;

        if (this.fishingBobber != null) {
            this.fishingBobber.reset();
        }
    }

    public void resetarProgresso() {
        gunType = GunType.PISTOL;
        hasShotgun = false;
        this.chavesColetadasTotal = 0;
        this.chaves = 0;
        this.municao = 45;
        this.totalEnemyCount = 0;
        this.currentEnemyCount = 0;
        this.fasterFishing = false;
        this.pente = 15;
        iscas = 0;
        moedas = 0;
        this.vida = this.vidaMaxima;
        this.isDead = false;
        this.hasFishingRod = false;
        setDesbloqueouRecompensa(false);
        setExtendedMag(false);
        setFasterReload(false);
        if (this.fishingBobber != null) {
            this.fishingBobber.reset();
        }

        limparSolicitacaoCheckpoint();
    }

    public int getChaves() {
        return chaves;
    }

    public int getMoedas() {
        return moedas;
    }

    public int getIscas() {
        return iscas;
    }

    public void setMoedas(int valor) {
        moedas = valor;
    }

    public void equiparArma(String tipoArma) {
        armas++;
        System.out.println("equipou arma: " + tipoArma + " total: " + armas);
    }

    public int getPente() {
        return pente;
    }

    public int getPenteMax() {
        return maxpente;
    }

    public int getMunicao() {
        return municao;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void loadLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
    }

    public boolean hasFishingRod() {
        return this.hasFishingRod;
    }

    public void setFishingRod(boolean status) {
        this.hasFishingRod = status;
    }

    public boolean getFasterFishing() {
        return this.fasterFishing;
    }

    public void setFasterFishing(boolean set) {
        this.fasterFishing = set;
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
        shootCooldownTimer = value;
    }

    public void setBlockInputs(boolean valor) {
        this.blockInputs = valor;
    }

    public boolean isBlockInputs() {
        return blockInputs;
    }

    public Boolean isMoving() {
        return Math.hypot(velX, velY) > 0.2;
    }

    private void aplicarDashDirecional(Vetor2D direcaoMovimento) {
        double absX = Math.abs(direcaoMovimento.x);
        double absY = Math.abs(direcaoMovimento.y);

        if (absX == 0.0 && absY == 0.0) {
            return;
        }

        double referenciaX = ultimaDirecaoMovimentoX;
        double referenciaY = ultimaDirecaoMovimentoY;

        velX += direcaoMovimento.x * dashForca;
        velY += direcaoMovimento.y * dashForca;

        if (Math.abs(absX - absY) < 0.0001) {
            if (Math.abs(referenciaX) >= Math.abs(referenciaY)) {
                dashDirX = Math.signum(referenciaX != 0.0 ? referenciaX : direcaoMovimento.x);
                dashDirY = 0;
            } else {
                dashDirX = 0;
                dashDirY = Math.signum(referenciaY != 0.0 ? referenciaY : direcaoMovimento.y);
            }
        } else if (absX > absY) {
            dashDirX = Math.signum(direcaoMovimento.x);
            dashDirY = 0;
        } else {
            dashDirX = 0;
            dashDirY = Math.signum(direcaoMovimento.y);
        }

        podeDash = false;
        emDash = true;
        this.isAirborne = true;

        dashCooldownTimer = dashCooldown;
        dashDuracaoTimer = dashDuracao;
    }

    private void registrarUltimaDirecaoMovimento(Vetor2D direcaoMovimento) {
        if (direcaoMovimento == null) {
            return;
        }

        if (direcaoMovimento.x != 0.0 || direcaoMovimento.y != 0.0) {
            Vetor2D normalizada = direcaoMovimento.normalized();
            ultimaDirecaoMovimentoX = normalizada.x;
            ultimaDirecaoMovimentoY = normalizada.y;
        }
    }

    public boolean isCheckpointSolicitado() {
        return checkpointSolicitado;
    }

    public int getTotalChavesColetadas() {
        return chavesColetadasTotal;
    }
}
