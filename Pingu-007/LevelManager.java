import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class LevelManager{

    private GameCore Game;
    private BufferedImage[] levelSprite;
    private Level level_1;
    private int SpriteNum = 80;
    private int Colunas = 16;
    private int Linhas = 5;

    public LevelManager(GameCore Game){
        this.Game = Game;
        importOutsideSprites();
        //Por enquanto o GetLevelData so pega o do level 1
        //mas em breve poderemos passar pela função o nivel que
        //ele quer carregar
        level_1 = new Level(LoadSave.GetLevelData());

    }

    private void importOutsideSprites(){
        //Importa os Sprites e separa eles no vetor
        BufferedImage img = LoadSave.GetSpriteAtlas(LoadSave.LEVEL_ATLAS);
        levelSprite = new BufferedImage[SpriteNum];
        for(int j=0; j<Linhas; j++){
            for(int i=0; i<Colunas; i++){
                int index = j*Colunas + i;
                levelSprite[index] = img.getSubimage(i*16, j*16, 16, 16);
            }
        }


    }

    public void draw(Graphics g){
        //Desenha os sprites
        for(int j=0; j<Game.tiles_in_height; j++){
            for(int i=0; i<Game.tiles_in_width; i++){
                int index = level_1.getSpriteIndex(i, j);
                g.drawImage(levelSprite[index], i*Game.tiles_size, j*Game.tiles_size,Game.tiles_size,Game.tiles_size, null);
            }
        }
        
    }
    public void update() {

    }


}