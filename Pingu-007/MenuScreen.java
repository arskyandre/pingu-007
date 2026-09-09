import java.awt.Graphics2D;

/** Common lifecycle for interactive menu screens. */
public interface MenuScreen {

    void layout(MenuViewport viewport);

    GameState update(MenuContext context);

    void render(Graphics2D graphics, MenuViewport viewport);
}
