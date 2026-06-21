import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Central atlas accessor for arena visuals sourced from tile_set.png.
 * Tile IDs match the map matrix convention (1-based GID values).
 */
public final class ArenaAtlas {

    private static BufferedImage[] tileSprites;
    private static int tileSize = GameCore.tiles_default_size;

    private ArenaAtlas() {
    }

    public static void init() {
        if (tileSprites != null) {
            return;
        }

        BufferedImage atlas = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
        int columns = atlas.getWidth() / tileSize;
        int rows = atlas.getHeight() / tileSize;
        tileSprites = new BufferedImage[columns * rows + 1];

        int index = 1;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = col * tileSize;
                int y = row * tileSize;
                tileSprites[index++] = atlas.getSubimage(x, y, tileSize, tileSize);
            }
        }
    }

    public static BufferedImage getTileSprite(int tileId) {
        init();
        if (tileId <= 0 || tileId >= tileSprites.length) {
            return null;
        }
        return tileSprites[tileId];
    }

    public static void drawTile(Graphics2D g2, int tileId, double worldX, double worldY, double worldW, double worldH) {
        BufferedImage sprite = getTileSprite(tileId);
        if (sprite == null) {
            return;
        }
        g2.drawImage(sprite, (int) worldX, (int) worldY, (int) worldW, (int) worldH, null);
    }

    public static int getDefaultWallTileId() {
        return 125;
    }

    public static int getButtonUpTileId() {
        return 52;
    }

    public static int getButtonDownTileId() {
        return 53;
    }
}
