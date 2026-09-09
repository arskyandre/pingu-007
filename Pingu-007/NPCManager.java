import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NPCManager {

    private final ArrayList<NPC> npcs = new ArrayList<>();
    public record MarcadorMapa(String nome, double x, double y) {}

    // As descobertas sobrevivem a clearAll(), chamado ao trocar de fase.
    private final Map<String, Map<String, MarcadorMapa>> descobertasPorMapa = new LinkedHashMap<>();
    private final DialogueManager dialogueManager;
    private final ItemManager itemManager;
    private final SoundManager soundManager;

    public NPCManager(DialogueManager dialogueManager, ItemManager itemManager, SoundManager soundMGR) {
        this.dialogueManager = dialogueManager;
        this.itemManager = itemManager;
        soundManager = soundMGR;
    }

    public void spawn(NPC npc) {
        npcs.add(npc);
    }

    public void clearAll() {
        npcs.clear();
    }

    public void resetarDescobertas() {
        descobertasPorMapa.clear();
    }

    public List<MarcadorMapa> getMarcadoresMapa(String arquivoNivel, Iterable<TiledObject> objetos) {
        List<MarcadorMapa> marcadores = new ArrayList<>(
                descobertasPorMapa.getOrDefault(arquivoNivel, Map.of()).values());

        boolean conheceVendedor = descobertasPorMapa
                .getOrDefault(LoadSave.CASA_VENDEDOR, Map.of()).containsKey("vendedor");
        if (conheceVendedor) {
            for (TiledObject obj : objetos) {
                if ("trocar_mapa".equalsIgnoreCase(obj.acao)
                        && LoadSave.CASA_VENDEDOR.equals(obj.destino)) {
                    marcadores.add(new MarcadorMapa("vendedor",
                            obj.x + obj.width / 2.0, obj.y + obj.height / 2.0));
                }
            }
        }
        return marcadores;
    }

    public void update(Player player, InputManager input, String arquivoNivel) {

        for (NPC npc : npcs) {
            if (npc.isActive()) {
                npc.update(player, input, dialogueManager, soundManager, itemManager);
                if (npc.jaConversouComPlayer()) {
                    descobertasPorMapa.computeIfAbsent(arquivoNivel, chave -> new LinkedHashMap<>())
                            .computeIfAbsent(npc.getNomeMapa(), nome -> new MarcadorMapa(nome,
                                    npc.getX() + npc.getLargura() / 2.0,
                                    npc.getY() + npc.getAltura() / 2.0));
                }
            }
        }
    }

    public ArrayList<NPC> getNpcs() {
        return npcs;
    }
}
