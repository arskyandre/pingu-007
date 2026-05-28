
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.*;

public class Enemy {

    private double x;
    private double y;

    private double width;
    private double height;

    private double speed = 2;

    private double velX=0;
    private double velY=0;

    protected Rectangle hitbox;
    private int[][] lvlData;
    

    public Enemy(double x, double y, double width, double height, int[][] lvlData){
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
        hitbox = new Rectangle((int)x, (int)y, (int)width, (int)height);
        this.lvlData = lvlData;
    }


    public void update(Player player){

        if(player.getX() > x){
            velX = speed;
        }

        if(player.getX() < x){
            velX = -speed;
        }

        if(player.getY() > y){
            velY = speed;
        }

        if(player.getY() < y){
            velY = -speed;
        }

        //COLISAO Com Tiles
        int proxX = (int)(hitbox.x + velX);
        int proxY = (int)(hitbox.y + velY);
        if(!HelpMethods.CanMoveHere(proxX,hitbox.y,width,height,lvlData)){
            if(velX>0){
                hitbox.x = (int)(proxX - ((proxX+width)%GameCore.tiles_size)-1);
            }
            else if(velX<0){
                hitbox.x = (int)(proxX + (GameCore.tiles_size-(proxX%GameCore.tiles_size)));
            }
            velX=0;
        }
        else{
            hitbox.x += velX;
        }
        x = hitbox.x;
        
        if(!HelpMethods.CanMoveHere(hitbox.x,proxY,width,height,lvlData)){
            if(velY>0){
                hitbox.y = (int)(proxY - ((proxY+height)%GameCore.tiles_size)-1);
            }
            else if(velY<0){
                hitbox.y = (int)(proxY + (GameCore.tiles_size-(proxY%GameCore.tiles_size)));
            }
            velY=0;
        }
        else{
            hitbox.y += velY;
        }
        y = hitbox.y;
        //
        
        
    }

    public void draw(Graphics2D g2){
        g2.setColor(Color.MAGENTA);


        g2.fill(new Rectangle2D.Double(x,y,width, height));
}
    public double getX(){
        return x;
    }

    public double getY(){
        return y;
    }

    public void separarempilhamento(Enemy outro){
        
        double dx = x - outro.x;
        double dy = y - outro.y;

        double distancia = Math.sqrt(dx*dx+dy*dy);

        double minDistancia = width;

        if(distancia < minDistancia && distancia > 0){

            double sob = minDistancia - distancia;

            x += (dx/distancia)*sob*0.5;
            y += (dy/distancia)*sob*0.5;
        }
    }

}
    

