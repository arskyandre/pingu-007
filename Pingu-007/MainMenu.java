import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/** Main-menu composition and visuals. Shared input behavior lives in MenuController. */
public final class MainMenu extends AbstractMenuScreen {

    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;

    private static final double BOB_SPEED = 0.125;
    private static final double BOB_AMP = 4.0;

    private final List<MenuEntry> entries;
    private final MenuController controller;
    private final Font pixelFont;
    private final BufferedImage background;
    private double bobTime;

    public MainMenu(SoundManager ignoredSoundManager) {
        entries = List.of(
                new ButtonMenuDefinition("JOGAR", BTN_W, BTN_H, MenuAction.navigateTo(GameState.PLAYING))
                        .createEntry(),
                new ButtonMenuDefinition("OPÇÕES", BTN_W, BTN_H, MenuAction.navigateTo(GameState.OPTIONS))
                        .createEntry(),
                new ButtonMenuDefinition("SAIR DO JOGO", BTN_W, BTN_H, MenuAction.navigateTo(GameState.QUIT))
                        .createEntry());
        controller = MenuController.linear(entries, MenuInputBindings.gamepadMenu(), true)
                .withConfirmBeforePointerActivation(true);
        pixelFont = MenuFonts.titleFont(32f);
        background = loadBackground();
    }

    private BufferedImage loadBackground() {
        try {
            return ImageIO.read(new File("images/hud/menu_background.png"));
        } catch (Exception ignored) {
            System.err.println("menu_background.png not found");
            return null;
        }
    }

    @Override
    protected void layoutContent(MenuViewport viewport) {
        MenuLayouts.centeredVerticalStack(viewport, primaryControls(), viewport.height() / 2, BTN_GAP);
    }

    private List<MenuControl> primaryControls() {
        return entries.stream().map(MenuEntry::getPrimaryControl).toList();
    }

    @Override
    protected GameState updateScreen(MenuContext context) {
        bobTime += BOB_SPEED;
        return controller.update(context, GameState.MAIN_MENU);
    }

    @Override
    protected void drawScreen(Graphics2D graphics, MenuViewport viewport) {
        if (background != null) {
            graphics.drawImage(background, 0, 0, viewport.width(), viewport.height(), null);
        } else {
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, viewport.width(), viewport.height());
        }

        MenuPainter.drawDimOverlay(graphics, viewport.width(), viewport.height(), new Color(0, 0, 0, 100));
        MenuPainter.drawCenteredTextInWidth(graphics, "PINGU 007", pixelFont, viewport.width(), viewport.height() / 4,
                Color.WHITE, new Color(0, 0, 0, 180), 3, 3);

        Font hintFont = MenuFonts.gameTextFont(12f);
        String hint = "Pressione F11 para alternar a tela cheia!";
        Graphics2D metricsGraphics = (Graphics2D) graphics.create();
        int hintWidth;
        try {
            metricsGraphics.setFont(hintFont);
            metricsGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            hintWidth = metricsGraphics.getFontMetrics().stringWidth(hint);
        } finally {
            metricsGraphics.dispose();
        }
        int hintX = viewport.width() - hintWidth;
        int hintY = viewport.height() - 10 + (int) (Math.sin(bobTime) * BOB_AMP);
        MenuPainter.drawTextWithShadow(graphics, hint, hintX, hintY, hintFont, Color.GRAY,
                new Color(0, 0, 0, 180), 3, 3);

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
