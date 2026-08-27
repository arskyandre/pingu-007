
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PathFinder {

    private static final ExecutorService aiThreadPool = Executors.newFixedThreadPool(3);

    // MULTITHREADING (é sério)
    public static void solicitarCaminhoAsync(
            int startCol, int startRow,
            int targetCol, int targetRow,
            int[][] lvlData,
            ArrayList<JumpLink> jumpLinks,
            ArrayList<MapObject> objetosCenario,
            Consumer<ArrayList<Node>> callback) {

        aiThreadPool.submit(() -> {
            ArrayList<Node> caminhoPronto = encontrarCaminho(startCol, startRow, targetCol, targetRow, lvlData, jumpLinks, objetosCenario);
            callback.accept(caminhoPronto);
        });
    }

    public static void desligarIA() {
        aiThreadPool.shutdown();
    }

    public static ArrayList<Node> encontrarCaminho(
            int startCol, int startRow,
            int targetCol, int targetRow,
            int[][] lvlData,
            ArrayList<JumpLink> jumpLinks,
            ArrayList<MapObject> objetosCenario) {

        int maxRow = lvlData.length;
        int maxCol = lvlData[0].length;

        // Otimização : cria uma matriz para pre calcular os tiles com colisões de objetos (PERDE PRECISÃO)
        boolean[][] gridObjetos = new boolean[maxRow][maxCol];
        if (objetosCenario != null) {
            for (MapObject obj : objetosCenario) {
                if (obj == null || !obj.isSolid() || obj.getHitbox() == null) {
                    continue;
                }

                Rectangle2D bounds = obj.getHitbox().getBounds2D();
                int startC = Math.max(0, (int) (bounds.getX() / GameCore.tiles_size));
                int startR = Math.max(0, (int) (bounds.getY() / GameCore.tiles_size));
                int endC = Math.min(maxCol - 1, (int) ((bounds.getX() + bounds.getWidth()) / GameCore.tiles_size));
                int endR = Math.min(maxRow - 1, (int) ((bounds.getY() + bounds.getHeight()) / GameCore.tiles_size));

                for (int r = startR; r <= endR; r++) {
                    for (int c = startC; c <= endC; c++) {
                        Rectangle2D.Double tileRect = new Rectangle2D.Double(
                                c * GameCore.tiles_size, r * GameCore.tiles_size,
                                GameCore.tiles_size, GameCore.tiles_size);

                        if (obj.getHitbox().intersects(tileRect)) {
                            gridObjetos[r][c] = true;
                        }
                    }
                }
            }
        }

        ArrayList<Node> openList = new ArrayList<>();
        HashMap<Long, Node> openMap = new HashMap<>();
        HashMap<Long, Node> closedMap = new HashMap<>();

        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);
        openList.add(startNode);
        openMap.put(nodeKey(startCol, startRow), startNode);

        int limiteTentativas = 0;

        while (!openList.isEmpty()) {

            limiteTentativas++;
            if (limiteTentativas > 3000) {
                return null;
            }

            Node current = openList.get(0);
            for (int i = 1; i < openList.size(); i++) {
                Node n = openList.get(i);
                if (n.fCost < current.fCost
                        || (n.fCost == current.fCost && n.hCost < current.hCost)) {
                    current = n;
                }
            }

            openList.remove(current);
            openMap.remove(nodeKey(current.coluna, current.linha));
            closedMap.put(nodeKey(current.coluna, current.linha), current);

            if (current.coluna == targetNode.coluna && current.linha == targetNode.linha) {
                return construirCaminho(current);
            }

            for (Node vizinho : getVizinhos(current, lvlData, jumpLinks, closedMap, gridObjetos)) {
                long key = nodeKey(vizinho.coluna, vizinho.linha);
                int moveCost = current.gCost + calcularMoveCost(current, vizinho, lvlData);
                Node existingOpen = openMap.get(key);

                if (existingOpen != null) {
                    if (moveCost < existingOpen.gCost) {
                        existingOpen.gCost = moveCost;
                        existingOpen.hCost = calcularDistancia(existingOpen, targetNode);
                        existingOpen.calcularFCost();
                        existingOpen.parent = current;
                    }
                } else {
                    vizinho.gCost = moveCost;
                    vizinho.hCost = calcularDistancia(vizinho, targetNode);
                    vizinho.calcularFCost();
                    vizinho.parent = current;
                    openList.add(vizinho);
                    openMap.put(key, vizinho);
                }
            }
        }
        return null;
    }

    private static long nodeKey(int col, int row) {
        return ((long) col << 32) | (row & 0xFFFFFFFFL);
    }

    private static boolean isCaminhavel(int tileId) {
        return !TileProperties.isSolid(tileId) && !TileProperties.isHole(tileId);
    }

    private static ArrayList<Node> getVizinhos(
            Node current,
            int[][] lvlData,
            ArrayList<JumpLink> jumpLinks,
            HashMap<Long, Node> closedMap,
            boolean[][] gridObjetos) {

        ArrayList<Node> vizinhos = new ArrayList<>();
        int maxRow = lvlData.length;
        int maxCol = lvlData[0].length;

        int[] dirX = {0, 1, 0, -1};
        int[] dirY = {-1, 0, 1, 0};

        for (int i = 0; i < 4; i++) {
            int nCol = current.coluna + dirX[i];
            int nRow = current.linha + dirY[i];

            if (nCol >= 0 && nCol < maxCol && nRow >= 0 && nRow < maxRow) {
                if (isCaminhavel(lvlData[nRow][nCol])
                        && !gridObjetos[nRow][nCol]
                        && !closedMap.containsKey(nodeKey(nCol, nRow))) {
                    vizinhos.add(new Node(nCol, nRow));
                }
            }
        }

        if (jumpLinks != null) {
            for (JumpLink link : jumpLinks) {
                if (link.origemCol == current.coluna && link.origemRow == current.linha
                        && !closedMap.containsKey(nodeKey(link.destinoCol, link.destinoRow))
                        && !gridObjetos[link.destinoRow][link.destinoCol]) {

                    Node jumpNode = new Node(link.destinoCol, link.destinoRow);
                    jumpNode.requerSalto = true;
                    jumpNode.distanciaTiles = link.distanciaTiles;
                    vizinhos.add(jumpNode);

                } else if (link.destinoCol == current.coluna && link.destinoRow == current.linha
                        && !closedMap.containsKey(nodeKey(link.origemCol, link.origemRow))
                        && !gridObjetos[link.origemRow][link.origemCol]) {

                    Node jumpNode = new Node(link.origemCol, link.origemRow);
                    jumpNode.requerSalto = true;
                    jumpNode.distanciaTiles = link.distanciaTiles;
                    vizinhos.add(jumpNode);
                }
            }
        }
        return vizinhos;
    }

    private static int calcularDistancia(Node a, Node b) {
        return (Math.abs(a.coluna - b.coluna) + Math.abs(a.linha - b.linha)) * 10;
    }

    private static boolean temBuracoAdjacente(int row, int col, int[][] lvlData) {
        int[] dirX = {1, -1, 0, 0};
        int[] dirY = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            int r = row + dirY[i];
            int c = col + dirX[i];
            if (r >= 0 && r < lvlData.length && c >= 0 && c < lvlData[0].length) {
                if (TileProperties.isHole(lvlData[r][c])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int calcularMoveCost(Node from, Node to, int[][] lvlData) {
        int base;
        if (to.requerSalto && to.distanciaTiles > 0) {
            base = to.distanciaTiles * 10;
            int destHazard = TileProperties.getHazardMoveCost(to.linha, to.coluna, lvlData);
            base += destHazard;
            if (TileProperties.isTilePerigosoParaSalto(to.linha, to.coluna, lvlData)) {
                base += 20;
            }
        } else {
            base = calcularDistancia(from, to);
            base += TileProperties.getHazardMoveCost(to.linha, to.coluna, lvlData);

            if (TileProperties.isIce(lvlData[to.linha][to.coluna])) {
                if (temBuracoAdjacente(to.linha, to.coluna, lvlData)) {
                    base += 40;
                }
            }
        }
        return base;
    }

    private static ArrayList<Node> construirCaminho(Node targetNode) {
        ArrayList<Node> caminho = new ArrayList<>();
        Node atual = targetNode;
        while (atual != null) {
            caminho.add(atual);
            atual = atual.parent;
        }
        Collections.reverse(caminho);
        return caminho;
    }
}
