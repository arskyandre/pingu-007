import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class LandingMarker {

    private static final BufferedImage SPRITE =
            LoadSave.GetSpriteAtlas("images/enemy/landing_marker.png");
    private static final double MINIMUM_SCALE = 0.35;

    private LandingMarker() {
    }

    public static void draw(Graphics2D g2, double centerX, double centerY,
            double fullWidth, double fullHeight, double progress) {
        if (g2 == null || SPRITE == null || fullWidth <= 0 || fullHeight <= 0) {
            return;
        }

        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
        double scale = MINIMUM_SCALE + (1.0 - MINIMUM_SCALE) * clampedProgress;
        int drawWidth = Math.max(1, (int) Math.round(fullWidth * scale));
        int drawHeight = Math.max(1, (int) Math.round(fullHeight * scale));
        int drawX = (int) Math.round(centerX - drawWidth / 2.0);
        int drawY = (int) Math.round(centerY - drawHeight / 2.0);

        Graphics2D markerGraphics = (Graphics2D) g2.create();
        try {
            markerGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            markerGraphics.drawImage(SPRITE, drawX, drawY, drawWidth, drawHeight, null);
        } finally {
            markerGraphics.dispose();
        }
    }
}
