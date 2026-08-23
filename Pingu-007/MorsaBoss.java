
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class MorsaBoss extends Enemy {

    private BossMao maoEsquerda;
    private BossMao maoDireita;

    private double xHome, yHome;
    private BufferedImage[] Sprites;
    private int Direita = 1;
    private int dirS = 1;
    private int[] idle = {0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 4, 5};
    private int[] rugidoSprites = {8, 9};
    private double animT = 0;
    private double timerVirar = 0;

    private BulletManager bulletManager;
    private GameCore gameCore;
    private CameraManager camera;
    private EnemyManager enemyManager;

    // Controle do Rugido e Fase 2
    private boolean rugindo = false;
    private double timerRugido = 0;
    private double cooldownRugido = 30;
    private boolean podeRugir = false;

    // Lógica de Spawn (Mobs)
    private int contadorRugidos = 0;
    private int rugidosParaSpawn = 3;
    private ArrayList<Enemy> minionsSpawnados = new ArrayList<>();
    private ArrayList<Point2D.Double> pontosDeSpawn = new ArrayList<>();

    // Controle de Ataques
    private double timerAtaque = 120;
    private int ataqueSorteio = 0;
    private BossMao maoEsmagandoAtiva = null;
    private int contadorBote = 0;

    public MorsaBoss(double startX, double startY, int[][] lvlData, BulletManager bulmgr, SoundManager sound,
            GameCore GC, ArenaManager am) {
        super(startX, startY, GameCore.tiles_size * 6, GameCore.tiles_size * 6, lvlData, sound, am);
        gameCore = GC;
        this.bulletManager = bulmgr;
        this.vidaMaxima = 1100;
        this.vida = this.vidaMaxima;
        this.cor = Color.BLUE;
        this.aggroPermanente = true;
        this.bodyCollider = new Collider(0, 0, GameCore.tiles_size * 6, GameCore.tiles_size * 6);

        this.xHome = startX;
        this.yHome = startY;

        BufferedImage img = LoadSave.GetSpriteAtlas("images/enemy/MorsaBoss-Sheet.png");
        Sprites = new BufferedImage[10];
        for (int j = 0; j < 10; j++) {
            Sprites[j] = img.getSubimage(j * 96, 0, 96, 96);
        }
    }

    // MÉTODOS DE SPAWN E MANAGERS
    public void setEnemyManager(EnemyManager em) {
        this.enemyManager = em;
    }

    public EnemyManager getEnemyManager() {
        return this.enemyManager;
    }

    public void adicionarPontoDeSpawn(double x, double y) {
        pontosDeSpawn.add(new Point2D.Double(x, y));
    }

    private void spawnarMinions() {
        if (enemyManager == null || pontosDeSpawn.isEmpty()) {
            return;
        }

        int limiteShooters = 3;
        int shootersGerados = 0;

        for (Point2D.Double p : pontosDeSpawn) {
            String tipoSorteado;

            double chance = Math.random();
            if (chance < 0.50) {
                tipoSorteado = "lobo";
            } else if (chance < 0.80) {
                tipoSorteado = "bomber";
            } else {
                tipoSorteado = "shooter";
            }

            if (tipoSorteado.equals("shooter")) {
                if (shootersGerados < limiteShooters) {
                    shootersGerados++;
                } else {
                    tipoSorteado = "lobo";
                }
            }

            Enemy novo = enemyManager.adicionarE_RetornarInimigo(tipoSorteado, p.x, p.y, 0, 67);
            if (novo != null) {
                minionsSpawnados.add(novo);

                if (gameCore != null && gameCore.getArenaManager() != null) {
                    gameCore.getArenaManager().registrarInimigoNaArena(67, novo);
                }
            }
        }
        System.out.println("BOSS: Spawnou " + pontosDeSpawn.size() + " minions para ajuda do player!");
    }

    public boolean isFase2() {
        return this.vida <= (this.vidaMaxima / 2);
    }

    public void vincularMaos(BossMao esquerda, BossMao direita) {
        this.maoEsquerda = esquerda;
        this.maoDireita = direita;
    }

    public CameraManager getCamera() {
        return camera;
    }

    public BulletManager getBulletManager() {
        return bulletManager;
    }

    public double getCenterX() {
        return this.x + (this.width / 2.0);
    }

    public double getCenterY() {
        return this.y + (this.height / 2.0);
    }

    public BossMao getMaoEsquerda() {
        return maoEsquerda;
    }

    public BossMao getMaoDireita() {
        return maoDireita;
    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        velX = 0;
        velY = 0;
        this.x = xHome;
        this.y = yHome;

        /*
         * if (!isDead && this.bodyCollider != null && player.getHurtbox() != null) {
         * if (this.bodyCollider.intersects(this.x, this.y, player.getHurtbox(),
         * player.getX(), player.getY())) {
         * player.receberDano(this.danoContato); // Herda os 10 de dano do Enemy
         * }
         * }
         */
        if (timerVirar > 0) {
            timerVirar -= 1.0;
        }

        if (podeRugir) {
            atualizarRugido();

            if (!rugindo) {
                if (maoEsmagandoAtiva != null) {
                    if (!maoEsmagandoAtiva.isEsmagando()) {
                        maoEsmagandoAtiva = null;
                    } else if (maoEsmagandoAtiva.isPreparandoEsmagamento()) {
                        if (maoEsmagandoAtiva.getTimerEstado() < 35) {
                            double playerCenterX = player.getX() + player.getLargura() / 2.0;
                            double bossCenterX = x + (getLargura() / 2.0);

                            boolean playerNaEsquerda = playerCenterX < bossCenterX - 30;
                            boolean playerNaDireita = playerCenterX > bossCenterX + 30;

                            if (playerNaEsquerda && maoEsmagandoAtiva == maoDireita && maoEsquerda != null) {
                                double tempoSalvo = maoDireita.getTimerEstado();
                                double tempoComDesconto = Math.max(0, tempoSalvo - 20);

                                maoDireita.cancelarAtaque();
                                maoEsquerda.iniciarHoverSlam(tempoComDesconto);
                                maoEsmagandoAtiva = maoEsquerda;

                            } else if (playerNaDireita && maoEsmagandoAtiva == maoEsquerda && maoDireita != null) {
                                double tempoSalvo = maoEsquerda.getTimerEstado();
                                double tempoComDesconto = Math.max(0, tempoSalvo - 20);

                                maoEsquerda.cancelarAtaque();
                                maoDireita.iniciarHoverSlam(tempoComDesconto);
                                maoEsmagandoAtiva = maoDireita;
                            }
                        }
                    }
                }

                boolean maoEsqIdle = (maoEsquerda == null || maoEsquerda.isIdle());
                boolean maoDirIdle = (maoDireita == null || maoDireita.isIdle());

                if (maoEsqIdle && maoDirIdle) {
                    maoEsmagandoAtiva = null;

                    timerAtaque -= 1.0;
                    if (timerAtaque <= 0) {
                        escolherAtaque(player);

                        double baseTimer = 150 + (Math.random() * 100);
                        if (ataqueSorteio == 0 && (contadorBote % 3 == 0)) {
                            baseTimer += 60;
                        }
                        timerAtaque = isFase2() ? baseTimer * 0.6 : baseTimer;
                    }
                }
            }
        }

        if (!rugindo) {
            double playerCenterX = player.getX() + player.getLargura() / 2.0;
            double CenterX = x + (getLargura() / 2);
            if (CenterX < playerCenterX) {
                Direita = 1;
            } else if (CenterX > playerCenterX) {
                Direita = 0;
            }
        }
    }

    @Override
    public void receberDano(int dano) {
        super.receberDano(dano);

        if (this.isDead || this.vida <= 0) {
            for (Enemy minion : minionsSpawnados) {
                if (minion != null && !minion.isDead()) {
                    minion.marcarLootProcessado();
                    minion.receberDano(99999);
                }
            }
        }
    }

    private void escolherAtaque(Player player) {
        ataqueSorteio = (int) (Math.random() * 2);

        double playerCenterX = player.getX() + player.getLargura() / 2.0;
        double bossCenterX = x + (getLargura() / 2.0);
        boolean playerNaEsquerda = playerCenterX < bossCenterX;

        if (ataqueSorteio == 0) {
            contadorBote++;
            boolean duplo = (contadorBote % 3 == 0);

            if (duplo) {
                if (playerNaEsquerda) {
                    if (maoEsquerda != null) {
                        maoEsquerda.iniciarBote(true, false);
                    }
                    if (maoDireita != null) {
                        maoDireita.iniciarBote(true, true);
                    }
                } else {
                    if (maoDireita != null) {
                        maoDireita.iniciarBote(true, false);
                    }
                    if (maoEsquerda != null) {
                        maoEsquerda.iniciarBote(true, true);
                    }
                }
            } else {
                if (playerNaEsquerda && maoEsquerda != null) {
                    maoEsquerda.iniciarBote(false, false);
                } else if (!playerNaEsquerda && maoDireita != null) {
                    maoDireita.iniciarBote(false, false);
                }
            }
        } else {
            if (playerNaEsquerda && maoEsquerda != null) {
                maoEsquerda.iniciarHoverSlam();
                maoEsmagandoAtiva = maoEsquerda;
            } else if (!playerNaEsquerda && maoDireita != null) {
                maoDireita.iniciarHoverSlam();
                maoEsmagandoAtiva = maoDireita;
            }
        }
    }

    public void setPodeRugir(boolean set) {
        podeRugir = set;
    }

    public boolean isPodeRugir() {
        return podeRugir;
    }

    public void atualizarCutsceneIntro() {
        velX = 0;
        velY = 0;
        this.x = xHome;
        this.y = yHome;

        if (timerVirar > 0) {
            timerVirar -= 1.0;
        }
        if (podeRugir) {
            atualizarRugido();
        }
    }

    private void atualizarRugido() {
        if (rugindo) {
            timerRugido -= 1.0;

            if (timerRugido == 100) {
                atirarRajadaRugido(0);
            } else if (isFase2()) {
                if (timerRugido == 80) {
                    double offset = (Math.PI * 2) / 24.0 / 2.0;
                    atirarRajadaRugido(offset);
                } else if (timerRugido == 60) {
                    atirarRajadaRugido(0);
                }
            }

            if (timerRugido <= 0) {
                rugindo = false;
                double cdBase = 300 + (Math.random() * 300);
                cooldownRugido = isFase2() ? cdBase * 0.6 : cdBase;

                contadorRugidos++;
                minionsSpawnados.removeIf(Enemy::isDead);

                if (contadorRugidos >= rugidosParaSpawn && minionsSpawnados.isEmpty()) {
                    spawnarMinions();
                    contadorRugidos = 0;
                }
            }
        } else {
            if (cooldownRugido > 0) {
                cooldownRugido -= 1.0;
            } else {
                rugindo = true;
                timerRugido = 120;
                animT = 0;

                if (gameCore != null) {
                    gameCore.shakeCamera(12, 60);
                }
                if (soundManager != null) {
                    soundManager.playSFX(SoundManager.SFX.MORSA_ROAR);
                }
            }
        }
    }

    private void atirarRajadaRugido(double offsetAngle) {
        if (bulletManager != null) {
            int numBullets = 24;
            double angleStep = (Math.PI * 2) / numBullets;
            double centroBocaX = this.x + (this.width / 2.0);
            double centroBocaY = this.y + (this.height * 0.7);

            for (int i = 0; i < numBullets; i++) {
                bulletManager.shoot(
                        centroBocaX, centroBocaY,
                        Math.cos((angleStep * i) + offsetAngle),
                        Math.sin((angleStep * i) + offsetAngle),
                        BulletOwner.ENEMY);
            }
        }
    }

    public void vincularCamera(CameraManager camera) {
        this.camera = camera;
    }

    @Override
    public void draw(Graphics2D g, double delta) {
        int index = 0;
        int xx = (int) x;
        int inv = 1;

        if (dirS != Direita && timerVirar <= 0 && !rugindo) {
            timerVirar = 40;
        }

        if (timerVirar > 0 && !rugindo) {
            if (timerVirar > 30) {
                index = 6;
            } else if (timerVirar > 20) {
                index = 7;
            } else if (timerVirar > 10) {
                index = 7;
                dirS = Direita;
            } else {
                index = 6;
            }
        } else {
            if (rugindo) {
                animT += 5 * delta;
                if (animT >= 2) {
                    animT = 0;
                }
                index = rugidoSprites[(int) animT];
                if (timerRugido > 110) {
                    index = 6;
                } else if (timerRugido > 100) {
                    index = 7;
                } else if (timerRugido < 10) {
                    index = 6;
                } else if (timerRugido < 20) {
                    index = 7;
                }
            } else {
                double idleSpeed = isFase2() ? 5 : 3;
                animT += idleSpeed * delta;
                if (animT >= 12) {
                    animT = 0;
                }
                index = idle[(int) animT];
            }
        }

        if (dirS == 0) {
            xx = (int) x + (int) width;
            inv = -1;
        }

        if (Sprites[index] != null) {
            ProjectedShadow.drawForEntityAtFeet(g, x, y, width, height, 81.0 / 96.0,
                    new ProjectedShadow.Part(Sprites[index], xx, (int) y,
                            inv * (int) width, (int) height));
            g.drawImage(Sprites[index], xx, (int) y, inv * (int) width, (int) height, null);
        }

        desenharBarradevida(g);
    }

    private void desenharBarradevida(Graphics2D g) {
        int largura = (int) width;
        int altura = 14;
        int barraX = (int) x;
        int barraY = (int) y - altura - 8;

        double proporcao = Math.max(0, Math.min(1.0, this.vida / (double) this.vidaMaxima));

        g.setColor(Color.DARK_GRAY);
        g.fillRect(barraX, barraY, largura, altura);

        if (isFase2()) {
            g.setColor(new Color(255, 69, 0));
        } else {
            g.setColor(Color.RED);
        }

        g.fillRect(barraX, barraY, (int) (largura * proporcao), altura);

        g.setColor(Color.WHITE);
        g.drawRect(barraX, barraY, largura, altura);

        String titulo = isFase2() ? "MORSA GIGANTE (ENFURECIDA)" : "MORSA GIGANTE";
        g.drawString(titulo, barraX, barraY - 4);
    }

    @Override
    public Collider getHurtbox() {
        return this.bodyCollider;
    }
}

