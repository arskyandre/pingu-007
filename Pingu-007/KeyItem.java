
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class KeyItem extends Item {

    private static final Color COR_PLACEHOLDER = new Color(255, 215, 0);
    private BufferedImage sprite;

    public KeyItem(double x, double y) {
        super(x, y, 32, 32);

        String nomeDoAtlas = "images/tile_set.png";
        BufferedImage atlas = LoadSave.GetSpriteAtlas(nomeDoAtlas);

        int colunasNoAtlas = 14;
        int tileIndex = 23;

        int col = tileIndex % colunasNoAtlas;
        int row = tileIndex / colunasNoAtlas;

        if (atlas != null) {
            sprite = atlas.getSubimage(col * 16, row * 16, 16, 16);
        }
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
            g2.fillRect((int) x + 4, (int) drawY, (int) largura - 8, (int) altura);
            g2.fillRect((int) x + 2, (int) (drawY + altura / 2.0), 6, 6);
        }
    }
}
