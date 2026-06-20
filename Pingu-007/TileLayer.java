
class TileLayer {

    String name;
    int[][] data;

    public TileLayer(String name, int[][] data) {

        this.name = name;
        this.data = data;

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(int[][] data) {
        this.data = data;
    }

    public int[][] getData() {
        return data;
    }

    public String getName() {
        return name;
    }
}
