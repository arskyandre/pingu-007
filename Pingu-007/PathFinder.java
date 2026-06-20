
import java.util.ArrayList;
import java.util.Collections;

public class PathFinder {

    public static ArrayList<Node> encontrarCaminho(
            int startCol, int startRow,
            int targetCol, int targetRow,
            int[][] lvlData,
            ArrayList<JumpLink> jumpLinks) {

        ArrayList<Node> openList = new ArrayList<>();
        ArrayList<Node> closedList = new ArrayList<>();

        Node startNode = new Node(startCol, startRow);
        Node targetNode = new Node(targetCol, targetRow);
        openList.add(startNode);

        int limiteTentativas = 0; // A TRAVA ANTI-FREEZE

        while (!openList.isEmpty()) {

            limiteTentativas++;
            // Se testou mais de 300 blocos e não achou, o player está inalcançável
            // Aborta para salvar a CPU e forçar o inimigo a usar o modo de perseguição burra
            if (limiteTentativas > 300) {
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
            closedList.add(current);

            if (current.coluna == targetNode.coluna
                    && current.linha == targetNode.linha) {
                return construirCaminho(current);
            }

            for (Node vizinho : getVizinhos(current, lvlData, jumpLinks, closedList)) {

                int moveCost = current.gCost + calcularDistancia(current, vizinho);
                boolean inOpen = inList(openList, vizinho.coluna, vizinho.linha);

                if (moveCost < vizinho.gCost || !inOpen) {
                    vizinho.gCost = moveCost;
                    vizinho.hCost = calcularDistancia(vizinho, targetNode);
                    vizinho.calcularFCost();
                    vizinho.parent = current;
                    if (!inOpen) {
                        openList.add(vizinho);
                    }
                }
            }
        }
        return null;
    }

    private static boolean isCaminhavel(int tileId) {
        return !TileProperties.isSolid(tileId) && !TileProperties.isHole(tileId);
    }

    private static ArrayList<Node> getVizinhos(
            Node current,
            int[][] lvlData,
            ArrayList<JumpLink> jumpLinks,
            ArrayList<Node> closedList) {

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
                        && !inList(closedList, nCol, nRow)) {
                    vizinhos.add(new Node(nCol, nRow));
                }
            }
        }

        if (jumpLinks != null) {
            for (JumpLink link : jumpLinks) {
                if (link.origemCol == current.coluna
                        && link.origemRow == current.linha
                        && !inList(closedList, link.destinoCol, link.destinoRow)) {

                    Node jumpNode = new Node(link.destinoCol, link.destinoRow);
                    jumpNode.requerSalto = true;
                    jumpNode.distanciaTiles = link.distanciaTiles;
                    vizinhos.add(jumpNode);

                } else if (link.destinoCol == current.coluna
                        && link.destinoRow == current.linha
                        && !inList(closedList, link.origemCol, link.origemRow)) {

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

    private static boolean inList(ArrayList<Node> lista, int col, int row) {
        for (Node n : lista) {
            if (n.coluna == col && n.linha == row) {
                return true;
            }
        }
        return false;
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
