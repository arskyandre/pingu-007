
import java.util.ArrayList;

public class PathfindingPreCompiler {

    public static ArrayList<JumpLink> gerarJumpLinks(int[][] lvlData, int maxDashDistance) {
        ArrayList<JumpLink> links = new ArrayList<>();
        int maxRow = lvlData.length;
        int maxCol = lvlData[0].length;

        int[] dirX = {1, -1, 0, 0};
        int[] dirY = {0, 0, 1, -1};

        for (int row = 0; row < maxRow; row++) {
            for (int col = 0; col < maxCol; col++) {
                int currentTile = lvlData[row][col];

                if (!TileProperties.isHole(currentTile) && !TileProperties.isSolid(currentTile)) {

                    for (int i = 0; i < 4; i++) {
                        int vizinhoCol = col + dirX[i];
                        int vizinhoRow = row + dirY[i];

                        if (isDentroDosLimites(vizinhoCol, vizinhoRow, maxCol, maxRow)) {
                            int vizinhoTile = lvlData[vizinhoRow][vizinhoCol];

                            if (TileProperties.isHole(vizinhoTile)) {

                                for (int dist = 2; dist <= maxDashDistance; dist++) {
                                    int rayCol = col + (dirX[i] * dist);
                                    int rayRow = row + (dirY[i] * dist);

                                    if (!isDentroDosLimites(rayCol, rayRow, maxCol, maxRow)) {
                                        break;
                                    }

                                    int rayTile = lvlData[rayRow][rayCol];

                                    if (TileProperties.isSolid(rayTile)) {
                                        break;
                                    } else if (!TileProperties.isHole(rayTile)) {
                                        if (!TileProperties.isTilePerigosoParaSalto(rayRow, rayCol, lvlData)) {
                                            links.add(new JumpLink(col, row, rayCol, rayRow, dist));
                                            break;
                                        }
                                        if (dist >= maxDashDistance - 1) {
                                            links.add(new JumpLink(col, row, rayCol, rayRow, dist));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return links;
    }

    private static boolean isDentroDosLimites(int col, int row, int maxCol, int maxRow) {
        return col >= 0 && col < maxCol && row >= 0 && row < maxRow;
    }
}
