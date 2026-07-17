import java.util.LinkedList;
import java.util.Queue;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/** Mostra notificacoes no topo da tela, util para dar dicas de jogo */
public class ToastNotifications {
    public ToastNotifications() {
    }

    private static class ToastNotification {
        String texto;
        double duration;

        ToastNotification(String texto, double duration) {
            this.texto = texto;
            this.duration = duration;
        }
    }

    private static String notifAtual = null;
    private static Queue<ToastNotification> filaNotificacoes = new LinkedList<>();

    private static final double FADE_IN_DUR = 0.35;
    private static final double HOLD_DUR = 5.0;
    private static double customHoldDur = -1;
    private static final double FADE_OUT_DUR = 0.35;

    private static double tempoNaNotifAtual = 0.0;
    private static double alpha = 0.0;

    private static Font pixelFont;

    static {
        pixelFont = GameCore.pixelFont.deriveFont(Font.PLAIN, 12f);
    }

    public static void RequestNotification(String texto) {
        RequestNotification(texto, -1);
    }

    public static void RequestNotification(String texto, double duration) {
        if (notifAtual == null) {
            notifAtual = texto;
            customHoldDur = duration;
            tempoNaNotifAtual = 0.0;
        } else {
            filaNotificacoes.offer(new ToastNotification(texto, duration));
        }
    }

    public static void skipNotification() {
        if (notifAtual == null)
            return;
        tempoNaNotifAtual = FADE_IN_DUR + holdAtualDur();
    }

    public static void clearNotifQueue() {
        filaNotificacoes.clear();
    }

    private static double holdAtualDur() {
        return customHoldDur >= 0 ? customHoldDur : HOLD_DUR;
    }

    public static void update(double delta) {
        if (notifAtual == null) {
            return;
        }

        tempoNaNotifAtual += delta;

        double hold = holdAtualDur();
        double totalDur = FADE_IN_DUR + hold + FADE_OUT_DUR;

        if (tempoNaNotifAtual < FADE_IN_DUR) {
            alpha = tempoNaNotifAtual / FADE_IN_DUR;
        } else if (tempoNaNotifAtual < FADE_IN_DUR + hold) {
            alpha = 1.0;
        } else if (tempoNaNotifAtual < totalDur) {
            double t = tempoNaNotifAtual - (FADE_IN_DUR + hold);
            alpha = 1.0 - (t / FADE_OUT_DUR);
        } else {
            ToastNotification proximo = filaNotificacoes.poll();
            if (proximo != null) {
                notifAtual = proximo.texto;
                customHoldDur = proximo.duration;
            } else {
                notifAtual = null;
                customHoldDur = -1;
            }
            tempoNaNotifAtual = 0.0;
            alpha = 0.0;
        }

        alpha = Math.max(0.0, Math.min(1.0, alpha));
    }

    public static void draw(Graphics2D g2, int telaLargura, int telaAltura) {
        if (notifAtual == null || alpha <= 0.0) {
            return;
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setFont(pixelFont);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(notifAtual);
        int textH = fm.getHeight();

        int paddingX = 16;
        int paddingY = 10;
        int boxW = textW + paddingX * 2;
        int boxH = textH + paddingY * 2;

        int boxX = (telaLargura - boxW) / 2;

        int slideOffset = (int) ((1.0 - alpha) * 20);
        int boxY = 24 - slideOffset;

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));

        g2.setColor(new Color(20, 20, 20, 220));
        g2.fill(new Rectangle2D.Double(boxX, boxY, boxW, boxH));

        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(2));
        g2.draw(new Rectangle2D.Double(boxX, boxY, boxW, boxH));

        int textX = boxX + paddingX;
        int textY = boxY + paddingY + fm.getAscent();

        g2.setColor(Color.BLACK);
        g2.drawString(notifAtual, textX + 1, textY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(notifAtual, textX, textY);

        g2.setComposite(oldComposite);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }
}