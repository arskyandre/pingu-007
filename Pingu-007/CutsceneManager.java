import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

public class CutsceneManager {

    public enum Phase {
        NONE, OPENING, CLOSING
    }

    private Phase phase = Phase.NONE;
    private int timer = 0;
    private static final int DURATION = 100;
    private static final int TEXT_AREA_HEIGHT = 80;
    private static final int TRANSICAO_BORDA = 20;

    private GameCore gameCore;
    private String nomeBoss = "";

    public CutsceneManager(GameCore GC) {
        gameCore = GC;
    }

    public static int getDuracaoTotal() {
        return DURATION;
    }

    public void iniciar(String nomeBoss) {
        this.nomeBoss = nomeBoss;
        this.phase = Phase.OPENING;
        this.timer = 0;
        gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
    }

    public void update() {
        if (phase == Phase.NONE) {
            return;
        }
        timer++;
        if (phase == Phase.OPENING && timer >= DURATION - TRANSICAO_BORDA) {
            phase = Phase.CLOSING;
            gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
        }
        if (phase == Phase.CLOSING && timer >= DURATION) {
            phase = Phase.NONE;
            timer = 0;
            nomeBoss = "";
        }
    }

    public boolean isAtiva() {
        return phase != Phase.NONE;
    }

    public void draw(Graphics2D g2, int telaLargura, int telaAltura) {
        if (phase == Phase.NONE) {
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