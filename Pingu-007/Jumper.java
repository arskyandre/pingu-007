
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Jumper extends Enemy {

    private enum Status {
        PREPARANDO, PULANDO, FLUTUANDO, ATIRANDO, COOLDOWN
    }

    private Status estadoAtual = Status.PREPARANDO;

    private final BulletManager bulletManager;
    private int timer = 0;

    public int pulosParaAtirar = 3;
    private int pulosDados = 0;

    private final int tempoPreparo = 25;
    private final int tempoPulo = 40;
    private final int tempoFlutuando = 60;
    private final int tempoCooldown = 180;

    private final double distAtivacao = 300.0;

    private BufferedImage[] Sprites;
    private double alt = 0;
    private double squash = 0;
    private boolean saltoTerminaFlutuando = false;

    public Jumper(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr,
            SoundManager soundManager, ArenaManager arenaManager) {
        super(startX, startY, width, height, lvlData, soundManager, arenaManager);
        this.bulletManager = bulmgr;
        this.vidaMaxima = 40;
        this.vida = this.vidaMaxima;
        this.podePularBuracos = true;
        this.velocidadeAndar = 1.5;
        this.velocidadeMax = 30.0;
        this.raioDeteccao = GameCore.tiles_size * 5.0;

        this.aceleracao = 0.3;
        this.peso = 0.8;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
        this.timer = tempoPreparo;

        BufferedImage img = LoadSave.GetSpriteAtlas("images/enemy/boneve_sprite_sheet.png");
        Sprites = new BufferedImage[21];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                int index = i * 7 + j;
                Sprites[index] = img.getSubimage(j * 16, i * 16, 16, 16);
            }
        }
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
        double dx = (player.getX() + player.getLargura() / 2.0) - centerX;
        double dy = (player.getY() + player.getAltura() / 2.0) - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double atritoSalvo = this.atritoAtual;
        double velMaxSalva = this.velocidadeMax;

        this.isInvulneravel = (estadoAtual == Status.PULANDO || estadoAtual == Status.FLUTUANDO);
        this.podeSerPuxado = (!this.isInvulneravel && !emSaltoCinematico);

        if (isHooked || isPuxado) {
            if (estadoAtual == Status.PREPARANDO) {
                timer = tempoPreparo;
            }

            if (isPuxado) {
                this.velocidadeMax = 60.0;
            } else {
                this.velocidadeMax = 0.8;
                if (dist > 0) {
                    double oldAccel = this.aceleracao;
                    this.aceleracao = 0.1;
                    seguirCaminhoAStar(player, jumpLinks);
                    this.aceleracao = oldAccel;
                }
            }
        } else {
            switch (estadoAtual) {
                case PREPARANDO -> {
                    aplicarFreioDePreparacao(0.25);
                    /*
                    * if (dist > 0) {
                    * double dirX = dx / dist;
                    * double dirY = dy / dist;
                    * this.velX = dirX * velocidadeAndar;
                    * this.velY = dirY * velocidadeAndar;
                    * }
                     */
                    timer--;
                    if (timer <= 0) {
                        if (!podePularBuracos && !temLinhaDeVisaoLivre(player)) {
                            estadoAtual = Status.COOLDOWN;
                            timer = tempoCooldown;
                        } else {
                            estadoAtual = Status.PULANDO;
                            timer = tempoPulo;
                            this.isAirborne = true;
                            saltoTerminaFlutuando = pulosDados >= pulosParaAtirar - 1
                                    && dist <= distAtivacao && temAggro();

                            if (dist > 0) {
                                double atritoNoAr = 0.94;
                                double forcaDinamica = dist * (1.0 - atritoNoAr);

                                if (forcaDinamica > 25.0) {
                                    forcaDinamica = 25.0;
                                }
                                if (forcaDinamica < 6.0) {
                                    forcaDinamica = 6.0;
                                }

                                double jumpDx = dx / dist;
                                double jumpDy = dy / dist;
                                if (isSlippery) {
                                    double[] proj = preverDeslocamentoGelo(3);
                                    jumpDx = (dx - proj[0]) / dist;
                                    jumpDy = (dy - proj[1]) / dist;
                                    forcaDinamica += 1.5;
                                }

                                velX = jumpDx * forcaDinamica;
                                velY = jumpDy * forcaDinamica;
                                timerLedgeSnap = isSlippery ? 22 : 14;
                            }
                        }
                    }
                }
                case PULANDO -> {
                    if (emSaltoCinematico) {
                        executarSaltoCinematico();
                        if (!emSaltoCinematico) {
                            pulosDados++;
                            estadoAtual = Status.PREPARANDO;
                            timer = tempoPreparo;
                        }
                    } else {
                        this.velocidadeMax = 40.0;
                        this.atritoAtual = 0.94;
                        timer--;

                        if (timer <= 0) {
                            if (saltoTerminaFlutuando) {
                                estadoAtual = Status.FLUTUANDO;
                                timer = tempoFlutuando;
                            } else {
                                if (pulosDados < pulosParaAtirar - 1) {
                                    pulosDados++;
                                }
                                estadoAtual = Status.PREPARANDO;
                                timer = tempoPreparo;
                                this.isAirborne = false;
                                saltoTerminaFlutuando = false;
                            }
                        }
                    }
                }
                case FLUTUANDO -> {
                    aplicarFreioDePreparacao(0.25);
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
                    this.isAirborne = false;
                    saltoTerminaFlutuando = false;
                }
                case COOLDOWN -> {
                    this.velocidadeMax = 0.8;
                    if (dist > 0) {
                        double oldAccel = this.aceleracao;
                        this.aceleracao = 0.1;
                        seguirCaminhoAStar(player, jumpLinks);
                        this.aceleracao = oldAccel;
                    }
                    timer--;
                    if (timer <= 0) {
                        estadoAtual = Status.PREPARANDO;
                        timer = tempoPreparo;
                    }
                }
            }
        }

        if (!emSaltoCinematico) {
            aplicarFisicaBasica();
            this.atritoAtual = atritoSalvo;
            this.velocidadeMax = velMaxSalva;
            moveAndCollideWithMap(lvlData, arenaManager.getObjetosDeCenario());
        }
        if (isMoving()) {
            updateFootsteps(soundManager, lvlData);
        }

        if (!isHooked && !isPuxado && !isDead && !isCaindo && !isAirborne) {
            if (this.hitbox != null && player.getHurtbox() != null) {
                if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                    player.receberDano(danoContato);
                }
            }
        }
    }

    @Override
    public void drawGroundTelegraph(Graphics2D g2, double delta) {
        double progress;

        if (estadoAtual == Status.PULANDO) {
            if (emSaltoCinematico) {
                progress = lerpFramesMax > 0
                        ? (double) lerpFrameAtual / lerpFramesMax
                        : 0.0;
            } else if (saltoTerminaFlutuando) {
                double totalDuration = tempoPulo + tempoFlutuando;
                progress = (tempoPulo - timer) / totalDuration;
            } else {
                progress = (double) (tempoPulo - timer) / tempoPulo;
            }
        } else if (estadoAtual == Status.FLUTUANDO) {
            double totalDuration = tempoPulo + tempoFlutuando;
            progress = (tempoPulo + (tempoFlutuando - timer)) / totalDuration;
        } else if (estadoAtual == Status.ATIRANDO) {
            progress = 1.0;
        } else {
            return;
        }

        LandingMarker.draw(g2, x + width / 2.0, y + height,
                width, height, progress);
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        // Tiro Perfeito: Sobrepõe a invulnerabilidade e pune o Jumper jogando-o no
        // buraco
        /*
         * if (emSaltoCinematico) {
         * emSaltoCinematico = false;
         * isAirborne = true; // Religa a física da fase
         * velX = 0;
         * velY = 0;
         * estadoAtual = Status.COOLDOWN;
         * timer = tempoCooldown;
         * pulosDados = 0;
         * super.receberDano(dano, sourceX, sourceY, knockbackForce);
         * System.out.
         * println("Tiro perfeito! Jumper perdeu o salto cinemático e caiu no buraco!");
         * return;
         * }
         */

        if (estadoAtual == Status.PULANDO || estadoAtual == Status.FLUTUANDO || emSaltoCinematico) {
            return;
        }

        super.receberDano(dano, sourceX, sourceY, knockbackForce);

        if (estadoAtual == Status.PREPARANDO) {
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            pulosDados = 0;
            velX = 0;
            velY = 0;
        }
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        int spIndex = 0;
        if (null != estadoAtual) {
            switch (estadoAtual) {
                case FLUTUANDO -> {
                    squash = 0;
                    if (alt < 65) {
                        alt += 0.4;
                    }
                    if (timer > 54) {
                        spIndex = 1;
                    } else if (timer > 48) {
                        spIndex = 2;
                    } else if (timer > 42) {
                        spIndex = 3;
                    } else if (timer > 36) {
                        spIndex = 4;
                    } else if (timer > 0) {
                        spIndex = 5;
                    }
                }
                case COOLDOWN -> {
                    alt = 0;
                    squash = 0;
                    if (timer > 155) {
                        spIndex = 7;
                    } else if (timer > 130) {
                        spIndex = 8;
                    } else if (timer > 105) {
                        spIndex = 9;
                    } else if (timer > 80) {
                        spIndex = 10;
                    } else if (timer > 55) {
                        spIndex = 11;
                    } else if (timer > 30) {
                        spIndex = 12;
                    } else if (timer > 0) {
                        spIndex = 13;
                    }
                }
                case PULANDO -> {
                    squash = 0;
                    alt = 25;
                }
                case PREPARANDO -> {
                    alt = 0;
                    if (squash < 10) {
                        squash += 40f * delta;
                    }
                }
                default -> {
                }
            }
        }

        if (timerDano > 0) {
            spIndex = spIndex + 7;
        }

        int spriteX = (int) x - (int) squash / 2;
        int spriteY = (int) (y - alt) + (int) squash;
        int spriteWidth = (int) width + (int) squash;
        int spriteHeight = (int) height - (int) squash;
        ProjectedShadow.drawForEntity(g2, x, y, width, height,
                new ProjectedShadow.Part(Sprites[spIndex], spriteX, spriteY,
                        spriteWidth, spriteHeight));
        g2.drawImage(Sprites[spIndex], spriteX, spriteY, spriteWidth, spriteHeight, null);
    }
}
