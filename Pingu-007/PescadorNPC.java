
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;

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
                "PESCADOR: Ei, Pingu! Vejo que você ainda não tem uma vara de pesca.",
                "PESCADOR: Ainda bem que tenho uma sobrando. Pode ficar com ela!",
                "PESCADOR: Sabe como pescar? É fácil, e só chegar perto de um buraco de água e apertar E para lançar a linha.",
                "PESCADOR: Se bobear, você consegue até fisgar um inimigo e trazer ele pra perto. Não tenho a coragem pra testar, então, se funcionar, me conta depois!",
                "PESCADOR: Ouvi rumores de que um buraco de pesca por aí esconde um tesouro secreto! Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho.",
                "PESCADOR: Se encontrar, tente pescar lá!",
                "PESCADOR: ...",
                "PESCADOR: Ah, quase me esqueci! Você vai precisar de iscas para pescar. Tome 5 de graça para começar. Se acabar, é só voltar aqui que eu vendo mais."
        };
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
        Sprite = LoadSave.GetSpriteAtlas("images/npc/pescador.png");
    }

    private void loopCompra(DialogueManager dialogueManager, Player player) {
        loopCompra(dialogueManager, player, "Deseja comprar mais iscas?");
    }

    private void loopCompra(DialogueManager dialogueManager, Player player, String pergunta) {
        dialogueManager.iniciarEscolha(
                pergunta,
                new String[] {
                        "Não",
                        "Comprar 5 Iscas: 10 moedas",
                        "Comprar 10 Iscas: 20 moedas"
                }, GameCore.pescador_portrait,
                escolha -> {
                    switch (escolha) {
                        case 0 -> dialogueManager.iniciarDialogo(
                                new String[] {
                                        "Tudo bem, quando quiser comprar iscas, estarei aqui!"
                                },
                                new BufferedImage[] { GameCore.pescador_portrait },
                                true);

                        case 1 -> {
                            if (player.getMoedas() >= 10) {
                                player.addMoedas(-10);
                                player.addIscas(5);
                                loopCompra(dialogueManager, player, "Aqui estão 5 iscas. Mais alguma coisa?");
                            } else {
                                loopCompra(dialogueManager, player,
                                        "Você não tem moedas suficientes. Ainda deseja algo?");
                            }
                        }

                        case 2 -> {
                            if (player.getMoedas() >= 20) {
                                player.addMoedas(-20);
                                player.addIscas(10);
                                loopCompra(dialogueManager, player, "Aqui estão 10 iscas. Mais alguma coisa?");
                            } else {
                                loopCompra(dialogueManager, player,
                                        "Você não tem moedas suficientes. Ainda deseja algo?");
                            }
                        }
                    }
                });
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
                            dialogueManager.iniciarDialogo(dialogo2_hasKey, DialogueCatalogo.PescadorFala2_hasKey,
                                    new BufferedImage[] { GameCore.pescador_portrait }, true);
                        else
                            dialogueManager.iniciarDialogo(dialogo2_noKey, DialogueCatalogo.PescadorFala2_noKey,
                                    new BufferedImage[] { GameCore.pescador_portrait }, true);

                        dialogueManager.setAoTerminarDialogo(() -> {
                            loopCompra(dialogueManager, player);
                        });
                        laEle = true;
                    } else {

                        dialogueManager.iniciarDialogo(dialogo1, DialogueCatalogo.PescadorFala1,
                                new BufferedImage[] { GameCore.pescador_portrait }, true);
                        dialogueManager.setAoTerminarDialogo(() -> {
                            player.addIscas(5);

                        });
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
