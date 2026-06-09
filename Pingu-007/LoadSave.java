
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.imageio.ImageIO;

//LoadSave serve apenas para Pegar Imagens e Data do Mapa!
//Não é necessario criar um objeto para usar a função
public class LoadSave {

    //public static final String PLAYER_ATLAS = "player_sprites.png";
    public static final String LEVEL_ATLAS = "tileset_placeholder.png";
    public static final String LEVEL_1_DATA = "LEVEL_1_DATA.json";
    
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
    

    //Agora a função pode receber o nome do arquivo, permitindo carregar qualquer level!
    public static int[][] GetLevelData(String filename){
        int[][] lvlData = new int [GameCore.tiles_in_height][GameCore.tiles_in_width];
        InputStream is = LoadSave.class.getResourceAsStream("/" + filename);

        try(Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())){
            String jsonText = scanner.useDelimiter("\\A").next();

            int startIndex = jsonText.indexOf("\"data\":[") + 8;
            int endIndex = jsonText.indexOf("]", startIndex);
            
            String dataString = jsonText.substring(startIndex, endIndex);
            String[] stringValues = dataString.split(",");

            int index = 0;

            for(int j = 0; j < GameCore.tiles_in_height; j++){
                for(int i = 0; i < GameCore.tiles_in_width; i++){
                    int tileValue = Integer.parseInt(stringValues[index].trim());

                    lvlData[j][i] = tileValue;
                    index++;
                }
            }
        }catch(Exception e){
            System.err.println("Erro ao ler arquivo JSON do mapa: " + filename);
            e.printStackTrace();
        }

        return lvlData;
    }
    
}
