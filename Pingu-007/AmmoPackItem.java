
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
    public void draw(Graphics2D g2, double delta) {
        if (!ativo) {
            return;
        }

        double drawY = getVisualY();
        ProjectedShadow.drawForEntity(g2, x, y, largura, altura,
                ProjectedShadow.solidPart((int) x, (int) drawY,
                        (int) largura, (int) altura));

        g2.setColor(COR_PLACEHOLDER);
        g2.fillRect((int) x, (int) drawY, (int) largura, (int) altura);
    }

    public int getQuantidade() {
        return quantidade;
    }
}
