import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class ScreenTransition {

    private static final int UPDATES_PER_SECOND = 60;
    private static final double DEFAULT_TOTAL_DURATION_SECONDS = 0.6;
    private static final String BORDER_IMG = "images/hud/transition_border.png";

    private enum Phase {
        IDLE,
        COVERING,
        REVEALING
    }

    private final BufferedImage borderImg;
    private Phase phase = Phase.IDLE;
    private int tick;
    private int phaseTicks;
    private Runnable actionOnCover;

    public ScreenTransition() {
        BufferedImage img = null;
        try {
            img = LoadSave.GetSpriteAtlas(BORDER_IMG);
        } catch (RuntimeException e) {
            System.err.println("Borda da transição não encontrada; usando borda rígida: " + e.getMessage());
        }
        borderImg = img;
    }

    public void start(Runnable action) {
        start(action, DEFAULT_TOTAL_DURATION_SECONDS);
    }

    public void start(Runnable action, double totalDurationSeconds) {
        if (isAtivo()) {
            return;
        }

        validarDuracao(totalDurationSeconds);

        actionOnCover = action;
        phaseTicks = calcularTicksPorFase(totalDurationSeconds);
        tick = 0;
        phase = Phase.COVERING;
    }

    public void update() {
        if (!isAtivo()) {
            return;
        }

        tick++;
        if (tick < phaseTicks) {
            return;
        }

        if (phase == Phase.COVERING) {
            Runnable action = actionOnCover;
            actionOnCover = null;
            if (action != null) {
                action.run();
            }
            phase = Phase.REVEALING;
            tick = 0;
        } else {
            phase = Phase.IDLE;
            tick = 0;
        }
    }

    public void draw(Graphics2D g2, int width, int height) {
        if (!isAtivo() || width <= 0 || height <= 0) {
            return;
        }

        double progress = suavizarEntradaESaida(tick / (double) phaseTicks);
        int edgeW = obterLarguraDaBordaRedimensionada(height);
        int edgeX = (int) Math.round(-edgeW + (width + edgeW) * progress);

        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (phase == Phase.COVERING) {
            desenharCobrindo(g2, width, height, edgeX, edgeW);
        } else {
            desenharRevelando(g2, width, height, edgeX, edgeW);
        }

        if (oldInterp != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        }
    }

    private void desenharCobrindo(Graphics2D g2, int width, int height, int edgeX, int edgeWidth) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, Math.max(0, Math.min(width, edgeX + 1)), height);

        if (borderImg != null) {
            g2.drawImage(borderImg, edgeX, 0, edgeWidth, height, null);
        } else {
            desenharBordaInicialRígida(g2, edgeX, edgeWidth, height);
        }
    }

    private void desenharRevelando(Graphics2D g2, int width, int height, int edgeX, int edgeWidth) {
        int blackStart = edgeX + edgeWidth - 1;
        g2.setColor(Color.BLACK);
        g2.fillRect(Math.max(0, blackStart), 0, Math.max(0, width - blackStart), height);

        if (borderImg != null) {
            g2.drawImage(borderImg,
                    edgeX + edgeWidth, height, edgeX, 0,
                    0, 0, borderImg.getWidth(), borderImg.getHeight(), null);
        } else {
            desenharBordaFinalRígida(g2, edgeX, edgeWidth, height);
        }
    }

    private void desenharBordaInicialRígida(Graphics2D g2, int edgeX, int edgeWidth, int height) {
        int slant = Math.max(1, Math.min(edgeWidth, height / 2));
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(edgeX, 0);
        edge.lineTo(edgeX + edgeWidth, 0);
        edge.lineTo(edgeX + edgeWidth - slant, height);
        edge.lineTo(edgeX, height);
        edge.closePath();
        g2.fill(edge);
    }

    private void desenharBordaFinalRígida(Graphics2D g2, int edgeX, int edgeWidth, int height) {
        int slant = Math.max(1, Math.min(edgeWidth, height / 2));
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(edgeX + slant, 0);
        edge.lineTo(edgeX + edgeWidth, 0);
        edge.lineTo(edgeX + edgeWidth, height);
        edge.lineTo(edgeX, height);
        edge.closePath();
        g2.fill(edge);
    }

    private int obterLarguraDaBordaRedimensionada(int height) {
        if (borderImg == null || borderImg.getHeight() <= 0) {
            return Math.max(1, (int) Math.round(height * (2.0 / 3.0)));
        }
        return Math.max(1, (int) Math.ceil(
                borderImg.getWidth() * (height / (double) borderImg.getHeight())));
    }

    private double suavizarEntradaESaida(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped < 0.5
                ? 4.0 * clamped * clamped * clamped
                : 1.0 - Math.pow(-2.0 * clamped + 2.0, 3.0) / 2.0;
    }

    private void validarDuracao(double totalDurationSeconds) {
        if (!Double.isFinite(totalDurationSeconds) || totalDurationSeconds <= 0.0) {
            throw new IllegalArgumentException(
                    "A duracao total da transicao deve ser finita e maior que zero.");
        }

        double ticksPorFase = totalDurationSeconds * UPDATES_PER_SECOND / 2.0;
        if (ticksPorFase > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "A duracao total da transicao e grande demais.");
        }
    }

    private int calcularTicksPorFase(double totalDurationSeconds) {
        return Math.max(1, (int) Math.round(
                totalDurationSeconds * UPDATES_PER_SECOND / 2.0));
    }

    public boolean isAtivo() {
        return phase != Phase.IDLE;
    }

    public boolean deveBloquearAtualizacaoDaCena() {
        return phase == Phase.COVERING || (phase == Phase.REVEALING && tick == 0);
    }
}
