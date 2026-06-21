import java.awt.Graphics2D;

public abstract class ArenaObject {

    protected final TiledObject data;

    protected ArenaObject(TiledObject data) {
        this.data = data;
    }

    public TiledObject getData() {
        return data;
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
