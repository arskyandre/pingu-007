
import java.awt.geom.Rectangle2D;

public final class ArenaTriggers {

    private ArenaTriggers() {
    }

    public static boolean collides(TiledObject trigger, Player player) {
        if (trigger == null || player == null) {
            return false;
        }

        if (trigger.isPolygon) {
            return trigger.getPolygonShape().contains(player.getX(), player.getY());
        }

        Rectangle2D.Double rect = new Rectangle2D.Double(trigger.x, trigger.y, trigger.width, trigger.height);
        return rect.intersects(player.getX(), player.getY(), player.getLargura(), player.getAltura());
    }
}
