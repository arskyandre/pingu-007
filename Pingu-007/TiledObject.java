
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class TiledObject {

    public int id = 0;
    public double x, y, width, height;
    public int gid = 0;
    public BufferedImage sprite;
    public Shape hitbox;
    public double rotation = 0; // graus (horário)

    // Flags de Renderização e Física
    public boolean isTransparent = false;
    public boolean isInteractive = false;
    public boolean flipH = false;
    public boolean flipV = false;
    public boolean flipDiagonal = false; // rotação 90°
    public boolean collision = false;

    public boolean solidoPorPadrao = false;

    // Propriedades de Jogo
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

    // public int key = 0;
    public int id_button = -1;
    public boolean isToggle = false;
    public int timer = 0;

    public boolean isPoint = false;
    public boolean isPolygon = false;
    public double[] polygonXs = new double[0];
    public double[] polygonYs = new double[0];

    public Shape getPolygonShape() {
        if (!isPolygon || polygonXs.length == 0) {
            return null;
        }

        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < polygonXs.length; i++) {
            double rx = polygonXs[i] * cos - polygonYs[i] * sin;
            double ry = polygonXs[i] * sin + polygonYs[i] * cos;
            double px = x + rx;
            double py = y + ry;
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        path.closePath();
        return path;
    }
}
