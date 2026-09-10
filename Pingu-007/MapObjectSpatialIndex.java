import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

public final class MapObjectSpatialIndex {

    private static final int CELL_SIZE = 128;
    private static final IdentityHashMap<ArrayList<MapObject>, Cache> caches = new IdentityHashMap<>();
    private static final ThreadLocal<ArrayList<MapObject>> results = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<IdentityHashMap<MapObject, Boolean>> seen = ThreadLocal.withInitial(IdentityHashMap::new);

    private MapObjectSpatialIndex() {
    }

    public static ArrayList<MapObject> query(ArrayList<MapObject> objects, Rectangle2D area) {
        ArrayList<MapObject> result = results.get();
        result.clear();
        if (objects == null || objects.isEmpty() || area == null) {
            return result;
        }

        Cache cache;
        synchronized (caches) {
            cache = caches.get(objects);
            long version = MapObject.getSpatialVersion();
            if (cache == null || cache.size != objects.size() || cache.version != version) {
                cache = new Cache(objects, version);
                caches.put(objects, cache);
            }
        }

        IdentityHashMap<MapObject, Boolean> visited = seen.get();
        visited.clear();
        int minCol = cell(area.getMinX());
        int maxCol = cell(area.getMaxX());
        int minRow = cell(area.getMinY());
        int maxRow = cell(area.getMaxY());

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                ArrayList<MapObject> bucket = cache.buckets.get(key(col, row));
                if (bucket == null) {
                    continue;
                }
                for (MapObject object : bucket) {
                    if (visited.put(object, Boolean.TRUE) == null) {
                        result.add(object);
                    }
                }
            }
        }
        return result;
    }

    private static int cell(double value) {
        return (int) Math.floor(value / CELL_SIZE);
    }

    private static long key(int col, int row) {
        return ((long) col << 32) ^ (row & 0xffffffffL);
    }

    private static final class Cache {
        final HashMap<Long, ArrayList<MapObject>> buckets = new HashMap<>();
        final int size;
        final long version;

        Cache(ArrayList<MapObject> objects, long version) {
            this.size = objects.size();
            this.version = version;
            for (MapObject object : objects) {
                if (object == null || !object.isSolid()) {
                    continue;
                }
                Shape hitbox = object.getHitbox();
                if (hitbox == null) {
                    continue;
                }
                Rectangle2D bounds = hitbox.getBounds2D();
                int minCol = cell(bounds.getMinX());
                int maxCol = cell(bounds.getMaxX());
                int minRow = cell(bounds.getMinY());
                int maxRow = cell(bounds.getMaxY());
                for (int row = minRow; row <= maxRow; row++) {
                    for (int col = minCol; col <= maxCol; col++) {
                        buckets.computeIfAbsent(key(col, row), ignored -> new ArrayList<>()).add(object);
                    }
                }
            }
        }
    }
}
