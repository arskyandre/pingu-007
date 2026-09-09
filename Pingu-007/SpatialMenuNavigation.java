import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bounds-based navigation used by the responsive Options layout. */
public final class SpatialMenuNavigation implements MenuNavigationStrategy {

    private final Map<Integer, Map<MenuInputIntent, Integer>> explicitNeighbors = new java.util.HashMap<>();

    public SpatialMenuNavigation withNeighbor(int fromIndex, MenuInputIntent direction, int toIndex) {
        explicitNeighbors.computeIfAbsent(fromIndex, ignored -> new EnumMap<>(MenuInputIntent.class))
                .put(direction, toIndex);
        return this;
    }

    @Override
    public int findNext(List<MenuEntry> entries, int currentIndex, MenuInputIntent direction) {
        if (entries.isEmpty() || currentIndex < 0 || currentIndex >= entries.size()) {
            return currentIndex;
        }
        if (direction != MenuInputIntent.UP && direction != MenuInputIntent.DOWN
                && direction != MenuInputIntent.LEFT && direction != MenuInputIntent.RIGHT) {
            return currentIndex;
        }

        Map<MenuInputIntent, Integer> overrides = explicitNeighbors.get(currentIndex);
        if (overrides != null) {
            Integer target = overrides.get(direction);
            if (target != null && target >= 0 && target < entries.size()
                    && entries.get(target).isFocusable()) {
                return target;
            }
        }

        Rectangle current = entries.get(currentIndex).getFocusBoundsCopy();
        int candidateIndex = currentIndex;
        int bestScore = Integer.MAX_VALUE;
        for (int index = 0; index < entries.size(); index++) {
            if (index == currentIndex || !entries.get(index).isFocusable()) {
                continue;
            }

            Rectangle candidate = entries.get(index).getFocusBoundsCopy();
            if (direction == MenuInputIntent.UP || direction == MenuInputIntent.DOWN) {
                int deltaY = candidate.y - current.y;
                int requestedSign = direction == MenuInputIntent.DOWN ? 1 : -1;
                if (Integer.signum(deltaY) != requestedSign) {
                    continue;
                }
                int score = Math.abs(deltaY) * 100 + Math.abs(candidate.x - current.x);
                if (score < bestScore) {
                    bestScore = score;
                    candidateIndex = index;
                }
            } else {
                int deltaX = candidate.x - current.x;
                int requestedSign = direction == MenuInputIntent.RIGHT ? 1 : -1;
                if (Integer.signum(deltaX) != requestedSign
                        || Math.abs(candidate.y - current.y) > Math.max(current.height, candidate.height)) {
                    continue;
                }
                int score = Math.abs(deltaX);
                if (score < bestScore) {
                    bestScore = score;
                    candidateIndex = index;
                }
            }
        }
        return candidateIndex;
    }
}
