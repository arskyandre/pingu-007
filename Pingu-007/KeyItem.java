
import java.awt.Color;
import java.awt.Graphics2D;

public class KeyItem extends Item {

    private static final Color COR_PLACEHOLDER = new Color(255, 215, 0);

    public KeyItem(double x, double y) {
        super(x, y, 20, 20);
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.PUZZLE;
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.addChave(1);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!ativo) {
            return;
        }

        double drawY = getVisualY();
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect((int) x - 2, (int) (y + altura - 4), (int) largura + 4, 6);
        g2.setColor(COR_PLACEHOLDER);
        g2.fillRect((int) x + 4, (int) drawY, (int) largura - 8, (int) altura);
        g2.fillRect((int) x + 2, (int) (drawY + altura / 2.0), 6, 6);
    }
}