enum MaoState {
    IDLE, BOTE_WINDUP, BOTE_DASH, BOTE_RECOVERY,
    RETURNING, HOVER_CHASE, HOVER_WINDUP, HOVER_SLAM, HOVER_RECOVERY,
    FISHED, PULLED_TO_PLAYER, SLINGSHOT, STUNNED
}

class BossMao extends Enemy {

    private MorsaBoss corpoPrincipal;
    private double xHome, yHome;
    private MaoState status = MaoState.IDLE;

    private double timerEstado = 0;
    private double targetX, targetY;
    private boolean isBoteDuplo = false;
    private boolean isSegundaMao = false;

    private double slamTargetX, slamTargetY;

    // Sombra
    private boolean mostrarSombra = false;
    private double sombraX, sombraY;
    private double sombraScale = 0.0;

    // Física do Estilingue e Controle de Dano
    private double slingshotDirX = 0;
    private double slingshotDirY = 0;
    private int cooldownDano = 0;

    public BossMao(double startX, double startY, int[][] lvlData, SoundManager sound, ArenaManager am, MorsaBoss corpo) {
        super(startX, startY, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5, lvlData, sound, am);
        this.bodyCollider = new Collider(-1, 0, GameCore.tiles_size * 1.5, GameCore.tiles_size * 1.5);
        this.xHome = startX;
        this.yHome = startY;
        this.cor = Color.RED;
        this.corpoPrincipal = corpo;
        this.vida = 150;
        this.danoContato = 10;
    }

