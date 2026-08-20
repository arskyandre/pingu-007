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
    private final double inimigosPorMoeda = 25 /* inimigos */ / 10 /* moedas */;
    private boolean proximo = false;
    private BufferedImage Sprite;
    private CameraManager camera;
    private BufferedImage portrait = LoadSave.GetSpriteAtlas("images/portrait/vendedor_portrait.png");
    private BufferedImage portrait2 = LoadSave.GetSpriteAtlas("images/portrait/vendedor_portrait2.png");
    private final OllamaClient ollamaClient = new OllamaClient();
    private long chatGeneration = 0;
    private boolean conversaIAAtiva = false;
    private boolean requisicaoIAAtiva = false;
    private boolean pensamentoTerminou = false;
    private boolean respostaIAPronta = false;
    private boolean encerrarAposResposta = false;
    private String respostaIAPendente;
    private Throwable erroIAPendente;

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
                GameCore.missing_image, 10, () -> {
                    player.addMunicao(10);
                }, true, false);
        shopMenu.addItem("Peixe", "Um delicioso peixe para curar um coração.", GameCore.missing_image, 15,
                () -> {
                    player.curar(10);
                }, true, false);
        shopMenu.addItem("Recarga Rápida",
                "Reduz o tempo necessário para recarregar a arma. ENDL (0.5s -> 0.25s)",
                GameCore.missing_image, 50, () -> {
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

    private void loopInteracao(String pergunta, SoundManager.SFX[] fala, Player player, DialogueManager dialogueManager,
            SoundManager soundManager) {

        dialogueManager.iniciarEscolha(pergunta, new String[] {
                "Quero comprar algo.",
                "Me dê minha recompensa!",
                "Vamos conversar!",
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
                        case 2 -> iniciarConversaIA(player, dialogueManager, soundManager);
                        case 3 -> {
                            dialogueManager.iniciarDialogo(new String[] {
                                    "VENDEDOR: Estou aqui sempre que precisar!"
                            }, DialogueCatalogo.VendedorTchau, new BufferedImage[] { portrait2 });
                            state = State.IDLE;
                        }
                    }
                });
    }

    private void iniciarConversaIA(Player player, DialogueManager dialogueManager, SoundManager soundManager) {
        System.out.println("[CHAT] Starting Vendedor AI conversation.");
        conversaIAAtiva = true;
        requisicaoIAAtiva = false;
        encerrarAposResposta = false;
        chatGeneration++;
        ollamaClient.clearConversation();
        state = State.TALKING;
        mostrarEntradaIA(player, dialogueManager, soundManager, chatGeneration);
    }

    private void mostrarEntradaIA(Player player, DialogueManager dialogueManager, SoundManager soundManager,
            long generation) {
        if (!conversaIAAtiva || generation != chatGeneration) {
            return;
        }

        dialogueManager.iniciarEntradaChat(
                texto -> dialogueManager.postarAcaoNoGameLoop(
                        () -> enviarMensagemIA(texto, player, dialogueManager, soundManager, generation)),
                () -> terminarConversaIA(player, dialogueManager, soundManager, generation));
    }

    private void enviarMensagemIA(String texto, Player player, DialogueManager dialogueManager,
            SoundManager soundManager, long generation) {
        if (!conversaIAAtiva || generation != chatGeneration || requisicaoIAAtiva) {
            System.out.println("[CHAT] Message ignored: conversation inactive, stale, or request already active.");
            return;
        }

        System.out.println("[CHAT] Sending player message to Ollama: " + texto);
        requisicaoIAAtiva = true;
        pensamentoTerminou = false;
        respostaIAPronta = false;
        respostaIAPendente = null;
        erroIAPendente = null;

        dialogueManager.iniciarDialogoBloqueado(new String[] {
                "VENDEDOR: O vendedor está pensando..."
        }, new BufferedImage[] { portrait });
        dialogueManager.setAoTerminarDialogo(() -> {
            pensamentoTerminou = true;
            mostrarRespostaIASePronta(player, dialogueManager, soundManager, generation);
        });

        ollamaClient.askAsync(texto).whenComplete((resposta, erro) -> dialogueManager.postarAcaoNoGameLoop(() -> {
            if (!conversaIAAtiva || generation != chatGeneration) {
                System.out.println("[CHAT] Ignoring late Ollama response from an ended conversation.");
                return;
            }
            System.out.println("[CHAT] Ollama result queued for display. Error=" + (erro != null));
            respostaIAPendente = resposta;
            erroIAPendente = erro;
            respostaIAPronta = true;
            dialogueManager.encerrarDialogoBloqueadoAoTerminarDigitacao();
        }));
    }

    private void mostrarRespostaIASePronta(Player player, DialogueManager dialogueManager,
            SoundManager soundManager, long generation) {
        if (!conversaIAAtiva || generation != chatGeneration || !pensamentoTerminou || !respostaIAPronta) {
            return;
        }

        System.out.println("[CHAT] Displaying Ollama response.");
        requisicaoIAAtiva = false;
        respostaIAPronta = false;
        String resposta;
        if (erroIAPendente != null || respostaIAPendente == null || respostaIAPendente.isBlank()) {
            resposta = "Não consegui falar com Ollama agora. Tente novamente.";
        } else {
            resposta = respostaIAPendente;
        }

        dialogueManager.iniciarDialogo(new String[] {
                "VENDEDOR: " + resposta
        }, new BufferedImage[] { portrait });
        dialogueManager.setAoTerminarDialogo(() -> {
            if (encerrarAposResposta && conversaIAAtiva && generation == chatGeneration) {
                finalizarConversaIA(player, dialogueManager, soundManager, generation);
            } else if (conversaIAAtiva && generation == chatGeneration) {
                mostrarEntradaIA(player, dialogueManager, soundManager, generation);
            }
        });
    }

    private void terminarConversaIA(Player player, DialogueManager dialogueManager,
            SoundManager soundManager, long generation) {
        if (generation != chatGeneration || !conversaIAAtiva) {
            System.out.println("[CHAT] Ignoring stale conversation close request.");
            return;
        }

        if (requisicaoIAAtiva || encerrarAposResposta) {
            System.out.println("[CHAT] Goodbye request already in progress.");
            return;
        }

        System.out.println("[CHAT] Sending goodbye message to Ollama: " + OllamaClient.GOODBYE_MESSAGE);
        encerrarAposResposta = true;
        dialogueManager.esconderEntradaChat();
        enviarMensagemIA(OllamaClient.GOODBYE_MESSAGE, player, dialogueManager, soundManager, generation);
    }

    private void finalizarConversaIA(Player player, DialogueManager dialogueManager,
            SoundManager soundManager, long generation) {
        if (generation != chatGeneration || !conversaIAAtiva) {
            return;
        }

        System.out.println("[CHAT] Ending Vendedor AI conversation after goodbye response.");
        conversaIAAtiva = false;
        requisicaoIAAtiva = false;
        encerrarAposResposta = false;
        chatGeneration++;
        ollamaClient.clearConversation();
        dialogueManager.esconderEntradaChat();
        loopInteracao("VENDEDOR: O que deseja fazer agora?", DialogueCatalogo.Vendedor_o_que_deseja,
                player, dialogueManager, soundManager);
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
