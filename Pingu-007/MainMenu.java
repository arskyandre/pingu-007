import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MainMenu {

    private final MenuButton playBtn;
    private final MenuButton optionsBtn;
    private final MenuButton quitBtn;

    private final SoundManager soundManager;

    private BufferedImage background;
    private final MainMenuEyes eyes = new MainMenuEyes();
    private Font pixelFont;

    private static final int BTN_W = 280;
    private static final int BTN_H = 52;
    private static final int BTN_GAP = 18;
    // A arte deixa livre a coluna da direita, centralizada em 79% da largura.
    private static final double MENU_CENTER_X = 0.79;

    private static final int PLAY_INDEX = 0;
    private static final int OPTIONS_INDEX = 1;
    private static final int QUIT_INDEX = 2;

    private double bobTime = 0;
    private static final double BOB_SPEED = 0.125;
    private static final double BOB_AMP = 4.0;

    private int selectedButton = PLAY_INDEX;

    public MainMenu(SoundManager sound) {
        soundManager = sound;

        playBtn = new MenuButton("JOGAR", 0, 0, BTN_W, BTN_H);
        optionsBtn = new MenuButton("OPÇÕES", 0, 0, BTN_W, BTN_H);
        quitBtn = new MenuButton("SAIR DO JOGO", 0, 0, BTN_W,
                BTN_H);

        try {
            background = ImageIO.read(new File("images/hud/menu_background.png"));
        } catch (Exception e) {
            System.err.println("menu_background.png not found");
        }
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.PLAIN, 32f);
        } catch (Exception e) {
            System.err.println("PressStart2P.ttf not found, falling back to Monospaced");
            pixelFont = new Font("Monospaced", Font.BOLD, 32);
        }
    }

    private void repositionButtons(int width, int height) {
        int buttonWidth = Math.max(180, Math.min(BTN_W, (int) (width * 0.26)));
        int buttonHeight = Math.max(40, Math.min(BTN_H, (int) (height * 0.08)));
        int gap = Math.max(12, Math.min(BTN_GAP, (int) (height * 0.027)));
        int x = (int) (width * MENU_CENTER_X) - buttonWidth / 2;
        int y = (int) (height * 0.44);
        playBtn.setSize(buttonWidth, buttonHeight);
        optionsBtn.setSize(buttonWidth, buttonHeight);
        quitBtn.setSize(buttonWidth, buttonHeight);
        playBtn.setPosition(x, y);
        optionsBtn.setPosition(x, y + buttonHeight + gap);
        quitBtn.setPosition(x, y + (buttonHeight + gap) * 2);
    }

    public GameState update(InputManager input, int width, int height,
            InputManager.MouseSpace mouseSpace) {
        repositionButtons(width, height);
        bobTime += BOB_SPEED;
        boolean controleAtivo = input.isControllerActive();

        if (input.isButtonJustPressed(InputManager.GamepadButton.DPAD_UP)) {
            moverSelecao(-1);
            input.iniciarBloqueioMouse();
        } else if (input.isButtonJustPressed(InputManager.GamepadButton.DPAD_DOWN)) {
            moverSelecao(1);
            input.iniciarBloqueioMouse();
        }

        boolean mouseAceito = !input.isMouseBloqueado();

        int playState;
        int optionsState;
        int quitState;

        if (mouseAceito) {
            eyes.update(input.getMouseX(), input.getMouseY(), width, height);
            playState = playBtn.update(input, mouseSpace);
            optionsState = optionsBtn.update(input, mouseSpace);
            quitState = quitBtn.update(input, mouseSpace);
        } else {
            playBtn.hovered = false;
            optionsBtn.hovered = false;
            quitBtn.hovered = false;
            playState = MenuButton.IDLE;
            optionsState = MenuButton.IDLE;
            quitState = MenuButton.IDLE;
        }

        boolean mouseEstaSobreBotao = false;
        if (mouseAceito) {
            if (playBtn.isHovered()) {
                selectedButton = PLAY_INDEX;
                mouseEstaSobreBotao = true;
            } else if (optionsBtn.isHovered()) {
                selectedButton = OPTIONS_INDEX;
                mouseEstaSobreBotao = true;
            } else if (quitBtn.isHovered()) {
                selectedButton = QUIT_INDEX;
                mouseEstaSobreBotao = true;
            }
        }

        if ((controleAtivo || input.isMouseBloqueado()) && !mouseEstaSobreBotao) {
            aplicarSelecaoVisual();
        }

        if (input.isButtonJustPressed(InputManager.GamepadButton.A)) {
            return ativarSelecionado();
        }

        if (playState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.PLAYING;
        }
        if (optionsState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.OPTIONS;
        }
        if (quitState == MenuButton.CLICKED) {
            soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
            return GameState.QUIT;
        }

        return GameState.MAIN_MENU;
    }

    public void render(Graphics2D g2, int width, int height) {
        repositionButtons(width, height);
        if (background != null) {
            g2.drawImage(background, 0, 0, width, height, null);
            eyes.render(g2, width, height);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, width, height);
        }

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(0, 0, width, height);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        float titleSize = Math.min(32f, width * 0.034f);
        g2.setFont(pixelFont.deriveFont(Font.PLAIN, titleSize));
        String title = "PINGU 007";
        int tw = g2.getFontMetrics().stringWidth(title);
        int centerX = (int) (width * MENU_CENTER_X);
        int titleX = centerX - tw / 2;
        int titleY = (int) (height * 0.31);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, titleX + 3, titleY + 3);
        g2.setColor(Color.WHITE);
        g2.drawString(title, titleX, titleY);

        g2.setFont(pixelFont.deriveFont(Font.PLAIN, 12f));
        String fscreen = "F11: tela cheia";
        tw = g2.getFontMetrics().stringWidth(fscreen);
        int textX = centerX - tw / 2;
        int bobOffset = (int) (Math.sin(bobTime) * BOB_AMP);
        int textY = height - 24 + bobOffset;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(fscreen, textX + 3, textY + 3);
        g2.setColor(Color.GRAY);
        g2.drawString(fscreen, textX, textY);

        playBtn.draw(g2);
        optionsBtn.draw(g2);
        quitBtn.draw(g2);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private void moverSelecao(int direcao) {
        selectedButton += direcao;
        if (selectedButton < PLAY_INDEX) {
            selectedButton = QUIT_INDEX;
        } else if (selectedButton > QUIT_INDEX) {
            selectedButton = PLAY_INDEX;
        }
    }

    private void aplicarSelecaoVisual() {
        playBtn.hovered = selectedButton == PLAY_INDEX;
        optionsBtn.hovered = selectedButton == OPTIONS_INDEX;
        quitBtn.hovered = selectedButton == QUIT_INDEX;
    }
    private GameState ativarSelecionado() {
        soundManager.playSFX(SoundManager.SFX.HUD_CLICK);
        return switch (selectedButton) {
            case PLAY_INDEX -> GameState.PLAYING;
            case OPTIONS_INDEX -> GameState.OPTIONS;
            case QUIT_INDEX -> GameState.QUIT;
            default -> GameState.MAIN_MENU;
        };
    }
}
