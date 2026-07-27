import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;

public class VendedorNPC extends NPC {

    private enum State {
        IDLE, TALKING, SHOP
    }

    private State state = State.IDLE;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;
    private BufferedImage Sprite;
    private CameraManager camera;

    public VendedorNPC(double x, double y, CameraManager cameraMgr) {
        super(x, y, WIDTH, HEIGHT);
        INTERACT_RANGE = GameCore.tiles_size * 4;
        camera = cameraMgr;
        Sprite = LoadSave.GetSpriteAtlas("images/npc/vendedor.png");
    }

    private void loopCompra(DialogueManager dialogueManager, Player player) {
        loopCompra(dialogueManager, player, "Deseja comprar mais iscas?", 2);
    }

    private void loopCompra(DialogueManager dialogueManager, Player player, String pergunta, int index) {
        dialogueManager.iniciarEscolha(
                pergunta,
                new String[] {
                        "Comprar 5 Iscas: 10 moedas",
                        "Comprar 10 Iscas: 20 moedas",
                        "Não"
                },
                GameCore.pescador_portrait, index,
                escolha -> {
                    switch (escolha) {
                        case 2 -> dialogueManager.iniciarDialogo(
                                new String[] {
                                        "Tudo bem, quando quiser comprar iscas, estarei aqui!"
                                },
                                new BufferedImage[] { GameCore.pescador_portrait },
                                true);

                        case 0 -> {
                            if (player.getMoedas() >= 10) {
                                player.addMoedas(-10);
                                player.addIscas(5);
                                loopCompra(dialogueManager, player, "Aqui estão 5 iscas. Mais alguma coisa?", 0);
                            } else {
                                loopCompra(dialogueManager, player,
                                        "Você não tem moedas suficientes. Ainda deseja algo?", 0);
                            }
                        }

                        case 1 -> {
                            if (player.getMoedas() >= 20) {
                                player.addMoedas(-20);
                                player.addIscas(10);
                                loopCompra(dialogueManager, player, "Aqui estão 10 iscas. Mais alguma coisa?", 1);
                            } else {
                                loopCompra(dialogueManager, player,
                                        "Você não tem moedas suficientes. Ainda deseja algo?", 1);
                            }
                        }
                    }
                });
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, SoundManager soundManager, ItemManager itemManager) {
        proximo = playerNearby(player);
        System.out.println(proximo);
        switch (state) {
            case IDLE -> {
                if (proximo && input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)) {
                    dialogueManager.iniciarDialogo(new String[] {
                            "PINGU: É hoje que vou da a bunda!",
                            "PINGU: Hehehe! Vou pega o lençol e bate punheta escondido!"

                    }, new SoundManager.SFX[][] { DialogueCatalogo.pingu_noot, DialogueCatalogo.pingu_noot }, true);
                    dialogueManager.setAoTerminarDialogo(() -> {

                        iniciarVenda(player, input, dialogueManager, soundManager, itemManager);

                    });
                    state = State.TALKING;
                }
            }

            case TALKING -> {
                if (!dialogueManager.isAtivo()) {
                    state = State.IDLE;
                }
            }
            case SHOP -> {
                return;
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

    private void iniciarVenda(Player player, InputManager input,
            DialogueManager dialogueManager, SoundManager soundManager, ItemManager itemManager) {
        dialogueManager.iniciarDialogo(new String[] { "TODO: Iniciar venda" }, true);
    }
}
