import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.awt.image.BufferedImage;

public class CutsceneManager {

    public enum CutsceneType {
        NONE, BOSS_INTRO, WALL_REVEAL
    }

    private enum BlackBarState {
        IN,
        OUT,
        IDLE
    }

    private CutsceneType type = CutsceneType.NONE;

    // --- boss intro state ---
    private enum BossIntroState {
        DIALOGUE, CAMERA_PAN, FINISHED
    }

    private BossIntroState bossIntroState = BossIntroState.DIALOGUE;
    private int timer = 0;
    private static final int DURATION = 120;
    private static final int BOSS_BGM_DELAY = 100;
    private static final int TRANSICAO_BORDA = 20;
    private Player bossIntroPlayer;
    // desenhar barra preta e nome boss
    private double blackBarProgress = 0.0;
    private double blackBarDuration = 0.5;
    private BlackBarState blackBarState = BlackBarState.IDLE;
    private String nomeBoss = "Morsa Gigante, o terror do Ártico";
    private String[] falasBossIntro;

    // --- wall reveal state ---
    private static final int WALL_REVEAL_DURATION = 150;
    private int wallTimer = 0;
    private Rectangle2D.Double wallFadeRect;
    private Player wallRevealPlayer;
    private static final double WALL_SHAKE_AMPLITUDE_MAX = 2.5;
    private static final double WALL_SHAKE_SPEED = 1.2;

    private GameCore gameCore;
    private SoundManager soundManager;
    private EnemyManager enemyManager;
    private DialogueManager dialogueManager;
    private CameraManager cameraManager;
    private BufferedImage morsaFalaSprite;

    public CutsceneManager(GameCore GC, SoundManager sound, DialogueManager dialogo, CameraManager camera) {
        gameCore = GC;
        soundManager = sound;
        dialogueManager = dialogo;
        cameraManager = camera;
        falasBossIntro = new String[] {
                "... : Então foi você quem atravessou toda a minha base...",
                "... : Você ousou desafiar o meu domínio e eliminar meus homens.",
                "... : Sua missão termina aqui, Pingu. Prepare-se para dar adeus a esse mundo!"
        };
        morsaFalaSprite = LoadSave.GetSpriteAtlas("images/portrait/morsa_portrait.png");
    }

    public static int getDuracaoTotal() {
        return DURATION;
    }

    // ---------------- Boss intro ----------------

    public void iniciarBossIntro(CameraManager camera, Player player, double focoX, double focoY, EnemyManager EM) {
        GameCore.setGameState(GameState.CUTSCENE);
        player.setTemporarySpriteOverride(14, 2);
        this.type = CutsceneType.BOSS_INTRO;
        bossIntroState = BossIntroState.DIALOGUE;
        enemyManager = EM;
        dialogueManager.iniciarDialogo(falasBossIntro,
                new BufferedImage[] { morsaFalaSprite });
        dialogueManager.setAoTerminarDialogo(() -> {
            bossIntroState = BossIntroState.CAMERA_PAN;
            enemyManager.getMorsaBoss().setPodeRugir(true);
            timer = 0;
            if (cameraManager != null) {
                cameraManager.focarEm(enemyManager.getMorsaBoss().getCenterX(),
                        enemyManager.getMorsaBoss().getCenterY(), DURATION, true);
            }

            blackBarState = BlackBarState.IN;
            blackBarProgress = 0;

            gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
        });
        blackBarProgress = 0.0;
        this.timer = 0;
        this.bossIntroPlayer = player;
        soundManager.playRandomSnowStep();

        gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
    }

    // ---------------- Wall reveal (fade always, camera focus só na primeira arena)
    // ----------------

    public void iniciarWallFade(Rectangle2D.Double wallRect) {
        this.wallTimer = 0;
        if (type == CutsceneType.BOSS_INTRO) {
            return; // não interrompe uma cutscene de boss já em andamento
        }
        if (type == CutsceneType.WALL_REVEAL && wallRevealPlayer != null) {
            return;
        }
        this.type = CutsceneType.WALL_REVEAL;
        this.wallFadeRect = wallRect;
        this.wallRevealPlayer = null;
    }

    public void iniciarWallRevealComCamera(Rectangle2D.Double wallRect, CameraManager camera, Player player) {
        this.wallTimer = 0;
        if (type == CutsceneType.BOSS_INTRO) {
            return; // não interrompe uma cutscene de boss já em andamento
        }
        this.type = CutsceneType.WALL_REVEAL;
        this.wallFadeRect = wallRect;
        this.wallRevealPlayer = player;

        camera.focarEmRect(wallRect, WALL_REVEAL_DURATION, gameCore.getWidth(), gameCore.getHeight(), false);
        player.setBlockInputs(true);
    }

    public boolean isWallRevealAtiva() {
        return isWallFadeAtiva();
    }

    public Rectangle2D.Double getWallFadeRect() {
        return wallFadeRect;
    }

    public boolean isBossIntroAtiva() {
        return type == CutsceneType.BOSS_INTRO;
    }

    public float getWallFadeAlpha() {
        if (type != CutsceneType.WALL_REVEAL) {
            return 1f;
        }
        return Math.min(1f, (float) wallTimer / (float) WALL_REVEAL_DURATION);
    }

