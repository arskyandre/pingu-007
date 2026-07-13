
import java.awt.geom.Rectangle2D;

public class CollisionBlock extends ArenaObject {

    private boolean active = false;
    private DialogueManager dialogueManager;
    private int[][] physicalSnapshot;

    public CollisionBlock(TiledObject data) {
        super(data);

    }

    @Override
    public String getTipo() {
        return "colision";
    }

    public void setDialogueManager(DialogueManager dm) {
        this.dialogueManager = dm;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void onLoad(ArenaContext context) {
        setActive(context, true, null);
    }

    public void setActive(ArenaContext context, boolean state, Player player) {
        if (!data.colision) {
            return;
        }

        this.active = state;
        int wallTile = ArenaAtlas.getDefaultWallTileId();
        int[][] physics = context.getMainLayer();

        if (state && player != null) {
            Rectangle2D.Double blockRect = new Rectangle2D.Double(data.x, data.y, data.width, data.height);
            context.pushEntityOutOfRect(blockRect, player);
        }

        int startCol = ArenaContext.tileCol(data.x);
        int startRow = ArenaContext.tileRow(data.y);
        int endCol = ArenaContext.tileEndCol(data.x, data.width);
        int endRow = ArenaContext.tileEndRow(data.y, data.height);

        if (state && physicalSnapshot == null) {
            physicalSnapshot = ArenaContext.createSnapshot(endRow - startRow, endCol - startCol);
        }

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < physics.length && c >= 0 && c < physics[0].length) {
                    if (state) {
                        physicalSnapshot[r - startRow][c - startCol] = physics[r][c];
                        physics[r][c] = wallTile;
                    } else if (physicalSnapshot != null) {
                        physics[r][c] = physicalSnapshot[r - startRow][c - startCol];
                    } else {
                        physics[r][c] = 0;
                    }
                }
            }
        }
    }

    @Override
    public boolean handlesInteraction() {
        return true;
    }

    @Override
    public boolean tryInteract(ArenaContext context, Player player, int chavesDoPlayer) {
        if (!active || !ArenaTriggers.collides(data, player)) {
            return false;
        }

        if (chavesDoPlayer >= 3) {
            setActive(context, false, null);
            GateReplacer.applyUnlockVisuals(context.getMapData());
            dialogueManager.iniciarDialogo(new String[] {
                    "RADIO: Você conseguiu! O portão abriu." }, DialogueSounds.PortaoAbriu, GameCore.cellphone_image,
                    true);
            return true;
        }
        String qtdChaves;
        if (chavesDoPlayer == 0) {
            qtdChaves = "RADIO: Você ainda não tem nenhuma chave. Continue procurando as chaves, Pingu.";
        } else
            qtdChaves = "RADIO: Agora, você possui " + Integer.toString(chavesDoPlayer) + " chaves. Procure a"
                    + ((3 - chavesDoPlayer == 1) ? " última chave" : "s 2 outras chaves") + ", Pingu";
        dialogueManager.iniciarDialogo(new String[] {
                "RADIO: De acordo com as nossas informações, Você precisará de 3 chaves para abrir esse portão.",
                "Ago" }, GameCore.cellphone_image,
                true);
        System.out.println("Você precisa de 3 chaves para abrir isso!");

        return true;
    }
}
