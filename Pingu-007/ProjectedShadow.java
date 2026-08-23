import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ProjectedShadow {

    private static final double PLAYER_HEIGHT = 48.0;
    private static final double PLAYER_FEET_HEIGHT = 45.0;
    private static final double PLAYER_SHADOW_LENGTH = 42.0;
    private static final float SHADOW_OPACITY = 0.42f;
    private static final BufferedImage SOLID_PIXEL = createSolidPixel();

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

    public static Part solidPart(int x, int y, int width, int height) {
        return new Part(SOLID_PIXEL, x, y, width, height);
    }

    public static void drawForEntity(Graphics2D g2, double x, double y, double width, double height,
            Part... parts) {
        drawForEntity(g2, x, y, width, height,
                PLAYER_SHADOW_LENGTH * (height / PLAYER_HEIGHT), SHADOW_OPACITY, parts);
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
        double shadowLength = PLAYER_SHADOW_LENGTH * (referenceHeight / PLAYER_FEET_HEIGHT);
        draw(g2, feetX, feetY, referenceHeight, shadowLength, SHADOW_OPACITY, parts);
    }

    private static void draw(Graphics2D g2, double worldFeetX, double worldFeetY,
            double referenceHeight, double shadowLength, float opacity, Part... parts) {
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
        BufferedImage shadowMask = new BufferedImage(combined.getWidth(), combined.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < combined.getHeight(); py++) {
            double heightAboveFeet = Math.max(0.0, sourceFeetY - py);
            double normalizedHeight = heightAboveFeet / referenceHeight;
            float proximity = (float) Math.max(0.0, 1.0 - normalizedHeight);
            float rowStrength = 0.12f + 0.88f * (float) Math.pow(proximity, 0.75);
            for (int px = 0; px < combined.getWidth(); px++) {
                int originalAlpha = (combined.getRGB(px, py) >>> 24) & 0xFF;
                int finalAlpha = Math.round(originalAlpha * rowStrength);
                shadowMask.setRGB(px, py, finalAlpha << 24);
            }
        }

        double shadowAngle = GameCore.getSunAngle() + Math.PI;
        double shadowDirX = Math.cos(shadowAngle);
        double shadowDirY = Math.sin(shadowAngle);
        double southFactor = Math.max(0.0, Math.min(1.0, (shadowDirY + 1.0) / 2.0));
        double lengthMultiplier = 0.55 + (1.60 - 0.55) * southFactor;
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

        BufferedImage blurredShadow = Renderer.gaussianBlur(shadowLayer, 4, 2.0);
        int drawX = (int) Math.round(worldFeetX - localFeetX);
        int drawY = (int) Math.round(worldFeetY - localFeetY);
        Composite oldComposite = g2.getComposite();
        try {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2.drawImage(blurredShadow, drawX, drawY, null);
        } finally {
            g2.setComposite(oldComposite);
        }
    }
}
