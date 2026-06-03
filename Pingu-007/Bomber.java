
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class Bomber extends Enemy {

    private enum Status {
        PERSEGUINDO, ACIONADO
    }
    private Status estadoAtual = Status.PERSEGUINDO;

    private final BulletManager bulletManager;
    private final ArrayList<Enemy> todosInimigos;

    private int timer = 0;
    private final int tempoPavio = 40;
    private final double distAtivacao = 65.0;

    private final double raioExplosao = 140.0;
    private final int danoExplosao = 30;
    private final double forcaExplosao = 25.0; // Knockback massivo gerado pela explosão

    // Variações mutáveis do inimigo
    public boolean soltaBalas = true;
    public boolean danoAosInimigos = true;
    private boolean jaExplodiu = false;

    public Bomber(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr, ArrayList<Enemy> inimigos) {
        super(startX, startY, width, height, lvlData);
        this.bulletManager = bulmgr;
        this.todosInimigos = inimigos;

        this.vidaMaxima = 15;
        this.vida = this.vidaMaxima;

        this.velocidadeMax = 2.0;
        this.aceleracao = 0.4;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
    }

    @Override
    public void update(Player player) {
        if (jaExplodiu) {
            return;
        }
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;

        double pCenterX = player.getX() + player.getLargura() / 2.0;
        double pCenterY = player.getY() + player.getAltura() / 2.0;

        double dx = pCenterX - centerX;
        double dy = pCenterY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        switch (estadoAtual) {
            case PERSEGUINDO -> {
                if (dist <= distAtivacao) {
                    estadoAtual = Status.ACIONADO;
                    timer = tempoPavio;
                } else if (dist > 0) {
                    velX += (dx / dist) * aceleracao;
                    velY += (dy / dist) * aceleracao;
                }
            }
            case ACIONADO -> {
                // Freia bruscamente enquanto o pavio queima
                velX *= 0.8;
                velY *= 0.8;

                timer--;
                if (timer <= 0) {
                    detonar(player, centerX, centerY);
                }
            }
        }

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData);
    }

    private void detonar(Player player, double meuCenterX, double meuCenterY) {
        jaExplodiu = true;
        double distPlayer = Math.sqrt(Math.pow(player.getX() + player.getLargura() / 2.0 - meuCenterX, 2)
                + Math.pow(player.getY() + player.getAltura() / 2.0 - meuCenterY, 2));
        if (distPlayer <= raioExplosao) {
            player.receberDano(danoExplosao);
        }

        if (danoAosInimigos && todosInimigos != null) {
            for (Enemy outro : todosInimigos) {
                if (outro == this || outro.isDead()) {
                    continue;
                }

                double outroCenterX = outro.getX() + outro.getLargura() / 2.0;
                double outroCenterY = outro.getY() + outro.getAltura() / 2.0;
                double distInimigo = Math.sqrt(Math.pow(outroCenterX - meuCenterX, 2)
                        + Math.pow(outroCenterY - meuCenterY, 2));

                if (distInimigo <= raioExplosao) {
                    // Causa dano e arremessa o inimigo para longe da explosão
                    outro.receberDano(danoExplosao, meuCenterX, meuCenterY, forcaExplosao);
                }
            }
        }

        // Tiro em Círculo
        if (soltaBalas) {
            int numBalas = 8;
            for (int i = 0; i < numBalas; i++) {
                double angulo = Math.toRadians(i * (360.0 / numBalas));
                bulletManager.shoot(meuCenterX, meuCenterY, Math.cos(angulo), Math.sin(angulo), BulletOwner.ENEMY);
            }
        }

        this.vida = 0;
        this.isDead = true;
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        super.receberDano(dano, sourceX, sourceY, knockbackForce * 2.0);

        // Se o tiro o matou e ele ainda não explodiu, força a explosão
        if (this.vida <= 0 && !jaExplodiu) {
            this.isDead = false; // Ressuscita por 1 frame para não ser deletado pelo Manager
            this.vida = 1;

            this.estadoAtual = Status.ACIONADO;
            this.timer = 16;

            // Congela o Bomber
            this.velX = 0;
            this.velY = 0;
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        int drawX = (int) x;
        int drawY = (int) y;
        int w = (int) width;
        int h = (int) height;

        cor = Color.BLACK;

        if (estadoAtual == Status.ACIONADO) {
            if (timer % 10 > 4) {
                cor = Color.RED;
            } else {
                cor = Color.WHITE;
            }

            g2.setColor(new Color(255, 0, 0, 50));
            int rx = (int) (x + w / 2.0 - raioExplosao);
            int ry = (int) (y + h / 2.0 - raioExplosao);
            g2.fillOval(rx, ry, (int) raioExplosao * 2, (int) raioExplosao * 2);
        }

        g2.setColor(cor);
        g2.fillOval(drawX, drawY, w, h);
    }
}
