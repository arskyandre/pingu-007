import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ProjectedShadow {

    private static final double PLAYER_HEIGHT = 48.0;
    private static final double PLAYER_FEET_HEIGHT = 45.0;
    private static final double PLAYER_SHADOW_LENGTH = 42.0;
    private static final double MAX_LENGTH_MULTIPLIER = 1.60;
    private static final double CULL_PADDING = 48.0;
    public static final double SHADOW_LENGTH_PER_REFERENCE_HEIGHT =
            PLAYER_SHADOW_LENGTH / PLAYER_FEET_HEIGHT;
    public static final float DEFAULT_SHADOW_OPACITY = 0.42f;
    private static final int SUN_ANGLE_BUCKETS = 360;
    private static final double SUN_ANGLE_STEP = Math.PI * 2.0 / SUN_ANGLE_BUCKETS;
    private static final int MAX_CACHED_SHADOWS = 48;
    private static final BufferedImage SOLID_PIXEL = createSolidPixel();
    private static final Map<BufferedImage, AlphaMetrics> ALPHA_METRICS = new WeakHashMap<>();
    private static final Map<ShadowCacheKey, CachedShadow> SHADOW_CACHE =
            new LinkedHashMap<>(MAX_CACHED_SHADOWS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ShadowCacheKey, CachedShadow> eldest) {
                    return size() > MAX_CACHED_SHADOWS;
                }
            };
    private static long generatedShadowCount;
    private static long shadowCacheHitCount;

    private ProjectedShadow() {
    }

    private static BufferedImage createSolidPixel() {
        BufferedImage pixel = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        pixel.setRGB(0, 0, 0xFF000000);
        return pixel;
    }

    public static final class Part {
        private final BufferedImage image;
        private final int x, y, width, height;

        public Part(BufferedImage image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static final class VisualAnchor {
        private final double x;
        private final double y;
        private final double visibleHeight;
        private final boolean hasVisiblePixels;

        private VisualAnchor(double x, double y, double visibleHeight, boolean hasVisiblePixels) {
            this.x = x;
            this.y = y;
            this.visibleHeight = visibleHeight;
            this.hasVisiblePixels = hasVisiblePixels;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getVisibleHeight() {
            return visibleHeight;
        }

        public boolean hasVisiblePixels() {
            return hasVisiblePixels;
        }
    }

    private static final class AlphaMetrics {
        private final int firstVisibleRow;
        private final int lastVisibleRow;
        private final int firstRowMinX;
        private final int firstRowMaxX;
        private final int lastRowMinX;
        private final int lastRowMaxX;

        private AlphaMetrics(int firstVisibleRow, int lastVisibleRow,
                int firstRowMinX, int firstRowMaxX, int lastRowMinX, int lastRowMaxX) {
            this.firstVisibleRow = firstVisibleRow;
            this.lastVisibleRow = lastVisibleRow;
            this.firstRowMinX = firstRowMinX;
            this.firstRowMaxX = firstRowMaxX;
            this.lastRowMinX = lastRowMinX;
            this.lastRowMaxX = lastRowMaxX;
        }

        private boolean hasVisiblePixels() {
            return firstVisibleRow >= 0;
        }
    }

    private static final class ShadowCacheKey {
        private final int angleBucket;
        private final long referenceHeightBits;
        private final long shadowLengthBits;
        private final int opacityBits;
        private final BufferedImage[] images;
        private final int[] geometry;
        private final byte[] flipFlags;
        private final int hash;

        private ShadowCacheKey(int angleBucket, double referenceHeight, double shadowLength,
                float opacity, int anchorX, int anchorY, Part... parts) {
            this.angleBucket = angleBucket;
            this.referenceHeightBits = Double.doubleToLongBits(referenceHeight);
            this.shadowLengthBits = Double.doubleToLongBits(shadowLength);
            this.opacityBits = Float.floatToIntBits(opacity);

            int validPartCount = 0;
            for (Part part : parts) {
                if (part != null && part.image != null && part.width != 0 && part.height != 0) {
                    validPartCount++;
                }
            }
            this.images = new BufferedImage[validPartCount];
            this.geometry = new int[validPartCount * 4];
            this.flipFlags = new byte[validPartCount];

            int imageIndex = 0;
            int geometryIndex = 0;
            int calculatedHash = angleBucket;
            calculatedHash = 31 * calculatedHash + Long.hashCode(referenceHeightBits);
            calculatedHash = 31 * calculatedHash + Long.hashCode(shadowLengthBits);
            calculatedHash = 31 * calculatedHash + opacityBits;
            for (Part part : parts) {
                if (part == null || part.image == null || part.width == 0 || part.height == 0) {
                    continue;
                }
                images[imageIndex++] = part.image;
                flipFlags[imageIndex - 1] = (byte) ((part.width < 0 ? 1 : 0)
                        | (part.height < 0 ? 2 : 0));
                geometry[geometryIndex++] = part.x - anchorX;
                geometry[geometryIndex++] = part.y - anchorY;
                geometry[geometryIndex++] = part.width;
                geometry[geometryIndex++] = part.height;
                calculatedHash = 31 * calculatedHash + System.identityHashCode(part.image);
                calculatedHash = 31 * calculatedHash + part.x - anchorX;
                calculatedHash = 31 * calculatedHash + part.y - anchorY;
                calculatedHash = 31 * calculatedHash + part.width;
                calculatedHash = 31 * calculatedHash + part.height;
            }
            for (byte flipFlag : flipFlags) {
                calculatedHash = 31 * calculatedHash + flipFlag;
            }
            this.hash = calculatedHash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ShadowCacheKey other)
                    || angleBucket != other.angleBucket
                    || referenceHeightBits != other.referenceHeightBits
                    || shadowLengthBits != other.shadowLengthBits
                    || opacityBits != other.opacityBits
                    || images.length != other.images.length
                    || geometry.length != other.geometry.length
                    || flipFlags.length != other.flipFlags.length) {
                return false;
            }
            for (int i = 0; i < images.length; i++) {
                if (images[i] != other.images[i]) {
                    return false;
                }
            }
            for (int i = 0; i < geometry.length; i++) {
                if (geometry[i] != other.geometry[i]) {
                    return false;
                }
            }
            for (int i = 0; i < flipFlags.length; i++) {
                if (flipFlags[i] != other.flipFlags[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class CachedShadow {
        private final BufferedImage image;
        private final int drawOffsetX, drawOffsetY, drawWidth, drawHeight;

        private CachedShadow(BufferedImage image, int drawOffsetX, int drawOffsetY,
                int drawWidth, int drawHeight) {
            this.image = image;
            this.drawOffsetX = drawOffsetX;
            this.drawOffsetY = drawOffsetY;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
        }
    }

    public static Part solidPart(int x, int y, int width, int height) {
        return new Part(SOLID_PIXEL, x, y, width, height);
    }

    public static double shadowLengthForReferenceHeight(double referenceHeight) {
        return Math.max(0.0, referenceHeight) * SHADOW_LENGTH_PER_REFERENCE_HEIGHT;
    }

    public static double cullingToleranceForReferenceHeight(double referenceHeight) {
        double minimumTolerance = GameCore.tiles_size * 2.0;
        double projectedTolerance = Math.max(0.0, referenceHeight) * MAX_LENGTH_MULTIPLIER + CULL_PADDING;
        return Math.max(minimumTolerance, projectedTolerance);
    }

    public static VisualAnchor getVisualGroundAnchor(BufferedImage image,
            int dx, int dy, int drawWidth, int drawHeight) {
        if (image == null || drawWidth == 0 || drawHeight == 0) {
            return new VisualAnchor(dx, dy, 1.0, false);
        }

        AlphaMetrics metrics = getAlphaMetrics(image);
        double renderedWidth = Math.abs((double) drawWidth);
        double renderedHeight = Math.abs((double) drawHeight);
        double left = Math.min(dx, dx + (double) drawWidth);
        double top = Math.min(dy, dy + (double) drawHeight);

        if (!metrics.hasVisiblePixels()) {
            return new VisualAnchor(left + renderedWidth / 2.0, top + renderedHeight,
                    1.0, false);
        }

        boolean flipH = drawWidth < 0;
        boolean flipV = drawHeight < 0;
        int sourceRow = flipV ? metrics.firstVisibleRow : metrics.lastVisibleRow;
        int rowMinX = flipV ? metrics.firstRowMinX : metrics.lastRowMinX;
        int rowMaxX = flipV ? metrics.firstRowMaxX : metrics.lastRowMaxX;
        double sourceCenterX = (rowMinX + rowMaxX + 1) / 2.0;
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double anchorSourceX = flipH ? sourceWidth - sourceCenterX : sourceCenterX;
        double anchorSourceY = flipV ? sourceHeight - sourceRow : sourceRow + 1.0;
        double visibleHeight = Math.max(1.0,
                (metrics.lastVisibleRow - metrics.firstVisibleRow + 1) * renderedHeight / sourceHeight);

        return new VisualAnchor(
                left + anchorSourceX * renderedWidth / sourceWidth,
                top + anchorSourceY * renderedHeight / sourceHeight,
                visibleHeight,
                true);
    }

    private static AlphaMetrics getAlphaMetrics(BufferedImage image) {
        synchronized (ALPHA_METRICS) {
            AlphaMetrics cached = ALPHA_METRICS.get(image);
            if (cached != null) {
                return cached;
            }

            int firstRow = -1;
            int lastRow = -1;
            int firstMinX = Integer.MAX_VALUE;
            int firstMaxX = Integer.MIN_VALUE;
            int lastMinX = Integer.MAX_VALUE;
            int lastMaxX = Integer.MIN_VALUE;
            for (int sourceY = 0; sourceY < image.getHeight(); sourceY++) {
                int rowMinX = Integer.MAX_VALUE;
                int rowMaxX = Integer.MIN_VALUE;
                for (int sourceX = 0; sourceX < image.getWidth(); sourceX++) {
                    int alpha = (image.getRGB(sourceX, sourceY) >>> 24) & 0xFF;
                    if (alpha != 0) {
                        rowMinX = Math.min(rowMinX, sourceX);
                        rowMaxX = Math.max(rowMaxX, sourceX);
                    }
                }
                if (rowMinX == Integer.MAX_VALUE) {
                    continue;
                }
                if (firstRow == -1) {
                    firstRow = sourceY;
                    firstMinX = rowMinX;
                    firstMaxX = rowMaxX;
                }
                lastRow = sourceY;
                lastMinX = rowMinX;
                lastMaxX = rowMaxX;
            }

            AlphaMetrics metrics = new AlphaMetrics(firstRow, lastRow,
                    firstMinX, firstMaxX, lastMinX, lastMaxX);
            ALPHA_METRICS.put(image, metrics);
            return metrics;
        }
    }

    private static int getSunAngleBucket() {
        int bucket = (int) Math.round(GameCore.getSunAngle() / SUN_ANGLE_STEP);
        return Math.floorMod(bucket, SUN_ANGLE_BUCKETS);
    }

    private static CachedShadow getCachedShadow(ShadowCacheKey key) {
        synchronized (SHADOW_CACHE) {
            return SHADOW_CACHE.get(key);
        }
    }

    private static void cacheShadow(ShadowCacheKey key, CachedShadow shadow) {
        synchronized (SHADOW_CACHE) {
            SHADOW_CACHE.put(key, shadow);
        }
    }

    public static void resetCacheStatistics() {
        synchronized (SHADOW_CACHE) {
            SHADOW_CACHE.clear();
            generatedShadowCount = 0;
            shadowCacheHitCount = 0;
        }
    }

    public static long getGeneratedShadowCount() {
        synchronized (SHADOW_CACHE) {
            return generatedShadowCount;
        }
    }

    public static long getShadowCacheHitCount() {
        synchronized (SHADOW_CACHE) {
            return shadowCacheHitCount;
        }
    }

    private static void drawCachedShadow(Graphics2D g2, CachedShadow shadow,
            int anchorX, int anchorY, float opacity) {
        Composite oldComposite = g2.getComposite();
        RenderingHints oldHints = (RenderingHints) g2.getRenderingHints().clone();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(shadow.image,
                    anchorX + shadow.drawOffsetX,
                    anchorY + shadow.drawOffsetY,
                    shadow.drawWidth, shadow.drawHeight, null);
        } finally {
            g2.setRenderingHints(oldHints);
            g2.setComposite(oldComposite);
        }
    }

    public static void drawForEntity(Graphics2D g2, double x, double y, double width, double height,
            Part... parts) {
        drawForEntity(g2, x, y, width, height,
                PLAYER_SHADOW_LENGTH * (height / PLAYER_HEIGHT), DEFAULT_SHADOW_OPACITY, parts);
    }

    public static void drawForEntity(Graphics2D g2, double x, double y, double width, double height,
            double shadowLength, float shadowOpacity, Part... parts) {
        if (!Renderer.isRenderShadows() || parts == null || parts.length == 0) {
            return;
        }

        double scale = height / PLAYER_HEIGHT;
        double feetX = x + width / 2.0;
        double feetY = y + PLAYER_FEET_HEIGHT * scale;
        double referenceHeight = PLAYER_FEET_HEIGHT * scale;
        double safeLength = Math.max(0.0, shadowLength);
        float safeOpacity = Math.max(0.0f, Math.min(1.0f, shadowOpacity));
        draw(g2, feetX, feetY, referenceHeight, safeLength, safeOpacity, parts);
    }

    public static void drawForEntityAtFeet(Graphics2D g2, double x, double y,
            double width, double height, double feetRatioY, Part... parts) {
        if (!Renderer.isRenderShadows() || parts == null || parts.length == 0) {
            return;
        }

        double safeFeetRatioY = Math.max(0.01, feetRatioY);
        double referenceHeight = Math.max(1.0, height * safeFeetRatioY);
        double feetX = x + width / 2.0;
        double feetY = y + referenceHeight;
        double shadowLength = shadowLengthForReferenceHeight(referenceHeight);
        draw(g2, feetX, feetY, referenceHeight, shadowLength, DEFAULT_SHADOW_OPACITY, parts);
    }

    public static void drawAtGroundAnchor(Graphics2D g2, double groundAnchorX, double groundAnchorY,
            double referenceHeight, double shadowLength, float opacity, Part... parts) {
        if (!Renderer.isRenderShadows() || parts == null || parts.length == 0) {
            return;
        }

        double safeReferenceHeight = Math.max(1.0, referenceHeight);
        double safeLength = Math.max(0.0, shadowLength);
        float safeOpacity = Math.max(0.0f, Math.min(1.0f, opacity));
        draw(g2, groundAnchorX, groundAnchorY, safeReferenceHeight,
                safeLength, safeOpacity, parts);
    }

    private static void draw(Graphics2D g2, double worldFeetX, double worldFeetY,
            double referenceHeight, double shadowLength, float opacity, Part... parts) {
        int anchorX = (int) Math.floor(worldFeetX);
        int anchorY = (int) Math.floor(worldFeetY);
        int angleBucket = getSunAngleBucket();
        ShadowCacheKey cacheKey = new ShadowCacheKey(angleBucket, referenceHeight, shadowLength,
                opacity, anchorX, anchorY, parts);
        CachedShadow cachedShadow = getCachedShadow(cacheKey);
        if (cachedShadow != null) {
            synchronized (SHADOW_CACHE) {
                shadowCacheHitCount++;
            }
            drawCachedShadow(g2, cachedShadow, anchorX, anchorY, opacity);
            return;
        }

        int worldLeft = Integer.MAX_VALUE;
        int worldTop = Integer.MAX_VALUE;
        int worldRight = Integer.MIN_VALUE;
        int worldBottom = Integer.MIN_VALUE;

        for (Part part : parts) {
            if (part == null || part.image == null || part.width == 0 || part.height == 0) {
                continue;
            }
            worldLeft = Math.min(worldLeft, Math.min(part.x, part.x + part.width));
            worldTop = Math.min(worldTop, Math.min(part.y, part.y + part.height));
            worldRight = Math.max(worldRight, Math.max(part.x, part.x + part.width));
            worldBottom = Math.max(worldBottom, Math.max(part.y, part.y + part.height));
        }
        if (worldLeft == Integer.MAX_VALUE) {
            return;
        }

        synchronized (SHADOW_CACHE) {
            generatedShadowCount++;
        }

        worldLeft = Math.min(worldLeft, (int) Math.floor(worldFeetX));
        worldRight = Math.max(worldRight, (int) Math.ceil(worldFeetX));
        worldTop = Math.min(worldTop, (int) Math.floor(worldFeetY - referenceHeight));
        worldBottom = Math.max(worldBottom, (int) Math.ceil(worldFeetY));

        BufferedImage combined = new BufferedImage(
                Math.max(1, worldRight - worldLeft),
                Math.max(1, worldBottom - worldTop),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = combined.createGraphics();
        try {
            cg.setComposite(AlphaComposite.SrcOver);
            cg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (Part part : parts) {
                if (part != null && part.image != null && part.width != 0 && part.height != 0) {
                    cg.drawImage(part.image, part.x - worldLeft, part.y - worldTop,
                            part.width, part.height, null);
                }
            }
        } finally {
            cg.dispose();
        }

        double sourceFeetX = worldFeetX - worldLeft;
        double sourceFeetY = worldFeetY - worldTop;
        int maskWidth = combined.getWidth();
        int maskHeight = combined.getHeight();
        int[] maskPixels = combined.getRGB(0, 0, maskWidth, maskHeight, null, 0, maskWidth);
        for (int py = 0; py < combined.getHeight(); py++) {
            double heightAboveFeet = Math.max(0.0, sourceFeetY - py);
            double normalizedHeight = heightAboveFeet / referenceHeight;
            float proximity = (float) Math.max(0.0, 1.0 - normalizedHeight);
            float rowStrength = 0.12f + 0.88f * (float) Math.pow(proximity, 0.75);
            for (int px = 0; px < combined.getWidth(); px++) {
                int pixelIndex = py * maskWidth + px;
                int originalAlpha = (maskPixels[pixelIndex] >>> 24) & 0xFF;
                int finalAlpha = Math.round(originalAlpha * rowStrength);
                maskPixels[pixelIndex] = finalAlpha << 24;
            }
        }
        BufferedImage shadowMask = new BufferedImage(maskWidth, maskHeight, BufferedImage.TYPE_INT_ARGB);
        shadowMask.setRGB(0, 0, maskWidth, maskHeight, maskPixels, 0, maskWidth);

        double shadowAngle = angleBucket * SUN_ANGLE_STEP + Math.PI;
        double shadowDirX = Math.cos(shadowAngle);
        double shadowDirY = Math.sin(shadowAngle);
        double southFactor = Math.max(0.0, Math.min(1.0, (shadowDirY + 1.0) / 2.0));
        double lengthMultiplier = 0.55 + (MAX_LENGTH_MULTIPLIER - 0.55) * southFactor;
        double widthMultiplier = 0.80 + (1.20 - 0.80) * southFactor;
        double effectiveLength = shadowLength * lengthMultiplier;
        double maximumHeight = Math.max(referenceHeight, sourceFeetY);
        double maximumProjectedDistance = effectiveLength * Math.pow(maximumHeight / referenceHeight, 0.90);
        int blurPadding = 12;
        int safetyPadding = 12;

        int bufferWidth = (int) Math.ceil(shadowMask.getWidth() * widthMultiplier
                + Math.abs(shadowDirX) * maximumProjectedDistance + blurPadding * 2 + safetyPadding * 2);
        int bufferHeight = (int) Math.ceil(shadowMask.getHeight()
                + Math.abs(shadowDirY) * maximumProjectedDistance + blurPadding * 2 + safetyPadding * 2);
        BufferedImage shadowLayer = new BufferedImage(Math.max(1, bufferWidth), Math.max(1, bufferHeight),
                BufferedImage.TYPE_INT_ARGB);

        double localFeetX = blurPadding + safetyPadding
                + Math.max(0.0, -shadowDirX * maximumProjectedDistance) + sourceFeetX * widthMultiplier;
        double localFeetY = blurPadding + safetyPadding
                + Math.max(0.0, -shadowDirY * maximumProjectedDistance) + 6.0;

        Graphics2D sg = shadowLayer.createGraphics();
        try {
            sg.setComposite(AlphaComposite.Src);
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            for (int sourceY = 0; sourceY < shadowMask.getHeight(); sourceY++) {
                double heightAboveFeet = Math.max(0.0, sourceFeetY - sourceY);
                double distanceFromFeet = heightAboveFeet / referenceHeight;
                double projectedDistance = effectiveLength * Math.pow(distanceFromFeet, 0.90);
                double rowCenterY = localFeetY + shadowDirY * projectedDistance;
                double rowPerspective = Math.max(0.45, 0.72 + 0.28 * (1.0 - distanceFromFeet));
                double horizontalScale = widthMultiplier * rowPerspective;
                int destinationHeight = Math.max(2,
                        (int) Math.ceil(Math.abs(shadowDirY) * effectiveLength / referenceHeight) + 1);
                double projectedFeetX = localFeetX + shadowDirX * projectedDistance;
                int dx1 = (int) Math.round(projectedFeetX - sourceFeetX * horizontalScale);
                int dx2 = (int) Math.round(projectedFeetX
                        + (shadowMask.getWidth() - sourceFeetX) * horizontalScale);
                if (dx2 <= dx1) {
                    dx2 = dx1 + 1;
                }
                int dy1 = (int) Math.round(rowCenterY - destinationHeight / 2.0);
                sg.drawImage(shadowMask, dx1, dy1, dx2, dy1 + destinationHeight,
                        0, sourceY, shadowMask.getWidth(), sourceY + 1, null);
            }
        } finally {
            sg.dispose();
        }

        int reducedWidth = Math.max(1, (shadowLayer.getWidth() + 1) / 2);
        int reducedHeight = Math.max(1, (shadowLayer.getHeight() + 1) / 2);
        BufferedImage reducedShadow = new BufferedImage(
                reducedWidth, reducedHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D reducedGraphics = reducedShadow.createGraphics();
        try {
            reducedGraphics.setComposite(AlphaComposite.Src);
            reducedGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            reducedGraphics.drawImage(shadowLayer, 0, 0, reducedWidth, reducedHeight, null);
        } finally {
            reducedGraphics.dispose();
        }
        BufferedImage blurredShadow = Renderer.gaussianBlur(reducedShadow, 2, 1.0);
        int drawX = (int) Math.round(worldFeetX - localFeetX);
        int drawY = (int) Math.round(worldFeetY - localFeetY);
        CachedShadow renderedShadow = new CachedShadow(blurredShadow,
                drawX - anchorX, drawY - anchorY,
                shadowLayer.getWidth(), shadowLayer.getHeight());
        if (cacheKey != null) {
            cacheShadow(cacheKey, renderedShadow);
        }
        drawCachedShadow(g2, renderedShadow, anchorX, anchorY, opacity);
    }
}
