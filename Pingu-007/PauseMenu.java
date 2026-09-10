import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/** Pause overlay composition and visuals. */
public final class PauseMenu extends AbstractMenuScreen {

    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 360;

    private final List<MenuEntry> entries;
    private final MenuController controller;
    private final Font pixelFont;

    public PauseMenu(SoundManager ignoredSoundManager) {
        entries = List.of(
                new ButtonMenuDefinition("RESUMIR", BTN_W, BTN_H, MenuAction.navigateTo(GameState.PLAYING))
                        .createEntry(),
                new ButtonMenuDefinition("OPÇÕES", BTN_W, BTN_H, MenuAction.navigateTo(GameState.OPTIONS))
                        .createEntry(),
                new ButtonMenuDefinition("VOLTAR AO MENU PRINCIPAL", BTN_W, BTN_H,
                        MenuAction.navigateTo(GameState.MAIN_MENU)).createEntry());
        controller = MenuController.linear(entries, MenuInputBindings.pauseMenu(), true)
                .withBackAction(MenuAction.navigateTo(GameState.PLAYING), false)
                .withConfirmBeforePointerActivation(true);
        pixelFont = MenuFonts.titleFont(24f);
    }

    @Override
    protected void layoutContent(MenuViewport viewport) {
        MenuLayouts.centeredVerticalStack(viewport, primaryControls(), viewport.height() / 2 - 20, BTN_GAP);
    }

    private List<MenuControl> primaryControls() {
        return entries.stream().map(MenuEntry::getPrimaryControl).toList();
    }

    @Override
    protected GameState updateScreen(MenuContext context) {
        return controller.update(context, GameState.PAUSED);
    }

    @Override
    protected void drawScreen(Graphics2D graphics, MenuViewport viewport) {
        MenuPainter.drawDimOverlay(graphics, viewport.width(), viewport.height(), new Color(0, 0, 0, 160));

        Rectangle panel = new Rectangle((viewport.width() - PANEL_W) / 2,
                (viewport.height() - PANEL_H) / 2, PANEL_W, PANEL_H);
        MenuPainter.drawPanel(graphics, panel, new Color(10, 10, 10, 220), Color.WHITE, 2f);
        MenuPainter.drawCenteredTextInWidth(graphics, "PAUSADO", pixelFont, viewport.width(), panel.y + 55,
                Color.WHITE, new Color(0, 0, 0, 180), 2, 2);

        for (MenuEntry entry : entries) {
            for (MenuControl control : entry.getControls()) {
                control.render(graphics);
            }
        }
    }

    public MenuController controller() {
        return controller;
    }
}
