
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.*;

public class Enemy {

    private double x;
    private double y;

    private double width;
    private double height;

    private double speed = 2;

    

    public Enemy(double x, double y, double width, double height){
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }


    public void update(Player player){
        
        if(player.getX() > x){
            x+=speed;
        }

        if(player.getX() < x){
        x-=speed;
        }

        if(player.getY() > y){
        y+=speed;
        }

        if(player.getY() < y){
        y-=speed;
        }
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
    

