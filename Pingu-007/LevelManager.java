
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class LevelManager {

    private GameCore Game;
    private BufferedImage[] levelSprite;
    private Level level_1;
    private MapDATA mapDataAtual;
    private ArrayList<JumpLink> jumpLinksDaFase;

    public LevelManager(GameCore Game) {
        this.Game = Game;
        importOutsideSprites();
        carregarMapData(LoadSave.LEVEL_2_DATA);
    }

    public void inicializarPrimeiroNivel() {
        Game.processarNovoMapa(mapDataAtual.objects);
    }

    public void carregarNivel(String filename) {
        carregarMapData(filename);
        Game.processarNovoMapa(mapDataAtual.objects);
    }

    private void carregarMapData(String filename) {
        mapDataAtual = LoadSave.GetMapData(filename);
        level_1 = new Level(mapDataAtual.getMainLayer());
        jumpLinksDaFase = PathfindingPreCompiler.gerarJumpLinks(
                mapDataAtual.getMainLayer(), 4);
    }

    private void importOutsideSprites() {
        BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
        if (img == null) {
            throw new RuntimeException("Falha ao carregar atlas: " + LoadSave.LEVEL_ATLAS);
        }

        final int TILE_SIZE = 16;
        int colunas = img.getWidth() / TILE_SIZE;
        int linhas = img.getHeight() / TILE_SIZE;
        levelSprite = new BufferedImage[colunas * linhas];

        int index = 0;
        for (int j = 0; j < linhas; j++) {
            for (int i = 0; i < colunas; i++) {
                int x = i * TILE_SIZE;
                int y = j * TILE_SIZE;
                if (x + TILE_SIZE <= img.getWidth() && y + TILE_SIZE <= img.getHeight()) {
                    levelSprite[index++] = img.getSubimage(x, y, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        System.out.println("Atlas carregado: " + index + " sprites.");
    }

    private void drawFilteredLayers(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura, java.util.function.Predicate<MapDATA.TileLayer> filter) {
        if (mapDataAtual == null || mapDataAtual.layers == null) {
            return;
        }

        double viewLeft = camera.getX();
        double viewTop = camera.getY();
        double viewRight = camera.getX() + (telaLargura / camera.getZoom());
        double viewBottom = camera.getY() + (telaAltura / camera.getZoom());

        int startX = Math.max(0, (int) (viewLeft / GameCore.tiles_size));
        int startY = Math.max(0, (int) (viewTop / GameCore.tiles_size));

        for (int i = 0; i < mapDataAtual.layers.size(); i++) {
            MapDATA.TileLayer layer = mapDataAtual.layers.get(i);

            if (filter.test(layer)) {
                int[][] lvlData = layer.data;
                int endX = Math.min(lvlData[0].length - 1, (int) (viewRight / GameCore.tiles_size) + 1);
                int endY = Math.min(lvlData.length - 1, (int) (viewBottom / GameCore.tiles_size) + 1);

                for (int y = startY; y <= endY; y++) {
                    for (int x = startX; x <= endX; x++) {
                        int id = lvlData[y][x];
                        if (id <= 0) {
                            continue;
                        }

                        int sprite = id - 1;
                        if (sprite >= 0 && sprite < levelSprite.length && levelSprite[sprite] != null) {
                            g2.drawImage(levelSprite[sprite],
                                    x * GameCore.tiles_size,
                                    y * GameCore.tiles_size,
                                    GameCore.tiles_size, GameCore.tiles_size, null);
                        }
                    }
                }
            }
        }
    }

    public void drawBackground(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        drawFilteredLayers(g2, camera, telaLargura, telaAltura, layer -> layer.name.equalsIgnoreCase("sea"));
    }

    public void drawGround(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        drawFilteredLayers(g2, camera, telaLargura, telaAltura, layer -> {
            String n = layer.name.toLowerCase();
            return n.startsWith("b") || n.equals("ground") || n.equals("fence");
        });
    }

    public void drawForeground(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        drawFilteredLayers(g2, camera, telaLargura, telaAltura, layer -> layer.name.toLowerCase().startsWith("t"));
    }

    public MapDATA getMapData() {
        return mapDataAtual;
    }

    public void update() {
    }

    public Level getCurLevel() {
        return level_1;
    }

    public int[][] getCurLevelData() {
        return level_1.getLevelData();
    }

    public ArrayList<JumpLink> getJumpLinks() {
        return jumpLinksDaFase;
    }
}
