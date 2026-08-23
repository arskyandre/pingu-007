
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class FishingRodItem extends Item {

    private static final Color COR_HASTE = new Color(139, 69, 19);
    private BufferedImage sprite;

    public FishingRodItem(double x, double y) {
        super(x, y, 32, 32);

        String nomeDoAtlas = "images/tile_set.png";
        BufferedImage atlas = LoadSave.GetSpriteAtlas(nomeDoAtlas);

        int colunasNoAtlas = 14;
        int tileIndex = 38;

        int col = tileIndex % colunasNoAtlas;
        int row = tileIndex / colunasNoAtlas;

        if (atlas != null) {
            sprite = atlas.getSubimage(col * 16, row * 16, 16, 16);
        }
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.EQUIPMENT;
    }

    @Override
    protected void aplicarEfeito(Player player) {
        player.setFishingRod(true);
        System.out.println("Vara de pesca coletada e equipada!");
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        if (!ativo) {
            return;
        }

        double drawY = getVisualY();

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect((int) x - 2, (int) (y + altura - 4), (int) largura + 4, 8);

        if (sprite != null) {
            g2.drawImage(sprite, (int) x, (int) drawY, (int) largura, (int) altura, null);
        } else {
            g2.setColor(COR_HASTE);
            g2.fillRect((int) x + 8, (int) drawY, 4, (int) altura);
            g2.setColor(Color.WHITE);
            g2.drawLine((int) x + 10, (int) drawY, (int) x + 20, (int) drawY + 15);
        }

    }
}
