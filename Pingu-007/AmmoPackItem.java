
import java.awt.Color;
import java.awt.Graphics2D;

public class AmmoPackItem extends ConsumableItem {

    private static final Color COR_PLACEHOLDER = new Color(210, 160, 40);
    private final int quantidade;

    public AmmoPackItem(double x, double y) {
        this(x, y, 15);
    }

    public AmmoPackItem(double x, double y, int quantidade) {
        super(x, y, 24, 24);
        this.quantidade = quantidade;
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.addMunicao(quantidade);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!ativo) {
            return;
        }

        double drawY = getVisualY();
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect((int) x - 2, (int) (y + altura - 4), (int) largura + 4, 8);

        g2.setColor(COR_PLACEHOLDER);
        g2.fillRect((int) x, (int) drawY, (int) largura, (int) altura);
    }

    public int getQuantidade() {
        return quantidade;
    }
}
