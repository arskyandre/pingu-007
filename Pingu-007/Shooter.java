
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Shooter extends Enemy {

    private enum Status {
        PERSEGUINDO, PREPARANDO, ATIRANDO, COOLDOWN
    }

    private Status estadoAtual = Status.PERSEGUINDO;

    private final BulletManager bulletManager;
    private int timer = 0;

    private final double distAtivacao = 350.0;
    private final int tempoPreparo = 45;
    private final int tempoCooldown = 120;

    private final int maxTirosSpray = 5;
    private final int delaySpray = 6;
    private int tirosDisparados = 0;
    private int sprayTimer = 0;
    private double lockedAngle = 0;
    private int sinal = 1;

    private BufferedImage[] Sprites, arma;
    private int dirS = 0;
    private int animIndex = 0;
    private double animTick = 0;
    private double anguloArma = 0;

    public boolean modoShotgun = true;
    public boolean interromperNoTiro = true;

    public Shooter(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr,
            SoundManager soundManager, ArenaManager arenaManager) {
        super(startX, startY, width, height, lvlData, soundManager, arenaManager);
        this.bulletManager = bulmgr;
        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;
        this.velocidadeAndar = 2.0;
        this.velocidadeMax = 30.0;
        this.aceleracao = 0.5;
        this.peso = 1.0;
        this.raioDeteccao = GameCore.tiles_size * 10.0;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        this.cor = Color.RED;

        BufferedImage img = LoadSave.GetSpriteAtlas("images/enemy/shooter.png");
        Sprites = new BufferedImage[5];
        arma = new BufferedImage[2];
        Sprites[0] = img.getSubimage(0, 0, 20, 20);
        Sprites[1] = img.getSubimage(20, 0, 20, 20);
        Sprites[2] = img.getSubimage(40, 0, 20, 20);
        Sprites[3] = img.getSubimage(60, 0, 20, 20);
        Sprites[4] = img.getSubimage(80, 0, 20, 20);
        arma[0] = img.getSubimage(100, 0, 20, 20);
        arma[1] = img.getSubimage(120, 0, 20, 20);

    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        if (isDead) {
            return;
        }

        atualizarAggro(player);
        atualizarTimersKnockback();

        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double pCenterX = player.getX() + player.getLargura() / 2.0;
        double pCenterY = player.getY() + player.getAltura() / 2.0;

        double dx = pCenterX - centerX;
        double dy = pCenterY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (isHooked) {
            if (estadoAtual == Status.PREPARANDO || estadoAtual == Status.ATIRANDO) {
                estadoAtual = Status.COOLDOWN;
                timer = tempoCooldown;
                tirosDisparados = 0;
                velX = 0;
                velY = 0;
            }

            double oldAccel = this.aceleracao;
            this.aceleracao = 0.1;
            if (dist > 0 && !isPuxado) {
                seguirCaminhoAStar(player, jumpLinks);
            }
            this.aceleracao = oldAccel;
        } else {
            switch (estadoAtual) {
                case PERSEGUINDO -> {
                    if (dist < distAtivacao && temAggro()) {
                        estadoAtual = Status.PREPARANDO;
                        timer = tempoPreparo;
                        velX = 0;
                        velY = 0;
                    } else if (dist > 0) {
                        // O Atirador usa o A* para chegar até à zona de tiro
                        seguirCaminhoAStar(player, jumpLinks);
                    }
                }

                case PREPARANDO -> {
                    timer--;
                    velX = 0;
                    velY = 0;

                    if (timer <= 0) {
                        estadoAtual = Status.ATIRANDO;
                        lockedAngle = Math.atan2(dy, dx);
                        if (!modoShotgun) {
                            tirosDisparados = 0;
                            sprayTimer = 0;
                        }
                    }
                }

                case ATIRANDO -> {
                    velX = 0;
                    velY = 0;

                    if (modoShotgun) {
                        soundManager.playSFX(SoundManager.SFX.EXPLOSION);
                        double angulo1 = lockedAngle;
                        double angulo2 = lockedAngle - Math.toRadians(15);
                        double angulo3 = lockedAngle + Math.toRadians(15);

                        bulletManager.shoot(centerX, centerY, Math.cos(angulo1), Math.sin(angulo1), BulletOwner.ENEMY);
                        bulletManager.shoot(centerX, centerY, Math.cos(angulo2), Math.sin(angulo2), BulletOwner.ENEMY);
                        bulletManager.shoot(centerX, centerY, Math.cos(angulo3), Math.sin(angulo3), BulletOwner.ENEMY);

                        estadoAtual = Status.COOLDOWN;
                        timer = tempoCooldown;

                    } else {
                        if (sprayTimer <= 0) {
                            if (tirosDisparados == 0) {
                                soundManager.playSFX(SoundManager.SFX.SHOOTER_METRALHADA);
                            }
                            double anguloSpray = lockedAngle
                                    + Math.toRadians((-20 * sinal) + (tirosDisparados * 10 * sinal));
                            bulletManager.shoot(centerX, centerY, Math.cos(anguloSpray), Math.sin(anguloSpray),
                                    BulletOwner.ENEMY);

                            anguloArma = anguloSpray;
                            dirS = 1;
                            if (anguloArma > Math.PI / 2 || anguloArma < -Math.PI / 2) {
                                if (anguloArma > 0) {
                                    anguloArma = Math.PI - anguloArma;
                                } else {
                                    anguloArma = (-Math.PI) - anguloArma; // atenção

                                }
                                dirS = 0;
                            }

                            tirosDisparados++;
                            sprayTimer = delaySpray;
                            // System.out.println(anguloSpray);
                        } else {
                            sprayTimer--;
                        }

                        if (tirosDisparados >= maxTirosSpray) {
                            sinal = -sinal;
                            estadoAtual = Status.COOLDOWN;
                            timer = tempoCooldown;
                        }
                    }
                }

                case COOLDOWN -> {
                    timer--;
                    if (dist > 0) {
                        double oldAccel = this.aceleracao;
                        this.aceleracao *= 0.5;
                        seguirCaminhoAStar(player, jumpLinks);
                        this.aceleracao = oldAccel;
                    }

                    if (timer <= 0) {
                        estadoAtual = Status.PERSEGUINDO;
                    }
                }
            }
        }

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData, arenaManager.getObjetosDeCenario());

        if (!isDead && !isCaindo && !isHooked && !isPuxado) {
            if (this.hitbox != null && player.getHurtbox() != null) {
                if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                    player.receberDano(danoContato);
                }
            }
        }

        if (estadoAtual != Status.ATIRANDO) {
            if (velX > 0) {
                dirS = 1;
            } else if (velX < 0) {
                dirS = 0;
            }
        }

        if (isMoving()) {
            updateFootsteps(soundManager, lvlData);
        }
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        int xx = (int) x;
        int inv = 1;
        int indexArma = 0;

        animTick += 2f * delta;
        if (animTick >= 4) {
            animTick = 0;
        }
        animIndex = (int) animTick;

        if (estadoAtual != Status.ATIRANDO) {
            anguloArma = 0;
            indexArma = 0;
        } else {
            if (sprayTimer > 3) {
                indexArma = 1;
            }
        }

        if (!isMoving()) {
            animIndex = 0;
        }
        if (timerDano > 0) {
            animIndex = 4;
        }

        BufferedImage gun = HelpMethods.rotateImageByDegrees(arma[indexArma], anguloArma);
        int gap = gun.getWidth() * 3 - 60, yy = (int) y;
        gap /= 2;

        yy -= gap;
        if (dirS == 0) {
            inv = -1;
            xx = (int) (x + width);
        } else {
            gap *= -1;
        }

        g2.drawImage(Sprites[animIndex], xx, (int) y, inv * (int) width, (int) height, null);
        g2.drawImage(gun, xx + gap, yy, inv * gun.getWidth() * 3, gun.getHeight() * 3, null);
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        super.receberDano(dano, sourceX, sourceY, knockbackForce);
        if (interromperNoTiro && (estadoAtual == Status.PREPARANDO || estadoAtual == Status.ATIRANDO)) {
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            tirosDisparados = 0;
            velX = 0;
            velY = 0;
            System.out.println("Shooter atordoado! Ataque cancelado.");
        }
    }

    @Override
    public Boolean isMoving() {
        return (velX > 0.2 || velX < -0.2) || (velY > 0.2 || velY < -0.2);
    }
}
