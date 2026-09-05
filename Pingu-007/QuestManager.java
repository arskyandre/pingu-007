
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Set;

public final class QuestManager implements ArenaManager.ObservadorArenas {

    public enum QuestState {
        NENHUMA, ATIVA, PRONTA_PARA_ENTREGAR
    }
    private QuestState questState = QuestState.NENHUMA;
    private int idArenaQuestAtual = -1;
    private static final Set<Integer> QUEST_BLACKLIST = Set.of(0, 2, 3, 4, 5, 9, 10, 14, 15, 67, 101, 102, 999);
    // Mude para false para manter somente o bonus por quantidade de hordas
    private static final boolean BonusDistanciaNaRecompensa = true;
    private static final int ID_TRIGGER_LOJA = 9;
    private static final int RECOMPENSA_BASE_MOEDAS = 50;
    private static final double BONUS_HORDA_EXTRA = 0.5;
    private static final double TILES_BONUS_DISTANCIA = 100.0;

    private final ArenaManager arenaManager;
    private final java.util.Set<Integer> arenasConcluidasConhecidas;
    private final java.util.Set<Integer> arenasValidasParaQuest = new java.util.HashSet<>();
    private final java.util.Map<Integer, Integer> hordasConhecidasPorArena = new java.util.HashMap<>();
    private final java.util.Map<Integer, Point2D.Double> centrosTriggersConhecidos = new java.util.HashMap<>();

