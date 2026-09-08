
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

public final class ShadowMaxComposite implements Composite {

    public static final ShadowMaxComposite INSTANCE = new ShadowMaxComposite();

    private ShadowMaxComposite() {
    }

    @Override
    public CompositeContext createContext(ColorModel sourceColorModel,
            ColorModel destinationColorModel, RenderingHints hints) {
        return new MaxAlphaContext(sourceColorModel, destinationColorModel);
    }

    private static final class MaxAlphaContext implements CompositeContext {

        private final ColorModel sourceColorModel;
        private final ColorModel destinationColorModel;

        private MaxAlphaContext(ColorModel sourceColorModel, ColorModel destinationColorModel) {
            this.sourceColorModel = sourceColorModel;
            this.destinationColorModel = destinationColorModel;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void compose(Raster source, Raster destinationIn, WritableRaster destinationOut) {
            int width = Math.min(source.getWidth(),
                    Math.min(destinationIn.getWidth(), destinationOut.getWidth()));
            int height = Math.min(source.getHeight(),
                    Math.min(destinationIn.getHeight(), destinationOut.getHeight()));
            if (width <= 0 || height <= 0) {
                return;
            }

            if (composePackedInts(source, destinationIn, destinationOut, width, height)) {
                return;
            }
            composeGeneric(source, destinationIn, destinationOut, width, height);
        }

        private static boolean composePackedInts(Raster source, Raster destinationIn,
                WritableRaster destinationOut, int width, int height) {
            if (!(source.getDataBuffer() instanceof DataBufferInt sourceBuffer)
                    || !(destinationIn.getDataBuffer() instanceof DataBufferInt destinationBuffer)
                    || !(destinationOut.getDataBuffer() instanceof DataBufferInt outputBuffer)
                    || !(source.getSampleModel() instanceof SinglePixelPackedSampleModel sourceModel)
                    || !(destinationIn.getSampleModel() instanceof SinglePixelPackedSampleModel destinationModel)
                    || !(destinationOut.getSampleModel() instanceof SinglePixelPackedSampleModel outputModel)
                    || sourceBuffer.getNumBanks() != 1
                    || destinationBuffer.getNumBanks() != 1
                    || outputBuffer.getNumBanks() != 1) {
                return false;
            }

            int[] sourcePixels = sourceBuffer.getData();
            int[] destinationPixels = destinationBuffer.getData();
            int[] outputPixels = outputBuffer.getData();
            int sourceX = source.getMinX();
            int sourceY = source.getMinY();
            int destinationX = destinationIn.getMinX();
            int destinationY = destinationIn.getMinY();
            int outputX = destinationOut.getMinX();
            int outputY = destinationOut.getMinY();

            for (int row = 0; row < height; row++) {
                int sourceOffset = sourceBuffer.getOffset() + sourceModel.getOffset(
                        sourceX - source.getSampleModelTranslateX(),
                        sourceY + row - source.getSampleModelTranslateY());
                int destinationOffset = destinationBuffer.getOffset() + destinationModel.getOffset(
                        destinationX - destinationIn.getSampleModelTranslateX(),
                        destinationY + row - destinationIn.getSampleModelTranslateY());
                int outputOffset = outputBuffer.getOffset() + outputModel.getOffset(
                        outputX - destinationOut.getSampleModelTranslateX(),
                        outputY + row - destinationOut.getSampleModelTranslateY());

                for (int column = 0; column < width; column++) {
                    int sourcePixel = sourcePixels[sourceOffset + column];
                    int destinationPixel = destinationPixels[destinationOffset + column];
                    outputPixels[outputOffset + column] = (sourcePixel >>> 24) > (destinationPixel >>> 24)
                            ? sourcePixel : destinationPixel;
                }
            }
            return true;
        }

        private void composeGeneric(Raster source, Raster destinationIn,
                WritableRaster destinationOut, int width, int height) {
            Object sourcePixel = null;
            Object destinationPixel = null;
            int sourceX = source.getMinX();
            int sourceY = source.getMinY();
            int destinationX = destinationIn.getMinX();
            int destinationY = destinationIn.getMinY();
            int outputX = destinationOut.getMinX();
            int outputY = destinationOut.getMinY();

            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    sourcePixel = source.getDataElements(
                            sourceX + column, sourceY + row, sourcePixel);
                    destinationPixel = destinationIn.getDataElements(
                            destinationX + column, destinationY + row, destinationPixel);
                    if (sourceColorModel.getAlpha(sourcePixel)
                            > destinationColorModel.getAlpha(destinationPixel)) {
                        destinationOut.setDataElements(outputX + column, outputY + row, sourcePixel);
                    } else {
                        destinationOut.setDataElements(outputX + column, outputY + row, destinationPixel);
                    }
                }
            }
        }
    }
}
