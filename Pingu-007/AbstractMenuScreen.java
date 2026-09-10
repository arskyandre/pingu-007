import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

/** Thin lifecycle base that caches viewport layout without imposing a visual design. */
public abstract class AbstractMenuScreen implements MenuScreen {

    private MenuViewport lastLayoutViewport;

    @Override
    public final void layout(MenuViewport viewport) {
        Objects.requireNonNull(viewport, "viewport");
        if (!viewport.equals(lastLayoutViewport)) {
            layoutContent(viewport);
            lastLayoutViewport = viewport;
        }
    }

    @Override
    public final GameState update(MenuContext context) {
        Objects.requireNonNull(context, "context");
        layout(context.viewport());
        return updateScreen(context);
    }

    @Override
    public final void render(Graphics2D graphics, MenuViewport viewport) {
        Objects.requireNonNull(graphics, "graphics");
        layout(viewport);
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            drawScreen(copy, viewport);
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        } finally {
            copy.dispose();
        }
    }

    protected abstract void layoutContent(MenuViewport viewport);

    protected abstract GameState updateScreen(MenuContext context);

    protected abstract void drawScreen(Graphics2D graphics, MenuViewport viewport);
}
