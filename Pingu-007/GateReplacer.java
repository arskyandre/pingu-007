
public final class GateReplacer {

    private GateReplacer() {
    }

    public static void applyUnlockVisuals(MapDATA mapData) {
        for (MapDATA.TileLayer layer : mapData.layers) {
            if (!layer.name.equals("bStone") && !layer.name.equals("tStone")) {
                continue;
            }

            for (int[] data : layer.data) {
                for (int c = 0; c < layer.data[0].length; c++) {
                    int tile = data[c];
                    data[c] = mapUnlockTile(tile);
                }
            }
        }

        int[][] mainLayer = mapData.getMainLayer();
        if (mainLayer != null) {
            for (int[] mainLayer1 : mainLayer) {
                for (int c = 0; c < mainLayer[0].length; c++) {
                    int tile = mainLayer1[c];
                    mainLayer1[c] = mapUnlockTile(tile);
                }
            }
        }
    }

    private static int mapUnlockTile(int tile) {
        if (tile == 0) {
            return 0;
        }

        int tempID = tile - 1;
        return switch (tempID) {
            case 84 ->
                88 + 1;
            case 98 ->
                102 + 1;
            case 112 ->
                116 + 1;

            case 85, 99, 113, 86, 100, 114 ->
                0;//15 + 1;

            case 87 ->
                89 + 1;
            case 101 ->
                103 + 1;
            case 115 ->
                117 + 1;

            default ->
                tile;
        };
    }
}