    public QuestManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
        this.arenasConcluidasConhecidas = arenaManager.getHistoricoArenasConcluidas();
        arenaManager.observarArenas(this);
    }

    @Override
    public void arenaCriada(ArenaManager.Arena arena) {
        if (questState == QuestState.ATIVA && arena.id == idArenaQuestAtual) {
            arenaManager.configurarSpawns(arena, true, false);
        }
    }

    @Override
    public void hordasCarregadas(int idArena, int totalHordas) {
        hordasConhecidasPorArena.put(idArena, totalHordas);

        Rectangle2D.Double bounds = getArenaTriggerBounds(idArena);
        if (bounds != null) {
            centrosTriggersConhecidos.put(idArena,
                    new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
        }
    }

    @Override
    public void combateConcluido(int idArena) {
        checarConclusaoQuest(idArena);
    }

    public QuestState getQuestState() {
        return questState;
    }

    public void resetarProgressoDeQuests() {
        questState = QuestState.NENHUMA;
        idArenaQuestAtual = -1;
        arenaManager.limparHistoricoArenasConcluidas();
        arenasValidasParaQuest.clear();
        hordasConhecidasPorArena.clear();
        centrosTriggersConhecidos.clear();
    }

    public void atualizarArenasValidasParaQuest() {
        arenasValidasParaQuest.clear();

        for (int id : arenasConcluidasConhecidas) {
            Integer totalHordas = hordasConhecidasPorArena.get(id);

            if (totalHordas != null && totalHordas > 0 && !QUEST_BLACKLIST.contains(id)) {
                arenasValidasParaQuest.add(id);
            }
        }

        System.out.println("[DEBUG QUEST] Arenas validas no checkpoint: "
                + getArenasValidasParaQuest());
    }

    public ArrayList<Integer> getArenasValidasParaQuest() {
        ArrayList<Integer> ids = new ArrayList<>(arenasValidasParaQuest);
        ids.sort(Integer::compareTo);
        return ids;
    }

    public Point2D.Double getQuestTargetPoint() {
        Rectangle2D.Double bounds = getQuestTargetBounds();

        if (bounds != null) {
            return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        }

        return null;
    }

    public Rectangle2D.Double getQuestTargetBounds() {
        if (questState == QuestState.ATIVA && idArenaQuestAtual != -1) {
            return getArenaTriggerBounds(idArenaQuestAtual);
        }
        return null;
    }

    private Rectangle2D.Double getArenaTriggerBounds(int idArena) {
        ArenaManager.Arena arena = arenaManager.getOuCriarArena(idArena);
        if (arena.trigger == null) {
            return null;
        }

        java.awt.Shape triggerShape = null;
        if (arena.trigger.isPolygon) {
            triggerShape = arena.trigger.getPolygonShape();
        }
        if (triggerShape == null && arena.trigger.hitbox != null) {
            triggerShape = arena.trigger.hitbox;
        }
        if (triggerShape != null) {
            Rectangle2D bounds = triggerShape.getBounds2D();
            return new Rectangle2D.Double(
                    bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }

        return new Rectangle2D.Double(
                arena.trigger.x, arena.trigger.y, arena.trigger.width, arena.trigger.height);
    }

    public boolean isQuestArenaAtiva() {
        if (questState == QuestState.ATIVA && idArenaQuestAtual != -1) {
            ArenaManager.Arena a = arenaManager.getOuCriarArena(idArenaQuestAtual);
            return a.ativa && !a.concluida;
        }
        return false;
    }

    public boolean gerarQuestArenaAleatoria(Player player) {
        System.out.println("\n--- [DEBUG ARENA] GERANDO QUEST ALEATÓRIA ---");
        System.out.println("Estado atual da Quest: " + questState);

        if (questState != QuestState.NENHUMA) {
            System.out.println("-> Falha: Já existe uma quest ativa ou pronta.");
            return false;
        }

        ArrayList<Integer> validas = getArenasValidasParaQuest();
        System.out.println("Arenas concluidas conhecidas: " + arenasConcluidasConhecidas);
        System.out.println("Hordas mapeadas: " + hordasConhecidasPorArena);
        System.out.println("Blacklist (Proibidas): " + QUEST_BLACKLIST);
        System.out.println("Arenas validas no ultimo checkpoint: " + validas);

        if (validas.isEmpty()) {
            System.out.println("-> Falha: Nenhuma arena válida encontrada para gerar a missão.");
            return false;
        }

        int arenaEscolhida = validas.get((int) (Math.random() * validas.size()));
        System.out.println("-> SUCESSO! Arena sorteada: " + arenaEscolhida);

        this.idArenaQuestAtual = arenaEscolhida;
        this.questState = QuestState.ATIVA;
        arenaManager.prepararArenaParaRepeticao(arenaEscolhida);

        player.solicitarCheckpoint();

        return true;
    }

    private void checarConclusaoQuest(int idArena) {
        if (questState == QuestState.ATIVA && idArenaQuestAtual == idArena) {
            questState = QuestState.PRONTA_PARA_ENTREGAR;
            ToastNotifications.RequestNotification("Missão concluída! Volte ao vendedor para receber o prêmio.", 3.0);
        }
    }

    public boolean entregarQuest(Player player) {
        if (questState == QuestState.PRONTA_PARA_ENTREGAR) {
            int recompensaMoedas = calcularRecompensaMoedas();
            player.addMoedas(recompensaMoedas);
            player.addIscas(2);

            if (idArenaQuestAtual != -1) {
                ArenaManager.Arena arenaQuest = arenaManager.getOuCriarArena(idArenaQuestAtual);
                arenaManager.configurarSpawns(arenaQuest, false, true);
            }

            questState = QuestState.NENHUMA;
            idArenaQuestAtual = -1;
            player.solicitarCheckpoint();

            return true;
        }
        return false;
    }

    private int calcularRecompensaMoedas() {
        int totalHordas = Math.max(1, hordasConhecidasPorArena.getOrDefault(idArenaQuestAtual, 1));
        double multiplicadorHordas = 1.0 + (totalHordas - 1) * BONUS_HORDA_EXTRA;
        double distanciaEmTiles = calcularDistanciaDaLojaEmTiles();
        double multiplicadorDistancia = BonusDistanciaNaRecompensa
                ? 1.0 + distanciaEmTiles / TILES_BONUS_DISTANCIA
                : 1.0;

        int recompensa = (int) Math.round(
                RECOMPENSA_BASE_MOEDAS * multiplicadorHordas * multiplicadorDistancia);
        System.out.printf("[DEBUG QUEST] Recompensa da arena %d: %d moedas (%d horda(s), distância %.1f tiles, bônus de distância %s).%n",
                idArenaQuestAtual, recompensa, totalHordas, distanciaEmTiles,
                BonusDistanciaNaRecompensa ? "ativo" : "inativo");
        return recompensa;
    }

    private double calcularDistanciaDaLojaEmTiles() {
        Point2D.Double centroLoja = centrosTriggersConhecidos.get(ID_TRIGGER_LOJA);
        Point2D.Double centroQuest = centrosTriggersConhecidos.get(idArenaQuestAtual);
        if (centroLoja == null || centroQuest == null) {
            return 0.0;
        }
        return centroLoja.distance(centroQuest) / GameCore.tiles_size;
    }

    public void reaplicarQuestFisica(Player player) {
        if (questState == QuestState.ATIVA && idArenaQuestAtual != -1) {
            arenaManager.restaurarArenaParaRepeticao(idArenaQuestAtual, player);
        }
    }

}
