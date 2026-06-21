import java.awt.geom.Rectangle2D;

public class DoorObject extends ArenaObject {

    private boolean closed = false;
    private int arenaId;
    private int[][] physicalSnapshot;
    private int[][] visualSnapshot;

    public DoorObject(TiledObject data) {
        super(data);
        this.arenaId = data.id_arena;
    }

    @Override
    public String getTipo() {
        return data.tipo != null ? data.tipo.toLowerCase() : "door";
    }

    public int getArenaId() {
        return arenaId;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void onLoad(ArenaContext context) {
    }

    public void setClosed(ArenaContext context, boolean state, Player player) {
        if (this.closed == state) {
            return;
        }

        this.closed = state;
        int wallTile = ArenaAtlas.getDefaultWallTileId();
        int[][] physics = context.getMainLayer();
        int[][] visual = context.getOrCreateVisualLayer("bWall");

        if (state) {
            Rectangle2D.Double wallRect = new Rectangle2D.Double(data.x, data.y, data.width, data.height);
            context.pushEntityOutOfRect(wallRect, player);
            context.pushEnemiesOutOfRect(wallRect);
        }

        int startCol = ArenaContext.tileCol(data.x);
        int startRow = ArenaContext.tileRow(data.y);
        int endCol = ArenaContext.tileEndCol(data.x, data.width);
        int endRow = ArenaContext.tileEndRow(data.y, data.height);

        if (state && physicalSnapshot == null) {
            physicalSnapshot = ArenaContext.createSnapshot(endRow - startRow, endCol - startCol);
            visualSnapshot = ArenaContext.createSnapshot(endRow - startRow, endCol - startCol);
        }

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < physics.length && c >= 0 && c < physics[0].length) {
                    if (state) {
                        physicalSnapshot[r - startRow][c - startCol] = physics[r][c];
                        visualSnapshot[r - startRow][c - startCol] = visual[r][c];
                        physics[r][c] = wallTile;
                        visual[r][c] = wallTile;
                    } else if (physicalSnapshot != null) {
                        physics[r][c] = physicalSnapshot[r - startRow][c - startCol];
                        visual[r][c] = visualSnapshot[r - startRow][c - startCol];
                    } else {
                        physics[r][c] = 0;
                        visual[r][c] = 0;
                    }
                }
            }
        }
    }
}
