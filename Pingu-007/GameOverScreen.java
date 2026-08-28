import java.awt.*;
import java.io.File;

public class GameOverScreen {

    private final MenuButton respawnBtn;
    private final MenuButton mainMenuBtn;

    private final SoundManager soundManager;

    private static final int BTN_W = 220;
    private static final int BTN_H = 46;
    private static final int BTN_GAP = 18;
    private static final int RESPAWN_INDEX = 0;
    private static final int MAIN_MENU_INDEX = 1;

    private Font pixelFont;
    private int selectedButton = RESPAWN_INDEX;

    public GameOverScreen(SoundManager sound) {
        soundManager = sound;
        respawnBtn = new MenuButton("RENASCER NO CHECKPOINT", 0, 0, BTN_W, BTN_H);
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
        respawnBtn.setPosition(x, y);
        mainMenuBtn.setPosition(x, y + (BTN_H + BTN_GAP));
    }

    public GameState update(InputManager input, int width, int height) {
        repositionButtons(width, height);

        boolean controleAtivo = input.isControllerActive();

        if (input.isButtonJustPressed(InputManager.GamepadButton.DPAD_UP)) {
            moverSelecao(-1);
            input.iniciarBloqueioMouse();
        } else if (input.isButtonJustPressed(InputManager.GamepadButton.DPAD_DOWN)) {
            moverSelecao(1);
            input.iniciarBloqueioMouse();
        }

        boolean mouseAceito = !input.isMouseBloqueado();

        int respawnState;
        int mainMenuState;

        if (mouseAceito) {
            respawnState = respawnBtn.update(input);
            mainMenuState = mainMenuBtn.update(input);
        } else {
            respawnBtn.hovered = false;
            mainMenuBtn.hovered = false;
            respawnState = MenuButton.IDLE;
            mainMenuState = MenuButton.IDLE;
        }

        boolean mouseEstaSobreBotao = false;
        if (mouseAceito) {
            if (respawnBtn.isHovered()) {
                selectedButton = RESPAWN_INDEX;
                mouseEstaSobreBotao = true;
            } else if (mainMenuBtn.isHovered()) {
                selectedButton = MAIN_MENU_INDEX;
                mouseEstaSobreBotao = true;
            }
        }

        if ((controleAtivo || input.isMouseBloqueado()) && !mouseEstaSobreBotao) {
            aplicarSelecaoVisual();
        }

        if (input.isButtonJustPressed(InputManager.GamepadButton.A)) {
            return ativarSelecionado();
        }

        if (respawnState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.PLAYING;
        }
        if (mainMenuState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.MAIN_MENU;
        }

        return GameState.GAME_OVER;
    }

    public void render(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, height);

        int panelW = 320, panelH = 296;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        g2.setColor(new Color(10, 10, 10, 220));
        g2.fillRect(px, py, panelW, panelH);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(px, py, panelW, panelH);

        g2.setFont(pixelFont);
        String title = "GAME OVER";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, (width - tw) / 2 + 2, py + 55 + 2);
        g2.setColor(Color.RED);
        g2.drawString(title, (width - tw) / 2, py + 55);

        respawnBtn.draw(g2);
        mainMenuBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void moverSelecao(int direcao) {
        selectedButton += direcao;
        if (selectedButton < RESPAWN_INDEX) {
            selectedButton = MAIN_MENU_INDEX;
        } else if (selectedButton > MAIN_MENU_INDEX) {
            selectedButton = RESPAWN_INDEX;
        }
    }

    private void aplicarSelecaoVisual() {
        respawnBtn.hovered = selectedButton == RESPAWN_INDEX;
        mainMenuBtn.hovered = selectedButton == MAIN_MENU_INDEX;
    }

    private GameState ativarSelecionado() {
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        return switch (selectedButton) {
            case RESPAWN_INDEX -> GameState.PLAYING;
            case MAIN_MENU_INDEX -> GameState.MAIN_MENU;
            default -> GameState.GAME_OVER;
        };
    }
}
