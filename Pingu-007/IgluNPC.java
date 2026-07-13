
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class IgluNPC extends NPC {

    private enum State {
        IDLE, TALKING, SELLING
    }

    private State state = State.IDLE;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;

    private BufferedImage Sprite;
    private String[] falas = new String[] {
            "INSTRUTOR: Agente Pingu! Finalmente você chegou.",
            "INSTRUTOR: A Morsa tomou conta desta região. Sua missão é atravessar o complexo e derrotá-la.",
            "INSTRUTOR: Mas antes, você vai precisar encontrar as 9 chaves. Só elas abrem o portão da Morsa.",
            "INSTRUTOR: Use WASD para se mover, ESPAÇO para dar um dash e o botão esquerdo do mouse para atirar.",
            "INSTRUTOR: Não desperdice munição. Pressione R para recarregar sempre que estiver em segurança.",
            "INSTRUTOR: Se uma arena fechar atrás de você, elimine todos os inimigos. A porta abre quando o último cair.",
            "INSTRUTOR: Está vendo aqueles buracos na água? Dá para pescar neles... mas você ainda está sem uma vara(la ele pro max 9000).",
            "INSTRUTOR: Dizem que tem um pescador por aí. Se encontrar o sujeito, talvez ele possa te ajudar.",
            "INSTRUTOR: Agora vá, agente. O destino da colônia está em suas asas!"
    };

    public IgluNPC(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
        Sprite = LoadSave.GetSpriteAtlas("igluNPC.png");
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, ItemManager itemManager) {
        proximo = playerNearby(player);
        switch (state) {
            case IDLE -> {
                if (proximo && input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)) {
                    dialogueManager.iniciarDialogo(falas, DialogueSounds.InstrutorFalas, true);
                    state = State.TALKING;
                }
            }
            case TALKING -> {
                if (!dialogueManager.isAtivo()) {
                    state = State.IDLE;
                }
            }
            case SELLING -> {
                if (!dialogueManager.isAtivo()) {

                }
            }
        }
    }

    @Override
    public void draw(Graphics2D g2, double delta) {

        if (!active) {
            return;
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