    // ---------------- Shared update/draw ----------------

    public void update() {
        if (type == CutsceneType.BOSS_INTRO) {
            if (bossIntroState == BossIntroState.CAMERA_PAN) {
                GameCore.setGameState(GameState.CUTSCENE);

                if (timer == BOSS_BGM_DELAY) {

                    soundManager.playBGM(SoundManager.BGM.BOSS_INTRO, SoundManager.BGM.BOSS_LOOP);
                }

                if (timer >= DURATION) {
                    cameraManager.desfocarCamera();
                }

                if (timer >= DURATION) {

                    bossIntroState = BossIntroState.FINISHED;
                    type = CutsceneType.NONE;

                    bossIntroPlayer.setBlockInputs(false);
                }

            }

        } else if (type == CutsceneType.WALL_REVEAL) {

            if (wallTimer == WALL_REVEAL_DURATION - TRANSICAO_BORDA) {
                if (gameCore.getArenaManager() != null && !gameCore.getArenaManager().existeCombateAtivo()) {
                    gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                }
            }

            if (wallTimer >= WALL_REVEAL_DURATION) {
                wallFadeRect = null;
                wallRevealPlayer = null;
                wallTimer = 0;
                type = CutsceneType.NONE;
            }
        }
        timer++;

        wallTimer++;
    }

    public boolean isAtiva() {
        return type == CutsceneType.BOSS_INTRO || (type == CutsceneType.WALL_REVEAL && wallRevealPlayer != null);
    }

    public boolean isWallFadeAtiva() {
        return type == CutsceneType.WALL_REVEAL;
    }

    private double getWallShakeAmplitude() {
        double progress = wallTimer / (double) WALL_REVEAL_DURATION;
        return WALL_SHAKE_AMPLITUDE_MAX * Math.pow(1.0 - progress, 2.0);
    }

    public double getWallShakeX() {
        if (type != CutsceneType.WALL_REVEAL) {
            return 0;
        }

        return Math.sin(wallTimer * WALL_SHAKE_SPEED) * getWallShakeAmplitude();
    }

    public double getWallShakeY() {
        if (type != CutsceneType.WALL_REVEAL) {
            return 0;
        }

        return Math.cos(wallTimer * WALL_SHAKE_SPEED * 1.3) * getWallShakeAmplitude();
    }

    public void draw(Graphics2D g2, int telaLargura, int telaAltura, double delta) {
        if (type != CutsceneType.BOSS_INTRO && bossIntroState != BossIntroState.CAMERA_PAN) {
            return;
        }
        AffineTransform transformOriginal = g2.getTransform();
        g2.setTransform(new AffineTransform());

        if (nomeBoss != null && !nomeBoss.isEmpty()) {
            desenharBarraPreta(g2, telaLargura, telaAltura, delta);
        }

        g2.setTransform(transformOriginal);
    }

    private void desenharBarraPreta(Graphics2D g2, int telaLargura, int telaAltura, double delta) {
        double progressSpeed = 1.0 / blackBarDuration;
        if (blackBarState == BlackBarState.IN) {
            blackBarProgress += progressSpeed * delta;

            if (blackBarProgress >= 1.0) {
                blackBarProgress = 1.0;
                blackBarState = BlackBarState.IDLE;
            }
        } else if (blackBarState == BlackBarState.OUT) {
            blackBarProgress -= progressSpeed * delta;

            if (blackBarProgress <= 0.0) {
                blackBarProgress = 0.0;
                blackBarState = BlackBarState.IDLE;
            }
        }
        double eased;

        if (blackBarState == BlackBarState.OUT) {
            eased = blackBarProgress * blackBarProgress;
        } else {
            eased = 1.0 - (1.0 - blackBarProgress) * (1.0 - blackBarProgress);
        }
        int alturaFinal = telaAltura / 12;
        int alturaAtual = (int) (alturaFinal * eased);
        int y = (telaAltura - alturaAtual) * 3 / 4;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, y, telaLargura, alturaAtual);

        // Nome do Boss

        Object antialiasAntigo = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font fonteNome = GameCore.pixelFont.deriveFont(Font.PLAIN, telaAltura * 32 / 672);
        g2.setFont(fonteNome);
        Rectangle2D textBounds = g2.getFontMetrics().getStringBounds(nomeBoss, g2);
        int textoLargura = (int) textBounds.getWidth();
        int textoX = (telaLargura - textoLargura) / 2;
        int alturaBarra = telaAltura / 10;
        int barraY = (telaAltura - alturaBarra) * 3 / 4;
        FontMetrics fm = g2.getFontMetrics();

        int textoY = barraY + (alturaBarra - (int) textBounds.getHeight()) / 2 + fm.getAscent();

        int alpha = (int) (255 * eased);

        g2.setColor(new Color(0, 0, 0, alpha * 160 / 255));
        g2.drawString(nomeBoss, textoX + 2, textoY + 2);

        g2.setColor(new Color(230, 230, 230, alpha));
        g2.drawString(nomeBoss, textoX, textoY);
        g2.setColor(Color.RED);

        if (antialiasAntigo != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antialiasAntigo);
        }
    }

}
