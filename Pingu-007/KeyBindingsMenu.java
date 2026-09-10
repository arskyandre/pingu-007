import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/** Static help screen with its Back action represented by a one-entry controller. */
public final class KeyBindingsMenu extends AbstractMenuScreen {

    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 46;

    private static final List<String> HELP_ROWS = List.of(
            "ANDAR: WASD",
            "ATIRAR: BOTÃO ESQUERDO DO MOUSE",
            "RECARREGAR: R",
            "INTERAGIR/FORÇA(PESCA): E",
            "LANÇAR LINHA DE PESCA: BOTÃO DIREITO DO MOUSE",
            "ALTERNAR ARMAS: G");

    private final MenuButton backButton = new MenuButton("VOLTAR", 0, 0, BUTTON_WIDTH, BUTTON_HEIGHT);
    private final List<MenuEntry> entries;
    private final MenuController controller;
    private final Font pixelFont = MenuFonts.titleFont(24f);
    private GameState returnTo = GameState.OPTIONS;

    public KeyBindingsMenu(SoundManager ignoredSoundManager) {
        MenuAction returnAction = MenuAction.returnTo(() -> returnTo);
        entries = List.of(MenuEntry.button(backButton, returnAction));
        controller = MenuController.linear(entries, MenuInputBindings.keyBindingsMenu(), true)
                .withBackAction(returnAction, false)
                .withMouseLockOnConfirm(true)
                .withConfirmBeforePointerActivation(false);
    }

    public void onEnter(GameState returnState) {
        returnTo = returnState;
    }

    @Override
    protected void layoutContent(MenuViewport viewport) {
        backButton.setPosition((viewport.width() - BUTTON_WIDTH) / 2,
                viewport.height() * 3 / 4 + 76);
    }

    @Override
    protected GameState updateScreen(MenuContext context) {
        return controller.update(context, GameState.KEYBINDINGS);
    }

    @Override
    protected void drawScreen(Graphics2D graphics, MenuViewport viewport) {
        graphics.setColor(new Color(10, 10, 10));
        graphics.fillRect(0, 0, viewport.width(), viewport.height());

        MenuPainter.drawCenteredTextInWidth(graphics, "TECLAS DE AÇÃO", pixelFont, viewport.width(), viewport.height() / 8,
                Color.WHITE, new Color(0, 0, 0, 180), 2, 2);

        int textY = viewport.height() / 8 + 64;
        for (String row : HELP_ROWS) {
            MenuPainter.drawCenteredTextInWidth(graphics, row, pixelFont, viewport.width(), textY,
                    Color.WHITE, new Color(0, 0, 0, 180), 2, 2);
            textY += 32;
        }

        backButton.render(graphics);
    }

    public MenuController controller() {
        return controller;
    }
}
