import java.awt.Graphics2D;
import java.util.ArrayList;

public class EnemyManager {
    
    private ArrayList<Enemy> enemies = new ArrayList<>();

    public EnemyManager(){
    enemies.add(new Enemy(100, 100, 40 ,40));

    enemies.add(new Enemy(500, 400, 40 ,40));
    
}

    public void update(Player player){
        for(Enemy enemy: enemies){
            enemy.update(player);

            for(Enemy outro: enemies){

                if(enemy != outro){
                    enemy.separarempilhamento(outro);
                }
            }
        }
    }

    public void draw(Graphics2D g2){
        for(Enemy enemy: enemies){
            enemy.draw(g2);
        }
    }

}

