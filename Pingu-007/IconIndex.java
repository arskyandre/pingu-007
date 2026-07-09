
import java.awt.image.BufferedImage;

public enum IconIndex {
    UNMUTED(0, 0),
    MUTED(1, 0),
    FISHING(2, 0),
    FULLSCREEN(3, 0),
    FULLSCREEN_HELD(4,0);

    private final int col, row;
    private static final int ICON_SIZE = 32;

    IconIndex(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public BufferedImage getSprite() {
        if (ButtonIcons.getIMG() == null)
            return null;
        return ButtonIcons.getIMG().getSubimage(col * ICON_SIZE, row * ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}