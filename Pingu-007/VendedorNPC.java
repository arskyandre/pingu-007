import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;

public class VendedorNPC extends NPC {

    private enum State {
        IDLE, TALKING
    }

    private State state = State.IDLE;
    private final ShopMenu shopMenu;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;
    private BufferedImage Sprite;
    private CameraManager camera;

    public VendedorNPC(double x, double y, CameraManager cameraMgr, SoundManager soundManager) {
        super(x, y, WIDTH, HEIGHT);
        INTERACT_RANGE = GameCore.tiles_size * 4;
        camera = cameraMgr;
        Sprite = LoadSave.GetSpriteAtlas("images/npc/vendedor.png");

        shopMenu = new ShopMenu(soundManager);
    }

    private void popularItens(Player player) {
        shopMenu.limparItens();

        shopMenu.addItem("10 Balas", "Está sem munição? Você pode comprar balas para a sua jornada aqui!",
                GameCore.missing_image, 10, () -> player.addMunicao(10));
        shopMenu.addItem("Peixe", "Um delicioso peixe para curar um coração.", GameCore.missing_image, 15,
                () -> player.curar(10));
        shopMenu.addItem("Recarga Rápida",
                "Reduz o tempo necessário para recarregar a arma.",
                GameCore.missing_image, 50, () -> {
                    /* reduz tempo de recarga */ });
        shopMenu.addItem("Pente Estendido",
                "Aumenta a capacidade do pente da sua arma.",
                GameCore.missing_image, 75, () -> {
                    /* aumenta pente máximo */ });
        shopMenu.addItem("Espingarda (Shotgun)",
                "Uma espingarda que dispara diversos projéteis de uma só vez, causando alto dano a curta distância. Sua eficiência diminui conforme a distância aumenta.",
                GameCore.missing_image, 100, () -> player.setHasShotgun(true));
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, SoundManager soundManager, ItemManager itemManager) {
        proximo = playerNearby(player);
        System.out.println(proximo);
        switch (state) {
            case IDLE -> {
                if (proximo && input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)) {
                    if (Player.getDesbloqueouRecompensa()) {
                        dialogueManager.iniciarDialogo(new String[] {
                                "VENDEDOR: E aí, Pingu? Deseja comprar algo?"

                        }, new SoundManager.SFX[][] { DialogueCatalogo.pingu_noot, DialogueCatalogo.pingu_noot }, true);
                        dialogueManager.setAoTerminarDialogo(() -> {
                            popularItens(player);
                            shopMenu.setAoFechar(() -> {
                                dialogueManager.iniciarDialogo(new String[] {
                                        "VENDEDOR: Estou aqui sempre que precisar!"
                                }, active);
                            });
                            shopMenu.abrir(player);
                            GameCore.setShopMenu(shopMenu);
                            state = State.IDLE;
                        });
                    } else {
                        dialogueManager.iniciarDialogo(new String[] {
                                "VENDEDOR: E aí pingu, beleza?",
                                "VENDEDOR: Obrigado por salvar o nosso bairro, os soldados da Morsa estavam aterrorizando a nossa região!",
                                "VENDEDOR: Como agradecimento, quero lhe oferecer uma recompensa. A partir de agora, vou te pagar em moedas pelos inimigos que você eliminar!",
                        }, active);
                        dialogueManager.setAoTerminarDialogo(() -> {
                            Player.setDesbloqueouRecompensa(true);
                            state = State.IDLE;
                        });
                    }
                    state = State.TALKING;
                }
            }

            case TALKING -> {
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
