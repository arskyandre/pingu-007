import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** Testes unitarios leves, executaveis com: java -ea RenderingPipelineTest. */
public final class RenderingPipelineTest {

    private RenderingPipelineTest() {
    }

    public static void main(String[] args) {
        testProjectedShadowGlobalBuffer();
        testAlphaBoundsAndFlip();
        testOcclusionCulling();
        testSpriteLighting();
        testRotationCache();
        System.out.println("RenderingPipelineTest: OK");
    }

    private static void testAlphaBoundsAndFlip() {
        BufferedImage sprite = new BufferedImage(6, 5, BufferedImage.TYPE_INT_ARGB);
        sprite.setRGB(0, 0, 0x40000000);
        for (int y = 1; y <= 3; y++) {
            for (int x = 2; x <= 4; x++) {
                sprite.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        Rectangle2D visible = ProjectedShadow.getVisibleAlphaBounds(sprite, 10, 20, 12, 10);
        assertRect(visible, 10, 20, 10, 8, "alpha bounds visiveis");

        Rectangle2D opaque = ProjectedShadow.getOpaqueAlphaBounds(sprite, 10, 20, 12, 10);
        assertRect(opaque, 14, 22, 6, 6, "maior retangulo opaco");

        Rectangle2D flipped = ProjectedShadow.getOpaqueAlphaBounds(sprite, 22, 20, -12, 10);
        assertRect(flipped, 12, 22, 6, 6, "bounds opacos com flip horizontal");
    }

    private static void testProjectedShadowGlobalBuffer() {
        BufferedImage sprite = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D spriteGraphics = sprite.createGraphics();
        try {
            spriteGraphics.setColor(Color.WHITE);
            spriteGraphics.fillRect(0, 0, 8, 8);
        } finally {
            spriteGraphics.dispose();
        }

        BufferedImage globalMask = new BufferedImage(160, 120, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D shadowGraphics = globalMask.createGraphics();
        ProjectedShadow.beginGlobalBuffer(shadowGraphics);
        try {
            ProjectedShadow.drawAtGroundAnchor(shadowGraphics, 84, 88, 8, 12, 0.35f,
                    new ProjectedShadow.Part(sprite, 80, 80, 8, 8));
            int firstMaximum = maximumAlpha(globalMask);
            assertTrue(firstMaximum > 0, "ProjectedShadow deve escrever no buffer global");
            assertTrue(alphaAt(globalMask, 84, 88) > 0,
                    "a sombra deve tocar o ponto de origem no chao sem folga vertical");

            ProjectedShadow.drawAtGroundAnchor(shadowGraphics, 84, 88, 8, 12, 0.35f,
                    new ProjectedShadow.Part(sprite, 80, 80, 8, 8));
            assertNear(maximumAlpha(globalMask), firstMaximum, 0,
                    "ProjectedShadow sobreposto nao pode acumular alpha");

            ProjectedShadow.drawAtGroundAnchor(shadowGraphics, 84, 88, 8, 12, 0.45f,
                    new ProjectedShadow.Part(sprite, 80, 80, 8, 8));
            assertNear(maximumAlpha(globalMask), firstMaximum, 0,
                    "opacidade global deve ser aplicada apenas ao compor a mascara final");
        } finally {
            ProjectedShadow.endGlobalBuffer();
            shadowGraphics.dispose();
        }
    }

    private static void testOcclusionCulling() {
        OcclusionCuller culler = new OcclusionCuller();
        ArrayList<Renderable> input = new ArrayList<>();
        ArrayList<Renderable> output = new ArrayList<>();

        FakeRenderable hidden = new FakeRenderable(10, new Rectangle2D.Double(0, 0, 32, 32), null);
        FakeRenderable front = new FakeRenderable(20,
                new Rectangle2D.Double(0, 0, 32, 32), new Rectangle2D.Double(0, 0, 32, 32));
        input.add(hidden);
        input.add(front);
        culler.filter(input, output);
        assertTrue(output.size() == 1 && output.get(0) == front,
                "objeto exatamente sobreposto deveria ser descartado");

        input.clear();
        FakeRenderable partial = new FakeRenderable(10, new Rectangle2D.Double(0, 0, 40, 32), null);
        input.add(partial);
        input.add(front);
        culler.filter(input, output);
        assertTrue(output.size() == 2, "objeto parcialmente escondido nao pode ser descartado");

        input.clear();
        FakeRenderable transparentFront = new FakeRenderable(20,
                new Rectangle2D.Double(0, 0, 32, 32), null);
        input.add(hidden);
        input.add(transparentFront);
        culler.filter(input, output);
        assertTrue(output.size() == 2, "objeto transparente nao pode ocluir outro");

        input.clear();
        FakeRenderable farAway = new FakeRenderable(30,
                new Rectangle2D.Double(1024, 1024, 32, 32),
                new Rectangle2D.Double(1024, 1024, 32, 32));
        input.add(hidden);
        input.add(farAway);
        culler.filter(input, output);
        assertTrue(output.size() == 2, "hash espacial nao pode comparar celulas distantes");

        input.clear();
        FakeRenderable cellBorderTarget = new FakeRenderable(10,
                new Rectangle2D.Double(120, 12, 20, 20), null);
        FakeRenderable cellBorderFront = new FakeRenderable(20,
                new Rectangle2D.Double(100, 0, 64, 64),
                new Rectangle2D.Double(100, 0, 64, 64));
        input.add(cellBorderTarget);
        input.add(cellBorderFront);
        culler.filter(input, output);
        assertTrue(output.size() == 1 && output.get(0) == cellBorderFront,
                "culling deve funcionar quando bounds cruzam celulas do hash");
    }

    private static void testSpriteLighting() {
        double noonAngle = Math.PI / 2.0;
        double midnightAngle = -Math.PI / 2.0;
        assertNear(SpriteLighting.calculateBrightness(noonAngle), 1.0, 0.0001, "luz ao meio-dia");
        assertNear(SpriteLighting.calculateBrightness(midnightAngle), SpriteLighting.AMBIENT_LIGHT,
                0.0001, "luz ambiente minima");
        float front = SpriteLighting.calculateBrightness(Math.PI / 2.0, 1.0);
        float lateral = SpriteLighting.calculateBrightness(0.0, 1.0);
        float back = SpriteLighting.calculateBrightness(-Math.PI / 2.0, 1.0);
        assertTrue(front >= lateral && lateral > back,
                "luz lateral deve permanecer clara e apenas as costas devem escurecer muito");

        BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        source.setRGB(0, 0, 0x80C08040);
        source.setRGB(1, 0, 0x00000000);
        int originalPixel = source.getRGB(0, 0);
        BufferedImage destination = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = destination.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            SpriteLighting.configure(midnightAngle, 0.0, true);
            SpriteLighting.drawImage(g, source, 0, 0, source.getWidth(), source.getHeight());
        } finally {
            g.dispose();
        }
        assertNear(alphaAt(destination, 0, 0), 128, 0, "tonalizacao deve preservar alpha");
        assertNear(alphaAt(destination, 1, 0), 0, 0, "tonalizacao deve preservar transparencia");
        assertTrue(source.getRGB(0, 0) == originalPixel, "filtro nao pode alterar o frame original");

        BufferedImage unlit = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D unlitGraphics = unlit.createGraphics();
        try {
            unlitGraphics.setComposite(AlphaComposite.Src);
            SpriteLighting.configure(midnightAngle, 0.0, false);
            SpriteLighting.drawImage(unlitGraphics, source, 0, 0, source.getWidth(), source.getHeight());
        } finally {
            unlitGraphics.dispose();
        }
        assertTrue(unlit.getRGB(0, 0) == originalPixel,
                "sprites devem manter a cor quando a iluminacao estiver desligada");

        BufferedImage flipped = new BufferedImage(8, 2, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D flippedGraphics = flipped.createGraphics();
        try {
            flippedGraphics.setComposite(AlphaComposite.Src);
            SpriteLighting.configure(midnightAngle, 0.0, true);
            SpriteLighting.drawImage(flippedGraphics, source, 8, 0, -8, 2);
        } finally {
            flippedGraphics.dispose();
        }
        assertNear(alphaAt(flipped, 0, 0), 0, 0,
                "tonalizacao deve respeitar transparencia ao espelhar");
        assertNear(alphaAt(flipped, 7, 0), 128, 0,
                "tonalizacao deve respeitar escala e espelhamento");

        BufferedImage indexedSource = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_INDEXED);
        indexedSource.setRGB(0, 0, 0xFFC08040);
        indexedSource.setRGB(1, 0, 0xFF204080);
        BufferedImage indexedDestination = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D indexedGraphics = indexedDestination.createGraphics();
        try {
            indexedGraphics.setComposite(AlphaComposite.Src);
            SpriteLighting.configure(midnightAngle, 0.0, true);
            SpriteLighting.drawImage(indexedGraphics, indexedSource, 0, 0, 2, 1);
        } finally {
            indexedGraphics.dispose();
        }
        assertNear(alphaAt(indexedDestination, 0, 0), 255, 0,
                "tonalizacao deve aceitar imagens com paleta indexada");
        assertTrue((indexedDestination.getRGB(0, 0) & 0x00FFFFFF)
                        != (indexedSource.getRGB(0, 0) & 0x00FFFFFF),
                "imagem indexada deve receber tonalizacao apos conversao para ARGB");
    }

    private static void testRotationCache() {
        BufferedImage source = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB_PRE);
        assertTrue(HelpMethods.rotateImageByDegrees(source, 0.0) == source,
                "rotacao zero deve reutilizar o frame original");

        BufferedImage first = HelpMethods.rotateImageByDegrees(source, 0.4);
        BufferedImage repeated = HelpMethods.rotateImageByDegrees(source, 0.4);
        assertTrue(first == repeated,
                "rotacoes quantizadas devem reutilizar a mesma BufferedImage");
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }

    private static int maximumAlpha(BufferedImage image) {
        int maximum = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                maximum = Math.max(maximum, alphaAt(image, x, y));
            }
        }
        return maximum;
    }

    private static void assertRect(Rectangle2D actual, double x, double y, double width, double height,
            String message) {
        assertTrue(actual != null, message + ": bounds ausentes");
        assertNear(actual.getX(), x, 0.0001, message + " x");
        assertNear(actual.getY(), y, 0.0001, message + " y");
        assertNear(actual.getWidth(), width, 0.0001, message + " width");
        assertNear(actual.getHeight(), height, 0.0001, message + " height");
    }

    private static void assertNear(double actual, double expected, double tolerance, String message) {
        if (Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(message + ": esperado=" + expected + ", atual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FakeRenderable implements Renderable {
        private final double depth;
        private final Rectangle2D visibleBounds;
        private final Rectangle2D opaqueBounds;

        private FakeRenderable(double depth, Rectangle2D visibleBounds, Rectangle2D opaqueBounds) {
            this.depth = depth;
            this.visibleBounds = visibleBounds;
            this.opaqueBounds = opaqueBounds;
        }

        @Override
        public double getProfundidade() {
            return depth;
        }

        @Override
        public void draw(Graphics2D g2, double delta) {
        }

        @Override
        public Rectangle2D getOcclusionBounds() {
            return visibleBounds;
        }

        @Override
        public Rectangle2D getOpaqueOcclusionBounds() {
            return opaqueBounds;
        }
    }
}
