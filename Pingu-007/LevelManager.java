
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class LevelManager {

    private GameCore Game;
    private BufferedImage[] levelSprite;
    private Level level_1;
    private int SpriteNum = 80;
    private int Colunas = 16;
    private int Linhas = 5;

    public LevelManager(GameCore Game) {
        this.Game = Game;
        importOutsideSprites();
        //Por enquanto o GetLevelData so pega o do level 1
        //mas em breve poderemos passar pela função o nivel que
        //ele quer carregar
        level_1 = new Level(LoadSave.GetLevelData());

    }

    private void importOutsideSprites() {
        //Importa os Sprites e separa eles no vetor
        BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
        levelSprite = new BufferedImage[SpriteNum];
        for (int j = 0; j < Linhas; j++) {
            for (int i = 0; i < Colunas; i++) {
                int index = j * Colunas + i;
                levelSprite[index] = img.getSubimage(i * 16, j * 16, 16, 16);
            }
        }

    }

    public void draw(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        int[][] lvlData = getCurLevelData();
        int maxLinhas = lvlData.length;
        int maxColunas = lvlData[0].length;

        // Calcula a área visível do mundo baseando-se na posição da câmera e no zoom
        double viewLeft = camera.getX();
        double viewTop = camera.getY();
        double viewRight = camera.getX() + (telaLargura / camera.getZoom());
        double viewBottom = camera.getY() + (telaAltura / camera.getZoom());

        // Converte as coordenadas de pixel da visão para índices de Tiles da sua matriz
        int startCol = Math.max(0, (int) (viewLeft / GameCore.tiles_size));
        int endCol = Math.min(maxColunas - 1, (int) (viewRight / GameCore.tiles_size) + 1);

        int startRow = Math.max(0, (int) (viewTop / GameCore.tiles_size));
        int endRow = Math.min(maxLinhas - 1, (int) (viewBottom / GameCore.tiles_size) + 1);

        // Renderiza apenas as linhas e colunas calculadas como visíveis
        for (int j = startRow; j <= endRow; j++) {
            for (int i = startCol; i <= endCol; i++) {

                int index = level_1.getSpriteIndex(i, j);

                g2.drawImage(levelSprite[index],
                        i * GameCore.tiles_size,
                        j * GameCore.tiles_size,
                        GameCore.tiles_size,
                        GameCore.tiles_size,
                        null);
            }
        }
    }

    public void update() {

    }

    public Level getCurLevel() {
        return level_1;
    }

    public int[][] getCurLevelData() {
        return getCurLevel().getLevelData();
    }

}
