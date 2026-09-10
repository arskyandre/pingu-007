import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/** Slider control retaining the original expanded hit area and value math. */
public class MenuSlider implements MenuControl {

    private static final int HANDLE_W = 10;
    private static final int HANDLE_H = 22;

    private final Rectangle rect;
    private boolean dragging;
    private boolean hovered;
    private boolean focused;
    private float value;

    public MenuSlider(int x, int y, int width, int height, float initialValue) {
        rect = new Rectangle(x, y, width, height);
        value = clamp(initialValue);
    }

    @Override
    public MenuInteraction updatePointer(InputManager input) {
        int mouseX = input.getMouseX();
        int mouseY = input.getMouseY();
        boolean clicking = input.isMouseButtonPressed(MouseEvent.BUTTON1);
        Rectangle hitArea = getHitBoundsCopy();
        hovered = hitArea.contains(mouseX, mouseY);

        if (input.isMouseButtonJustPressed(MouseEvent.BUTTON1) && hovered) {
            dragging = true;
            value = valueFromMouse(mouseX);
            return MenuInteraction.CLICKED;
        }
        if (!clicking) {
            dragging = false;
        }

        if (dragging) {
            value = valueFromMouse(mouseX);
            return MenuInteraction.DRAGGING;
        }
        return hovered ? MenuInteraction.HOVERED : MenuInteraction.IDLE;
    }

    private float valueFromMouse(int mouseX) {
        return clamp((float) (mouseX - rect.x) / rect.width);
    }

    @Override
    public void render(Graphics2D graphics) {
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setColor(new Color(60, 60, 60));
            copy.fillRect(rect.x, rect.y, rect.width, rect.height);

            copy.setColor(Color.WHITE);
            copy.fillRect(rect.x, rect.y, (int) (rect.width * value), rect.height);

            int handleX = rect.x + (int) (rect.width * value) - HANDLE_W / 2;
            int handleY = rect.y - (HANDLE_H - rect.height) / 2;
            copy.setColor(Color.WHITE);
            copy.fillRect(handleX, handleY, HANDLE_W, HANDLE_H);
            copy.setColor(new Color(150, 150, 150));
            copy.setStroke(new BasicStroke(1));
            copy.drawRect(handleX, handleY, HANDLE_W, HANDLE_H);
        } finally {
            copy.dispose();
        }
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = clamp(value);
    }

    private static float clamp(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    @Override
    public void setPosition(int x, int y) {
        rect.setLocation(x, y);
    }

    @Override
    public Rectangle getBoundsCopy() {
        return new Rectangle(rect);
    }

    public Rectangle getHitBoundsCopy() {
        return new Rectangle(rect.x, rect.y - 8, rect.width, rect.height + 16);
    }

    public int getRightX() {
        return rect.x + rect.width;
    }

    public int getCenterY() {
        return rect.y + rect.height / 2;
    }

    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean isHovered() {
        return hovered;
    }

    @Override
    public void clearPointerHover() {
        hovered = false;
        dragging = false;
    }
}
