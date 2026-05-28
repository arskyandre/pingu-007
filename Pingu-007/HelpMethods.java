public class HelpMethods {
    
    public static boolean CanMoveHere(double x, double y, double largura, double altura, int[][] lvlData){
        if(!isSolid(x, y, lvlData)){
            if(!isSolid(x+largura, y+altura, lvlData)){
                if(!isSolid(x+largura, y, lvlData)){
                    if(!isSolid(x, y+altura, lvlData)){
                        return true;
                    }  
                }
            }     
        }
            
        return false;

    }


    public static boolean isSolid(double x, double y, int[][]lvlData) {
        double yIndex = y / GameCore.tiles_size;
        double xIndex = x / GameCore.tiles_size;
        int tile;

        try{
            tile = lvlData[(int) yIndex][(int) xIndex];
        }catch(Exception e){
            System.out.println("Out of bounds");
            tile=-1;
        }
        
        //System.out.println(tile);
        if (tile>=80||tile<0||(tile!=17)){
            return true;
        }
        else{
            return false;
        }

    }

}
