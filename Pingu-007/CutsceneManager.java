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
    private static final int DURATION = 100;
    private static final int TEXT_AREA_HEIGHT = 80;
    private static final int TRANSICAO_BORDA = 20;
    private String nomeBoss = "";

    // --- wall reveal state ---
    private static final int WALL_REVEAL_DURATION = 90; // 1.5s @ 60fps
    private int wallTimer = 0;
    private Rectangle2D.Double wallFadeRect;
    private Player wallRevealPlayer;

    private GameCore gameCore;

    public CutsceneManager(GameCore GC) {
        gameCore = GC;
    }

    public static int getDuracaoTotal() {
        return DURATION;
    }

    // ---------------- Boss intro ----------------

    public void iniciar(String nomeBoss) {
        this.type = CutsceneType.BOSS_INTRO;
        this.nomeBoss = nomeBoss;
        this.phase = Phase.OPENING;
        this.timer = 0;
        gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
    }

    /** inicia cutscene */
    public void iniciarWallReveal(double wallCenterX, double wallCenterY, Rectangle2D.Double wallRect,
            CameraManager camera, Player player) {
        this.type = CutsceneType.WALL_REVEAL;
        this.wallTimer = 0;
        this.wallFadeRect = wallRect;
        this.wallRevealPlayer = player;

        camera.focarEm(wallCenterX, wallCenterY, WALL_REVEAL_DURATION);
        player.setBlockInputs(true);
    }

    public boolean isWallRevealAtiva() {
        return type == CutsceneType.WALL_REVEAL;
    }

    public Rectangle2D.Double getWallFadeRect() {
        return wallFadeRect;
    }

    public float getWallFadeAlpha() {
        if (type != CutsceneType.WALL_REVEAL) {
            return 1f;
        }
        return Math.min(1f, wallTimer / (float) WALL_REVEAL_DURATION);
    }

    // ---------------- Shared update/draw ----------------

    public void update() {
        if (type == CutsceneType.BOSS_INTRO) {
            timer++;
            if (phase == Phase.OPENING && timer >= DURATION - TRANSICAO_BORDA) {
                phase = Phase.CLOSING;
                gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
            }
            if (phase == Phase.CLOSING && timer >= DURATION) {
                phase = Phase.NONE;
                timer = 0;
                nomeBoss = "";
                type = CutsceneType.NONE;
            }
        } else if (type == CutsceneType.WALL_REVEAL) {
            wallTimer++;
            if (wallTimer >= WALL_REVEAL_DURATION) {
                wallFadeRect = null;
                wallRevealPlayer = null;
                wallTimer = 0;
                type = CutsceneType.NONE;
            }
        }
    }

    public boolean isAtiva() {
        return type != CutsceneType.NONE;
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