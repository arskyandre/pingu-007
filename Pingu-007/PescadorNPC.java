
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
    private String[] dialogo1;
    private String[] dialogo2_noKey;
    private String[] dialogo2_hasKey;
    private BufferedImage Sprite;

    public PescadorNPC(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
        dialogo1 = new String[] {
                "PESCADOR: Ei, agente! Vejo que você ainda não tem uma vara de pesca.",
                "PESCADOR: Ainda bem que tenho uma sobrando. Pode ficar com ela!",
                "PESCADOR: Sabe como pescar? É fácil, e só chegar perto de um buraco de água e apertar E para lançar a linha.",
                "PESCADOR: Ouvi rumores de que um buraco de pesca por aí esconde um tesouro secreto! Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho.",
                "PESCADOR: Se encontrar, tente pescar lá!" };
        dialogo2_noKey = new String[] {
                "PESCADOR: Esqueceu como pescar? É fácil, é só chegar perto de um buraco de água e apertar E para lançar a linha.",
                "PESCADOR: Ouvi rumores de que um buraco de pesca por aí esconde um tesouro secreto! Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho.",
                "PESCADOR: Se encontrar, tente pescar lá!" };
        dialogo2_hasKey = new String[] {
                "PESCADOR: Quer dizer que você encontrou o tesouro no buraco de pesca? O que era?",
                "PESCADOR: Uma chave para o portão da Morsa? Uau, talvez você seja bom o suficiente para derrotá-la!",
                "PESCADOR: Boa jornada, agente!" };
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
                        if (FishingManager.isPlayerHasKey())
                            dialogueManager.iniciarDialogo(dialogo2_hasKey, DialogueSounds.PescadorFala2_hasKey, true);
                        else
                            dialogueManager.iniciarDialogo(dialogo2_noKey, DialogueSounds.PescadorFala2_noKey, true);
                        laEle = true;
                    } else {
                        dialogueManager.iniciarDialogo(dialogo1, DialogueSounds.PescadorFala1, true);
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
            y -= 80.0 * delta;
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
