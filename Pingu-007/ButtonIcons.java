import java.awt.image.BufferedImage;

public class ButtonIcons {
    private static BufferedImage img;
    static {
        try {
            img = LoadSave.GetSpriteAtlas("images/hud/button_icons.png");

        } catch (Exception e) {
            System.err.println("Erro ao carregar images/hud/button_icons.png: " + e.getMessage());
        }
    }

    public static BufferedImage getIMG() {
        return img;
    }
}
