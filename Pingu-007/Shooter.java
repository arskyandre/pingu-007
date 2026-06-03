
import java.awt.Color;

public class Shooter extends Enemy {

    private enum Status {
        PERSEGUINDO, PREPARANDO, ATIRANDO, COOLDOWN
    }
    private Status estadoAtual = Status.PERSEGUINDO;

    private final BulletManager bulletManager;
    private int timer = 0;

    // Configurações do Atirador
    private final double distAtivacao = 350.0;
    private final int tempoPreparo = 45;
    private final int tempoCooldown = 120;

    // Variáveis de controle do Spray
    private final int maxTirosSpray = 5;
    private final int delaySpray = 6;          // Frames entre cada bala do spray
    private int tirosDisparados = 0;
    private int sprayTimer = 0;
    private double lockedAngle = 0;            // Guarda o ângulo travado na hora de atirar
    private int sinal = 1;                     // Controla a direção do Spray

    // Variáveis de Comportamento
    public boolean modoShotgun = false;         // True = Shotgun (3 tiros), False = Spray (Várias balas)
    public boolean interromperNoTiro = true;    // True = Leva Stun e cancela o tiro

    public Shooter(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr) {
        super(startX, startY, width, height, lvlData);
        this.bulletManager = bulmgr;

        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;

        this.velocidadeMax = 2.0;
        this.aceleracao = 0.5;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        this.cor = Color.RED;
    }

    @Override
    public void update(Player player) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double pCenterX = player.getX() + player.getLargura() / 2.0;
        double pCenterY = player.getY() + player.getAltura() / 2.0;

        double dx = pCenterX - centerX;
        double dy = pCenterY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        switch (estadoAtual) {
            case PERSEGUINDO -> {
                if (dist < distAtivacao) {
                    estadoAtual = Status.PREPARANDO;
                    timer = tempoPreparo;
                    velX = 0;
                    velY = 0;
                } else if (dist > 0) {
                    // Anda até o jogador
                    velX += (dx / dist) * aceleracao;
                    velY += (dy / dist) * aceleracao;
                }
            }

            case PREPARANDO -> {
                timer--;
                velX = 0;
                velY = 0;

                if (timer <= 0) {
                    estadoAtual = Status.ATIRANDO;
                    // Trava o ângulo exato do jogador no momento antes de atirar
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
                    // --- MODO SHOTGUN ---
                    double angulo1 = lockedAngle;
                    double angulo2 = lockedAngle - Math.toRadians(15); // 15 graus pra esquerda
                    double angulo3 = lockedAngle + Math.toRadians(15); // 15 graus pra direita

                    // Dispara as 3 balas
                    bulletManager.shoot(centerX, centerY, Math.cos(angulo1), Math.sin(angulo1), BulletOwner.ENEMY);
                    bulletManager.shoot(centerX, centerY, Math.cos(angulo2), Math.sin(angulo2), BulletOwner.ENEMY);
                    bulletManager.shoot(centerX, centerY, Math.cos(angulo3), Math.sin(angulo3), BulletOwner.ENEMY);

                    estadoAtual = Status.COOLDOWN;
                    timer = tempoCooldown;

                } else {
                    // --- MODO SPRAY ---
                    if (sprayTimer <= 0) {
                        // Calcula o desvio do ângulo: vai de -20 graus até +20 graus
                        double anguloSpray = lockedAngle + Math.toRadians((-20 * sinal) + (tirosDisparados * 10 * sinal));

                        bulletManager.shoot(centerX, centerY, Math.cos(anguloSpray), Math.sin(anguloSpray), BulletOwner.ENEMY);

                        tirosDisparados++;
                        sprayTimer = delaySpray; // Reseta o delay para a próxima bala
                    } else {
                        sprayTimer--;
                    }

                    // Verifica se já atirou todas as balas do padrão
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
                    velX += (dx / dist) * (aceleracao * 0.5);
                    velY += (dy / dist) * (aceleracao * 0.5);
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
    public void receberDano(int dano) {
        super.receberDano(dano);
        if (interromperNoTiro && (estadoAtual == Status.PREPARANDO || estadoAtual == Status.ATIRANDO)) {
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            tirosDisparados = 0; // Limpa a contagem do spray caso estivesse no meio dele
            velX = 0;
            velY = 0;
            System.out.println("Shooter atordoado! Ataque cancelado.");
        }
    }
}
