import java.awt.*;

public class MenuSlider {

    private final Rectangle rect;
    private boolean dragging = false;
    private float value;

    private static final int HANDLE_W = 10;
    private static final int HANDLE_H = 22;

    public MenuSlider(int x, int y, int width, int height, float initialValue) {
        this.rect = new Rectangle(x, y, width, height);
        this.value = Math.clamp(initialValue, 0f, 1f);
    }

    public static final int IDLE = 0;
    public static final int HOVERED = 1;
    public static final int CLICKED = 2;
    public static final int DRAGGING = 3;

    public int update(PointerSnapshot pointer) {
        int mx = pointer.x();
        int my = pointer.y();
        boolean clicking = pointer.isDown(java.awt.event.MouseEvent.BUTTON1);

        Rectangle hitArea = new Rectangle(rect.x, rect.y - 8, rect.width, rect.height + 16);

        if (pointer.wasPressed(java.awt.event.MouseEvent.BUTTON1) && hitArea.contains(mx, my)) {
            dragging = true;
            value = Math.clamp((float) (mx - rect.x) / rect.width, 0f, 1f);
            return CLICKED;
        }
        if (!clicking) {
            dragging = false;
        }

        if (dragging) {
            value = Math.clamp((float) (mx - rect.x) / rect.width, 0f, 1f);
            return DRAGGING;
        }

        if (pointer.isActive() && hitArea.contains(mx, my))
            return HOVERED;
        return IDLE;
    }

    public void draw(Graphics2D g2) {

        g2.setColor(new Color(60, 60, 60));
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);

        g2.setColor(Color.WHITE);
        g2.fillRect(rect.x, rect.y, (int) (rect.width * value), rect.height);

        int hx = rect.x + (int) (rect.width * value) - HANDLE_W / 2;
        int hy = rect.y - (HANDLE_H - rect.height) / 2;
        g2.setColor(Color.WHITE);
        g2.fillRect(hx, hy, HANDLE_W, HANDLE_H);
        g2.setColor(new Color(150, 150, 150));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(hx, hy, HANDLE_W, HANDLE_H);
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        this.value = Math.clamp(v, 0f, 1f);
    }

    public void setPosition(int x, int y) {
        rect.setLocation(x, y);
    }

    public Rectangle getRect() {
        return rect;
    }

    // posicao da direita para colocar botoes do lado do slider
    public int getRightX() {
        return rect.x + rect.width;
    }

    public int getCenterY() {
        return rect.y + rect.height / 2;
    }
}
