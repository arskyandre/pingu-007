import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class CutsceneManager {

    public enum Phase {
        NONE, OPENING, CLOSING
    }

    public enum CutsceneType {
        NONE, BOSS_INTRO, WALL_REVEAL
    }

    private CutsceneType type = CutsceneType.NONE;

    // --- boss intro state ---
    private Phase phase = Phase.NONE;
    private int timer = 0;
    private static final int DURATION = 240;
    private static final int BOSS_BGM_DELAY = 120;
    private static final int TEXT_AREA_HEIGHT = 80;
    private static final int TRANSICAO_BORDA = 20;
    private String nomeBoss = "";
    private boolean bossBgmTocada = false;
    private Player bossIntroPlayer;

    // --- wall reveal state ---
    private static final int WALL_REVEAL_DURATION = 150;
    private int wallTimer = 0;
    private Rectangle2D.Double wallFadeRect;
    private Player wallRevealPlayer;
    private static final double WALL_SHAKE_AMPLITUDE_MAX = 2.5;
    private static final double WALL_SHAKE_SPEED = 1.2;

    private GameCore gameCore;
    private SoundManager soundManager;

    public CutsceneManager(GameCore GC, SoundManager sound) {
        gameCore = GC;
        soundManager = sound;
    }

    public static int getDuracaoTotal() {
        return DURATION;
    }

    // ---------------- Boss intro ----------------

    /**
     * Inicia a cutscene de entrada do boss. Agora o CutsceneManager cuida
     * de tudo: foco de câmera, bloqueio de input do player, borda cinematica
     * e troca de BGM — nada disso deve ser feito fora daqui.
     */
    public void iniciarBossIntro(String nomeBoss, CameraManager camera, Player player, double focoX, double focoY) {
        this.type = CutsceneType.BOSS_INTRO;
        this.nomeBoss = nomeBoss;
        this.phase = Phase.OPENING;
        this.timer = 0;
        this.bossBgmTocada = false;
        this.bossIntroPlayer = player;
        soundManager.playRandomSnowStep();
        if (camera != null) {
            camera.focarEm(focoX, focoY, DURATION);
        }
        if (player != null) {
            player.setBlockInputs(true);
        }

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

        camera.focarEmRect(wallRect, WALL_REVEAL_DURATION, gameCore.getWidth(), gameCore.getHeight());
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
        timer++;
        wallTimer++;
        if (type == CutsceneType.BOSS_INTRO) {
            if (!bossBgmTocada && timer >= BOSS_BGM_DELAY) {
                soundManager.playBGM(SoundManager.BGM.BOSS_INTRO, SoundManager.BGM.BOSS_LOOP);
                bossBgmTocada = true;
            }

            if (phase == Phase.OPENING && timer >= DURATION - TRANSICAO_BORDA) {
                phase = Phase.CLOSING;
                gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
            }

            if (phase == Phase.CLOSING && timer >= DURATION) {
                phase = Phase.NONE;
                timer = 0;
                type = CutsceneType.NONE;

                if (bossIntroPlayer != null) {
                    bossIntroPlayer.setBlockInputs(false);
                    bossIntroPlayer = null;
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

    public void draw(Graphics2D g2, int telaLargura, int telaAltura) {
        if (type != CutsceneType.BOSS_INTRO) {
            return;
        }
        AffineTransform transformOriginal = g2.getTransform();
        g2.setTransform(new AffineTransform());

        if (nomeBoss != null && !nomeBoss.isEmpty()) {
            desenharNomeBoss(g2, telaLargura, telaAltura);
        }

        g2.setTransform(transformOriginal);
    }

    private void desenharNomeBoss(Graphics2D g2, int telaLargura, int telaAltura) {
        Object antialiasAntigo = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font fonteNome = new Font("Serif", Font.BOLD, 26);
        g2.setFont(fonteNome);
        FontMetrics fm = g2.getFontMetrics();
        int textoLargura = fm.stringWidth(nomeBoss);
        int textoX = (telaLargura - textoLargura) / 2;
        int textoY = (TEXT_AREA_HEIGHT / 2) + (fm.getAscent() / 2) - 4;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(nomeBoss, textoX + 2, textoY + 2);
        g2.setColor(new Color(230, 230, 230));
        g2.drawString(nomeBoss, textoX, textoY);

        if (antialiasAntigo != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antialiasAntigo);
        }
    }
}
