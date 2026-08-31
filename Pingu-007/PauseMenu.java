import java.awt.*;
import java.io.File;

public class PauseMenu {

    private final MenuButton resumeBtn;
    private final MenuButton optionsBtn;
    private final MenuButton mainMenuBtn;

    private final SoundManager soundManager;

    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;
    private static final int RESUME_INDEX = 0;
    private static final int OPTIONS_INDEX = 1;
    private static final int MAIN_MENU_INDEX = 2;

    private Font pixelFont;
    private int selectedButton = RESUME_INDEX;

    public PauseMenu(SoundManager sound) {
        soundManager = sound;
        resumeBtn = new MenuButton("RESUMIR", 0, 0, BTN_W, BTN_H);
        optionsBtn = new MenuButton("OPÇÕES", 0, 0, BTN_W, BTN_H);
        mainMenuBtn = new MenuButton("VOLTAR AO MENU PRINCIPAL", 0, 0, BTN_W, BTN_H);

        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 24f);
        } catch (Exception e) {
            System.err.println("fonte nao encontrada nos arquivos.");
            pixelFont = new Font("Monospaced", Font.BOLD, 24);
        }
    }

    private void repositionButtons(int width, int height) {
        int x = (width - BTN_W) / 2;
        int y = height / 2 - 20;
        resumeBtn.setPosition(x, y);
        optionsBtn.setPosition(x, y + BTN_H + BTN_GAP);
        mainMenuBtn.setPosition(x, y + (BTN_H + BTN_GAP) * 2);
    }

    public GameState update(InputManager input, int width, int height) {
        repositionButtons(width, height);
        MenuInputController menuInput = new MenuInputController(input);

        if (menuInput.wasPressed(InputAction.CANCEL))
            return GameState.PLAYING;

        boolean controleAtivo = menuInput.usesController();

        if (menuInput.navigate(InputAction.MENU_UP)) {
            moverSelecao(-1);
        } else if (menuInput.navigate(InputAction.MENU_DOWN)) {
            moverSelecao(1);
        }

        PointerSnapshot pointer = menuInput.pointer();
        boolean mouseAceito = pointer.isActive();

        int resumeState;
        int optionsState;
        int mainMenuState;

        if (mouseAceito) {
            resumeState = resumeBtn.update(pointer);
            optionsState = optionsBtn.update(pointer);
            mainMenuState = mainMenuBtn.update(pointer);
        } else {
            resumeBtn.hovered = false;
            optionsBtn.hovered = false;
            mainMenuBtn.hovered = false;
            resumeState = MenuButton.IDLE;
            optionsState = MenuButton.IDLE;
            mainMenuState = MenuButton.IDLE;
        }

        boolean mouseEstaSobreBotao = false;
        if (mouseAceito) {
            if (resumeBtn.isHovered()) {
                selectedButton = RESUME_INDEX;
                mouseEstaSobreBotao = true;
            } else if (optionsBtn.isHovered()) {
                selectedButton = OPTIONS_INDEX;
                mouseEstaSobreBotao = true;
            } else if (mainMenuBtn.isHovered()) {
                selectedButton = MAIN_MENU_INDEX;
                mouseEstaSobreBotao = true;
            }
        }

        if ((controleAtivo || input.isMouseBloqueado()) && !mouseEstaSobreBotao) {
            aplicarSelecaoVisual();
        }

        if (menuInput.wasPressed(InputAction.CONFIRM)) {
            return ativarSelecionado();
        }

        if (resumeState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.PLAYING;
        }
        if (optionsState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.OPTIONS;
        }
        if (mainMenuState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.MAIN_MENU;
        }

        return GameState.PAUSED;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // dim game behind
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, height);

        // panel
        int panelW = 320, panelH = 360;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        g2.setColor(new Color(10, 10, 10, 220));
        g2.fillRect(px, py, panelW, panelH);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(px, py, panelW, panelH);

        // title
        g2.setFont(pixelFont);
        String title = "PAUSADO";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, py + 55 + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(title, (width - tw) / 2, py + 55);

        // buttons
        resumeBtn.draw(g2);
        optionsBtn.draw(g2);
        mainMenuBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void moverSelecao(int direcao) {
        selectedButton += direcao;
        if (selectedButton < RESUME_INDEX) {
            selectedButton = MAIN_MENU_INDEX;
        } else if (selectedButton > MAIN_MENU_INDEX) {
            selectedButton = RESUME_INDEX;
        }
    }

    private void aplicarSelecaoVisual() {
        resumeBtn.hovered = selectedButton == RESUME_INDEX;
        optionsBtn.hovered = selectedButton == OPTIONS_INDEX;
        mainMenuBtn.hovered = selectedButton == MAIN_MENU_INDEX;
    }

    private GameState ativarSelecionado() {
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        return switch (selectedButton) {
            case RESUME_INDEX -> GameState.PLAYING;
            case OPTIONS_INDEX -> GameState.OPTIONS;
            case MAIN_MENU_INDEX -> GameState.MAIN_MENU;
            default -> GameState.PAUSED;
        };
    }
}
