
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class OcclusionCuller {

    private static final double CELL_SIZE = 128.0;
    private static final double CONTAINMENT_EPSILON = 0.01;

    private final Map<Long, ArrayList<Rectangle2D>> opaqueCells = new HashMap<>();
    private final ArrayList<ArrayList<Rectangle2D>> activeCellLists = new ArrayList<>();
    private boolean[] occluded = new boolean[256];

    public void filter(ArrayList<Renderable> depthSortedInput, ArrayList<Renderable> output) {
        int size = depthSortedInput.size();
        ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            occluded[i] = false;
        }

        for (ArrayList<Rectangle2D> list : activeCellLists) {
            list.clear();
        }
        activeCellLists.clear();

        for (int i = size - 1; i >= 0; i--) {
            Renderable renderable = depthSortedInput.get(i);
            Rectangle2D visibleBounds = renderable.getOcclusionBounds();
            if (isUsable(visibleBounds) && isFullyCovered(visibleBounds)) {
                occluded[i] = true;
                continue;
            }

            Rectangle2D opaqueBounds = renderable.getOpaqueOcclusionBounds();
            if (isUsable(opaqueBounds)) {
                addOpaqueRegion(opaqueBounds);
            }
        }

        output.clear();
        output.ensureCapacity(size);
        for (int i = 0; i < size; i++) {
            if (!occluded[i]) {
                output.add(depthSortedInput.get(i));
            }
        }
    }

    private boolean isFullyCovered(Rectangle2D target) {
        int minCellX = cell(target.getMinX());
        int maxCellX = cell(Math.nextDown(target.getMaxX()));
        int minCellY = cell(target.getMinY());
        int maxCellY = cell(Math.nextDown(target.getMaxY()));

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                ArrayList<Rectangle2D> candidates = opaqueCells.get(cellKey(cellX, cellY));
                if (candidates == null) {
                    continue;
                }
                for (Rectangle2D opaque : candidates) {
                    if (contains(opaque, target)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addOpaqueRegion(Rectangle2D bounds) {
        int minCellX = cell(bounds.getMinX());
        int maxCellX = cell(Math.nextDown(bounds.getMaxX()));
        int minCellY = cell(bounds.getMinY());
        int maxCellY = cell(Math.nextDown(bounds.getMaxY()));

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                long key = cellKey(cellX, cellY);
                ArrayList<Rectangle2D> cellList = opaqueCells.get(key);
                if (cellList == null) {
                    cellList = new ArrayList<>(4);
                    opaqueCells.put(key, cellList);
                }
                if (cellList.isEmpty()) {
                    activeCellLists.add(cellList);
                }
                cellList.add(bounds);
            }
        }
    }

    private void ensureCapacity(int size) {
        if (occluded.length >= size) {
            return;
        }
        int newSize = occluded.length;
        while (newSize < size) {
            newSize *= 2;
        }
        occluded = new boolean[newSize];
    }

    private static boolean isUsable(Rectangle2D bounds) {
        return bounds != null
                && bounds.getWidth() > 0.0 && bounds.getHeight() > 0.0
                && Double.isFinite(bounds.getMinX()) && Double.isFinite(bounds.getMinY())
                && Double.isFinite(bounds.getMaxX()) && Double.isFinite(bounds.getMaxY());
    }

    private static boolean contains(Rectangle2D outer, Rectangle2D inner) {
        return outer.getMinX() <= inner.getMinX() + CONTAINMENT_EPSILON
                && outer.getMinY() <= inner.getMinY() + CONTAINMENT_EPSILON
                && outer.getMaxX() >= inner.getMaxX() - CONTAINMENT_EPSILON
                && outer.getMaxY() >= inner.getMaxY() - CONTAINMENT_EPSILON;
    }

    private static int cell(double coordinate) {
        return (int) Math.floor(coordinate / CELL_SIZE);
    }

    private static long cellKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }
}
