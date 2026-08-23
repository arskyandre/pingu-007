
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class HealthPackItem extends ConsumableItem {

    private static final Color COR_PLACEHOLDER = new Color(50, 200, 80);
    private final int quantidade;

    public HealthPackItem(double x, double y) {
        this(x, y, 15);
        String nomeDoAtlas = "images/tile_set.png";
        BufferedImage atlas = LoadSave.GetSpriteAtlas(nomeDoAtlas);

        int colunasNoAtlas = 14;
        int tileIndex = 37;

        int col = tileIndex % colunasNoAtlas;
        int row = tileIndex / colunasNoAtlas;

        if (atlas != null) {
            sprite = atlas.getSubimage(col * 16, row * 16, 16, 16);
        }
    }

    public HealthPackItem(double x, double y, int quantidade) {
        super(x, y, 32, 32);
        this.quantidade = quantidade;
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.curar(quantidade);
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        if (!ativo) {
            return;
        }
        double drawY = getVisualY();

        if (sprite != null) {
            ProjectedShadow.drawForEntity(g2, x, y, largura, altura,
                    new ProjectedShadow.Part(sprite, (int) x, (int) drawY,
                            (int) largura, (int) altura));
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
