
import java.awt.Color;
import java.awt.Graphics2D;

public class Jumper extends Enemy {

    private enum Status {
        PREPARANDO, PULANDO, FLUTUANDO, ATIRANDO, COOLDOWN
    }
    private Status estadoAtual = Status.PREPARANDO;

    private final BulletManager bulletManager;
    private int timer = 0;

    // Configurações do Pulo e Combate
    public int pulosParaAtirar = 3;
    private int pulosDados = 0;

    private final int tempoPreparo = 25;
    private final int tempoPulo = 40;
    private final int tempoFlutuando = 60; // Frames a mais que ele passará congelado no ar
    private final int tempoCooldown = 180;

    private final double distAtivacao = 250.0; // Distância limite para descarregar o círculo de balas

    public Jumper(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr) {
        super(startX, startY, width, height, lvlData);
        this.bulletManager = bulmgr;

        this.vidaMaxima = 40;
        this.vida = this.vidaMaxima;

        // Valores normais base
        this.velocidadeMax = 30;
        this.aceleracao = 0.3;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        this.timer = tempoPreparo;
    }

    @Override
    public void update(Player player) {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double dx = (player.getX() + player.getLargura() / 2.0) - centerX;
        double dy = (player.getY() + player.getAltura() / 2.0) - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double atritoSalvo = this.atritoPadrao;
        double velMaxSalva = this.velocidadeMax;

        switch (estadoAtual) {
            case PREPARANDO -> {
                velX = 0;
                velY = 0;
                timer--;
                if (timer <= 0) {
                    estadoAtual = Status.PULANDO;
                    timer = tempoPulo;
                    if (dist > 0) {
                        // --- FÓRMULA DE IMPULSO DINÂMICO ---
                        // Calcula a força exata necessária para zerar a inércia em cima do player baseado no atrito (0.94)
                        double atritoNoAr = 0.94;
                        double forcaDinamica = dist * (1.0 - atritoNoAr);

                        // Travas de segurança
                        if (forcaDinamica > 25.0) {
                            forcaDinamica = 25.0;
                        }
                        if (forcaDinamica < 6.0) {
                            forcaDinamica = 6.0;
                        }

                        velX = (dx / dist) * forcaDinamica;
                        velY = (dy / dist) * forcaDinamica;
                    }
                }
            }

            case PULANDO -> {
                this.velocidadeMax = 40.0; // Libera o teto de velocidade
                this.atritoPadrao = 0.94;
                timer--;

                if (timer <= 0) {
                    if (pulosDados >= pulosParaAtirar - 1) {
                        // RESTRICÃO DE DISTÂNCIA
                        if (dist <= distAtivacao) {
                            estadoAtual = Status.FLUTUANDO;
                            timer = tempoFlutuando;
                        } else {
                            // Longe demais
                            estadoAtual = Status.PREPARANDO;
                            timer = tempoPreparo;
                        }
                    } else {
                        // Pulo comum
                        pulosDados++;
                        estadoAtual = Status.PREPARANDO;
                        timer = tempoPreparo;
                    }
                }
            }

            case FLUTUANDO -> {
                velX = 0;
                velY = 0;
                timer--;
                if (timer <= 0) {
                    estadoAtual = Status.ATIRANDO;
                }
            }

            case ATIRANDO -> {
                int numBalas = 12;
                for (int i = 0; i < numBalas; i++) {
                    double angulo = Math.toRadians(i * (360.0 / numBalas));
                    bulletManager.shoot(centerX, centerY, Math.cos(angulo), Math.sin(angulo), BulletOwner.ENEMY);
                }

                pulosDados = 0;
                estadoAtual = Status.COOLDOWN;
                timer = tempoCooldown;
            }

            case COOLDOWN -> {
                this.velocidadeMax = 0.8;
                if (dist > 0) {
                    velX += (dx / dist) * 0.1;
                    velY += (dy / dist) * 0.1;
                }
                timer--;
                if (timer <= 0) {
                    estadoAtual = Status.PREPARANDO;
                    timer = tempoPreparo;
                }
            }
        }

        aplicarFisicaBasica();

        this.atritoPadrao = atritoSalvo;
        this.velocidadeMax = velMaxSalva;

        moveAndCollideWithMap(lvlData);

        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        // Invulnerabilidade Total
        if (estadoAtual == Status.PULANDO || estadoAtual == Status.FLUTUANDO) {
            return;
        }

        super.receberDano(dano, sourceX, sourceY, knockbackForce);

        // Stun
        if (estadoAtual == Status.PREPARANDO) {
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            pulosDados = 0;
            velX = 0;
            velY = 0;
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        int drawX = (int) x;
        int drawY = (int) y;
        int w = (int) width;
        int h = (int) height;

        int elevacao = 0;
        Color corCorpo = Color.WHITE;

        switch (estadoAtual) {
            case PREPARANDO ->
                corCorpo = Color.ORANGE;
            case PULANDO -> {
                corCorpo = Color.YELLOW;
                elevacao = 15;
            }
            case FLUTUANDO -> {
                corCorpo = Color.GREEN;
                elevacao = 25;
            }
            case COOLDOWN ->
                corCorpo = Color.DARK_GRAY;
            default -> {
            }
        }

        // Sombra
        if (elevacao > 0) {
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(drawX + 5, drawY + h - 10, w - 10, 10);
        }

        g2.setColor(corCorpo);
        g2.fillRect(drawX, drawY - elevacao, w, h);
    }
}
