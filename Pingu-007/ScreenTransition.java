import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class ScreenTransition {

    private static final int UPDATES_PER_SECOND = 60;
    // Total time for cover + reveal. Change this value to tune transition speed.
    private static final double TOTAL_DURATION_SECONDS = 0.6;
    private static final int PHASE_DURATION_TICKS = Math.max(1,
            (int) Math.round(TOTAL_DURATION_SECONDS * UPDATES_PER_SECOND / 2.0));
    private static final String BORDER_IMAGE = "images/hud/transition_border.png";

    private enum Phase {
        IDLE,
        COVERING,
        REVEALING
    }

    private final BufferedImage borderImage;
    private Phase phase = Phase.IDLE;
    private int phaseTick;
    private Runnable coveredAction;

    public ScreenTransition() {
        BufferedImage loadedImage = null;
        try {
            loadedImage = LoadSave.GetSpriteAtlas(BORDER_IMAGE);
        } catch (RuntimeException e) {
            System.err.println("Transition border not found; using hard edge: " + e.getMessage());
        }
        borderImage = loadedImage;
    }

    public void start(Runnable action) {
        if (isActive()) {
            return;
        }

        coveredAction = action;
        phaseTick = 0;
        phase = Phase.COVERING;
    }

    public void update() {
        if (!isActive()) {
            return;
        }

        phaseTick++;
        if (phaseTick < PHASE_DURATION_TICKS) {
            return;
        }

        if (phase == Phase.COVERING) {
            Runnable action = coveredAction;
            coveredAction = null;
            if (action != null) {
                action.run();
            }
            phase = Phase.REVEALING;
            phaseTick = 0;
        } else {
            phase = Phase.IDLE;
            phaseTick = 0;
        }
    }

    public void draw(Graphics2D g2, int width, int height) {
        if (!isActive() || width <= 0 || height <= 0) {
            return;
        }

        double progress = easeInOut(phaseTick / (double) PHASE_DURATION_TICKS);
        int edgeWidth = getScaledEdgeWidth(height);
        int edgeX = (int) Math.round(-edgeWidth + (width + edgeWidth) * progress);

        Object oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (phase == Phase.COVERING) {
            drawCovering(g2, width, height, edgeX, edgeWidth);
        } else {
            drawRevealing(g2, width, height, edgeX, edgeWidth);
        }

        if (oldInterpolation != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
    }

    private void drawCovering(Graphics2D g2, int width, int height, int edgeX, int edgeWidth) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, Math.max(0, Math.min(width, edgeX + 1)), height);

        if (borderImage != null) {
            g2.drawImage(borderImage, edgeX, 0, edgeWidth, height, null);
        } else {
            drawHardLeadingEdge(g2, edgeX, edgeWidth, height);
        }
    }

    private void drawRevealing(Graphics2D g2, int width, int height, int edgeX, int edgeWidth) {
        int blackStart = edgeX + edgeWidth - 1;
        g2.setColor(Color.BLACK);
        g2.fillRect(Math.max(0, blackStart), 0, Math.max(0, width - blackStart), height);

        if (borderImage != null) {
            g2.drawImage(borderImage,
                    edgeX + edgeWidth, height, edgeX, 0,
                    0, 0, borderImage.getWidth(), borderImage.getHeight(), null);
        } else {
            drawHardTrailingEdge(g2, edgeX, edgeWidth, height);
        }
    }

    private void drawHardLeadingEdge(Graphics2D g2, int edgeX, int edgeWidth, int height) {
        int slant = Math.max(1, Math.min(edgeWidth, height / 2));
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(edgeX, 0);
        edge.lineTo(edgeX + edgeWidth, 0);
        edge.lineTo(edgeX + edgeWidth - slant, height);
        edge.lineTo(edgeX, height);
        edge.closePath();
        g2.fill(edge);
    }

    private void drawHardTrailingEdge(Graphics2D g2, int edgeX, int edgeWidth, int height) {
        int slant = Math.max(1, Math.min(edgeWidth, height / 2));
        Path2D.Double edge = new Path2D.Double();
        edge.moveTo(edgeX + slant, 0);
        edge.lineTo(edgeX + edgeWidth, 0);
        edge.lineTo(edgeX + edgeWidth, height);
        edge.lineTo(edgeX, height);
        edge.closePath();
        g2.fill(edge);
    }

    private int getScaledEdgeWidth(int height) {
        if (borderImage == null || borderImage.getHeight() <= 0) {
            return Math.max(1, (int) Math.round(height * (2.0 / 3.0)));
        }
        return Math.max(1, (int) Math.ceil(
                borderImage.getWidth() * (height / (double) borderImage.getHeight())));
    }

    private double easeInOut(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped < 0.5
                ? 4.0 * clamped * clamped * clamped
                : 1.0 - Math.pow(-2.0 * clamped + 2.0, 3.0) / 2.0;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public boolean shouldBlockSceneUpdate() {
        return phase == Phase.COVERING || (phase == Phase.REVEALING && phaseTick == 0);
    }
}
