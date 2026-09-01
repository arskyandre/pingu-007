import java.awt.image.BufferedImage;

public class DialogueCatalogo {

        public static void loopDialogoInicial(DialogueManager dialogueManager, SoundManager soundManager) {
                dialogueManager.iniciarEscolha("RADIO: Deseja ouvir o protocolo novamente, Pingu?",
                                new String[] { "Sim", "Não" }, GameCore.cellphone_image, 1, escolha -> {
                                        ToastNotifications.skipNotification();
                                        soundManager.playSFX(SoundManager.SFX.NOOT_NOOT);
                                        if (escolha == 0) {

                                                dialogueManager.iniciarDialogo(
                                                                DialogueCatalogo.TextoInicialRadioRepetido,
                                                                new BufferedImage[] { GameCore.cellphone_image });

                                                dialogueManager.setAoTerminarDialogo(() -> {
                                                        loopDialogoInicial(dialogueManager, soundManager);
                                                });
                                        } else {

                                                dialogueManager.iniciarDialogo(DialogueCatalogo.TextoInicialRadioFinal,
                                                                new BufferedImage[] { GameCore.cellphone_image });
                                        }
                                });
        };

        public static final SoundManager.SFX[] pingu_noot = new SoundManager.SFX[] {
                        SoundManager.SFX.NOOT_NOOT
        };

        public static final SoundManager.SFX[] PescadorPergunta = new SoundManager.SFX[] {
                        SoundManager.SFX.PESCADOR_PERGUNTA
        };
        public static final SoundManager.SFX[] VendedorFala1 = new SoundManager.SFX[] {
                        SoundManager.SFX.VENDEDOR_FALA1_1,
                        SoundManager.SFX.VENDEDOR_FALA1_2,
                        SoundManager.SFX.VENDEDOR_FALA1_3

        };
        public static final SoundManager.SFX[] Vendedor_insuficiente_recompensa = new SoundManager.SFX[] {
                        SoundManager.SFX.VENDEDOR_INSUFICIENTE_RECOMPENSA
        };
        public static final SoundManager.SFX[] Vendedor_o_que_deseja = new SoundManager.SFX[] {
                        SoundManager.SFX.VENDEDOR_O_QUE_DESEJA
        };
        public static final SoundManager.SFX[] Vendedor_algo_a_mais = new SoundManager.SFX[] {
                        SoundManager.SFX.VENDEDOR_ALGO_A_MAIS
        };
        public static final SoundManager.SFX[] VendedorTchau = new SoundManager.SFX[] {
                        SoundManager.SFX.VENDEDOR_TCHAU
        };
        public static final String[] TextoInicialRadio = new String[] {
                        "PINGU: Entrando na base de operações.",
                        "RADIO: Cuidado, 007. Os lobos estão em alerta máximo.",
                        "PINGU: Eles não vão nem ver de onde veio.",
                        "RADIO: Antes de prosseguir, vamos recapitular o protocolo da missão.",
                        "RADIO: Seu objetivo é atravessar o complexo e eliminar a Morsa.",
                        "RADIO: Mas, para chegar à Morsa, você precisa encontrar as 3 chaves que destrancam o portão da sua arena.",
                        "RADIO: Movimente-se com WASD, use ESPAÇO para dar um dash e o botão esquerdo do mouse para atirar.",
                        "RADIO: Mantenha sua munição sob controle. Pressione R para recarregar sua pistola.",
                        "RADIO: Se uma arena fechar atrás de você, elimine todos os inimigos. A saída será liberada quando o último cair.",
                        "RADIO: Há buracos de pesca espalhados pela região, mas você ainda não possui uma vara.",
                        "RADIO: Nossos relatórios indicam a presença de um pescador. Se encontrá-lo, ele pode ser útil."
        };
        public static final String[] TextoInicialRadioRepetido = new String[] {
                        "RADIO: Ok, vamos recapitular o protocolo da missão.",
                        "RADIO: Seu objetivo é atravessar o complexo e eliminar a Morsa.",
                        "RADIO: Mas, para chegar à Morsa, você precisa encontrar as 3 chaves que destrancam o portão da sua arena.",
                        "RADIO: Movimente-se com WASD, use ESPAÇO para dar um dash e o botão esquerdo do mouse para atirar.",
                        "RADIO: Mantenha sua munição sob controle. Pressione R para recarregar sua pistola.",
                        "RADIO: Se uma arena fechar atrás de você, elimine todos os inimigos. A saída será liberada quando o último cair.",
                        "RADIO: Há buracos de pesca espalhados pela região, mas você ainda não possui uma vara.",
                        "RADIO: Nossos relatórios indicam a presença de um pescador. Se encontrá-lo, ele pode ser útil."
        };
        public static final String[] TextoInicialRadioFinal = new String[] {

                        "RADIO: Isso é tudo, agente. Boa sorte. A colônia está contando com você."
        };
        public static final String[] TextoBossFinal = new String[] {
          "MORSA: Não acredito...um pinguim com duas pistolas de meia tigela...me derrotou.",
          "MORSA: Isso não vai ficar assim, eu vou me vingar!"
        };
        public static final SoundManager.SFX[] FalaInicialRadio = new SoundManager.SFX[] {
                        SoundManager.SFX.NOOT_NOOT,
                        null,
                        SoundManager.SFX.NOOT_NOOT
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[] PescadorFala1_part1 = new SoundManager.SFX[] {
                        SoundManager.SFX.PESCADOR_FALA1_PART1_1,
                        SoundManager.SFX.PESCADOR_FALA1_PART1_2
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo

        public static final SoundManager.SFX[] PescadorFala1_part2 = new SoundManager.SFX[] {
                        SoundManager.SFX.PESCADOR_FALA1_PART2_1,
                        SoundManager.SFX.PESCADOR_FALA1_PART2_2,
                        SoundManager.SFX.PESCADOR_FALA1_PART2_3,
                        SoundManager.SFX.PESCADOR_FALA1_PART2_4,
                        SoundManager.SFX.PESCADOR_FALA1_PART2_5,
                        SoundManager.SFX.PESCADOR_FALA1_PART2_6
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[] PescadorFala2_noKey = new SoundManager.SFX[] {
                        SoundManager.SFX.PESCADOR_FALA1_NOKEY_1,
                        SoundManager.SFX.PESCADOR_FALA1_NOKEY_2
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[] PescadorFala2_hasKey = new SoundManager.SFX[] {
                        SoundManager.SFX.PESCADOR_FALA2_HASKEY_1,
                        SoundManager.SFX.PESCADOR_FALA2_HASKEY_2,
                        SoundManager.SFX.PESCADOR_FALA2_HASKEY_3
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[] PortaoAbriu = new SoundManager.SFX[] {
                        SoundManager.SFX.PORTAO_ABRIU
        };
}
