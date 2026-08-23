
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

public abstract class ArenaObject implements DebugRenderable {

    protected final TiledObject data;

    protected ArenaObject(TiledObject data) {
        this.data = data;
    }

    public TiledObject getData() {
        return data;
    }

    @Override
    public TiledObject getDadosTiled() {
        return data;
    }

    @Override
    public Shape getHitboxAtual() {
        if (data.hitbox != null) {
            return data.hitbox;
        } else if (data.isPolygon) {
            return data.getPolygonShape();
        } else if (data.width > 0 && data.height > 0) {
            return new Rectangle2D.Double(data.x, data.y, data.width, data.height);
        }
        return null;
    }

    public abstract String getTipo();

    public abstract void onLoad(ArenaContext context);

    public void update(ArenaContext context, Player player) {
    }

    public void drawOverlay(Graphics2D g2) {
    }

    public boolean handlesInteraction() {
        return false;
    }

    public boolean tryInteract(ArenaContext context, Player player, int chavesDoPlayer) {
        return false;
    }
}
