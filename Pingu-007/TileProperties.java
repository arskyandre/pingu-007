
public class TileProperties {

    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int HOLE = 2;
    public static final int ICE = 3;

    public static boolean isSolid(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 45 && tempID <= 50) || (tempID >= 6 && tempID <= 8) || (tempID >= 20 && tempID <= 22) || (tempID >= 118 && tempID <= 124) || (tempID >= 25 && tempID <= 27) || (tempID >= 53 && tempID <= 55)
                    || (tempID >= 81 && tempID <= 83) || (tempID >= 112 && tempID <= 117);
        }
        return false;
    }

    public static boolean isSemiSolid(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 42 && tempID <= 44) || (tempID >= 61 && tempID <= 63) || (tempID >= 70 && tempID <= 75) || tempID == 9 || tempID == 56 || tempID == 57;
        }
        return false;
    }

    public static boolean isHole(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 42 && tempID <= 44) || (tempID >= 61 && tempID <= 63) || (tempID >= 70 && tempID <= 75) || tempID == 9 || tempID == 56 || tempID == 57;
        }
        return false;
    }

    public static boolean isIce(int tileID) {
        int tempID = tileID - 1;
        if (tempID >= 0) {
            return (tempID >= 3 && tempID <= 5) || (tempID >= 17 && tempID <= 19) || (tempID >= 31 && tempID <= 33);
        }
        return false;
    }
}
