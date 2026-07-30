
import java.awt.Graphics2D;

public class InteractiveObject extends ArenaObject {

    private int[][] visualSnapshot;
    private boolean visible = true;

    public InteractiveObject(TiledObject data) {
        super(data);
    }

    @Override
    public String getTipo() {
        return "interativo";
    }

    @Override
    public void onLoad(ArenaContext context) {
        String acaoSegura = data.acao != null ? data.acao.trim() : "";
        if ("pescar".equalsIgnoreCase(acaoSegura)) {
            data.gid = ArenaAtlas.getStoolTileId();
        }
        setVisible(context, true);
    }

    public void setVisible(ArenaContext context, boolean state) {
        this.visible = state;
        if (data.gid <= 0) {
            return;
        }

        int tileId = state ? data.gid : 0;
        int[][] visual = context.getOrCreateVisualLayer("bWall");
        int rows = ArenaContext.snapshotRows(data);
        int cols = ArenaContext.snapshotCols(data);

        if (visualSnapshot == null) {
            visualSnapshot = ArenaContext.createSnapshot(rows, cols);
        }

        int startCol = ArenaContext.tileCol(data.x);
        int startRow = ArenaContext.tileRow(data.y);
        int endCol = ArenaContext.tileEndCol(data.x, data.width);
        int endRow = ArenaContext.tileEndRow(data.y, data.height);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < visual.length && c >= 0 && c < visual[0].length) {
                    if (state) {
                        visualSnapshot[r - startRow][c - startCol] = visual[r][c];
                        visual[r][c] = tileId;
                        context.clearStoneLayersAt(r, c);
                    } else {
                        visual[r][c] = visualSnapshot[r - startRow][c - startCol];
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
        if (!ArenaTriggers.collides(data, player)) {
            return false;
        }
        String acaoSegura = data.acao != null ? data.acao.trim() : "";
        if ("trocar_mapa".equalsIgnoreCase(acaoSegura)) {
            return true;
        }
        if ("pescar".equalsIgnoreCase(acaoSegura)) {
            System.out.println("Iniciando minigame de pesca no tile ID 10!");
            return true;
        }
        return false;
    }

    @Override
    public void drawOverlay(Graphics2D g2) {
        if (visible && data.gid > 0) {
            ArenaAtlas.drawTile(g2, data.gid, data.x, data.y, data.width, data.height);
        }
    }
}
