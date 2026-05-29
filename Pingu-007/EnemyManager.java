
import java.awt.Graphics2D;
import java.util.ArrayList;

public class EnemyManager {

    private ArrayList<Enemy> enemies = new ArrayList<>();
    private LevelManager levelManager;
    private int[][] lvlData;

    public EnemyManager(LevelManager levelManager) {
        this.levelManager = levelManager;
        lvlData = levelManager.getCurLevelData();

        enemies.add(new Enemy(100, 100, 40, 40, lvlData));
        enemies.add(new Enemy(500, 400, 40, 40, lvlData));
    }

    public void update(Player player) {
        enemies.removeIf(Entity::isDead);

        for (Enemy enemy : enemies) {
            enemy.update(player);

            for (Enemy outro : enemies) {
                if (enemy != outro) {
                    enemy.separarempilhamento(outro);
                }
            }
        }
    }

    public void draw(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        for (Enemy e : enemies) {
            if (camera.onScreen(e.getX(), e.getY(), e.getLargura(), e.getAltura(), telaLargura, telaAltura)) {
                e.draw(g2);
            }
        }
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

}
