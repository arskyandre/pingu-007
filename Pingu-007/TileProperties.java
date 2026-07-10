
public class TileProperties {

    int HOLEid = 9 + 1; // esse é o ID especifico dos buracos
    static int keyFishingHoleID = 65;

    // isSolid diz o que não é atravessavel
    public static boolean isSolid(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 45 && tempID <= 50) || (tempID >= 6 && tempID <= 8) || (tempID >= 20 && tempID <= 22)
                    || (tempID >= 118 && tempID <= 124) || (tempID >= 25 && tempID <= 27)
                    || (tempID >= 53 && tempID <= 55)
                    || (tempID >= 81 && tempID <= 83) || (tempID >= 112 && tempID <= 117);
        }
        return false;
    }

    // isSemiSolid diz o que é atravessavel usando alguma movimentação especifica
    // (ex: dash)
    public static boolean isSemiSolid(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 42 && tempID <= 44) || (tempID >= 61 && tempID <= 63) || (tempID >= 70 && tempID <= 75)
                    || tempID == 9 || tempID == 56 || tempID == 57 || tempID == keyFishingHoleID;
        }
        return false;
    }

    // isHole diz o que é possivel cair ou não
    public static boolean isHole(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 42 && tempID <= 44) || (tempID >= 61 && tempID <= 63) || (tempID >= 70 && tempID <= 75)
                    || tempID == 9 || tempID == 56 || tempID == 57 || tempID == keyFishingHoleID;
        }
        return false;
    }

    public static boolean isFishingHole(int tileID) {
        int tempID = tileID - 1;
        return tempID == 9;
    }

    public static boolean isKeyFishingHole(int tileID) {
        int tempID = tileID - 1;
        if (tempID == keyFishingHoleID)
            System.out.println("is key fishing hole");
        return tempID == keyFishingHoleID;
    }

    // isIce diz o que é gelo, para aplicar a fisica "escorregadia"
    public static boolean isIce(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 3 && tempID <= 5) || (tempID >= 17 && tempID <= 19) || (tempID >= 31 && tempID <= 33);
        }
        return false;
    }

    public static boolean isAdjacentToHole(int row, int col, int[][] lvlData) {
        int maxRow = lvlData.length;
        int maxCol = lvlData[0].length;
        int[] dr = { -1, 1, 0, 0 };
        int[] dc = { 0, 0, -1, 1 };

        for (int i = 0; i < 4; i++) {
            int nr = row + dr[i];
            int nc = col + dc[i];
            if (nr >= 0 && nr < maxRow && nc >= 0 && nc < maxCol) {
                if (isHole(lvlData[nr][nc])) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getHazardMoveCost(int row, int col, int[][] lvlData) {
        if (row < 0 || row >= lvlData.length || col < 0 || col >= lvlData[0].length) {
            return 0;
        }

        int tile = lvlData[row][col];
        int cost = 0;

        if (isIce(tile) && isAdjacentToHole(row, col, lvlData)) {
            cost += 35;
        } else if (isAdjacentToHole(row, col, lvlData)) {
            cost += 18;
        }

        if (isIce(tile)) {
            cost += 4;
        }

        return cost;
    }

    public static boolean isTilePerigosoParaSalto(int row, int col, int[][] lvlData) {
        if (row < 0 || row >= lvlData.length || col < 0 || col >= lvlData[0].length) {
            return true;
        }
        int tile = lvlData[row][col];
        return isHole(tile) || isSolid(tile)
                || (isIce(tile) && isAdjacentToHole(row, col, lvlData));
    }
}
