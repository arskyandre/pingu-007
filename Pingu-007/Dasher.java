
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Dasher extends Enemy {

    private enum Status {
        PERSEGUINDO, PREPARANDO, DASHING, COOLDOWN
    }
    private Status estadoAtual = Status.PERSEGUINDO;

    private int timer = 0;
    private double dashDirX = 0;
    private double dashDirY = 0;
    private int animTick = 0;
    private int animIndex = 0;
    private int dirS = 0;

    private final double distAtivacao = 200.0;
    private final int tempoPreparo = 60;
    private final int tempoDash = 30;
    private final int tempoCooldown = 120;
    private final double forcaDash = 30.0;
    private final double atritoDash = 0.96;

    public boolean interromperNoTiro = true;
    private BufferedImage[] Sprites;

    public Dasher(double startX, double startY, double width, double height, int[][] lvlData) {
        super(startX, startY, width, height, lvlData);
        this.vidaMaxima = 45;
        this.vida = this.vidaMaxima;
        this.podePularBuracos = true;

        this.velocidadeAndar = 3.5;
        this.velocidadeMax = 45.0;
        this.aceleracao = 0.8;
        this.peso = 1.0;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
        this.cor = Color.CYAN;

        BufferedImage img = LoadSave.GetSpriteAtlas("narval_sprite_sheet.png");
        Sprites = new BufferedImage[9];
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 4; i++) {
                int index = j * 4 + i;
                Sprites[index] = img.getSubimage(i * 16, j * 16, 16, 16);
            }
        }
        Sprites[8] = img.getSubimage(0, 32, 19, 16);
    }

    @Override
    protected void prepararSaltoAStar(Node noDestino) {
        this.pendingJumpNode = noDestino;
        this.estadoAtual = Status.PREPARANDO;
        this.timer = tempoPreparo;
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
        double dist = Math.hypot(dx, dy);

        if (!isPuxado && !isCaindo) {
            if (emSaltoCinematico) {
                executarSaltoCinematico();
                if (!emSaltoCinematico) { // Terminou o LERP
                    estadoAtual = Status.COOLDOWN;
                    timer = tempoCooldown;
                }
            } else if (isAirborne) {
                seguirCaminhoAStar(player, jumpLinks);
            } else {
                switch (estadoAtual) {
                    case PERSEGUINDO -> {
                        if (dist < distAtivacao && temAggro()) {
                            if (!podePularBuracos && !temLinhaDeVisaoLivre(player)) {
                                seguirCaminhoAStar(player, jumpLinks);
                            } else {
                                estadoAtual = Status.PREPARANDO;
                                timer = tempoPreparo;
                            }
                        } else {
                            seguirCaminhoAStar(player, jumpLinks);
                        }
                    }
                    case PREPARANDO -> {
                        timer--;
                        if (timer <= 0) {
                            estadoAtual = Status.DASHING;
                            if (pendingJumpNode != null) {
                                iniciarSaltoCinematico(pendingJumpNode, tempoDash);
                                pendingJumpNode = null;
                            } else {
                                timer = tempoDash;
                                if (dist > 0) {
                                    dashDirX = dx / dist;
                                    dashDirY = dy / dist;
                                    velX = dashDirX * forcaDash;
                                    velY = dashDirY * forcaDash;
                                }
                            }
                        }
                    }
                    case DASHING -> {
                        timer--;
                        this.velX *= atritoDash;
                        this.velY *= atritoDash;
                        if (timer <= 0) {
                            estadoAtual = Status.COOLDOWN;
                            timer = tempoCooldown;
                        }
                    }
                    case COOLDOWN -> {
                        timer--;
                        double oldAccel = this.aceleracao;
                        this.aceleracao *= 0.5;
                        seguirCaminhoAStar(player, jumpLinks);
                        this.aceleracao = oldAccel;

                        if (timer <= 0) {
                            estadoAtual = Status.PERSEGUINDO;
                        }
                    }
                }
            }
        }

        if (!emSaltoCinematico) {
            double andarSalvo = this.velocidadeAndar;
            double atritoSalvo = this.atritoAtual;

            if (estadoAtual == Status.DASHING) {
                this.velocidadeAndar = 45.0;
                this.atritoAtual = atritoDash;
            } else if (isAirborne) {
                this.velocidadeAndar = 45.0;
                this.atritoAtual = 0.95;
            } else if (estadoAtual == Status.PREPARANDO) {
                aplicarFreioDePreparacao(0.25);
            }

            aplicarFisicaBasica();

            this.velocidadeAndar = andarSalvo;
            this.atritoAtual = atritoSalvo;

            moveAndCollideWithMap(lvlData);
        }

        if (!isDead && !isCaindo) {
            if (this.hitbox != null && player.getHurtbox() != null) {
                if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                    player.receberDano(danoContato);
                }
            }
        }

        if (velX > 0) {
            dirS = 1;
        } else if (velX < 0) {
            dirS = 0;
        }
    }

    @Override
    public void animate(Graphics2D g2) {
        int xx = (int) x;
        int inv = 1;

        if (dirS == 0) {
            inv = -1;
            xx = (int) (x + width);
        }

        if (estadoAtual == Status.DASHING || isAirborne || emSaltoCinematico) {
            g2.drawImage(Sprites[8], xx, (int) y, inv * (int) (19 * width / 16), (int) height, null);
        } else {
            if (estadoAtual == Status.PREPARANDO) {
                if (timer > 54) {
                    animIndex = 3;
                } else if (timer > 48) {
                    animIndex = 4;
                } else if (timer > 42) {
                    animIndex = 5;
                } else if (timer > 36) {
                    animIndex = 6;
                } else {
                    animIndex = 7;
                }
            } else {
                animTick++;
                if (animTick >= 90) {
                    animTick = 0;
                    animIndex++;
                    if (animIndex >= 3) {
                        animIndex = 0;
                    }
                }
            }
            g2.drawImage(Sprites[animIndex], xx, (int) y, inv * (int) width, (int) height, null);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        super.receberDano(dano, sourceX, sourceY, knockbackForce);

        if (interromperNoTiro) {
            if (emSaltoCinematico) {
                emSaltoCinematico = false;
                isAirborne = true; // Religa a gravidade da engine
                velX = 0;
                velY = 0;
                estadoAtual = Status.COOLDOWN;
                timer = tempoCooldown;
                System.out.println("Tiro perfeito! Dasher teve o salto cinemático interrompido e vai cair!");
            } else if (estadoAtual == Status.DASHING || isAirborne) {
                estadoAtual = Status.COOLDOWN;
                timer = tempoCooldown;

                if (isAirborne) {
                    isAirborne = false;
                    System.out.println("Tiro perfeito! Dasher perdeu o salto orgânico e vai cair!");
                } else {
                    velX = 0;
                    velY = 0;
                    System.out.println("Dasher atordoado no chão!");
                }
            }
        }
    }
}
