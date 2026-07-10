
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PescadorNPC extends NPC {

    private enum State {
        IDLE, TALKING
    }

    private State state = State.IDLE;
    private boolean laEle = false;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;
    private double Yfinal;

    private BufferedImage Sprite;

    public PescadorNPC(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
        Yfinal = y;
        this.y = Yfinal + 40;
        Sprite = LoadSave.GetSpriteAtlas("pescador.png");
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, ItemManager itemManager) {
        if (Yfinal != y) {
            return;
        }
        proximo = playerNearby(player);
        switch (state) {
            case IDLE -> {
                if (proximo && input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)) {
                    if (laEle || player.hasFishingRod()) {
                        dialogueManager.iniciarDialogo(
                                new String[]{
                                    "PESCADOR: Esqueceu como pescar? É fácil, é só chegar perto de um buraco de água e apertar E para lançar a linha."});
                        laEle = true;
                    } else {
                        dialogueManager.iniciarDialogo(
                                new String[]{
                                    "PESCADOR: Ei, agente! Vejo que você ainda não tem uma vara de pesca.",
                                    "PESCADOR: Ainda bem que tenho uma sobrando. Pode ficar com ela!",
                                    "PESCADOR: Sabe como pescar? É fácil, e só chegar perto de um buraco de água e apertar E para lançar a linha."});
                    }
                    state = State.TALKING;
                }
            }
            case TALKING -> {
                if (!dialogueManager.isAtivo()) {
                    if (!laEle && !player.hasFishingRod()) {
                        itemManager.spawn(new FishingRodItem(x + (largura / 2.0), y + altura + 40));
                        laEle = true;
                    }
                    state = State.IDLE;
                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g2, double delta) {

        if (!active) {
            return;
        }
        if (y > Yfinal) {
            y -= 10.0 * delta;
            if (y <= Yfinal) {
                y = Yfinal;
            }
        }
        g2.drawImage(Sprite, (int) x, (int) y, (int) WIDTH, (int) HEIGHT, null);
        if (state == State.IDLE && proximo) {
            g2.setFont(MenuButton.pixelFont.deriveFont(7f));
            g2.setColor(new Color(20, 77, 55));
            String prompt = "[E]";
            int pw = g2.getFontMetrics().stringWidth(prompt);
            g2.drawString(prompt, (int) (x + largura / 2.0 - pw / 2.0), (int) (y - 8));
        }
    }
}
