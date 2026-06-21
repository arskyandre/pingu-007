public final class PuzzleStoneReplacer {

    private PuzzleStoneReplacer() {
    }

    public static void applyUnlockVisuals(MapDATA mapData) {
        for (MapDATA.TileLayer layer : mapData.layers) {
            if (!layer.name.equals("bStone") && !layer.name.equals("tStone")) {
                continue;
            }

            for (int r = 0; r < layer.data.length; r++) {
                for (int c = 0; c < layer.data[0].length; c++) {
                    int tile = layer.data[r][c];
                    layer.data[r][c] = mapUnlockTile(tile);
                }
            }
        }
    }

    private static int mapUnlockTile(int tile) {
        return switch (tile) {
            case 112 -> 116;
            case 113, 114, 99, 100, 85, 86 -> 23;
            case 115 -> 117;
            case 98 -> 102;
            case 84 -> 88;
            case 101 -> 103;
            case 87 -> 89;
            default -> tile;
        };
    }
}
