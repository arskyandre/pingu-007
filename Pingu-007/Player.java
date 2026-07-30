
import java.awt.Graphics2D;
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
            SoundManager sound) {
        super(sound);
        this.x = startX;
        this.y = startY;
        this.aceleracao = 1.0;
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
                Sprites[index] = img.getSubimage(i * 16, j * 16, 16, 16);
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
        } else
            maxpente = 15;
    }

    public boolean getExtendedMag() {
        return extendedMag;
    }

    public boolean getHasShotgun() {
        return hasShotgun;
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

        moveAndCollideWithMap(lvlData);

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
            boolean andaX = false, andaY = false;
            if (input.isKeyPressed(KeyEvent.VK_D)) {
                velX += aceleracao * controleAtual;
                dirX = 1;
                andaX = true;
            }
            if (input.isKeyPressed(KeyEvent.VK_A)) {
                velX -= aceleracao * controleAtual;
                dirX = -1;
                andaX = true;
            }
            if (!andaX || (input.isKeyPressed(KeyEvent.VK_D) && input.isKeyPressed(KeyEvent.VK_A))) {
                dirX = 0;
            }

            if (input.isKeyPressed(KeyEvent.VK_S)) {
                velY += aceleracao * controleAtual;
                dirY = 1;
                andaY = true;
            }
            if (input.isKeyPressed(KeyEvent.VK_W)) {
                velY -= aceleracao * controleAtual;
                dirY = -1;
                andaY = true;
            }
            if (!andaY || (input.isKeyPressed(KeyEvent.VK_W) && input.isKeyPressed(KeyEvent.VK_S))) {
                dirY = 0;
            }

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

            if (input.isKeyJustPressed(KeyEvent.VK_T)) {
                setX((double) (250.5 * GameCore.tiles_size));
                setY((double) (60 * GameCore.tiles_size));
            }

            if (input.isKeyPressed(KeyEvent.VK_6) && input.isKeyJustPressed(KeyEvent.VK_7)) {
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
                    double tamanho = Math.sqrt(dirX * dirX + dirY * dirY);
                    dirX /= tamanho;
                    dirY /= tamanho;

                    velX += dirX * dashForca;
                    velY += dirY * dashForca;

                    dashDirX = dirX;
                    dashDirY = dirY;

                    podeDash = false;
                    emDash = true;
                    this.isAirborne = true;

                    dashCooldownTimer = dashCooldown;
                    dashDuracaoTimer = dashDuracao;
                }
            }

            updatePlayerDirection(mouseXWorld, mouseYWorld);
            updateFishing(input, camera, enemies);
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
        if(tiroTimer > 0){
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
        angulo = Math.atan2(dy,dx);
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
        
        //System.out.println(angulo);
    }

    public void updateFishing(InputManager input, CameraManager camera, ArrayList<Enemy> enemies) {
        if (!hasFishingRod) {
            return;
        }

        if (fishingCooldown > 0) {
            fishingCooldown--;
        }

        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON3) && fishingCooldown == 0) {
            System.out.println(">>> CLIQUE DIREITO PROCESSADO COM SUCESSO! <<<");
            if (ToastNotifications.getNotifAtual() != null && ToastNotifications.getNotifAtual()
                    .equals("DICA: Pressione o botão direito do mouse para fisgar inimigos.")) {
                ToastNotifications.skipNotification();
            }
            if (fishingBobber.isAtivo()) {
                fishingBobber.pull();
            } else {
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
    public void animate(Graphics2D g2, double delta) {
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

        int ginv = 1, xgun = (int)x;
        
        double ang = angulo;
        if (angulo > Math.PI / 2 || angulo < -Math.PI / 2) {
            ginv = -1;
            if (angulo > 0) {
                ang = Math.PI - ang;
            } else {
                ang = (-Math.PI) - ang; // atenção
            }
        }
        int indexArma = 0;
        if(gunType == GunType.SHOTGUN){
            indexArma = 2;
        }
        if(tiroTimer > 0){
            indexArma = indexArma+1;
        }
        BufferedImage gun = HelpMethods.rotateImageByDegrees(arma[indexArma], ang);
        int gap = gun.getWidth() * 3 - 48, yy = (int) y;
        gap /= 2;

        yy = yy-gap+6;

        if(ginv == -1){
            xgun = (int)x + 48;
        }
        
        if(gunType == GunType.PISTOL){
            g2.drawImage(gun, xgun - (gap-20)*ginv, yy, gun.getWidth() * 3 * ginv, gun.getHeight() * 3 ,null);//gun render under pingu
        }
        if(gunType != GunType.PISTOL && direction == Direction.UP){
            g2.drawImage(gun, xgun - (gap-12)*ginv, yy, gun.getWidth() * 3 * ginv, gun.getHeight() * 3 ,null);//gun render under pingous
        }
        g2.drawImage(Sprites[spriteFinal], xx, (int) y, 48 * inv, 48, null);
        if(gunType != GunType.PISTOL && direction != Direction.UP){
            g2.drawImage(gun, xgun - (gap-12)*ginv, yy, gun.getWidth() * 3 * ginv, gun.getHeight() * 3 ,null);//gun render above pingous
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
        return (velX > 0.2 || velX < -0.2) || (velY > 0.2 || velY < -0.2);
    }

    public boolean isCheckpointSolicitado() {
        return checkpointSolicitado;
    }

    public int getTotalChavesColetadas() {
        return chavesColetadasTotal;
    }
}