    public boolean isIdle() {
        return this.status == MaoState.IDLE;
    }

    @Override
    public boolean includedInCombatCamera() {
        return true;
    }

    public void iniciarBote(boolean duplo, boolean segundaMao) {
        if (status == MaoState.IDLE) {
            status = MaoState.BOTE_WINDUP;
            timerEstado = 0;
            isBoteDuplo = duplo;
            isSegundaMao = segundaMao;
        }
    }

    public double getTimerEstado() {
        return this.timerEstado;
    }

    public void iniciarHoverSlam() {
        iniciarHoverSlam(0);
    }

    public void iniciarHoverSlam(double tempoHerdado) {
        if (status == MaoState.IDLE || status == MaoState.RETURNING) {
            status = MaoState.HOVER_CHASE;
            timerEstado = tempoHerdado;
        }
    }

    public void cancelarAtaque() {
        status = MaoState.RETURNING;
        timerEstado = 0;
    }

    public boolean isEsmagando() {
        return status == MaoState.HOVER_CHASE || status == MaoState.HOVER_WINDUP
                || status == MaoState.HOVER_SLAM || status == MaoState.HOVER_RECOVERY
                || status == MaoState.FISHED || status == MaoState.PULLED_TO_PLAYER
                || status == MaoState.SLINGSHOT || status == MaoState.STUNNED
                || status == MaoState.RETURNING;
    }

