import java.awt.image.BufferedImage;

/**
 * a feiura mais assustadora, escabrosa, macabra e triste de toda a historia da
 * programacao
 * <p>
 * <b>null = nenhum som = pausa</b>
 */
public class DialogueCatalogo {

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
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

        public static final String[] PescadorFala1_part1 = new String[] {
          "Ei Pingu... vejo que você ainda não tem uma vara de pesca.",
            "Ainda bem que tenho uma sobrando...pode ficar com ela"
        };

        public static final String[] PescadorFala1_part2 = new String[]{
          "Sabe como pescar? É facil, é só chegar perto de um buraco de água e apertar o botão direito do mouse para lnaçar a linha.",
            "Se bobecar, você consegue até fisgar um inimigo e trazê-lo para perto... Não tenho a coragem para testar, então se funcionar me conta depois!",
              "Ouvi rumores de que um buraco de pesca por ai esconde um tesouro... Tentei pesca por lá, mas não tive sorte e ainda esqueci meu banquinho.",
                "Se encontrar, tente pescar lá",
                  "...",
                    "Ah, quase me esqueci! Você vai precisar de iscas para pescar, Tome 5 de graça para começar... Se acabar é só voltar aqui que eu vendo outra mais."
        };

        public static final String[] PescadorFala2_noKey = new String[]{
          "Ouvi rumores de que um buraco de pesca por aí esconde um tesouro... Tentei pescar por lá, mas não tive sorte e ainda esqueci meu banquinho.",
            "Se enconntra, tente pescar lá."
        };

        public static final String[] PescadorFala2_hasKey = new String[]{
          "Quer dizer que você encontrou o tesouro no buraco de pesca? O que era?",
            "Uma chave para o portão da Morsa? Uau... Talve você seja bom o suficiente para derrotá-lo.",
              "Boa jornada, agente!"
        };

        public static final String[] PortaoAbriu = new String[]{
          "Você conseguiu! O portão abriu!"
        };
}

