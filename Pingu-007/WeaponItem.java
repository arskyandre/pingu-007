
import java.awt.Color;
import java.awt.Graphics2D;

public class WeaponItem extends Item {

    private static final Color COR_PLACEHOLDER = new Color(160, 80, 200);
    private final String tipoArma;

    public WeaponItem(double x, double y, String tipoArma) {
        super(x, y, 28, 28);
        this.tipoArma = tipoArma;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.EQUIPMENT;
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.equiparArma(tipoArma);
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        if (!ativo) {
            return;
        }

        double drawY = getVisualY();
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect((int) x - 2, (int) (y + altura - 4), (int) largura + 4, 8);
        g2.setColor(COR_PLACEHOLDER);
        g2.fillRect((int) x, (int) drawY, (int) largura, (int) altura);
    }

    public String getTipoArma() {
        return tipoArma;
    }
}
