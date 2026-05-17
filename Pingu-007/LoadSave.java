
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

//LoadSave serve apenas para Pegar Imagens e Data do Mapa!
//Não é necessario criar um objeto para usar a função
public class LoadSave {

    //public static final String PLAYER_ATLAS = "player_sprites.png";
    public static final String LEVEL_ATLAS = "tileset_placeholder.png";
    public static final String LEVEL_1_DATA = "LEVEL_1_DATA.png";
    
    public static BufferedImage GetSpriteAtlas(String filename){
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream("/" + filename);
        try{
            img = ImageIO.read(is);
        }catch (IOException ex){
            ex.printStackTrace();
        } finally{
            try{
                is.close();
            } catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return img;
    }
    

    //TODO: Implementar para que a função receba qual
    //level ele quer pegar data, por enquanto so pega do level 1
    public static int[][] GetLevelData(){
        int[][] lvlData = new int [GameCore.tiles_in_height][GameCore.tiles_in_width];
        BufferedImage img = GetSpriteAtlas(LEVEL_1_DATA);

        for(int j=0; j<img.getHeight(); j++){
            for(int i=0; i<img.getWidth(); i++){
                Color color = new Color(img.getRGB(i, j));
                int value = color.getRed();
                if(value>=80)
                    value=18;
                lvlData[j][i] = value;
                //Debug
                    //System.out.print(value+" ");
            }
            //Debug
                //System.out.println();

        }
        return lvlData;
    }
    
}
