
public class TiledObject {

    public double x, y, width, height;
    public int gid = 0;

    public String tipo = "";
    public String acao = "";
    public String inimigo = "";
    public String npc_nome = "";
    public String destino = "";

    public int id_arena = -1;
    public int horda = 1;
    public int totalHordas = 1;
    public boolean ativa = false;
    public boolean isScaled = false;

    public boolean colision = true;
    //public int key = 0;
    public int id_button = -1;
    public boolean isToggle = false;
    public int timer = 0;

    public boolean isPoint = false;
    public boolean isPolygon = false;
    public double[] polygonXs = new double[0];
    public double[] polygonYs = new double[0];

    public java.awt.Polygon getPolygon() {
        if (isPolygon) {
            int[] xPts = new int[polygonXs.length];
            int[] yPts = new int[polygonYs.length];
            for (int i = 0; i < xPts.length; i++) {
                xPts[i] = (int) (x + polygonXs[i]);
                yPts[i] = (int) (y + polygonYs[i]);
            }
            return new java.awt.Polygon(xPts, yPts, xPts.length);
        }
        return null;
    }
}