    public boolean isPreparandoEsmagamento() {
        return status == MaoState.HOVER_CHASE || status == MaoState.HOVER_WINDUP;
    }

    private boolean colideCom(Entity e) {
        Collider meu = this.bodyCollider;
        Collider dele = e.getHurtbox();
        if (meu == null || dele == null) {
            return false;
        }

        Rectangle2D.Double r1 = new Rectangle2D.Double(this.x + meu.getOffsetX(), this.y + meu.getOffsetY(),
                meu.getWidth(), meu.getHeight());
        Rectangle2D.Double r2 = new Rectangle2D.Double(e.getX() + dele.getOffsetX(), e.getY() + dele.getOffsetY(),
                dele.getWidth(), dele.getHeight());
        return r1.intersects(r2);
    }

    @Override
    public void update(Player player, ArrayList<JumpLink> jumpLinks) {
        if (corpoPrincipal == null || corpoPrincipal.isDead()) {
            this.receberDano(99999);
            this.isDead = true;
            return;
        }
        mostrarSombra = false;

        if (cooldownDano > 0) {
            cooldownDano--;
        }

        if (status != MaoState.IDLE && status != MaoState.PULLED_TO_PLAYER
                && status != MaoState.SLINGSHOT && status != MaoState.FISHED
                && status != MaoState.RETURNING && status != MaoState.STUNNED) {

            if (!isDead && !isCaindo && cooldownDano <= 0) {
                if (this.bodyCollider != null && player.getHurtbox() != null) {
                    if (this.bodyCollider.intersects(this.x, this.y, player.getHurtbox(), player.getX(),
                            player.getY())) {
                        player.receberDano(danoContato);
                        cooldownDano = 45;
                    }
                }
            }
        }
        // BLINDAGEM DA VARA DE PESCA
        if (status != MaoState.HOVER_SLAM && status != MaoState.HOVER_RECOVERY && status != MaoState.FISHED
                && status != MaoState.PULLED_TO_PLAYER && status != MaoState.SLINGSHOT
                && status != MaoState.STUNNED) {
            this.isHooked = false;
            this.isPuxado = false;
        }

        switch (status) {
            case IDLE:
                timerEstado += 0.05;
                double swayMult = corpoPrincipal.isFase2() ? 10 : 6;
                this.y = yHome + Math.sin(timerEstado) * swayMult;
                this.x = xHome;
                break;

            case BOTE_WINDUP:
                if (timerEstado == 0) {
                    targetX = player.getX();
                    targetY = player.getY();
                }
                timerEstado += 1;

                double dxArmar = this.x - targetX;
                double dyArmar = this.y - targetY;
                double distArmar = Math.hypot(dxArmar, dyArmar);

                if (distArmar > 0) {
                    this.x += (dxArmar / distArmar) * 2.0;
                    this.y += (dyArmar / distArmar) * 2.0;
                }

                this.x += (Math.random() > 0.5 ? 2 : -2);
                this.y += (Math.random() > 0.5 ? 2 : -2);

                int limiteWindup = 35;
                if (isBoteDuplo) {
                    limiteWindup = isSegundaMao ? 70 : 35;
                }

                if (timerEstado > limiteWindup) {
                    status = MaoState.BOTE_DASH;
                    timerEstado = 0;

                    double pX = player.getX();
                    double pY = player.getY();
                    double dirX = pX - this.x;
                    double dirY = pY - this.y;
                    double distToPlayer = Math.hypot(dirX, dirY);

                    if (distToPlayer > 0) {
                        targetX = pX + (dirX / distToPlayer) * 300.0;
                        targetY = pY + (dirY / distToPlayer) * 300.0;
                    } else {
                        targetX = pX;
                        targetY = pY;
                    }
                }
                break;

            case BOTE_DASH:
                timerEstado += 1;
                double dxBote = targetX - this.x;
                double dyBote = targetY - this.y;
                double distBote = Math.hypot(dxBote, dyBote);

                double spdBote = corpoPrincipal.isFase2() ? 35.0 : 28.0;

                if (distBote > 15 && timerEstado < 40) {
                    this.x += (dxBote / distBote) * spdBote;
                    this.y += (dyBote / distBote) * spdBote;
                } else {
                    status = MaoState.BOTE_RECOVERY;
                    timerEstado = 0;
                }
                break;

            case BOTE_RECOVERY:
                timerEstado += 1;
                if (timerEstado > 15) {
                    status = MaoState.RETURNING;
                    timerEstado = 0;
                }
                break;

            case RETURNING:
                double dxHome = xHome - this.x;
                double dyHome = yHome - this.y;
                double distHome = Math.hypot(dxHome, dyHome);

                double retSpeed = corpoPrincipal.isFase2() ? 11.0 : 8.0;

                if (distHome > 5) {
                    this.x += (dxHome / distHome) * retSpeed;
                    this.y += (dyHome / distHome) * retSpeed;
                } else {
                    this.x = xHome;
                    this.y = yHome;
                    status = MaoState.IDLE;
                }
                break;

            case HOVER_CHASE:
                timerEstado += 1;
                targetX = player.getX();
                targetY = player.getY() - (GameCore.tiles_size * 3.5);

                double dxH = targetX - this.x;
                double dyH = targetY - this.y;

                double trkSpeed = corpoPrincipal.isFase2() ? 0.16 : 0.12;
                this.x += dxH * trkSpeed;
                this.y += dyH * trkSpeed;

                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = player.getY() + player.getAltura();
                sombraScale = 0.3;

                if (timerEstado > 50) {
                    status = MaoState.HOVER_WINDUP;
                    timerEstado = 0;
                    slamTargetX = player.getX();
                    slamTargetY = player.getY() - (GameCore.tiles_size * 0.5);
                }
                break;

            case HOVER_WINDUP:
                timerEstado += 1;
                this.y -= 1.5;
                this.x += (Math.random() > 0.5 ? 4 : -4);

                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = slamTargetY + this.height;
                sombraScale = 0.45;

                int limiteHoverWindup = corpoPrincipal.isFase2() ? 18 : 25;
                if (timerEstado > limiteHoverWindup) {
                    status = MaoState.HOVER_SLAM;
                    timerEstado = 0;
                }
                break;

            case HOVER_SLAM:
                double slamFallSpeed = corpoPrincipal.isFase2() ? 36.0 : 28.0;
                this.y += slamFallSpeed;

                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = slamTargetY + this.height;

                double distCaindo = slamTargetY - this.y;
                double alturaMaxima = GameCore.tiles_size * 4.0;
                double progressoQueda = 1.0 - (distCaindo / alturaMaxima);
                progressoQueda = Math.max(0.0, Math.min(1.0, progressoQueda));

                sombraScale = 0.45 + (progressoQueda * 0.55);

                if (this.y >= slamTargetY) {
                    this.y = slamTargetY;
                    status = MaoState.HOVER_RECOVERY;
                    timerEstado = 0;

                    if (corpoPrincipal != null && corpoPrincipal.getCamera() != null) {
                        corpoPrincipal.getCamera().tremer(14, 25);
                    }
                    if (soundManager != null) {
                        soundManager.playSFX(SoundManager.SFX.EXPLOSION);
                    }

                    if (corpoPrincipal != null && corpoPrincipal.getBulletManager() != null) {
                        int numBullets = 12;
                        double angleStep = (Math.PI * 2) / numBullets;
                        double centroMaoX = this.x + (this.width / 2.0);
                        double centroMaoY = this.y + (this.height / 2.0);

                        for (int i = 0; i < numBullets; i++) {
                            corpoPrincipal.getBulletManager().shoot(
                                    centroMaoX, centroMaoY,
                                    Math.cos(angleStep * i), Math.sin(angleStep * i),
                                    BulletOwner.ENEMY);
                        }
                    }
                }
                break;

            case HOVER_RECOVERY:
                timerEstado += 1;
                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = slamTargetY + this.height;
                sombraScale = 1.0;

                // O Jogador fisgou a mão
                if (this.isHooked) {
                    status = MaoState.FISHED;
                    timerEstado = 0;
                    this.velX = 0;
                    this.velY = 0;
                    break;
                }

                int limiteHoverRecovery = corpoPrincipal.isFase2() ? 50 : 90;
                if (timerEstado > limiteHoverRecovery) {
                    status = MaoState.RETURNING;
                    timerEstado = 0;
                }
                break;

            case FISHED:
                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = this.y + this.height;
                sombraScale = 1.0;

                // Jogador puxa (botão direito)
                if (this.isPuxado || Math.abs(this.velX) > 2 || Math.abs(this.velY) > 2) {
                    status = MaoState.PULLED_TO_PLAYER;
                    timerEstado = 0;
                } // Linha quebra
                else if (!this.isHooked) {
                    status = MaoState.RETURNING;
                    timerEstado = 0;
                    this.isPuxado = false;
                }
                break;

            case PULLED_TO_PLAYER:
                timerEstado += 1;
                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = this.y + this.height;
                sombraScale = 1.0;

                this.x += this.velX;
                this.y += this.velY;
                this.velX *= 0.88;
                this.velY *= 0.88;

                if (timerEstado > 12) {
                    status = MaoState.SLINGSHOT;
                    timerEstado = 0;
                    this.isHooked = false;
                    this.isPuxado = false;

                    double alvoX = corpoPrincipal.getCenterX();
                    double alvoY = corpoPrincipal.getCenterY();

                    double centroMaoX = this.x + (this.width / 2.0);
                    double centroMaoY = this.y + (this.height / 2.0);

                    double dxS = alvoX - centroMaoX;
                    double dyS = alvoY - centroMaoY;
                    double distS = Math.hypot(dxS, dyS);

                    if (distS > 0) {
                        slingshotDirX = dxS / distS;
                        slingshotDirY = dyS / distS;
                    } else {
                        slingshotDirX = 0;
                        slingshotDirY = -1;
                    }

                    if (soundManager != null) {
                        // TODO: adicionar um som para isso, se puder
                        // soundManager.playSFX(SoundManager.SFX.SWOOSH);
                    }
                }
                break;

            case SLINGSHOT:
                timerEstado += 1;
                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = this.y + this.height;
                sombraScale = 0.5;

                // Velocidade estilingue
                this.x += slingshotDirX * 45.0;
                this.y += slingshotDirY * 45.0;

                boolean impacto = false;

                // Colisão Boss
                if (timerEstado > 3 && colideCom(corpoPrincipal)) {
                    corpoPrincipal.receberDano(100);
                    impacto = true;
                }

                // Colisão Mobs
                EnemyManager em = corpoPrincipal.getEnemyManager();
                if (em != null) {
                    for (Enemy e : em.getEnemies()) {
                        if (e != this && e != corpoPrincipal && !e.isDead()) {
                            if (colideCom(e)) {
                                e.receberDano(999);
                                impacto = true;
                            }
                        }
                    }
                }

                if (impacto || timerEstado > 45) {
                    status = MaoState.STUNNED;
                    timerEstado = 0;
                    if (corpoPrincipal.getCamera() != null) {
                        corpoPrincipal.getCamera().tremer(20, 30);
                    }
                    if (soundManager != null) {
                        soundManager.playSFX(SoundManager.SFX.EXPLOSION);
                    }
                }
                break;

            case STUNNED:
                timerEstado += 1;
                mostrarSombra = true;
                sombraX = this.x + (this.width / 2.0);
                sombraY = this.y + this.height;
                sombraScale = 1.0;
                this.x += (Math.random() > 0.5 ? 2 : -2);
                if (timerEstado > 15) {
                    status = MaoState.RETURNING;
                    timerEstado = 0;
                }
                break;
        }
    }

    @Override
    public void draw(Graphics2D g, double delta) {
        if (mostrarSombra) {
            double maxShadowWidth = this.width * 1.3;
            double maxShadowHeight = this.height * 0.4;
            double currentW = maxShadowWidth * sombraScale;
            double currentH = maxShadowHeight * sombraScale;
            g.setColor(new Color(0, 0, 0, 120));
            g.fillOval((int) (sombraX - currentW / 2.0), (int) (sombraY - currentH / 2.0), (int) currentW,
                    (int) currentH);
        }

        g.setColor(this.cor);
        g.fillRect((int) x, (int) y, (int) width, (int) height);
    }

    @Override
    public Collider getHurtbox() {
        return this.bodyCollider;
    }
}
