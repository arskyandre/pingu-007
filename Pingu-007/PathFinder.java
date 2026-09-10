import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PathFinder {

    private static final ExecutorService aiThreadPool = Executors.newFixedThreadPool(3);
    private static final IdentityHashMap<int[][], GridCache> gridCaches = new IdentityHashMap<>();
    private static final IdentityHashMap<ArrayList<JumpLink>, JumpCache> jumpCaches = new IdentityHashMap<>();
    private static final int[] DIR_X = {0, 1, 0, -1};
    private static final int[] DIR_Y = {-1, 0, 1, 0};

    public static void solicitarCaminhoAsync(
            int startCol, int startRow, int targetCol, int targetRow,
            int[][] lvlData, ArrayList<JumpLink> jumpLinks,
            ArrayList<MapObject> objects, Consumer<ArrayList<Node>> callback) {
        GridCache grid = getGrid(lvlData, objects);
        JumpCache jumps = getJumps(jumpLinks);
        aiThreadPool.submit(() -> callback.accept(encontrarCaminho(
                startCol, startRow, targetCol, targetRow, lvlData, grid, jumps)));
    }

    public static void solicitarCaminhoTaticoAsync(
            int startCol, int startRow, int targetCol, int targetRow, double range,
            int[][] lvlData, ArrayList<JumpLink> jumpLinks,
            ArrayList<MapObject> objects, Consumer<ArrayList<Node>> callback) {
        GridCache grid = getGrid(lvlData, objects);
        JumpCache jumps = getJumps(jumpLinks);
        int[] target = escolherAlvoTatico(startCol, startRow, targetCol, targetRow,
                range, lvlData, jumps, objects, grid);
        aiThreadPool.submit(() -> callback.accept(encontrarCaminho(
                startCol, startRow, target[0], target[1], lvlData, grid, null)));
    }

    public static void desligarIA() {
        aiThreadPool.shutdown();
    }

    public static ArrayList<Node> encontrarCaminho(
            int startCol, int startRow, int targetCol, int targetRow,
            int[][] lvlData, ArrayList<JumpLink> jumpLinks, ArrayList<MapObject> objects) {
        return encontrarCaminho(startCol, startRow, targetCol, targetRow,
                lvlData, getGrid(lvlData, objects), getJumps(jumpLinks));
    }

    private static ArrayList<Node> encontrarCaminho(
            int startCol, int startRow, int targetCol, int targetRow,
            int[][] lvlData, GridCache grid, JumpCache jumps) {
        if (!dentroDoGrid(startCol, startRow, lvlData)
                || !dentroDoGrid(targetCol, targetRow, lvlData)
                || !grid.walkable[targetRow][targetCol]) {
            return null;
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator
                .comparingInt((Node node) -> node.fCost)
                .thenComparingInt(node -> node.hCost));
        HashMap<Long, Node> best = new HashMap<>();
        HashSet<Long> closed = new HashSet<>();
        Node start = new Node(startCol, startRow);
        start.hCost = distancia(startCol, startRow, targetCol, targetRow);
        start.calcularFCost();
        open.add(start);
        best.put(nodeKey(startCol, startRow), start);

        int attempts = 0;
        while (!open.isEmpty() && ++attempts <= 3000) {
            Node current = open.poll();
            long currentKey = nodeKey(current.coluna, current.linha);
            if (best.get(currentKey) != current || !closed.add(currentKey)) {
                continue;
            }
            if (current.coluna == targetCol && current.linha == targetRow) {
                return construirCaminho(current);
            }

            for (int i = 0; i < 4; i++) {
                relaxar(current, current.coluna + DIR_X[i], current.linha + DIR_Y[i],
                        false, 1, targetCol, targetRow, lvlData, grid, open, best, closed);
            }

            if (jumps != null) {
                ArrayList<JumpTarget> targets = jumps.byOrigin.get(currentKey);
                if (targets != null) {
                    for (JumpTarget target : targets) {
                        if (dentroDoGrid(target.col, target.row, lvlData)
                                && saltoLivre(current.coluna, current.linha, target.col, target.row, grid)) {
                            relaxar(current, target.col, target.row, true, target.distance,
                                    targetCol, targetRow, lvlData, grid, open, best, closed);
                        }
                    }
                }
                adicionarSaltosTransparentes(current, targetCol, targetRow,
                        lvlData, grid, jumps.maxDistance, open, best, closed);
            }
        }
        return null;
    }

    private static void relaxar(
            Node current, int col, int row, boolean jump, int jumpDistance,
            int targetCol, int targetRow, int[][] lvlData, GridCache grid,
            PriorityQueue<Node> open, HashMap<Long, Node> best, HashSet<Long> closed) {
        if (!dentroDoGrid(col, row, lvlData) || !grid.walkable[row][col]) {
            return;
        }
        long key = nodeKey(col, row);
        if (closed.contains(key)) {
            return;
        }
        int moveCost = current.gCost + custoMovimento(current.coluna, current.linha,
                col, row, jump, jumpDistance, lvlData);
        Node previous = best.get(key);
        if (previous != null && moveCost >= previous.gCost) {
            return;
        }
        Node next = new Node(col, row);
        next.gCost = moveCost;
        next.hCost = distancia(col, row, targetCol, targetRow);
        next.calcularFCost();
        next.parent = current;
        next.requerSalto = jump;
        next.distanciaTiles = jumpDistance;
        best.put(key, next);
        open.add(next);
    }

    private static void adicionarSaltosTransparentes(
            Node current, int targetCol, int targetRow, int[][] lvlData,
            GridCache grid, int maxDistance, PriorityQueue<Node> open,
            HashMap<Long, Node> best, HashSet<Long> closed) {
        for (int i = 0; i < 4; i++) {
            int adjacentCol = current.coluna + DIR_X[i];
            int adjacentRow = current.linha + DIR_Y[i];
            if (!dentroDoGrid(adjacentCol, adjacentRow, lvlData)
                    || !grid.transparentObjects[adjacentRow][adjacentCol]) {
                continue;
            }
            for (int distance = 2; distance <= maxDistance; distance++) {
                int col = current.coluna + DIR_X[i] * distance;
                int row = current.linha + DIR_Y[i] * distance;
                if (!dentroDoGrid(col, row, lvlData)) {
                    break;
                }
                if (grid.transparentObjects[row][col]) {
                    continue;
                }
                if (!grid.walkable[row][col]) {
                    break;
                }
                relaxar(current, col, row, true, distance, targetCol, targetRow,
                        lvlData, grid, open, best, closed);
                break;
            }
        }
    }

    private static boolean saltoLivre(int startCol, int startRow, int targetCol, int targetRow, GridCache grid) {
        int dx = targetCol - startCol;
        int dy = targetRow - startRow;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 1; i < steps; i++) {
            int col = startCol + (int) Math.round(dx * (i / (double) steps));
            int row = startRow + (int) Math.round(dy * (i / (double) steps));
            if (grid.objects[row][col] && !grid.transparentObjects[row][col]) {
                return false;
            }
        }
        return true;
    }

    public static boolean temBuracoEntre(double x0, double y0, double x1, double y1, int[][] lvlData) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy) / (GameCore.tiles_size / 2.0)));
        for (int i = 1; i < steps; i++) {
            int col = (int) ((x0 + dx * i / steps) / GameCore.tiles_size);
            int row = (int) ((y0 + dy * i / steps) / GameCore.tiles_size);
            if (dentroDoGrid(col, row, lvlData) && TileProperties.isHole(lvlData[row][col])) {
                return true;
            }
        }
        return false;
    }

    public static boolean estaNaBordaVoltadaPara(
            double x, double y, double targetX, double targetY, int[][] lvlData) {
        int col = (int) (x / GameCore.tiles_size);
        int row = (int) (y / GameCore.tiles_size);
        int dx = Integer.compare((int) (targetX / GameCore.tiles_size), col);
        int dy = Integer.compare((int) (targetY / GameCore.tiles_size), row);
        if (Math.abs(targetX - x) >= Math.abs(targetY - y)) {
            return dentroDoGrid(col + dx, row, lvlData) && TileProperties.isHole(lvlData[row][col + dx]);
        }
        return dentroDoGrid(col, row + dy, lvlData) && TileProperties.isHole(lvlData[row + dy][col]);
    }

    public static boolean temLinhaDeVisaoLivre(
            double x0, double y0, double x1, double y1,
            int[][] lvlData, ArrayList<MapObject> objects) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy) / 16.0));
        for (int i = 0; i <= steps; i++) {
            int col = (int) ((x0 + dx * i / steps) / GameCore.tiles_size);
            int row = (int) ((y0 + dy * i / steps) / GameCore.tiles_size);
            if (dentroDoGrid(col, row, lvlData) && TileProperties.isOpaque(lvlData[row][col])) {
                return false;
            }
        }
        Line2D line = new Line2D.Double(x0, y0, x1, y1);
        Rectangle2D bounds = new Rectangle2D.Double(Math.min(x0, x1), Math.min(y0, y1),
                Math.max(1.0, Math.abs(dx)), Math.max(1.0, Math.abs(dy)));
        for (MapObject object : MapObjectSpatialIndex.query(objects, bounds)) {
            if (object == null || !object.isSolid() || object.isTransparent()) {
                continue;
            }
            Shape hitbox = object.getHitbox();
            if (hitbox instanceof Rectangle2D rectangle) {
                if (line.intersects(rectangle)) {
                    return false;
                }
            } else if (hitbox != null && hitbox.intersects(bounds)) {
                return false;
            }
        }
        return true;
    }

    private static int[] escolherAlvoTatico(
            int startCol, int startRow, int targetCol, int targetRow, double range,
            int[][] lvlData, JumpCache jumps, ArrayList<MapObject> objects, GridCache grid) {
        if (!dentroDoGrid(startCol, startRow, lvlData) || jumps == null) {
            return new int[]{targetCol, targetRow};
        }
        int component = grid.components[startRow][startCol];
        int directDistance = Math.abs(targetCol - startCol) + Math.abs(targetRow - startRow);
        int bestCol = targetCol;
        int bestRow = targetRow;
        int bestScore = Integer.MAX_VALUE;
        double targetX = (targetCol + 0.5) * GameCore.tiles_size;
        double targetY = (targetRow + 0.5) * GameCore.tiles_size;

        for (long endpoint : jumps.endpoints) {
            int col = (int) (endpoint >> 32);
            int row = (int) endpoint;
            if (!dentroDoGrid(col, row, lvlData) || grid.components[row][col] != component) {
                continue;
            }
            double x = (col + 0.5) * GameCore.tiles_size;
            double y = (row + 0.5) * GameCore.tiles_size;
            double targetDistance = Math.hypot(targetX - x, targetY - y);
            if (targetDistance > range || !temBuracoEntre(x, y, targetX, targetY, lvlData)
                    || !temLinhaDeVisaoLivre(x, y, targetX, targetY, lvlData, objects)) {
                continue;
            }
            int startDistance = Math.abs(col - startCol) + Math.abs(row - startRow);
            int score = startDistance * 10 + (int) (targetDistance / GameCore.tiles_size);
            if (score < bestScore) {
                bestScore = score;
                bestCol = col;
                bestRow = row;
            }
        }

        boolean playerReachable = dentroDoGrid(targetCol, targetRow, lvlData)
                && grid.components[targetRow][targetCol] == component;
        int edgeDistance = Math.abs(bestCol - startCol) + Math.abs(bestRow - startRow);
        if (bestScore != Integer.MAX_VALUE && (!playerReachable || edgeDistance + 2 < directDistance)) {
            return new int[]{bestCol, bestRow};
        }
        return new int[]{targetCol, targetRow};
    }

    private static GridCache getGrid(int[][] lvlData, ArrayList<MapObject> objects) {
        synchronized (gridCaches) {
            GridCache cache = gridCaches.get(lvlData);
            long version = MapObject.getSpatialVersion();
            int size = objects == null ? 0 : objects.size();
            if (cache == null || cache.objectsSource != objects
                    || cache.objectsSize != size || cache.version != version) {
                cache = new GridCache(lvlData, objects, version);
                gridCaches.put(lvlData, cache);
            }
            return cache;
        }
    }

    private static JumpCache getJumps(ArrayList<JumpLink> links) {
        if (links == null || links.isEmpty()) {
            return null;
        }
        synchronized (jumpCaches) {
            JumpCache cache = jumpCaches.get(links);
            if (cache == null || cache.size != links.size()) {
                cache = new JumpCache(links);
                jumpCaches.put(links, cache);
            }
            return cache;
        }
    }

    private static int custoMovimento(
            int fromCol, int fromRow, int col, int row,
            boolean jump, int jumpDistance, int[][] lvlData) {
        int cost = jump ? jumpDistance * 10 : distancia(fromCol, fromRow, col, row);
        cost += TileProperties.getHazardMoveCost(row, col, lvlData);
        if (jump && TileProperties.isTilePerigosoParaSalto(row, col, lvlData)) {
            cost += 20;
        } else if (!jump && TileProperties.isIce(lvlData[row][col]) && temBuracoAdjacente(row, col, lvlData)) {
            cost += 40;
        }
        return cost;
    }

    private static boolean temBuracoAdjacente(int row, int col, int[][] lvlData) {
        for (int i = 0; i < 4; i++) {
            int nextCol = col + DIR_X[i];
            int nextRow = row + DIR_Y[i];
            if (dentroDoGrid(nextCol, nextRow, lvlData) && TileProperties.isHole(lvlData[nextRow][nextCol])) {
                return true;
            }
        }
        return false;
    }

    private static boolean dentroDoGrid(int col, int row, int[][] lvlData) {
        return lvlData != null && row >= 0 && row < lvlData.length
                && col >= 0 && col < lvlData[0].length;
    }

    private static int distancia(int colA, int rowA, int colB, int rowB) {
        return (Math.abs(colA - colB) + Math.abs(rowA - rowB)) * 10;
    }

    private static long nodeKey(int col, int row) {
        return ((long) col << 32) | (row & 0xffffffffL);
    }

    private static ArrayList<Node> construirCaminho(Node target) {
        ArrayList<Node> path = new ArrayList<>();
        for (Node node = target; node != null; node = node.parent) {
            path.add(node);
        }
        Collections.reverse(path);
        return path;
    }

    private static final class GridCache {
        final boolean[][] objects;
        final boolean[][] transparentObjects;
        final boolean[][] walkable;
        final int[][] components;
        final ArrayList<MapObject> objectsSource;
        final int objectsSize;
        final long version;
        final int cols;

        GridCache(int[][] lvlData, ArrayList<MapObject> source, long version) {
            int rows = lvlData.length;
            cols = lvlData[0].length;
            objects = new boolean[rows][cols];
            transparentObjects = new boolean[rows][cols];
            walkable = new boolean[rows][cols];
            components = new int[rows][cols];
            objectsSource = source;
            objectsSize = source == null ? 0 : source.size();
            this.version = version;

            if (source != null) {
                for (MapObject object : source) {
                    if (object == null || !object.isSolid() || object.getHitbox() == null) {
                        continue;
                    }
                    Rectangle2D bounds = object.getHitbox().getBounds2D();
                    int minCol = Math.max(0, (int) (bounds.getMinX() / GameCore.tiles_size));
                    int maxCol = Math.min(cols - 1, (int) ((bounds.getMaxX() - 0.001) / GameCore.tiles_size));
                    int minRow = Math.max(0, (int) (bounds.getMinY() / GameCore.tiles_size));
                    int maxRow = Math.min(rows - 1, (int) ((bounds.getMaxY() - 0.001) / GameCore.tiles_size));
                    for (int row = minRow; row <= maxRow; row++) {
                        for (int col = minCol; col <= maxCol; col++) {
                            Rectangle2D tile = new Rectangle2D.Double(col * GameCore.tiles_size,
                                    row * GameCore.tiles_size, GameCore.tiles_size, GameCore.tiles_size);
                            if (object.getHitbox().intersects(tile)) {
                                objects[row][col] = true;
                                transparentObjects[row][col] |= object.isTransparent();
                            }
                        }
                    }
                }
            }
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    walkable[row][col] = !TileProperties.isSolid(lvlData[row][col])
                            && !TileProperties.isHole(lvlData[row][col]) && !objects[row][col];
                }
            }
            preencherComponentes();
        }

        private void preencherComponentes() {
            int component = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (int row = 0; row < walkable.length; row++) {
                for (int col = 0; col < cols; col++) {
                    if (!walkable[row][col] || components[row][col] != 0) {
                        continue;
                    }
                    components[row][col] = ++component;
                    queue.add(row * cols + col);
                    while (!queue.isEmpty()) {
                        int encoded = queue.removeFirst();
                        int currentCol = encoded % cols;
                        int currentRow = encoded / cols;
                        for (int i = 0; i < 4; i++) {
                            int nextCol = currentCol + DIR_X[i];
                            int nextRow = currentRow + DIR_Y[i];
                            if (nextRow >= 0 && nextRow < walkable.length && nextCol >= 0 && nextCol < cols
                                    && walkable[nextRow][nextCol] && components[nextRow][nextCol] == 0) {
                                components[nextRow][nextCol] = component;
                                queue.add(nextRow * cols + nextCol);
                            }
                        }
                    }
                }
            }
        }
    }

    private static final class JumpCache {
        final HashMap<Long, ArrayList<JumpTarget>> byOrigin = new HashMap<>();
        final ArrayList<Long> endpoints = new ArrayList<>();
        final int size;
        int maxDistance = 5;

        JumpCache(ArrayList<JumpLink> links) {
            size = links.size();
            HashSet<Long> uniqueEndpoints = new HashSet<>();
            for (JumpLink link : links) {
                add(link.origemCol, link.origemRow, link.destinoCol, link.destinoRow, link.distanciaTiles);
                add(link.destinoCol, link.destinoRow, link.origemCol, link.origemRow, link.distanciaTiles);
                uniqueEndpoints.add(nodeKey(link.origemCol, link.origemRow));
                uniqueEndpoints.add(nodeKey(link.destinoCol, link.destinoRow));
                maxDistance = Math.max(maxDistance, link.distanciaTiles);
            }
            endpoints.addAll(uniqueEndpoints);
        }

        private void add(int fromCol, int fromRow, int col, int row, int distance) {
            byOrigin.computeIfAbsent(nodeKey(fromCol, fromRow), ignored -> new ArrayList<>())
                    .add(new JumpTarget(col, row, distance));
        }
    }

    private static final class JumpTarget {
        final int col;
        final int row;
        final int distance;

        JumpTarget(int col, int row, int distance) {
            this.col = col;
            this.row = row;
            this.distance = distance;
        }
    }
}
