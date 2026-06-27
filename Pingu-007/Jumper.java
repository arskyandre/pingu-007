
import java.awt.Color;
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

    public Jumper(double startX, double startY, double width, double height, int[][] lvlData, BulletManager bulmgr, SoundManager sound) {
        super(startX, startY, width, height, lvlData, sound);
        this.bulletManager = bulmgr;
        this.vidaMaxima = 40;
        this.vida = this.vidaMaxima;
        this.podePularBuracos = true;
        this.velocidadeAndar = 1.5;
        this.velocidadeMax = 30.0;

        this.aceleracao = 0.3;
        this.peso = 0.8;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
        this.timer = tempoPreparo;

        BufferedImage img = LoadSave.GetSpriteAtlas("boneve_sprite_sheet.png");
        Sprites = new BufferedImage[14];
        for (int i = 0; i < 2; i++) {
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

        switch (estadoAtual) {
            case PREPARANDO -> {
                aplicarFreioDePreparacao(0.25);
                /*if (dist > 0) {
                    double dirX = dx / dist;
                    double dirY = dy / dist;
                    this.velX = dirX * velocidadeAndar;
                    this.velY = dirY * velocidadeAndar;
                }*/
                timer--;
                if (timer <= 0) {
                    if (!podePularBuracos && !temLinhaDeVisaoLivre(player)) {
                        estadoAtual = Status.COOLDOWN;
                        timer = tempoCooldown;
                    } else {
                        estadoAtual = Status.PULANDO;
                        timer = tempoPulo;
                        this.isAirborne = true;

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
                        if (pulosDados >= pulosParaAtirar - 1) {
                            if (dist <= distAtivacao && temAggro()) {
                                estadoAtual = Status.FLUTUANDO;
                                timer = tempoFlutuando;
                            } else {
                                estadoAtual = Status.PREPARANDO;
                                timer = tempoPreparo;
                                this.isAirborne = false;
                            }
                        } else {
                            pulosDados++;
                            estadoAtual = Status.PREPARANDO;
                            timer = tempoPreparo;
                            this.isAirborne = false;
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

        this.isInvulneravel = (estadoAtual == Status.PULANDO || estadoAtual == Status.FLUTUANDO);

        // Isola fisicamente durante o LERP
        if (!emSaltoCinematico) {
            aplicarFisicaBasica();
            this.atritoAtual = atritoSalvo;
            this.velocidadeMax = velMaxSalva;
            moveAndCollideWithMap(lvlData);
        }

        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
    }

    @Override
    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        // Tiro Perfeito: Sobrepõe a invulnerabilidade e pune o Jumper jogando-o no buraco
        /*if (emSaltoCinematico) {
            emSaltoCinematico = false;
            isAirborne = true; // Religa a física da fase
            velX = 0;
            velY = 0;
            estadoAtual = Status.COOLDOWN;
            timer = tempoCooldown;
            pulosDados = 0;
            super.receberDano(dano, sourceX, sourceY, knockbackForce);
            System.out.println("Tiro perfeito! Jumper perdeu o salto cinemático e caiu no buraco!");
            return;
        }*/

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
    public void animate(Graphics2D g2, double delta) {
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
                        squash += 40f*delta;
                    }
                }
                default -> {
                }
            }
        }
        g2.drawImage(Sprites[spIndex],
                (int) x - (int) squash / 2,
                (int) (y - alt) + (int) squash,
                (int) width + (int) squash,
                (int) height - (int) squash,
                null);
    }

    @Override
    public void draw(Graphics2D g2) {
        int drawX = (int) x;
        int drawY = (int) y;
        int w = (int) width;
        int h = (int) height;

        int elevacao = 0;
        /*Color corCorpo = Color.WHITE;

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
        }*/

        switch (estadoAtual) {
            case PULANDO -> {
                elevacao = 15;
            }
            case FLUTUANDO -> {
                elevacao = 25;
            }
            case COOLDOWN, PREPARANDO, ATIRANDO -> {
            }
        }
        // Sombra Dinâmica
        int shadowW = Math.max(10, w - 10 - (elevacao / 2));
        int shadowH = Math.max(4, 10 - (elevacao / 4));
        int shadowX = drawX + (w - shadowW) / 2;
        int shadowY = drawY + h - (shadowH / 2);

        int alpha = Math.max(20, 100 - (elevacao * 2));
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillOval(shadowX, shadowY, shadowW, shadowH);
        // Sombra
        /*if (elevacao > 0) {
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillOval(drawX + 5, drawY + h - 10, w - 10, 10);
        }*/
        // g2.setColor(corCorpo);
        //g2.fillRect(drawX, drawY - elevacao, w, h);
    }
}
