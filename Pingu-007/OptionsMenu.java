import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Options screen composition and visuals backed by an injected OptionsModel. */
public final class OptionsMenu extends AbstractMenuScreen {

    private static final int SLIDER_W = 320;
    private static final int SLIDER_H = 10;
    private static final int BTN_SIZE = 36;
    private static final int ROW_GAP = 34;
    private static final int LABEL_TO_SLIDER_GAP = 22;
    private static final int SLIDER_TO_BTN_GAP = 20;
    private static final int BTN_TO_PCT_GAP = 16;
    private static final int BUTTON_ROW_GAP = 16;
    private static final int CORNER_MARGIN = 32;
    private static final int TITLE_Y_FRACTION = 8;
    private static final int CONTENT_START_FRACTION = 3;

    private final OptionsModel model;
    private final List<MenuEntry> entries;
    private final MenuController controller;

    private final MenuSlider musicSlider;
    private final MenuSlider sfxSlider;
    private final MenuSlider fpsCapSlider;
    private final IconButton toggleMuteBGM;
    private final IconButton toggleMuteSFX;
    private final IconButton toggleUnlimitedFps;
    private final MenuButton backButton;
    private final MenuButton keyBindingsButton;
    private final IconButton fullscreenButton;
    private final MenuButton shadowsOffButton;
    private final MenuButton shadowsSharpButton;
    private final MenuButton shadowsSoftButton;
    private final IconButton showFpsButton;
    private final IconButton enableAaButton;

    private final Font pixelFont = MenuFonts.titleFont(24f);
    private final Font pixelFontSmall = MenuFonts.buttonFont(11f);
    private final Font pixelFontTiny = MenuFonts.bodyFont(9f);
    private GameState returnTo = GameState.MAIN_MENU;

