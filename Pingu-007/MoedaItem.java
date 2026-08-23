
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class MoedaItem extends ConsumableItem {

    private static final Color COR_PLACEHOLDER = new Color(50, 200, 80);
    private final int quantidade;

    public MoedaItem(double x, double y) {
        this(x, y, 10);

    }

    public MoedaItem(double x, double y, int qtd) {
        super(x, y, 32, 32);
        quantidade = qtd;
        String nomeDoAtlas = "images/tile_set.png";
        BufferedImage atlas = LoadSave.GetSpriteAtlas(nomeDoAtlas);

        int colunasNoAtlas = 14;
        int tileIndex = 66;

        int col = tileIndex % colunasNoAtlas;
        int row = tileIndex / colunasNoAtlas;

        if (atlas != null) {
            sprite = atlas.getSubimage(col * 16, row * 16, 16, 16);
        }
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.addMoedas(quantidade);
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        if (!ativo) {
            return;
        }
        double drawY = getVisualY();
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect((int) x - 2, (int) (y + altura - 4), (int) largura + 4, 6);

        if (sprite != null) {
            g2.drawImage(sprite, (int) x, (int) drawY, (int) largura, (int) altura, null);
        } else {
            g2.setColor(COR_PLACEHOLDER);
            g2.fillRect((int) x, (int) drawY, (int) largura, (int) altura);
        }
    }

    public int getQuantidade() {
        return quantidade;
    }
}
