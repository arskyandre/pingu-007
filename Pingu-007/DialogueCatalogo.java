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
                                                                new BufferedImage[] { GameCore.cellphone_image }, true);

                                                dialogueManager.setAoTerminarDialogo(() -> {
                                                        loopDialogoInicial(dialogueManager, soundManager);
                                                });
                                        } else {

                                                dialogueManager.iniciarDialogo(DialogueCatalogo.TextoInicialRadioFinal,
                                                                new BufferedImage[] { GameCore.cellphone_image }, true);
                                        }
                                });
        }

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
        public static final SoundManager.SFX[][] FalaInicialRadio = new SoundManager.SFX[][] {
                        new SoundManager.SFX[] {
                                        SoundManager.SFX.NOOT_NOOT
                        },
                        new SoundManager.SFX[] {
                                        null,
                        },
                        new SoundManager.SFX[] {
                                        SoundManager.SFX.NOOT_NOOT
                        },
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[][] PescadorFala1 = new SoundManager.SFX[][] {
                        new SoundManager.SFX[] {
                                        null,
                                        // Ei
                                        SoundManager.SFX.KATAKANA_E,
                                        SoundManager.SFX.KATAKANA_I,

                                        // Pingu
                                        SoundManager.SFX.KATAKANA_PI,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_GU,

                                        null, null, null,
                                        // vejo
                                        SoundManager.SFX.KATAKANA_BE,
                                        SoundManager.SFX.KATAKANA_JO,

                                        // que
                                        SoundManager.SFX.KATAKANA_KE,

                                        // você
                                        SoundManager.SFX.KATAKANA_BO,
                                        SoundManager.SFX.KATAKANA_SE,

                                        // ainda
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DA,

                                        // não
                                        SoundManager.SFX.KATAKANA_NA,
                                        SoundManager.SFX.KATAKANA_O,

                                        // tem
                                        SoundManager.SFX.KATAKANA_TE,
                                        SoundManager.SFX.KATAKANA_N,

                                        // uma
                                        SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_MA,

                                        // vara
                                        SoundManager.SFX.KATAKANA_BA,
                                        SoundManager.SFX.KATAKANA_RA,

                                        // de
                                        SoundManager.SFX.KATAKANA_DE,

                                        // pesca
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA
                        },
                        new SoundManager.SFX[] {
                                        // Ainda
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DA,

                                        // bem
                                        SoundManager.SFX.KATAKANA_BE,
                                        SoundManager.SFX.KATAKANA_N,

                                        // que
                                        SoundManager.SFX.KATAKANA_KE,

                                        // tenho
                                        SoundManager.SFX.KATAKANA_TE,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_YO,

                                        // uma
                                        SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_MA,

                                        // sobrando
                                        SoundManager.SFX.KATAKANA_SO,
                                        SoundManager.SFX.KATAKANA_BU,
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DO,
                                        null, null, null,
                                        // Pode
                                        SoundManager.SFX.KATAKANA_PO,
                                        SoundManager.SFX.KATAKANA_DE,

                                        // ficar
                                        SoundManager.SFX.KATAKANA_FI,
                                        SoundManager.SFX.KATAKANA_KA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        // com
                                        SoundManager.SFX.KATAKANA_KO,
                                        SoundManager.SFX.KATAKANA_N,

                                        // ela
                                        SoundManager.SFX.KATAKANA_E,
                                        SoundManager.SFX.KATAKANA_RA
                        },
                        new SoundManager.SFX[] {
                                        // Sabe
                                        SoundManager.SFX.KATAKANA_SA,
                                        SoundManager.SFX.KATAKANA_BE,

                                        // como
                                        SoundManager.SFX.KATAKANA_KO,
                                        SoundManager.SFX.KATAKANA_MO,

                                        // pescar
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,
                                        SoundManager.SFX.KATAKANA_RU,
                                        null, null,
                                        // É
                                        SoundManager.SFX.KATAKANA_E,

                                        // fácil
                                        SoundManager.SFX.KATAKANA_FA,
                                        SoundManager.SFX.KATAKANA_SI,
                                        SoundManager.SFX.KATAKANA_U,
                                        null,
                                        // e
                                        SoundManager.SFX.KATAKANA_E,

                                        // só
                                        SoundManager.SFX.KATAKANA_SO,

                                        // chegar
                                        SoundManager.SFX.KATAKANA_CHA,
                                        SoundManager.SFX.KATAKANA_GA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        // perto
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_RE,
                                        SoundManager.SFX.KATAKANA_TO,

                                        // de
                                        SoundManager.SFX.KATAKANA_DE,

                                        // um
                                        SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_N,

                                        // buraco
                                        SoundManager.SFX.KATAKANA_BU,
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_KO,

                                        // de
                                        SoundManager.SFX.KATAKANA_DE,

                                        // água
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_GU,
                                        SoundManager.SFX.KATAKANA_A,

                                        // e
                                        SoundManager.SFX.KATAKANA_E,

                                        // apertar
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_RE,
                                        SoundManager.SFX.KATAKANA_TA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        SoundManager.SFX.KATAKANA_O,

                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_TA,
                                        SoundManager.SFX.KATAKANA_O,

                                        SoundManager.SFX.KATAKANA_DI, SoundManager.SFX.KATAKANA_RE,
                                        SoundManager.SFX.KATAKANA_I, SoundManager.SFX.KATAKANA_TO,

                                        SoundManager.SFX.KATAKANA_DO,

                                        SoundManager.SFX.KATAKANA_MA, SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_ZI,

                                        // para
                                        SoundManager.SFX.KATAKANA_PA,
                                        SoundManager.SFX.KATAKANA_RA,

                                        // lançar
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_SA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        // a
                                        SoundManager.SFX.KATAKANA_A,

                                        // linha
                                        SoundManager.SFX.KATAKANA_RI,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_YA
                        },
                        new SoundManager.SFX[] {
                                        SoundManager.SFX.KATAKANA_SE,

                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_BE,
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_RU,

                                        null, null,

                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_SE,

                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_SE, SoundManager.SFX.KATAKANA_GE,

                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_TE,

                                        SoundManager.SFX.KATAKANA_FI, SoundManager.SFX.KATAKANA_ZU,
                                        SoundManager.SFX.KATAKANA_GA, SoundManager.SFX.KATAKANA_RU,

                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_N,

                                        SoundManager.SFX.KATAKANA_I, SoundManager.SFX.KATAKANA_NI,
                                        SoundManager.SFX.KATAKANA_MI, SoundManager.SFX.KATAKANA_GO,

                                        SoundManager.SFX.KATAKANA_E,

                                        SoundManager.SFX.KATAKANA_TO, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_ZE, SoundManager.SFX.KATAKANA_RU,

                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_RI,

                                        SoundManager.SFX.KATAKANA_PU, SoundManager.SFX.KATAKANA_RA,

                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TO,

                                        null, null,

                                        SoundManager.SFX.KATAKANA_NA, SoundManager.SFX.KATAKANA_O,

                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_NYO,

                                        SoundManager.SFX.KATAKANA_A,

                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_JE, SoundManager.SFX.KATAKANA_N,

                                        SoundManager.SFX.KATAKANA_PU, SoundManager.SFX.KATAKANA_RA,

                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_TA, SoundManager.SFX.KATAKANA_RU,

                                        null,

                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TA, SoundManager.SFX.KATAKANA_O,
                                        null,
                                        SoundManager.SFX.KATAKANA_SE,

                                        SoundManager.SFX.KATAKANA_FO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_SI, SoundManager.SFX.KATAKANA_O,
                                        SoundManager.SFX.KATAKANA_NA, SoundManager.SFX.KATAKANA_RU,

                                        null,
                                        SoundManager.SFX.KATAKANA_ME,

                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TA,

                                        SoundManager.SFX.KATAKANA_DE, SoundManager.SFX.KATAKANA_E,
                                        SoundManager.SFX.KATAKANA_PO,
                                        SoundManager.SFX.KATAKANA_I, SoundManager.SFX.KATAKANA_SU

                        },
                        new SoundManager.SFX[] {
                                        // OUVI
                                        SoundManager.SFX.KATAKANA_O, SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_BI,

                                        // RUMORES
                                        SoundManager.SFX.KATAKANA_RU, SoundManager.SFX.KATAKANA_MO,
                                        SoundManager.SFX.KATAKANA_RE, SoundManager.SFX.KATAKANA_SU,

                                        // DE
                                        SoundManager.SFX.KATAKANA_DE,

                                        // QUE
                                        SoundManager.SFX.KATAKANA_KE,

                                        // UM
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_MU,

                                        // BURACO
                                        SoundManager.SFX.KATAKANA_BU, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_KO,

                                        // DE
                                        SoundManager.SFX.KATAKANA_DE,

                                        // PESCA
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,

                                        // POR
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,

                                        // AI
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_I,

                                        // ESCONDE
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DE,

                                        // UM
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_MU,

                                        // TESOURO
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_ZO,
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_RO,

                                        // SECRETO
                                        SoundManager.SFX.KATAKANA_SE, SoundManager.SFX.KATAKANA_KU,
                                        SoundManager.SFX.KATAKANA_RE, SoundManager.SFX.KATAKANA_TO,

                                        null, null,

                                        // TENTEI
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_I,

                                        // PESCAR
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA, SoundManager.SFX.KATAKANA_RU,

                                        // POR
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,

                                        // LA
                                        SoundManager.SFX.KATAKANA_RA,

                                        null, null,

                                        // MAS
                                        SoundManager.SFX.KATAKANA_MA, SoundManager.SFX.KATAKANA_SU,

                                        // NAO
                                        SoundManager.SFX.KATAKANA_NA, SoundManager.SFX.KATAKANA_O,

                                        // TIVE
                                        SoundManager.SFX.KATAKANA_CHI, SoundManager.SFX.KATAKANA_BE,

                                        // SORTE
                                        SoundManager.SFX.KATAKANA_SO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TE,

                                        // E
                                        SoundManager.SFX.KATAKANA_E,

                                        // AINDA
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_N, SoundManager.SFX.KATAKANA_DA,

                                        // ESQUECI
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KE, SoundManager.SFX.KATAKANA_SI,

                                        // MEU
                                        SoundManager.SFX.KATAKANA_ME, SoundManager.SFX.KATAKANA_U,

                                        // BANQUINHO
                                        SoundManager.SFX.KATAKANA_BA, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KI, SoundManager.SFX.KATAKANA_NYO,

                        },
                        new SoundManager.SFX[] {
                                        // SE
                                        SoundManager.SFX.KATAKANA_SE,

                                        // ENCONTRAR
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TO, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        null, null,

                                        // TENTE
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TE,

                                        // PESCAR
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA, SoundManager.SFX.KATAKANA_RU,

                                        // LA
                                        SoundManager.SFX.KATAKANA_RA,
                        },
                        new SoundManager.SFX[] {
                                        null,
                        },
                        new SoundManager.SFX[] {
                                        SoundManager.SFX.KATAKANA_A,
                                        null, null,
                                        SoundManager.SFX.KATAKANA_KWA, SoundManager.SFX.KATAKANA_ZE,

                                        // "me esqueci"
                                        SoundManager.SFX.KATAKANA_ME, SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_SU, SoundManager.SFX.KATAKANA_KE,
                                        SoundManager.SFX.KATAKANA_SI,

                                        null, null,

                                        // "Você vai precisar"
                                        SoundManager.SFX.KATAKANA_BO,
                                        SoundManager.SFX.KATAKANA_SE,
                                        SoundManager.SFX.KATAKANA_BA,
                                        SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_RE,
                                        SoundManager.SFX.KATAKANA_SI,
                                        SoundManager.SFX.KATAKANA_ZA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        // "de iscas"
                                        SoundManager.SFX.KATAKANA_DE,
                                        SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,
                                        SoundManager.SFX.KATAKANA_SU,

                                        // "para pescar"
                                        SoundManager.SFX.KATAKANA_PA,
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_PE,
                                        SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        null, null,

                                        // "Tome 5 de graça"
                                        SoundManager.SFX.KATAKANA_TO,
                                        SoundManager.SFX.KATAKANA_ME,
                                        SoundManager.SFX.KATAKANA_SI,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KO,
                                        SoundManager.SFX.KATAKANA_DE,
                                        SoundManager.SFX.KATAKANA_GU,
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_SA,

                                        // "para começar"
                                        SoundManager.SFX.KATAKANA_PA,
                                        SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_KO,
                                        SoundManager.SFX.KATAKANA_ME,
                                        SoundManager.SFX.KATAKANA_SA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        null, null,

                                        // "Se acabar"
                                        SoundManager.SFX.KATAKANA_SE,
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_KA,
                                        SoundManager.SFX.KATAKANA_BA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        null,

                                        // "é só voltar aqui"
                                        SoundManager.SFX.KATAKANA_SO,
                                        SoundManager.SFX.KATAKANA_BO,
                                        SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TA,
                                        SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_KI,

                                        // "que eu vendo mais"
                                        SoundManager.SFX.KATAKANA_KE,
                                        SoundManager.SFX.KATAKANA_E,
                                        SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_BE,
                                        SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DO,
                                        SoundManager.SFX.KATAKANA_MA,
                                        SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_SU
                        }

        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[][] PescadorFala2_noKey = new SoundManager.SFX[][] {
                        new SoundManager.SFX[] {
                                        // OUVI
                                        SoundManager.SFX.KATAKANA_O, SoundManager.SFX.KATAKANA_U,
                                        SoundManager.SFX.KATAKANA_BI,

                                        // RUMORES
                                        SoundManager.SFX.KATAKANA_RU, SoundManager.SFX.KATAKANA_MO,
                                        SoundManager.SFX.KATAKANA_RE, SoundManager.SFX.KATAKANA_SU,

                                        // DE
                                        SoundManager.SFX.KATAKANA_DE,

                                        // QUE
                                        SoundManager.SFX.KATAKANA_KE,

                                        // UM
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_MU,

                                        // BURACO
                                        SoundManager.SFX.KATAKANA_BU, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_KO,

                                        // DE
                                        SoundManager.SFX.KATAKANA_DE,

                                        // PESCA
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,

                                        // POR
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,

                                        // AI
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_I,

                                        // ESCONDE
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_DE,

                                        // UM
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_MU,

                                        // TESOURO
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_ZO,
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_RO,

                                        // SECRETO
                                        SoundManager.SFX.KATAKANA_SE, SoundManager.SFX.KATAKANA_KU,
                                        SoundManager.SFX.KATAKANA_RE, SoundManager.SFX.KATAKANA_TO,

                                        null, null,

                                        // TENTEI
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_I,

                                        // PESCAR
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA, SoundManager.SFX.KATAKANA_RU,

                                        // POR
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,

                                        // LA
                                        SoundManager.SFX.KATAKANA_RA,

                                        null, null,

                                        // MAS
                                        SoundManager.SFX.KATAKANA_MA, SoundManager.SFX.KATAKANA_SU,

                                        // NAO
                                        SoundManager.SFX.KATAKANA_NA, SoundManager.SFX.KATAKANA_O,

                                        // TIVE
                                        SoundManager.SFX.KATAKANA_CHI, SoundManager.SFX.KATAKANA_BE,

                                        // SORTE
                                        SoundManager.SFX.KATAKANA_SO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TE,

                                        // E
                                        SoundManager.SFX.KATAKANA_E,

                                        // AINDA
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_I,
                                        SoundManager.SFX.KATAKANA_N, SoundManager.SFX.KATAKANA_DA,

                                        // ESQUECI
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KE, SoundManager.SFX.KATAKANA_SI,

                                        // MEU
                                        SoundManager.SFX.KATAKANA_ME, SoundManager.SFX.KATAKANA_U,

                                        // BANQUINHO
                                        SoundManager.SFX.KATAKANA_BA, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KI, SoundManager.SFX.KATAKANA_NYO,

                        }, new SoundManager.SFX[] {
                                        // SE
                                        SoundManager.SFX.KATAKANA_SE,

                                        // ENCONTRAR
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TO, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_RU,

                                        null, null,

                                        // TENTE
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TE,

                                        // PESCAR
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA, SoundManager.SFX.KATAKANA_RU,

                                        // LA
                                        SoundManager.SFX.KATAKANA_RA,
                        }
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[][] PescadorFala2_hasKey = new SoundManager.SFX[][] {
                        new SoundManager.SFX[] {

                                        // QUER
                                        SoundManager.SFX.KATAKANA_KE, SoundManager.SFX.KATAKANA_RU,

                                        // DIZER
                                        SoundManager.SFX.KATAKANA_DI, SoundManager.SFX.KATAKANA_SE,
                                        SoundManager.SFX.KATAKANA_RU,

                                        // QUE
                                        SoundManager.SFX.KATAKANA_KE,

                                        // VOCE
                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_SE,

                                        // ENCONTROU
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_TO, SoundManager.SFX.KATAKANA_RO,
                                        SoundManager.SFX.KATAKANA_U,

                                        // O
                                        SoundManager.SFX.KATAKANA_O,

                                        // TESOURO
                                        SoundManager.SFX.KATAKANA_TE, SoundManager.SFX.KATAKANA_SO,
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_RO,

                                        // NO
                                        SoundManager.SFX.KATAKANA_NO,

                                        // BURACO
                                        SoundManager.SFX.KATAKANA_BU, SoundManager.SFX.KATAKANA_RA,
                                        SoundManager.SFX.KATAKANA_KO,

                                        // DE
                                        SoundManager.SFX.KATAKANA_DE,

                                        // PESCA
                                        SoundManager.SFX.KATAKANA_PE, SoundManager.SFX.KATAKANA_SU,
                                        SoundManager.SFX.KATAKANA_KA,
                                        null, null,
                                        // O
                                        SoundManager.SFX.KATAKANA_O,

                                        // QUE
                                        SoundManager.SFX.KATAKANA_KE,

                                        // ERA
                                        SoundManager.SFX.KATAKANA_E, SoundManager.SFX.KATAKANA_RA
                        },

                        new SoundManager.SFX[] {

                                        // UMA
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_MA,

                                        // CHAVE
                                        SoundManager.SFX.KATAKANA_CHA, SoundManager.SFX.KATAKANA_BE,

                                        // PARA
                                        SoundManager.SFX.KATAKANA_PA, SoundManager.SFX.KATAKANA_RA,

                                        // O
                                        SoundManager.SFX.KATAKANA_O,

                                        // PORTAO
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TA, SoundManager.SFX.KATAKANA_O,

                                        // DA
                                        SoundManager.SFX.KATAKANA_DA,

                                        // MORSA
                                        SoundManager.SFX.KATAKANA_MO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_SA,
                                        null, null,
                                        // UAU
                                        SoundManager.SFX.KATAKANA_U, SoundManager.SFX.KATAKANA_A,
                                        SoundManager.SFX.KATAKANA_U,
                                        null,
                                        // TALVEZ
                                        SoundManager.SFX.KATAKANA_TA, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_BE,

                                        // VOCE
                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_SE,

                                        // SEJA
                                        SoundManager.SFX.KATAKANA_SE, SoundManager.SFX.KATAKANA_JA,

                                        // BOM
                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_MU,

                                        // O
                                        SoundManager.SFX.KATAKANA_O,

                                        // SUFICIENTE
                                        SoundManager.SFX.KATAKANA_SU, SoundManager.SFX.KATAKANA_FI,
                                        SoundManager.SFX.KATAKANA_SI, SoundManager.SFX.KATAKANA_E,
                                        SoundManager.SFX.KATAKANA_N, SoundManager.SFX.KATAKANA_TE,

                                        // PARA
                                        SoundManager.SFX.KATAKANA_PA, SoundManager.SFX.KATAKANA_RA,

                                        // DERROTALA
                                        SoundManager.SFX.KATAKANA_DE, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_RO, SoundManager.SFX.KATAKANA_TA,
                                        SoundManager.SFX.KATAKANA_RA
                        },

                        new SoundManager.SFX[] {

                                        // BOA
                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_A,

                                        // JORNADA
                                        SoundManager.SFX.KATAKANA_JO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_NA, SoundManager.SFX.KATAKANA_DA,
                                        null,
                                        // AGENTE
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_GE,
                                        SoundManager.SFX.KATAKANA_N, SoundManager.SFX.KATAKANA_TE
                        }
        };

        // separador

        // de variaveis

        // Deus tenha piedade

        // das almas penadas

        // que encostarem nesse arquivo
        public static final SoundManager.SFX[][] PortaoAbriu = new SoundManager.SFX[][] {
                        new SoundManager.SFX[] {

                                        null,
                                        // VOCE
                                        SoundManager.SFX.KATAKANA_BO, SoundManager.SFX.KATAKANA_SE,

                                        // CONSEGUIU
                                        SoundManager.SFX.KATAKANA_KO, SoundManager.SFX.KATAKANA_N,
                                        SoundManager.SFX.KATAKANA_SE, SoundManager.SFX.KATAKANA_GI,
                                        SoundManager.SFX.KATAKANA_U,

                                        // O
                                        SoundManager.SFX.KATAKANA_O,

                                        // PORTAO
                                        SoundManager.SFX.KATAKANA_PO, SoundManager.SFX.KATAKANA_RU,
                                        SoundManager.SFX.KATAKANA_TA, SoundManager.SFX.KATAKANA_O,

                                        // ABRIU
                                        SoundManager.SFX.KATAKANA_A, SoundManager.SFX.KATAKANA_BU,
                                        SoundManager.SFX.KATAKANA_RI, SoundManager.SFX.KATAKANA_U
                        }
        };
}
