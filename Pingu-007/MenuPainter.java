import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Parameterized painting and wrapping operations shared by menu screens. */
public final class MenuPainter {

    private MenuPainter() {
    }

    public static void drawTextWithShadow(Graphics2D graphics, String text, int x, int y,
            Color color, Color shadowColor, int shadowOffsetX, int shadowOffsetY) {
        drawTextWithShadow(graphics, text, x, y, null, color, shadowColor, shadowOffsetX, shadowOffsetY);
    }

    public static void drawTextWithShadow(Graphics2D graphics, String text, int x, int y,
            Font font, Color color, Color shadowColor, int shadowOffsetX, int shadowOffsetY) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            if (font != null) {
                copy.setFont(font);
            }
            copy.setColor(shadowColor);
            copy.drawString(text, x + shadowOffsetX, y + shadowOffsetY);
            copy.setColor(color);
            copy.drawString(text, x, y);
        } finally {
            copy.dispose();
        }
    }

    public static void drawTextWithShadow(Graphics2D graphics, String text, int x, int y, Color color) {
        drawTextWithShadow(graphics, text, x, y, color, new Color(0, 0, 0, 160), 2, 2);
    }

    public static void drawCenteredText(Graphics2D graphics, String text, Font font, int centerX, int baselineY,
            Color color, Color shadowColor, int shadowOffsetX, int shadowOffsetY) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setFont(font);
            int x = centerX - copy.getFontMetrics().stringWidth(text) / 2;
            copy.setColor(shadowColor);
            copy.drawString(text, x + shadowOffsetX, baselineY + shadowOffsetY);
            copy.setColor(color);
            copy.drawString(text, x, baselineY);
        } finally {
            copy.dispose();
        }
    }

    /** Centers text with the same width-first arithmetic used by the screens. */
    public static void drawCenteredTextInWidth(Graphics2D graphics, String text, Font font, int width,
            int baselineY, Color color, Color shadowColor, int shadowOffsetX, int shadowOffsetY) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setFont(font);
            int x = (width - copy.getFontMetrics().stringWidth(text)) / 2;
            copy.setColor(shadowColor);
            copy.drawString(text, x + shadowOffsetX, baselineY + shadowOffsetY);
            copy.setColor(color);
            copy.drawString(text, x, baselineY);
        } finally {
            copy.dispose();
        }
    }

    public static void drawDimOverlay(Graphics2D graphics, int width, int height, Color color) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setColor(color);
            copy.fillRect(0, 0, width, height);
        } finally {
            copy.dispose();
        }
    }

    public static void drawPanel(Graphics2D graphics, Rectangle bounds, Color fill, Color border, float strokeWidth) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setColor(fill);
            copy.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            copy.setColor(border);
            copy.setStroke(new BasicStroke(strokeWidth));
            copy.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        } finally {
            copy.dispose();
        }
    }

    public static void drawDashedLine(Graphics2D graphics, int x1, int y, int x2,
            Color color, float strokeWidth, float[] dash, float phase) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, dash, phase));
            copy.setColor(color);
            copy.drawLine(x1, y, x2, y);
        } finally {
            copy.dispose();
        }
    }

    public static List<String> wrapWords(FontMetrics metrics, String text, int maxWidth) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    public static List<String> wrapWordsWithEndl(FontMetrics metrics, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] parts = text.split("\\s*\\bENDL\\b\\s*");
        for (String part : parts) {
            lines.addAll(wrapWords(metrics, part, maxWidth));
        }
        return lines;
    }
}
