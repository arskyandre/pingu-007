
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RescaleOp;
import java.util.Map;
import java.util.WeakHashMap;

final class SpriteLighting {

    public static final float AMBIENT_LIGHT = 0.76f;

    private static final int LIGHT_LEVELS = 32;
    private static final RescaleOp[] ALPHA_FILTER_CACHE = new RescaleOp[LIGHT_LEVELS];
    private static final RescaleOp[] OPAQUE_FILTER_CACHE = new RescaleOp[LIGHT_LEVELS];
    private static final Map<BufferedImage, LitFrame> LIT_FRAME_CACHE = new WeakHashMap<>();
    private static boolean enabled;
    private static int activeLightLevel = LIGHT_LEVELS - 1;

    private SpriteLighting() {
    }

    static float calculateBrightness(double sunAngle) {
        return calculateBrightness(sunAngle, Math.max(0.0, Math.sin(sunAngle)));
    }

    static float calculateBrightness(double sunAngle, double sunElevation) {
        double shadowDirectionX = Math.cos(sunAngle + Math.PI);
        double shadowDirectionY = Math.sin(sunAngle + Math.PI);
        double lightDirectionX = -shadowDirectionX;
        double lightDirectionY = -shadowDirectionY;

        double frontDot = lightDirectionX * 0.0 + lightDirectionY;
        double behindAmount = smoothStep((-frontDot - 0.15) / 0.75);
        double elevation = smoothStep(sunElevation);
        double lowSunPenalty = 0.08 * (1.0 - elevation) * (1.0 - behindAmount);
        double brightness = 1.0 - (1.0 - AMBIENT_LIGHT) * behindAmount - lowSunPenalty;
        return (float) Math.max(AMBIENT_LIGHT, Math.min(1.0, brightness));
    }

    static void configure(double sunAngle, double sunElevation, boolean lightingEnabled) {
        enabled = lightingEnabled;
        float brightness = calculateBrightness(sunAngle, sunElevation);
        int level = Math.round(brightness * (LIGHT_LEVELS - 1));
        activeLightLevel = Math.max(0, Math.min(LIGHT_LEVELS - 1, level));
    }

    static void drawImage(Graphics2D g2, BufferedImage image,
            int x, int y, int width, int height) {
        if (image == null || width == 0 || height == 0) {
            return;
        }
        if (!enabled || activeLightLevel == LIGHT_LEVELS - 1) {
            g2.drawImage(image, x, y, width, height, null);
            return;
        }

        LitFrame cached = LIT_FRAME_CACHE.get(image);
        if (cached == null || cached.lightLevel != activeLightLevel) {
            BufferedImage indexedArgb = cached != null ? cached.indexedArgb : null;
            BufferedImage filterSource = image;
            if (image.getColorModel() instanceof IndexColorModel) {
                if (indexedArgb == null) {
                    indexedArgb = convertIndexedToArgb(image);
                }
                filterSource = indexedArgb;
            }

            RescaleOp filter = getFilter(activeLightLevel, filterSource.getColorModel().hasAlpha());
            cached = new LitFrame(activeLightLevel, indexedArgb, filter.filter(filterSource, null));
            LIT_FRAME_CACHE.put(image, cached);
        }
        g2.drawImage(cached.image, x, y, width, height, null);
    }

    private static BufferedImage convertIndexedToArgb(BufferedImage indexedImage) {
        BufferedImage argbImage = new BufferedImage(
                indexedImage.getWidth(), indexedImage.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = argbImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(indexedImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return argbImage;
    }

    private static RescaleOp getFilter(int level, boolean hasAlpha) {
        RescaleOp[] cache = hasAlpha ? ALPHA_FILTER_CACHE : OPAQUE_FILTER_CACHE;
        RescaleOp filter = cache[level];
        if (filter == null) {
            float quantizedBrightness = level / (float) (LIGHT_LEVELS - 1);
            filter = hasAlpha
                    ? new RescaleOp(
                            new float[]{quantizedBrightness, quantizedBrightness, quantizedBrightness, 1.0f},
                            new float[]{0.0f, 0.0f, 0.0f, 0.0f}, null)
                    : new RescaleOp(
                            new float[]{quantizedBrightness, quantizedBrightness, quantizedBrightness},
                            new float[]{0.0f, 0.0f, 0.0f}, null);
            cache[level] = filter;
        }
        return filter;
    }

    private static double smoothStep(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static final class LitFrame {

        private final int lightLevel;
        private final BufferedImage indexedArgb;
        private final BufferedImage image;

        private LitFrame(int lightLevel, BufferedImage indexedArgb, BufferedImage image) {
            this.lightLevel = lightLevel;
            this.indexedArgb = indexedArgb;
            this.image = image;
        }
    }
}
