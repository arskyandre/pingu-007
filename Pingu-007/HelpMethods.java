import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

public class HelpMethods {
    private static final int ROTATION_BUCKETS = 64;
    private static final double ROTATION_STEP = Math.PI * 2.0 / ROTATION_BUCKETS;
    private static final Map<BufferedImage, BufferedImage[]> ROTATION_CACHE = new WeakHashMap<>();

    public static BufferedImage rotateImageByDegrees(BufferedImage img, double rads) {
        if (img == null) {
            return null;
        }

        double normalized = rads % (Math.PI * 2.0);
        if (normalized < 0.0) {
            normalized += Math.PI * 2.0;
        }
        int bucket = Math.floorMod((int) Math.round(normalized / ROTATION_STEP), ROTATION_BUCKETS);
        if (bucket == 0) {
            return img;
        }

        BufferedImage[] rotations;
        synchronized (ROTATION_CACHE) {
            rotations = ROTATION_CACHE.computeIfAbsent(img, ignored -> new BufferedImage[ROTATION_BUCKETS]);
            BufferedImage cached = rotations[bucket];
            if (cached != null) {
                return cached;
            }
        }

        double quantizedRadians = bucket * ROTATION_STEP;
        BufferedImage rotated = createRotatedImage(img, quantizedRadians);
        synchronized (ROTATION_CACHE) {
            BufferedImage existing = rotations[bucket];
            if (existing != null) {
                return existing;
            }
            rotations[bucket] = rotated;
        }
        return rotated;
    }

    private static BufferedImage createRotatedImage(BufferedImage img, double rads) {
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = Math.max(1, (int) Math.ceil(w * cos + h * sin));
        int newHeight = Math.max(1, (int) Math.ceil(h * cos + w * sin));

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2.0, (newHeight - h) / 2.0);

        double x = w / 2.0;
        double y = h / 2.0;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return rotated;
    }
}
