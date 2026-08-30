import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class OptionsMenu {

    private final SoundManager soundManager;
    private GameState returnTo;

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

    private static final int MIN_FPS = 30;
    private static final int MAX_FPS = 240;

    private static class ItemFoco {
        final MenuSlider deslizador;
        final MenuButton botao;
        final Function<GameCore, GameState> acao;

        ItemFoco(MenuSlider deslizador, MenuButton botao, Function<GameCore, GameState> acao) {
            this.deslizador = deslizador;
            this.botao = botao;
            this.acao = acao;
        }

        ItemFoco(MenuButton botao, Function<GameCore, GameState> acao) {
            this(null, botao, acao);
        }

        ItemFoco(MenuSlider deslizador, Function<GameCore, GameState> acao) {
            this(deslizador, null, acao);
        }

        Rectangle limites() {
            if (deslizador != null && botao != null) {
                return deslizador.getRect().union(botao.getRect());
            }
            return deslizador != null ? deslizador.getRect() : botao.getRect();
        }

        void aplicarFoco(boolean focado) {
            if (botao != null) {
                botao.hovered = focado;
            }
        }
    }

    private final List<ItemFoco> itensFoco = new ArrayList<>();
    private ItemFoco itemFocado;
    private boolean fpsSliderInitialized = false;
    private int previousFpsLimit = 120;
    private boolean fpsUnlimited = false;

    private float previousMusicVolume;
    private float previousSfxVolume;
    private boolean musicMuted = false;
    private boolean sfxMuted = false;

    private final MenuSlider musicSlider;
    private final MenuSlider sfxSlider;
    private final MenuSlider fpsCapSlider;
    private final IconButton toggleMuteBGM;
    private final IconButton toggleMuteSFX;
    private final IconButton toggleUnlimitedFps;
    private final MenuButton backBtn;
    private final MenuButton keyBindBtn;
    private final IconButton fullScreenButton;
    private final IconButton renderShadowsButton;
    private final IconButton showFpsButton;
    private final IconButton enableAAButton;

    private Font pixelFont;
    private Font pixelFontSmall;
    private Font pixelFontTiny;

    public OptionsMenu(SoundManager soundManager) {
        this.soundManager = soundManager;
        this.previousMusicVolume = soundManager.getMusicVolume();
        this.previousSfxVolume = soundManager.getSfxVolume();

        musicSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getMusicVolume());
        sfxSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, soundManager.getSfxVolume());
        fpsCapSlider = new MenuSlider(0, 0, SLIDER_W, SLIDER_H, 0.5f);

        toggleMuteBGM = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleMuteSFX = new IconButton(0, 0, BTN_SIZE, IconIndex.UNMUTED, false);
        toggleUnlimitedFps = new IconButton(0, 0, BTN_SIZE, IconIndex.UNLIM_FPS_OFF, false);
        fullScreenButton = new IconButton(0, 0, BTN_SIZE, IconIndex.FULLSCREEN, false);
        renderShadowsButton = new IconButton(0, 0, BTN_SIZE, IconIndex.GREEN_CHECK, false);
        showFpsButton = new IconButton(0, 0, BTN_SIZE, IconIndex.RED_X, false);
        enableAAButton = new IconButton(0, 0, BTN_SIZE, IconIndex.GREEN_CHECK, false);

        backBtn = new MenuButton("VOLTAR", 0, 0, 160, 46);
        keyBindBtn = new MenuButton("CONSULTAR TECLAS", 0, 0, 200, 46);

        itensFoco.add(new ItemFoco(musicSlider, toggleMuteBGM, gc -> {
            toggleMusicMute();
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(sfxSlider, toggleMuteSFX, gc -> {
            toggleSfxMute();
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(fpsCapSlider, toggleUnlimitedFps, gc -> {
            alternarFpsIlimitado(gc);
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(renderShadowsButton, gc -> {
            gc.toggleRenderShadows();
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(enableAAButton, gc -> {
            gc.toggleAntiAliasing();
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(showFpsButton, gc -> {
            gc.toggleFpsCounter();
            return GameState.OPTIONS;
        }));
        itensFoco.add(new ItemFoco(keyBindBtn, gc -> GameState.KEYBINDINGS));
        itensFoco.add(new ItemFoco(backBtn, gc -> returnTo));
        itensFoco.add(new ItemFoco(fullScreenButton, gc -> {
            gc.toggleFullscreen();
            return GameState.OPTIONS;
        }));
        itemFocado = itensFoco.get(0);

        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 24f);
            pixelFontSmall = base.deriveFont(Font.PLAIN, 11f);
            pixelFontTiny = base.deriveFont(Font.PLAIN, 9f);
        } catch (Exception e) {
            System.err.println("Font not found, falling back");
            pixelFont = new Font("Monospaced", Font.BOLD, 24);
            pixelFontSmall = new Font("Monospaced", Font.BOLD, 11);
            pixelFontTiny = new Font("Monospaced", Font.PLAIN, 9);
        }
    }

    public void setReturnState(GameState state) {
        this.returnTo = state;
    }


    private static class LayoutCursor {
        int y;
        final int gap;

        LayoutCursor(int startY, int gap) {
            this.y = startY;
            this.gap = gap;
        }

        int nextRow(int rowHeight) {
            int rowY = y;
            y += rowHeight + gap;
            return rowY;
        }
    }

    public void repositionElements(int width, int height, GameCore GC) {
        int centerX = width / 2;
        int contentStartY = height / CONTENT_START_FRACTION;
        int sliderRowHeight = LABEL_TO_SLIDER_GAP + SLIDER_H;
        int fixedContentHeight = sliderRowHeight * 3 + BTN_SIZE * 3 + 46;
        int availableForGaps = height - contentStartY - 20 - fixedContentHeight;
        int responsiveGap = Math.clamp(availableForGaps / 6, 10, ROW_GAP);
        LayoutCursor cursor = new LayoutCursor(contentStartY, responsiveGap);

        int musicY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(musicSlider, toggleMuteBGM, centerX, musicY);

        int sfxY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(sfxSlider, toggleMuteSFX, centerX, sfxY);

        int fpsCapY = cursor.nextRow(sliderRowHeight);
        positionSliderRow(fpsCapSlider, toggleUnlimitedFps, centerX, fpsCapY);

        int shadowsToggleY = cursor.nextRow(BTN_SIZE);
        renderShadowsButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, shadowsToggleY);

        int AAToggleY = cursor.nextRow(BTN_SIZE);
        enableAAButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, AAToggleY);

        int fpsToggleY = cursor.nextRow(BTN_SIZE);
        showFpsButton.setPosition(centerX + SLIDER_W / 2 - BTN_SIZE, fpsToggleY);

        int buttonsY = cursor.nextRow(46);
        layoutButtonRowCentered(buttonsY, centerX, keyBindBtn, backBtn);

        fullScreenButton.setPosition(width - CORNER_MARGIN - BTN_SIZE, height - CORNER_MARGIN - BTN_SIZE);
    }

    private void positionSliderRow(MenuSlider slider, IconButton muteButton, int centerX, int y) {
        slider.setPosition(centerX - SLIDER_W / 2, y);
        if (muteButton != null) {
            muteButton.setPosition(
                    slider.getRightX() + SLIDER_TO_BTN_GAP,
                    slider.getCenterY() - BTN_SIZE / 2);
        }
    }

    private void layoutButtonRowCentered(int y, int centerX, MenuButton... buttons) {
        int totalWidth = 0;
        for (MenuButton b : buttons) {
            totalWidth += b.getRect().width;
        }
        totalWidth += BUTTON_ROW_GAP * (buttons.length - 1);

        int x = centerX - totalWidth / 2;
        for (MenuButton b : buttons) {
            b.setPosition(x, y);
            x += b.getRect().width + BUTTON_ROW_GAP;
        }
    }

    public GameState update(InputManager input, int width, int height, GameCore GC) {
        repositionElements(width, height, GC);

        if (!fpsSliderInitialized) {
            int limiteFps = GC.getTargetFps();
            fpsUnlimited = limiteFps == 0;
            if (!fpsUnlimited) {
                previousFpsLimit = Math.clamp(limiteFps, MIN_FPS, MAX_FPS);
            }
            fpsCapSlider.setValue(sliderValueFromFps(previousFpsLimit));
            fpsSliderInitialized = true;
        }

        atualizarIconesFps(GC);

        renderShadowsButton.setIcon(GC.isRenderShadows()
                ? IconIndex.GREEN_CHECK
                : IconIndex.RED_X);

        if (GC.isAntiAliasingEnabled()) {
            enableAAButton.setIcon(IconIndex.GREEN_CHECK);
        } else
            enableAAButton.setIcon(IconIndex.RED_X);

        if (GC.isShowFpsCounter()) {
            showFpsButton.setIcon(IconIndex.GREEN_CHECK);
        } else {
            showFpsButton.setIcon(IconIndex.RED_X);
        }

        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE) || input.isButtonJustPressed(InputManager.GamepadButton.B)) {
            return returnTo;
        }

        boolean mouseAceito = !input.isMouseBloqueado();
        if (mouseAceito) {
            updateMusicSlider(input);
            updateSfxSlider(input);
            updateFpsCapSlider(input, GC);
            GameState acaoMouse = atualizarBotoesMouse(input, GC);
            if (acaoMouse != GameState.OPTIONS) {
                return acaoMouse;
            }
            atualizarFocoPeloMouse(input);
        } else {
            limparEstadoHoverBotoes();
        }

        atualizarNavegacaoControle(input, GC);
        if ((input.isControllerActive() || input.isMouseBloqueado()) && !mouseAceito) {
            aplicarVisualFoco();
        }

        GameState acao = ativarItemFocado(input, GC);
        if (acao != GameState.OPTIONS) {
            return acao;
        }

        return GameState.OPTIONS;
    }

    private GameState atualizarBotoesMouse(InputManager input, GameCore GC) {
        for (ItemFoco item : itensFoco) {
            if (item.botao == null) {
                continue;
            }
            if (item.botao.update(input) == MenuButton.CLICKED) {
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
                return item.acao.apply(GC);
            }
        }
        return GameState.OPTIONS;
    }

    private void atualizarFocoPeloMouse(InputManager input) {
        for (ItemFoco item : itensFoco) {
            if (item.botao != null && item.botao.isHovered()) {
                itemFocado = item;
            }
        }
    }

    private void limparEstadoHoverBotoes() {
        for (ItemFoco item : itensFoco) {
            item.aplicarFoco(false);
        }
    }

    private void aplicarVisualFoco() {
        for (ItemFoco item : itensFoco) {
            item.aplicarFoco(item == itemFocado);
        }
    }

    private void updateMusicSlider(InputManager input) {
        int state = musicSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            itemFocado = encontrarItemFoco(musicSlider);
            if (state == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.setMusicVolume(musicSlider.getValue());
        }

        if (musicSlider.getValue() <= 0f && !musicMuted) {
            musicMuted = true;
            toggleMuteBGM.setIcon(IconIndex.MUTED);
        } else if (musicSlider.getValue() > 0f && musicMuted) {
            musicMuted = false;
            toggleMuteBGM.setIcon(IconIndex.UNMUTED);
        }
    }

    private void updateSfxSlider(InputManager input) {
        int state = sfxSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            itemFocado = encontrarItemFoco(sfxSlider);
            if (state == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            soundManager.setSfxVolume(sfxSlider.getValue());
        }

        if (sfxSlider.getValue() <= 0f && !sfxMuted) {
            sfxMuted = true;
            toggleMuteSFX.setIcon(IconIndex.MUTED);
        } else if (sfxSlider.getValue() > 0f && sfxMuted) {
            sfxMuted = false;
            toggleMuteSFX.setIcon(IconIndex.UNMUTED);
        }
    }

    private void updateFpsCapSlider(InputManager input, GameCore GC) {
        int state = fpsCapSlider.update(input);
        if (state == MenuSlider.DRAGGING || state == MenuSlider.CLICKED) {
            itemFocado = encontrarItemFoco(fpsCapSlider);
            if (state == MenuSlider.CLICKED)
                soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            definirLimiteFps(fpsFromSliderValue(fpsCapSlider.getValue()), GC);
        }
    }

    private int fpsFromSliderValue(float value) {
        return Math.round(MIN_FPS + value * (MAX_FPS - MIN_FPS));
    }

    private float sliderValueFromFps(int fps) {
        return Math.clamp((fps - MIN_FPS) / (float) (MAX_FPS - MIN_FPS), 0f, 1f);
    }

    private void atualizarNavegacaoControle(InputManager input, GameCore GC) {
        boolean up = input.isKeyJustPressed(KeyEvent.VK_UP)
                || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_UP);
        boolean down = input.isKeyJustPressed(KeyEvent.VK_DOWN)
                || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_DOWN);
        boolean left = input.isKeyJustPressed(KeyEvent.VK_LEFT)
                || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_LEFT);
        boolean right = input.isKeyJustPressed(KeyEvent.VK_RIGHT)
                || input.isButtonJustPressed(InputManager.GamepadButton.DPAD_RIGHT);

        if (up || down) {
            moverVerticalmente(up ? -1 : 1);
            input.iniciarBloqueioMouse();
        } else if (left || right) {
            if (itemFocado.deslizador != null) {
                ajustarDeslizadorFocado(left ? -0.05f : 0.05f, GC);
            } else {
                moverHorizontalmente(right ? 1 : -1);
            }
            input.iniciarBloqueioMouse();
        }
    }

    private void moverVerticalmente(int direction) {
        Rectangle atual = itemFocado.limites();
        ItemFoco candidato = null;
        int menorDistancia = Integer.MAX_VALUE;
        for (ItemFoco item : itensFoco) {
            if (item == itemFocado) {
                continue;
            }
            Rectangle limites = item.limites();
            int deltaY = limites.y - atual.y;
            if (Integer.signum(deltaY) != direction) {
                continue;
            }
            int distancia = Math.abs(deltaY) * 100 + Math.abs(limites.x - atual.x);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                candidato = item;
            }
        }
        if (candidato != null) {
            itemFocado = candidato;
        }
    }

    private void moverHorizontalmente(int direction) {
        Rectangle atual = itemFocado.limites();
        ItemFoco candidato = null;
        int menorDistancia = Integer.MAX_VALUE;
        for (ItemFoco item : itensFoco) {
            if (item == itemFocado) {
                continue;
            }
            Rectangle limites = item.limites();
            int deltaX = limites.x - atual.x;
            if (Integer.signum(deltaX) != direction
                    || Math.abs(limites.y - atual.y) > Math.max(atual.height, limites.height)) {
                continue;
            }
            if (Math.abs(deltaX) < menorDistancia) {
                menorDistancia = Math.abs(deltaX);
                candidato = item;
            }
        }
        if (candidato != null) {
            itemFocado = candidato;
        }
    }

    private void ajustarDeslizadorFocado(float variacao, GameCore GC) {
        MenuSlider slider = itemFocado.deslizador;
        if (slider == fpsCapSlider) {
            ajustarLimiteFps(variacao, GC);
            return;
        }

        float valorAnterior = slider.getValue();
        float valor = Math.clamp(valorAnterior + variacao, 0f, 1f);
        if (valor == valorAnterior && moverParaIrmaoHorizontal(variacao > 0 ? 1 : -1)) {
            return;
        }
        slider.setValue(valor);
        if (slider == musicSlider) {
            soundManager.setMusicVolume(valor);
        } else if (slider == sfxSlider) {
            soundManager.setSfxVolume(valor);
        } else {
            GC.setTargetFps(fpsFromSliderValue(valor));
        }
    }

    private void ajustarLimiteFps(float variacao, GameCore GC) {
        int fpsAtual = fpsFromSliderValue(fpsCapSlider.getValue());
        int passo = variacao > 0f ? 5 : -5;
        int novoFps = Math.clamp(fpsAtual + passo, MIN_FPS, MAX_FPS);
        fpsCapSlider.setValue(sliderValueFromFps(novoFps));
        definirLimiteFps(novoFps, GC);
    }

    private void definirLimiteFps(int limite, GameCore GC) {
        previousFpsLimit = limite;
        fpsUnlimited = false;
        GC.setTargetFps(limite);
        toggleUnlimitedFps.setIcon(IconIndex.UNLIM_FPS_OFF);
    }

    private void alternarFpsIlimitado(GameCore GC) {
        if (!fpsUnlimited) {
            previousFpsLimit = Math.clamp(fpsFromSliderValue(fpsCapSlider.getValue()), MIN_FPS, MAX_FPS);
            fpsUnlimited = true;
            GC.setTargetFps(0);
            toggleUnlimitedFps.setIcon(IconIndex.UNLIM_FPS_ON);
        } else {
            fpsUnlimited = false;
            fpsCapSlider.setValue(sliderValueFromFps(previousFpsLimit));
            GC.setTargetFps(previousFpsLimit);
            toggleUnlimitedFps.setIcon(IconIndex.UNLIM_FPS_OFF);
        }
    }

    private void atualizarIconesFps(GameCore GC) {
        fpsUnlimited = GC.getTargetFps() == 0;
        toggleUnlimitedFps.setIcon(fpsUnlimited ? IconIndex.UNLIM_FPS_ON : IconIndex.UNLIM_FPS_OFF);
    }

    private boolean moverParaIrmaoHorizontal(int direcao) {
        Rectangle atual = itemFocado.limites();
        ItemFoco candidato = null;
        int menorDistancia = Integer.MAX_VALUE;
        for (ItemFoco item : itensFoco) {
            if (item == itemFocado) {
                continue;
            }
            Rectangle limites = item.limites();
            int deltaX = limites.x - atual.x;
            if (Integer.signum(deltaX) != direcao
                    || Math.abs(limites.y - atual.y) > Math.max(atual.height, limites.height)) {
                continue;
            }
            if (Math.abs(deltaX) < menorDistancia) {
                menorDistancia = Math.abs(deltaX);
                candidato = item;
            }
        }
        if (candidato == null) {
            return false;
        }
        itemFocado = candidato;
        return true;
    }

    private GameState ativarItemFocado(InputManager input, GameCore GC) {
        if (!input.isButtonJustPressed(InputManager.GamepadButton.A)) {
            return GameState.OPTIONS;
        }
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        return itemFocado.acao.apply(GC);
    }

    private ItemFoco encontrarItemFoco(Object controle) {
        for (ItemFoco item : itensFoco) {
            if (item.deslizador == controle || item.botao == controle) {
                return item;
            }
        }
        return itemFocado;
    }

    private boolean estaFocado(Object controle) {
        return itemFocado != null
                && (itemFocado.deslizador == controle || itemFocado.botao == controle);
    }

    private void toggleMusicMute() {
        if (!musicMuted) {
            previousMusicVolume = soundManager.getMusicVolume();
            soundManager.setMusicVolume(0f);
            musicSlider.setValue(0f);
            toggleMuteBGM.setIcon(IconIndex.MUTED);
            musicMuted = true;
        } else {
            soundManager.setMusicVolume(previousMusicVolume);
            musicSlider.setValue(previousMusicVolume);
            toggleMuteBGM.setIcon(IconIndex.UNMUTED);
            musicMuted = false;
        }
    }

    private void toggleSfxMute() {
        if (!sfxMuted) {
            previousSfxVolume = soundManager.getSfxVolume();
            soundManager.setSfxVolume(0f);
            sfxSlider.setValue(0f);
            toggleMuteSFX.setIcon(IconIndex.MUTED);
            sfxMuted = true;
        } else {
            soundManager.setSfxVolume(previousSfxVolume);
            sfxSlider.setValue(previousSfxVolume);
            toggleMuteSFX.setIcon(IconIndex.UNMUTED);
            sfxMuted = false;
        }
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(10, 10, 10));
        g2.fillRect(0, 0, width, height);

        g2.setFont(pixelFont);
        String title = "OPÇÕES";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, height / TITLE_Y_FRACTION + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, height / TITLE_Y_FRACTION);

        drawSliderRow(g2, "VOLUME DA MÚSICA", musicSlider, estaFocado(musicSlider), width, true, true);
        drawSliderRow(g2, "VOLUME DOS EFEITOS", sfxSlider, estaFocado(sfxSlider), width, true, true);
        drawSliderRow(g2, rotuloLimiteFps(),
                fpsCapSlider, estaFocado(fpsCapSlider), width, true, false);

        drawLabelLeftOf(g2, "RENDERIZAR SOMBRAS", renderShadowsButton.getRect());
        drawLabelLeftOf(g2, "Habilitar Anti-Aliasing", enableAAButton.getRect());
        drawLabelLeftOf(g2, "MOSTRAR FPS", showFpsButton.getRect());
        renderShadowsButton.draw(g2);
        enableAAButton.draw(g2);
        toggleMuteBGM.draw(g2);
        toggleMuteSFX.draw(g2);
        toggleUnlimitedFps.draw(g2);
        showFpsButton.draw(g2);
        keyBindBtn.draw(g2);
        backBtn.draw(g2);
        fullScreenButton.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void drawSliderRow(Graphics2D g2, String label, MenuSlider slider, boolean highlighted,
            int width, boolean hasButton, boolean drawPercentage) {
        Rectangle r = slider.getRect();

        g2.setFont(pixelFontSmall);
        g2.setColor(highlighted ? Color.WHITE : new Color(180, 180, 180));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, (width - fm.stringWidth(label)) / 2, r.y - LABEL_TO_SLIDER_GAP + fm.getAscent());

        slider.draw(g2);
        if (drawPercentage) {
            g2.setFont(pixelFontTiny);
            g2.setColor(highlighted ? Color.WHITE : new Color(160, 160, 160));
            String pct = (int) (slider.getValue() * 100) + "%";
            int pctX = r.x + r.width + SLIDER_TO_BTN_GAP + (hasButton ? BTN_SIZE + BTN_TO_PCT_GAP : BTN_TO_PCT_GAP);
            g2.drawString(pct, pctX, r.y + r.height);
        }
    }

    private String rotuloLimiteFps() {
        return fpsUnlimited
                ? "LIMITE DE FPS: ILIMITADO"
                : "LIMITE DE FPS: " + fpsFromSliderValue(fpsCapSlider.getValue());
    }

    private void drawLabelLeftOf(Graphics2D g2, String text, Rectangle anchorRect) {
        g2.setFont(pixelFontSmall);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int x = anchorRect.x - tw - SLIDER_TO_BTN_GAP;
        int y = anchorRect.y + (anchorRect.height + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(text, x + 2, y + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }
}