    public OptionsMenu(OptionsModel model) {
        this.model = model;

        musicSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, model.musicVolume());
        sfxSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, model.sfxVolume());
        fpsCapSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, model.fpsSliderValue());

        toggleMuteBGM = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleMuteSFX = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleUnlimitedFps = new IconButton(0, 0, BTN_SIZE, IconIndex.UNLIM_FPS_OFF, false);
        fullscreenButton = new IconButton(0, 0, BTN_SIZE, IconIndex.FULLSCREEN, false);
        shadowsOffButton = new MenuButton("Desligadas", 0, 0, 160, 46);
        shadowsSharpButton = new MenuButton("Nitidas", 0, 0, 160, 46);
        shadowsSoftButton = new MenuButton("Suaves", 0, 0, 160, 46);
        showFpsButton = new IconButton(0, 0, BTN_SIZE, IconIndex.RED_X, false);
        enableAaButton = new IconButton(0, 0, BTN_SIZE, IconIndex.GREEN_CHECK, false);
        backButton = new MenuButton("VOLTAR", 0, 0, 160, 46);
        keyBindingsButton = new MenuButton("CONSULTAR TECLAS", 0, 0, 200, 46);

        List<MenuEntry> declaredEntries = new ArrayList<>();
        SliderOptionEntry musicEntry = new SliderOptionEntry("VOLUME DA MÚSICA", musicSlider, toggleMuteBGM,
                MenuAction.stay(GameState.OPTIONS, model::toggleMusicMute), this::handleMusicSlider);
        musicEntry.withAdjustment(MenuInputIntent.LEFT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustMusicVolume(-0.05f)));
        musicEntry.withAdjustment(MenuInputIntent.RIGHT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustMusicVolume(0.05f)));
        declaredEntries.add(musicEntry);

        SliderOptionEntry sfxEntry = new SliderOptionEntry("VOLUME DOS EFEITOS", sfxSlider, toggleMuteSFX,
                MenuAction.stay(GameState.OPTIONS, model::toggleSfxMute), this::handleSfxSlider);
        sfxEntry.withAdjustment(MenuInputIntent.LEFT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustSfxVolume(-0.05f)));
        sfxEntry.withAdjustment(MenuInputIntent.RIGHT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustSfxVolume(0.05f)));
        declaredEntries.add(sfxEntry);

        SliderOptionEntry fpsEntry = new SliderOptionEntry(model::fpsLabel, fpsCapSlider, toggleUnlimitedFps,
                MenuAction.stay(GameState.OPTIONS, () -> model.toggleUnlimitedFps(fpsCapSlider.getValue())),
                this::handleFpsSlider);
        fpsEntry.withAdjustment(MenuInputIntent.LEFT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustFpsByDirection(-1)));
        fpsEntry.withAdjustment(MenuInputIntent.RIGHT,
                MenuAction.stay(GameState.OPTIONS, () -> model.adjustFpsByDirection(1)));
        declaredEntries.add(fpsEntry);

        declaredEntries.add(MenuEntry.button(shadowsOffButton,
                MenuAction.stay(GameState.OPTIONS,
                        () -> model.setShadowMode(OptionsModel.ShadowMode.OFF))));
        declaredEntries.add(MenuEntry.button(shadowsSharpButton,
                MenuAction.stay(GameState.OPTIONS,
                        () -> model.setShadowMode(OptionsModel.ShadowMode.SHARP))));
        declaredEntries.add(MenuEntry.button(shadowsSoftButton,
                MenuAction.stay(GameState.OPTIONS,
                        () -> model.setShadowMode(OptionsModel.ShadowMode.SOFT))));
        declaredEntries.add(new ToggleOptionEntry("Habilitar Anti-Aliasing", enableAaButton,
                MenuAction.stay(GameState.OPTIONS, model::toggleAntiAliasing)));
        declaredEntries.add(new ToggleOptionEntry("MOSTRAR FPS", showFpsButton,
                MenuAction.stay(GameState.OPTIONS, model::toggleFpsCounter)));
        declaredEntries.add(MenuEntry.button(keyBindingsButton, MenuAction.navigateTo(GameState.KEYBINDINGS)));

        MenuAction returnAction = MenuAction.returnTo(() -> returnTo);
        declaredEntries.add(MenuEntry.button(backButton, returnAction));
        declaredEntries.add(MenuEntry.control(fullscreenButton,
                MenuAction.stay(GameState.OPTIONS, model::toggleFullscreen)));

        entries = List.copyOf(declaredEntries);
        controller = MenuController.spatial(entries, MenuInputBindings.optionsMenu())
                .withPointerBeforeNavigation(true)
                .withBackAction(returnAction, false);
        refreshControlState();
    }

    private GameState handleMusicSlider(MenuContext context, MenuControl control, MenuInteraction interaction) {
        model.setMusicVolume(musicSlider.getValue());
        return GameState.OPTIONS;
    }

    private GameState handleSfxSlider(MenuContext context, MenuControl control, MenuInteraction interaction) {
        model.setSfxVolume(sfxSlider.getValue());
        return GameState.OPTIONS;
    }

    private GameState handleFpsSlider(MenuContext context, MenuControl control, MenuInteraction interaction) {
        model.setFpsFromSliderValue(fpsCapSlider.getValue());
        return GameState.OPTIONS;
    }

    public void onEnter(GameState returnState) {
        returnTo = returnState;
        model.onEnter();
        refreshControlState();
    }

    /** Compatibility setter for callers that enter Options through the old API. */
    @Override
    protected void layoutContent(MenuViewport viewport) {
        int centerX = viewport.centerX();
        int contentStartY = viewport.height() / CONTENT_START_FRACTION;
        int sliderRowHeight = LABEL_TO_SLIDER_GAP + SLIDER_H;
        int shadowRowHeight = LABEL_TO_SLIDER_GAP + 46;
        int fixedContentHeight = sliderRowHeight * 3 + shadowRowHeight + BTN_SIZE * 2 + 46;
        int availableForGaps = viewport.height() - contentStartY - 20 - fixedContentHeight;
        int responsiveGap = Math.clamp(availableForGaps / 6, 10, ROW_GAP);
        MenuLayouts.LayoutCursor cursor = new MenuLayouts.LayoutCursor(contentStartY, responsiveGap);

        int musicY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(musicSlider, toggleMuteBGM, centerX, musicY);

        int sfxY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(sfxSlider, toggleMuteSFX, centerX, sfxY);

        int fpsY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(fpsCapSlider, toggleUnlimitedFps, centerX, fpsY);

        int shadowsY = cursor.nextRow(shadowRowHeight);
        MenuLayouts.centeredHorizontalRow(viewport, shadowsY + LABEL_TO_SLIDER_GAP, BUTTON_ROW_GAP,
                shadowsOffButton, shadowsSharpButton, shadowsSoftButton);

        int aaY = cursor.nextRow(BTN_SIZE);
        enableAaButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, aaY);

        int fpsToggleY = cursor.nextRow(BTN_SIZE);
        showFpsButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, fpsToggleY);

        int buttonsY = cursor.nextRow(46);
        MenuLayouts.centeredHorizontalRow(viewport, buttonsY, BUTTON_ROW_GAP, keyBindingsButton, backButton);
        MenuLayouts.bottomRight(viewport, CORNER_MARGIN, fullscreenButton);
    }

    private void positionSliderRow(MenuSlider slider, IconButton accessory, int centerX, int y) {
        slider.setPosition(centerX - SLIDER_W / 2, y);
        accessory.setPosition(slider.getRightX() + SLIDER_TO_BTN_GAP,
                slider.getCenterY() - BTN_SIZE / 2);
    }

    @Override
    protected GameState updateScreen(MenuContext context) {
        model.synchronize();
        refreshControlState();
        GameState result = controller.update(context, GameState.OPTIONS);
        model.synchronize();
        refreshControlState();
        return result;
    }

    private void refreshControlState() {
        musicSlider.setValue(model.musicVolume());
        sfxSlider.setValue(model.sfxVolume());
        fpsCapSlider.setValue(model.fpsSliderValue());

        toggleMuteBGM.setIcon(model.musicMuted() ? IconIndex.MUTED : IconIndex.UNMUTED);
        toggleMuteSFX.setIcon(model.sfxMuted() ? IconIndex.MUTED : IconIndex.UNMUTED);
        toggleUnlimitedFps.setIcon(model.fpsUnlimited() ? IconIndex.UNLIM_FPS_ON : IconIndex.UNLIM_FPS_OFF);
        enableAaButton.setIcon(model.isAntiAliasingEnabled() ? IconIndex.GREEN_CHECK : IconIndex.RED_X);
        showFpsButton.setIcon(model.isShowFpsCounter() ? IconIndex.GREEN_CHECK : IconIndex.RED_X);

        OptionsModel.ShadowMode shadowMode = model.shadowMode();
        shadowsOffButton.setSelected(shadowMode == OptionsModel.ShadowMode.OFF);
        shadowsSharpButton.setSelected(shadowMode == OptionsModel.ShadowMode.SHARP);
        shadowsSoftButton.setSelected(shadowMode == OptionsModel.ShadowMode.SOFT);
    }

    @Override
    protected void drawScreen(Graphics2D graphics, MenuViewport viewport) {
        model.synchronize();
        refreshControlState();

        graphics.setColor(new Color(10, 10, 10));
        graphics.fillRect(0, 0, viewport.width(), viewport.height());
        MenuPainter.drawCenteredTextInWidth(graphics, "OPÇÕES", pixelFont, viewport.width(),
                viewport.height() / TITLE_Y_FRACTION, Color.WHITE, new Color(0, 0, 0, 180), 2, 2);

        SliderOptionEntry musicEntry = (SliderOptionEntry) entries.get(0);
        SliderOptionEntry sfxEntry = (SliderOptionEntry) entries.get(1);
        SliderOptionEntry fpsEntry = (SliderOptionEntry) entries.get(2);
        drawSliderRow(graphics, musicEntry, viewport.width(), true);
        drawSliderRow(graphics, sfxEntry, viewport.width(), true);
        drawSliderRow(graphics, fpsEntry, viewport.width(), false);

        drawShadowModeRow(graphics, viewport.width());
        drawLabelLeftOf(graphics, "Habilitar Anti-Aliasing", enableAaButton.getBoundsCopy());
        drawLabelLeftOf(graphics, "MOSTRAR FPS", showFpsButton.getBoundsCopy());

        enableAaButton.render(graphics);
        toggleMuteBGM.render(graphics);
        toggleMuteSFX.render(graphics);
        toggleUnlimitedFps.render(graphics);
        showFpsButton.render(graphics);
        keyBindingsButton.render(graphics);
        backButton.render(graphics);
        fullscreenButton.render(graphics);
    }

    private void drawShadowModeRow(Graphics2D graphics, int width) {
        Rectangle firstButton = shadowsOffButton.getBoundsCopy();
        graphics.setFont(pixelFontSmall);
        FontMetrics metrics = graphics.getFontMetrics();
        String label = "SOMBRAS";
        int labelY = firstButton.y - LABEL_TO_SLIDER_GAP + metrics.getAscent();
        graphics.setColor(Color.WHITE);
        graphics.drawString(label, (width - metrics.stringWidth(label)) / 2, labelY);

        shadowsOffButton.render(graphics);
        shadowsSharpButton.render(graphics);
        shadowsSoftButton.render(graphics);
    }

    private void drawSliderRow(Graphics2D graphics, SliderOptionEntry entry, int width, boolean drawPercentage) {
        MenuSlider slider = entry.slider();
        Rectangle bounds = slider.getBoundsCopy();
        graphics.setFont(pixelFontSmall);
        boolean highlighted = controller.navigator().isFocused(entry);
        graphics.setColor(highlighted ? Color.WHITE : new Color(180, 180, 180));
        FontMetrics metrics = graphics.getFontMetrics();
        String label = entry.label();
        graphics.drawString(label, (width - metrics.stringWidth(label)) / 2,
                bounds.y - LABEL_TO_SLIDER_GAP + metrics.getAscent());

        slider.render(graphics);
        if (drawPercentage) {
            graphics.setFont(pixelFontTiny);
            graphics.setColor(highlighted ? Color.WHITE : new Color(160, 160, 160));
            String percentage = (int) (slider.getValue() * 100) + "%";
            int percentageX = bounds.x + bounds.width + SLIDER_TO_BTN_GAP + BTN_SIZE + BTN_TO_PCT_GAP;
            graphics.drawString(percentage, percentageX, bounds.y + bounds.height);
        }
    }

    private void drawLabelLeftOf(Graphics2D graphics, String text, Rectangle anchor) {
        graphics.setFont(pixelFontSmall);
        FontMetrics metrics = graphics.getFontMetrics();
        int textWidth = metrics.stringWidth(text);
        int x = anchor.x - textWidth - SLIDER_TO_BTN_GAP;
        int y = anchor.y + (anchor.height + metrics.getAscent() - metrics.getDescent()) / 2;
        MenuPainter.drawTextWithShadow(graphics, text, x, y, Color.WHITE,
                new Color(0, 0, 0, 180), 2, 2);
    }

    public OptionsModel model() {
        return model;
    }

    public MenuController controller() {
        return controller;
    }
}
