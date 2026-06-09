
public class HelpMethods {

    public static boolean CanMoveHere(double x, double y, double largura, double altura, int[][] lvlData) {
        // Recuo minusculo nas bordas para evitar raspar no tile vizinho
        double limiteX = x + largura - 0.1;
        double limiteY = y + altura - 0.1;

        if (!isSolid(x, y, lvlData)) {
            if (!isSolid(limiteX, limiteY, lvlData)) {
                if (!isSolid(limiteX, y, lvlData)) {
                    if (!isSolid(x, limiteY, lvlData)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isSolid(double x, double y, int[][] lvlData) {
        double yIndex = y / GameCore.tiles_size;
        double xIndex = x / GameCore.tiles_size;
        int tile;

        try {
            tile = lvlData[(int) yIndex][(int) xIndex];
        } catch (Exception e) {
            return true;
        }

        //System.out.println(tile);
        if(tile == 0 || tile == 17 || tile == 27 || tile == 18 || tile == 28 || tile == 76 || tile == 77){
            return false;
        }

        return true;
    }

}
