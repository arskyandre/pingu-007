
public class NPCRegistry {

    public static NPC create(TiledObject obj, CameraManager camera, SoundManager soundManager, ArenaManager arenaManager) {
        if (obj.npc_nome == null || obj.npc_nome.isEmpty()) {
            return null;
        }
        return switch (obj.npc_nome.toLowerCase().trim()) {
            case "pescador" -> {
                System.out.println("Spawnou npc PESCADOR");
                yield new PescadorNPC(obj.x, obj.y, camera, soundManager);
            }
            case "vendedor" -> {
                System.out.println("Spawnou npc VENDEDOR");
                yield new VendedorNPC(obj.x, obj.y, camera, soundManager, arenaManager);
            }
            default -> {
                System.out.println("AVISO: npc_nome desconhecido no Tiled: " + obj.npc_nome);
                yield null;
            }
        };
    }
}
