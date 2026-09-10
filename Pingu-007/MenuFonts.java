import java.awt.Font;
import java.io.File;

/** Cached menu font loader with the existing compatible fallback choices. */
public final class MenuFonts {

    private static final Font PIXEL_BASE = loadBaseFont();

    private MenuFonts() {
    }

    private static Font loadBaseFont() {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Font buttonFont(float size) {
        return PIXEL_BASE == null
                ? new Font("Monospaced", Font.BOLD, Math.max(1, Math.round(size)))
                : PIXEL_BASE.deriveFont(Font.PLAIN, size);
    }

    public static Font titleFont(float size) {
        return PIXEL_BASE == null
                ? new Font("Monospaced", Font.BOLD, Math.max(1, Math.round(size)))
                : PIXEL_BASE.deriveFont(Font.PLAIN, size);
    }

    /** Fallback used by text that previously derived from GameCore.pixelFont. */
    public static Font gameTextFont(float size) {
        return PIXEL_BASE == null
                ? new Font("Monospaced", Font.PLAIN, Math.max(1, Math.round(size)))
                : PIXEL_BASE.deriveFont(Font.PLAIN, size);
    }

    public static Font bodyFont(float size) {
        return gameTextFont(size);
    }
}
