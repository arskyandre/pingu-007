
import java.awt.geom.Rectangle2D;

public class ArenaContext {

    private final LevelManager levelManager;
    private final EnemyManager enemyManager;

    public ArenaContext(LevelManager levelManager, EnemyManager enemyManager) {
        this.levelManager = levelManager;
        this.enemyManager = enemyManager;
        ArenaAtlas.init();
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public EnemyManager getEnemyManager() {
        return enemyManager;
    }

    public MapDATA getMapData() {
        return levelManager.getMapData();
    }

    public int[][] getMainLayer() {
        return getMapData().getMainLayer();
    }

    public int[][] getOrCreateVisualLayer(String layerName) {
        MapDATA mapData = getMapData();
        for (MapDATA.TileLayer layer : mapData.layers) {
            if (layer.name.equals(layerName)) {
                return layer.data;
            }
        }

        int[][] matrizFisica = getMainLayer();
        int[][] nova = new int[matrizFisica.length][matrizFisica[0].length];
        mapData.layers.add(new MapDATA.TileLayer(layerName, nova));
        return nova;
    }

    public void clearStoneLayersAt(int row, int col) {
        for (MapDATA.TileLayer layer : getMapData().layers) {
            if (layer.name.equals("bStone") || layer.name.equals("tStone")) {
                if (row >= 0 && row < layer.data.length && col >= 0 && col < layer.data[0].length) {
                    layer.data[row][col] = 0;
                }
            }
        }

        int[][] camadaFisica = getMainLayer();
        if (camadaFisica != null && row >= 0 && row < camadaFisica.length && col >= 0 && col < camadaFisica[0].length) {
            camadaFisica[row][col] = 0;
        }
    }

    public void pushEntityOutOfRect(Rectangle2D.Double rect, Player player) {
        if (player == null) {
            return;
        }

        Rectangle2D.Double playerRect = new Rectangle2D.Double(
                player.getX(), player.getY(), player.getLargura(), player.getAltura());
        if (!rect.intersects(playerRect)) {
            return;
        }

        double pushX = (player.getX() + player.getLargura() / 2.0) - (rect.x + rect.width / 2.0);
        double pushY = (player.getY() + player.getAltura() / 2.0) - (rect.y + rect.height / 2.0);
        if (Math.abs(pushX) > Math.abs(pushY)) {
            player.setX(player.getX() + Math.signum(pushX) * GameCore.tiles_size);
        } else {
            player.setY(player.getY() + Math.signum(pushY) * GameCore.tiles_size);
        }
    }

    public void pushEnemiesOutOfRect(Rectangle2D.Double rect) {
        for (Enemy enemy : enemyManager.getEnemies()) {
            Rectangle2D.Double enemyRect = new Rectangle2D.Double(
                    enemy.getX(), enemy.getY(), enemy.getLargura(), enemy.getAltura());
            if (!rect.intersects(enemyRect)) {
                continue;
            }

            double pushX = (enemy.getX() + enemy.getLargura() / 2.0) - (rect.x + rect.width / 2.0);
            double pushY = (enemy.getY() + enemy.getAltura() / 2.0) - (rect.y + rect.height / 2.0);
            if (Math.abs(pushX) > Math.abs(pushY)) {
                enemy.setX(enemy.getX() + Math.signum(pushX) * GameCore.tiles_size);
            } else {
                enemy.setY(enemy.getY() + Math.signum(pushY) * GameCore.tiles_size);
            }
        }
    }

    public static int tileCol(double worldX) {
        return (int) (worldX / GameCore.tiles_size);
    }

    public static int tileRow(double worldY) {
        return (int) (worldY / GameCore.tiles_size);
    }

    public static int tileEndCol(double worldX, double width) {
        return (int) ((worldX + width) / GameCore.tiles_size);
    }

    public static int tileEndRow(double worldY, double height) {
        return (int) ((worldY + height) / GameCore.tiles_size);
    }

    public static void paintTileRect(int[][] matrix, TiledObject obj, int tileId, int[][] snapshot, boolean storeSnapshot) {
        int startCol = tileCol(obj.x);
        int startRow = tileRow(obj.y);
        int endCol = tileEndCol(obj.x, obj.width);
        int endRow = tileEndRow(obj.y, obj.height);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < matrix.length && c >= 0 && c < matrix[0].length) {
                    if (storeSnapshot && snapshot != null) {
                        snapshot[r - startRow][c - startCol] = matrix[r][c];
                    }
                    matrix[r][c] = tileId;
                }
            }
        }
    }

    public static void restoreTileRect(int[][] matrix, TiledObject obj, int[][] snapshot) {
        if (snapshot == null) {
            return;
        }

        int startCol = tileCol(obj.x);
        int startRow = tileRow(obj.y);
        int endCol = tileEndCol(obj.x, obj.width);
        int endRow = tileEndRow(obj.y, obj.height);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < matrix.length && c >= 0 && c < matrix[0].length) {
                    matrix[r][c] = snapshot[r - startRow][c - startCol];
                }
            }
        }
    }

    public static int[][] createSnapshot(int rows, int cols) {
        return new int[rows][cols];
    }

    public static int snapshotRows(TiledObject obj) {
        return tileEndRow(obj.y, obj.height) - tileRow(obj.y);
    }

    public static int snapshotCols(TiledObject obj) {
        return tileEndCol(obj.x, obj.width) - tileCol(obj.x);
    }
}
