
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class PescadorNPC extends NPC {

    private enum State {
        IDLE, TALKING
    }

    private ShopMenu shopMenu;
    private State state = State.IDLE;
    private boolean laEle = false;
    private static final double WIDTH = GameCore.tiles_size;
    private static final double HEIGHT = GameCore.tiles_size;
    private boolean proximo = false;
    private double Yfinal;
    private String[] dialogo1_part1;
    private String[] dialogo1_part2;
    private String[] dialogo2_noKey;
    private String[] dialogo2_hasKey;
    private BufferedImage Sprite;
    private BufferedImage isca_icone = LoadSave.GetSpriteAtlas("images/hud/iscasprite.png").getSubimage(16, 0, 16, 16);
    private BufferedImage peixe_icone = LoadSave.GetSpriteAtlas("images/tile_set.png").getSubimage(144, 33, 16, 16);
    private BufferedImage vara_icone = LoadSave.GetSpriteAtlas("images/hud/vara_premium_shopitem.png");
    private CameraManager camera;

    public PescadorNPC(double x, double y, CameraManager cameraMgr, SoundManager soundManager) {
        super(x, y, WIDTH, HEIGHT);
        shopMenu = new ShopMenu(soundManager);
        camera = cameraMgr;
        dialogo1_part1 = new String[]{
            "PESCADOR: Ei, Pingu! Vejo que você ainda não tem uma vara de pesca.",
            "PESCADOR: Ainda bem que tenho uma sobrando. Pode ficar com ela!"
        };
        dialogo1_part2 = new String[]{
            "PESCADOR: Sabe como pescar? É fácil, e só chegar perto de um buraco de água e apertar o botão direito do mouse para lançar a linha.",
            "PESCADOR: Se bobear, você consegue até fisgar um inimigo e trazer ele pra perto. Não tenho a coragem pra testar, então, se funcionar, me conta depois!",
            "PESCADOR: Ouvi rumores de que um buraco de pesca por aí esconde um tesouro. Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho...",
            "PESCADOR: Se encontrar, tente pescar lá!",
            "PESCADOR: ...",
            "PESCADOR: Ah, quase me esqueci! Você vai precisar de iscas para pescar. Tome 5 de graça para começar. Se acabar, é só voltar aqui que eu vendo mais."
        };

        dialogo2_noKey = new String[]{
            "PESCADOR: Ouvi rumores de que um buraco de pesca por aí esconde um tesouro. Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho...",
            "PESCADOR: Se encontrar, tente pescar lá!"};
        dialogo2_hasKey = new String[]{
            "PESCADOR: Quer dizer que você encontrou o tesouro no buraco de pesca? O que era?",
            "PESCADOR: Uma chave para o portão da Morsa... Ainda bem que caiu nas mãos certas!",
            "PESCADOR: Boa jornada, agente!"};
        Yfinal = y;
        this.y = Yfinal + 40;
        Sprite = LoadSave.GetSpriteAtlas("images/npc/pescador.png");
    }

    private void popularItens(Player player, SoundManager soundManager) {
        shopMenu.limparItens();
        shopMenu.addItem("5 Iscas", "Está sem iscas para pescar? Você pode comprar mais comigo!",
                isca_icone, 10, () -> {
                    player.addIscas(5);
                }, true, false);
        shopMenu.addItem("Peixe", "Um delicioso peixe para curar um coração.", peixe_icone, 15,
                () -> {
                    player.curar(10);
                }, true, false);
        shopMenu.addItem("Vara de pesca PREMIUM",
                "Essa vara de pescar atrai mais recompensas e requer menos força para puxar a recompensa.",
                vara_icone, 50, () -> {
                    if (!player.hasFishingRod()) {
                        player.setFishingRod(true);
                    }
                    player.setFasterFishing(true);
                }, !player.getFasterFishing(), true);
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, SoundManager soundManager, ItemManager itemManager) {
        if (Yfinal != y) {
            return;
        }
        proximo = playerNearby(player);
        switch (state) {
            case IDLE -> {
                if (proximo && (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)
                        || input.isButtonJustPressed(InputManager.GamepadButton.Y))) {
                    if (laEle || player.hasFishingRod()) {
                        if (FishingManager.isPlayerHasKey()) {
                            dialogueManager.iniciarDialogo(dialogo2_hasKey, DialogueCatalogo.PescadorFala2_hasKey,
                                    new BufferedImage[]{GameCore.pescador_portrait});
                        } else {
                            dialogueManager.iniciarDialogo(dialogo2_noKey, DialogueCatalogo.PescadorFala2_noKey,
                                    new BufferedImage[]{GameCore.pescador_portrait});
                        }

                        dialogueManager.setAoTerminarDialogo(() -> {

                            dialogueManager.iniciarEscolha("PESCADOR: Deseja comprar algo?",
                                    new String[]{"Sim", "Não"}, DialogueCatalogo.PescadorPergunta,
                                    GameCore.pescador_portrait, 0, escolha -> {
                                        switch (escolha) {
                                            case 0 -> {
                                                popularItens(player, soundManager);
                                                shopMenu.setAoFechar(() -> {
                                                    state = State.IDLE;
                                                });
                                                shopMenu.abrir(player);
                                                GameCore.setShopMenu(shopMenu);
                                            }
                                            case 1 -> {
                                                dialogueManager.iniciarDialogo(new String[]{
                                            "PESCADOR: Tudo bem, até a próxima!"
                                        }, new BufferedImage[]{GameCore.pescador_portrait});
                                                state = State.IDLE;
                                            }
                                        }
                                    });
                        });
                        laEle = true;
                    } else {

                        dialogueManager.iniciarDialogo(dialogo1_part1, DialogueCatalogo.PescadorFala1_part1,
                                new BufferedImage[]{GameCore.pescador_portrait});
                        dialogueManager.setAoTerminarDialogo(() -> {
                            camera.focarEm(x + (largura / 2.0), y + altura + 40, 60, false);
                            itemManager.spawn(new FishingRodItem(x + (largura / 2.0), y + altura + 40));
                            dialogueManager.iniciarDialogo(dialogo1_part2, DialogueCatalogo.PescadorFala1_part2,
                                    new BufferedImage[]{GameCore.pescador_portrait});
                            dialogueManager.setAoTerminarDialogo(() -> {
                                laEle = true;
                                player.addIscas(5);
                                state = State.IDLE;
                            });
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
        if (y > Yfinal) {
            y -= 80.0 * delta;
            if (y <= Yfinal) {
                y = Yfinal;
            }
        }
        drawSpriteWithShadow(g2, Sprite, (int) x, (int) y, (int) WIDTH, (int) HEIGHT);
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
