
import java.awt.Color;
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

    public boolean modoShotgun = false;
    public boolean interromperNoTiro = true;

    public Shooter(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr,
            SoundManager sound) {
        super(startX, startY, width, height, lvlData, sound);
        this.bulletManager = bulmgr;
        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;
        this.velocidadeAndar = 2.0;
        this.velocidadeMax = 30.0;
        this.aceleracao = 0.5;
        this.peso = 1.0;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        this.cor = Color.RED;
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
                    soundManager.playSFX(SoundManager.SFX.GUNSHOT);
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
                        double anguloSpray = lockedAngle
                                + Math.toRadians((-20 * sinal) + (tirosDisparados * 10 * sinal));
                        bulletManager.shoot(centerX, centerY, Math.cos(anguloSpray), Math.sin(anguloSpray),
                                BulletOwner.ENEMY);

                        tirosDisparados++;
                        sprayTimer = delaySpray;
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

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData);

        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
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
}
