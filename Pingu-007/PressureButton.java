import java.awt.Graphics2D;

public class PressureButton extends ArenaObject {

    private boolean pressed = false;
    private int releaseTimer = 0;
    private int[][] visualSnapshot;

    public PressureButton(TiledObject data) {
        super(data);
    }

    @Override
    public String getTipo() {
        return "button";
    }

    public boolean isPressed() {
        return pressed;
    }

    @Override
    public void onLoad(ArenaContext context) {
        setPressed(context, false);
    }

    @Override
    public void update(ArenaContext context, Player player) {
        boolean playerOnTop = ArenaTriggers.collides(data, player);

        if (playerOnTop && !pressed) {
            setPressed(context, true);
            releaseTimer = 0;
            System.out.println("Botão " + data.id_button + " pressionado!");
        } else if (!playerOnTop && pressed) {
            releaseTimer++;
            if (releaseTimer >= 360) {
                setPressed(context, false);
                releaseTimer = 0;
                System.out.println("Botão " + data.id_button + " desarmou por falta de peso!");
            }
        }
    }

    public void setPressed(ArenaContext context, boolean state) {
        this.pressed = state;
        int tileId = state ? ArenaAtlas.getButtonDownTileId() : ArenaAtlas.getButtonUpTileId();
        applyVisual(context, tileId, true);
    }

    private void applyVisual(ArenaContext context, int tileId, boolean storeSnapshot) {
        int[][] visual = context.getOrCreateVisualLayer("bWall");
        int rows = ArenaContext.snapshotRows(data);
        int cols = ArenaContext.snapshotCols(data);

        if (storeSnapshot && visualSnapshot == null) {
            visualSnapshot = ArenaContext.createSnapshot(rows, cols);
        }

        int startCol = ArenaContext.tileCol(data.x);
        int startRow = ArenaContext.tileRow(data.y);
        int endCol = ArenaContext.tileEndCol(data.x, data.width);
        int endRow = ArenaContext.tileEndRow(data.y, data.height);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < visual.length && c >= 0 && c < visual[0].length) {
                    if (storeSnapshot && visualSnapshot != null) {
                        visualSnapshot[r - startRow][c - startCol] = visual[r][c];
                    }
                    visual[r][c] = tileId;
                    context.clearStoneLayersAt(r, c);
                }
            }
        }
    }

    @Override
    public void drawOverlay(Graphics2D g2) {
        int tileId = pressed ? ArenaAtlas.getButtonDownTileId() : ArenaAtlas.getButtonUpTileId();
        ArenaAtlas.drawTile(g2, tileId, data.x, data.y, data.width, data.height);
    }
}
