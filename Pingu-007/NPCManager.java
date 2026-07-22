import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class NPCManager {

    private final ArrayList<NPC> npcs = new ArrayList<>();
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

    public void update(Player player, InputManager input) {

        for (NPC npc : npcs) {
            if (npc.isActive()) {
                npc.update(player, input, dialogueManager, soundManager, itemManager);
            }
        }
    }

    public ArrayList<NPC> getNpcs() {
        return npcs;
    }
}