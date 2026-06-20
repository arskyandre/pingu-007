
import java.util.ArrayList;

public class MapDATA {

    public static class TileLayer {

        public String name;
        public int[][] data;

        public TileLayer(String name, int[][] data) {
            this.name = name;
            this.data = data;
        }
    }

    public ArrayList<TileLayer> layers;
    public ArrayList<TiledObject> objects;

    private int[][] collisionLayer;

    public MapDATA(ArrayList<TileLayer> layers, ArrayList<TiledObject> objects) {
        this.layers = layers;
        this.objects = objects;
        gerarMatrizDeColisao();
    }

    private void gerarMatrizDeColisao() {
        if (layers == null || layers.isEmpty()) {
            return;
        }

        int height = layers.get(0).data.length;
        int width = layers.get(0).data[0].length;
        collisionLayer = new int[height][width];

        for (TileLayer layer : layers) {

            if (layer.name.toLowerCase().startsWith("t")) {
                continue;
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int tile = layer.data[y][x];

                    if (tile != 0) {
                        collisionLayer[y][x] = tile;
                    }
                }
            }
        }
    }

    public int[][] getMainLayer() {
        return collisionLayer;
    }
}
