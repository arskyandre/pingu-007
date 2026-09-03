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
    private final double inimigosPorMoeda = 25.0 /* inimigos */ / 15.0 /* moedas */;
    private boolean proximo = false;
    private BufferedImage Sprite;
    private CameraManager camera;
    private BufferedImage portrait = LoadSave.GetSpriteAtlas("images/portrait/vendedor_portrait.png");
    private BufferedImage portrait2 = LoadSave.GetSpriteAtlas("images/portrait/vendedor_portrait2.png");
    private BufferedImage nao_implementado = LoadSave.GetSpriteAtlas("images/portrait/Corinthians_simbolo.png");

    public VendedorNPC(double x, double y, CameraManager cameraMgr, SoundManager soundManager) {
        super(x, y, WIDTH, HEIGHT);
        INTERACT_RANGE = GameCore.tiles_size * 3.5;
        camera = cameraMgr;
        Sprite = LoadSave.GetSpriteAtlas("images/npc/vendedorFoca.png");

        shopMenu = new ShopMenu(soundManager);
    }

    private void popularItens(Player player, SoundManager soundManager) {
        shopMenu.limparItens();

        shopMenu.addItem("10 Balas", "Está sem munição? Você pode comprar balas para a sua jornada aqui!",
                LoadSave.GetSpriteAtlas("images/hud/balas.png"), 10, () -> {
                    player.addMunicao(10);
                }, true, false);
        shopMenu.addItem("Peixe", "Um delicioso peixe para curar um coração.",
                LoadSave.GetSpriteAtlas("images/tile_set.png").getSubimage(144, 33, 16, 16), 15,
                () -> {
                    player.curar(10);
                }, true, false);
        shopMenu.addItem("Capacete",
                "Se proteja de balas dos inimigos com esse capacete! Esse item oferece uma redução do dano recebido por inimigos. ENDL (-33%)",
                LoadSave.GetSpriteAtlas("images/hud/helmet.png"), 50, () -> {
                    player.setTemCapacete(true);
                }, !player.getTemCapacete(), true);
        shopMenu.addItem("Recarga Rápida",
                "Reduz o tempo necessário para recarregar a arma. ENDL (0.5s -> 0.25s)",
                LoadSave.GetSpriteAtlas("images/hud/clock.png"), 50, () -> {
                    player.setFasterReload(true);
                }, !player.getFasterReload(), true);
        shopMenu.addItem("Pente Estendido",
                "Aumenta a capacidade do pente da sua arma. ENDL (15 tiros -> 30 tiros)",
                GameCore.missing_image, 75, () -> {
                    player.setExtendedMag(true);
                }, !player.getExtendedMag(), true);
        shopMenu.addItem("Espingarda (Shotgun)",
                "Uma espingarda que dispara diversos projéteis de uma só vez, causando alto dano a curta distância. Sua eficiência diminui conforme a distância aumenta.",
                LoadSave.GetSpriteAtlas("images/hud/shotgun_shopitem.png"), 100, () -> {
                    player.setHasShotgun(true);
                }, !player.getHasShotgun(), true);
    }

    private void encerrarDialogo(DialogueManager dialogueManager) {
        dialogueManager.iniciarDialogo(new String[] {
                "VENDEDOR: Estou aqui sempre que precisar!"
        }, DialogueCatalogo.VendedorTchau, new BufferedImage[] { portrait2 });
        state = State.IDLE;
    }

    private void comoPossoAjudar(DialogueManager dialogueManager) {
        dialogueManager.iniciarDialogo(new String[] {
                "Sou um time fraco!"
        }, new BufferedImage[] { nao_implementado });
        dialogueManager.setAoTerminarDialogo(() -> {
            encerrarDialogo(dialogueManager);
        });
    }

    private void loopInteracao(String pergunta, SoundManager.SFX[] fala, Player player, DialogueManager dialogueManager,
            SoundManager soundManager) {

        dialogueManager.iniciarEscolha(pergunta, new String[] {
                "Quero comprar algo.",
                "Como posso ajudar?(NÃO IMPLEMENTADO)",
                "Me dê minha recompensa!",
                "Deixa pra lá."
        }, fala,
                portrait,
                0,
                escolha -> {
                    soundManager.playSFX(SoundManager.SFX.NOOT_NOOT);
                    switch (escolha) {
                        case 0 -> {
                            popularItens(player, soundManager);
                            shopMenu.setAoFechar(() -> {
                                loopInteracao("VENDEDOR: Algo a mais, Pingu?", DialogueCatalogo.Vendedor_algo_a_mais,
                                        player, dialogueManager, soundManager);
                            });
                            shopMenu.abrir(player);
                            GameCore.setShopMenu(shopMenu);
                        }
                        case 1 -> {
                            comoPossoAjudar(dialogueManager);
                        }
                        case 2 -> {
                            int moedas = (int) (player.getCurrentEnemyCount()
                                    * inimigosPorMoeda);
                            if (moedas == 0) {
                                dialogueManager.iniciarDialogo(new String[] {
                                        "VENDEDOR: Elimine mais inimigos para resgatar sua recompensa."
                                }, new BufferedImage[] { portrait });
                            } else {
                                dialogueManager.iniciarDialogo(new String[] {
                                        "Desde a última vez que veio aqui, você eliminou "
                                                + Integer.toString(player.getCurrentEnemyCount())
                                                + " inimigos. Isso dá um total de "
                                                + Integer.toString(moedas)
                                                + " moedas."
                                }, new BufferedImage[] { portrait });
                            }
                            dialogueManager.setAoTerminarDialogo(() -> {
                                player.addMoedas(moedas);
                                loopInteracao("VENDEDOR: Algo a mais, Pingu?", DialogueCatalogo.Vendedor_algo_a_mais,
                                        player, dialogueManager,
                                        soundManager);
                            });
                        }
                        case 3 -> {
                            encerrarDialogo(dialogueManager);
                        }
                    }
                });
    }

    @Override
    public void update(Player player, InputManager input,
            DialogueManager dialogueManager, SoundManager soundManager, ItemManager itemManager) {
        proximo = playerNearby(player);
        switch (state) {
            case IDLE -> {
                if (proximo && (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)
                        || input.isButtonJustPressed(InputManager.GamepadButton.Y))) {
                    if (Player.getDesbloqueouRecompensa()) {

                        loopInteracao("VENDEDOR: E aí, Pingu? O que deseja?", DialogueCatalogo.Vendedor_o_que_deseja,
                                player, dialogueManager, soundManager);

                    } else {
                        int moedas = (int) (player.getCurrentEnemyCount() * inimigosPorMoeda);
                        dialogueManager.iniciarDialogo(new String[] {
                                "VENDEDOR: E aí Pingu, beleza?",
                                "VENDEDOR: Obrigado por salvar o nosso bairro, os soldados da Morsa estavam aterrorizando a nossa região!",
                                "VENDEDOR: Como agradecimento, quero lhe oferecer uma recompensa. A partir de agora, vou te pagar em moedas pelos inimigos que você eliminar!",
                                "VENDEDOR: Até agora, você eliminou "
                                        + Integer.toString(player.getCurrentEnemyCount())
                                        + " inimigos. Aqui estão "
                                        + Integer.toString(moedas)
                                        + " moedas."
                        }, DialogueCatalogo.VendedorFala1, new BufferedImage[] { portrait });
                        dialogueManager.setAoTerminarDialogo(() -> {
                            ToastNotifications.RequestNotification(
                                    "Elimine inimigos e volte à loja do vendedor para receber recompensas!", 2.5);
                            Player.setDesbloqueouRecompensa(true);
                            player.addMoedas(moedas);
                            player.setCurrentEnemyCount(0);
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
