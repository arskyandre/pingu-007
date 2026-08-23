
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
    private final double forcaExplosao = 25.0;

    private int dirS = 0;
    private double animTick = 0;
    private int animIndex = 0;
    private BufferedImage[] Sprites;

    public boolean soltaBalas = true;
    public boolean danoAosInimigos = true;
    private boolean jaExplodiu = false;
    private boolean jaAvisou = false;

    public Bomber(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr,
            ArrayList<Enemy> inimigos, SoundManager soundManager, ArenaManager arenaManager) {
        super(startX, startY, width, height, lvlData, soundManager, arenaManager);
        this.bulletManager = bulmgr;
        this.todosInimigos = inimigos;

        this.vidaMaxima = 15;
        this.vida = this.vidaMaxima;

        this.velocidadeAndar = 5.0;
        this.velocidadeMax = 30.0;
        this.aceleracao = 0.4;
        this.peso = 0.5;
        this.raioDeteccao = GameCore.tiles_size * 10.0;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);

        BufferedImage img = LoadSave.GetSpriteAtlas("images/enemy/nineeleven_sprite_sheet.png");
        Sprites = new BufferedImage[23];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                int index = i * 7 + j;
                Sprites[index] = img.getSubimage(j * 16, i * 16, 16, 16);
            }
        }
        Sprites[21] = img.getSubimage(112, 0, 16, 16);
        Sprites[22] = img.getSubimage(112, 16, 16, 16);

    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        if (jaExplodiu) {
            return;
        }
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
                if (dist <= distAtivacao && temAggro()) {
                    estadoAtual = Status.ACIONADO;
                    timer = tempoPavio;
                } else if (dist > 0) {
                    seguirCaminhoAStar(player, jumpLinks);
                }
            }
            case ACIONADO -> {
                if (!jaAvisou) {
                    // if (timer == tempoPavio) {
                    soundManager.playSFX(SoundManager.SFX.BOMBER_AVISO);
                    // } else{
                    // soundManager.playSFX(SoundManager.SFX.AAAHHHH);
                    // }
                    jaAvisou = true;
                }
                velX *= 0.8;
                velY *= 0.8;

                timer--;
                if (timer <= 0) {
                    detonar(player, centerX, centerY);
                }
            }
        }

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData, arenaManager.getObjetosDeCenario());
        if (isMoving()) {
            updateFootsteps(soundManager, lvlData);
        }
        if (velY > 0) {
            dirS = 1;
        } else if (velY < 0) {
            dirS = 0;
        }
    }

    private void detonar(Player player, double meuCenterX, double meuCenterY) {
        soundManager.playSFX(SoundManager.SFX.EXPLOSION);
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
                double distInimigo = Math
                        .sqrt(Math.pow(outroCenterX - meuCenterX, 2) + Math.pow(outroCenterY - meuCenterY, 2));

                if (distInimigo <= raioExplosao) {
                    outro.receberDano(danoExplosao, meuCenterX, meuCenterY, forcaExplosao);
                }
            }
        }

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

        if (this.vida <= 0 && !jaExplodiu) {
            this.isDead = false;
            this.vida = 1;
            this.estadoAtual = Status.ACIONADO;
            this.timer = 16;
            this.velX = 0;
            this.velY = 0;
        }
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        int w = (int) width;
        int h = (int) height;

        if (estadoAtual == Status.ACIONADO) {
            g2.setColor(new Color(255, 0, 0, 50));
            int rx = (int) (x + w / 2.0 - raioExplosao);
            int ry = (int) (y + h / 2.0 - raioExplosao);
            g2.fillOval(rx, ry, (int) raioExplosao * 2, (int) raioExplosao * 2);
        }

        if (estadoAtual == Status.PERSEGUINDO) {
            animTick += 60f * delta;
            if (animTick >= 4) {
                animTick = 0;
                animIndex++;
            }
            if (animIndex >= 7) {
                animIndex = 0;
            }
            if (dirS == 0) {
                if (timerDano > 0) {
                    animIndex = 15;
                }
                g2.drawImage(Sprites[animIndex + 7], (int) x, (int) y, (int) width, (int) height, null);
            } else {
                if (timerDano > 0) {
                    animIndex = 21;
                }
                g2.drawImage(Sprites[animIndex], (int) x, (int) y, (int) width, (int) height, null);
            }
        } else if (estadoAtual == Status.ACIONADO) {
            if (timer > 34) {
                animIndex = 14;
            } else if (timer > 28) {
                animIndex = 15;
            } else if (timer > 22) {
                animIndex = 16;
            } else if (timer > 16) {
                animIndex = 17;
            } else if (timer > 10) {
                animIndex = 18;
            } else {
                animIndex = 19;
            }
            g2.drawImage(Sprites[animIndex], (int) x, (int) y, (int) width, (int) height, null);
        }
    }
}
