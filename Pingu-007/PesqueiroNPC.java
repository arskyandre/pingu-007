import java.awt.Color;
import java.awt.Graphics2D;

public class PesqueiroNPC extends NPC {

    private enum State {
        IDLE, TALKING
    }

    private State state = State.IDLE;
    private boolean dialogueStarted = false;
    private boolean laEle = false;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;

    public PesqueiroNPC(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, ItemManager itemManager) {
        if (playerNearby(player))
            proximo = true;
        else
            proximo = false;
        switch (state) {
            case IDLE -> {
                if (proximo && input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)) {
                    if (laEle)
                        dialogueManager.iniciarDialogo(
                                new String[] {
                                        "PESQUEIRO: Esqueceu como pescar? É fácil,",
                                        "chegue perto de um buraco de água e aperte E para lançar a linha." });
                    else
                        dialogueManager.iniciarDialogo(
                                new String[] {
                                        "PESQUEIRO: Ei, agente! Vejo que você ainda não tem uma vara de pesca.",
                                        "PESQUEIRO: Ainda bem que tenho uma sobrando. Pode ficar com ela!",
                                        "PESQUEIRO: Sabe como pescar? É fácil,",
                                        "chegue perto de um buraco de água e aperte E para lançar a linha." });
                    state = State.TALKING;
                }
            }
            case TALKING -> {

                if (!dialogueManager.isAtivo()) {
                    if (!laEle) {

                        itemManager.spawn(new FishingRodItem(x + largura / 2.0, y + altura + 16));
                        laEle = true;
                    }
                    state = State.IDLE;
                }

            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!active)
            return;

        g2.setColor(new Color(20, 77, 55));
        g2.fillRect((int) x, (int) y, (int) largura, (int) altura);

        if (state == State.IDLE && proximo) {
            g2.setFont(MenuButton.pixelFont.deriveFont(7f));
            g2.setColor(new Color(20, 77, 55));
            String prompt = "[E]";
            int pw = g2.getFontMetrics().stringWidth(prompt);
            g2.drawString(prompt, (int) (x + largura / 2.0 - pw / 2.0), (int) (y - 8));
        }
    }
}