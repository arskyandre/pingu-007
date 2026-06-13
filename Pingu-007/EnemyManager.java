
import java.awt.Graphics2D;
import java.util.ArrayList;

public class EnemyManager {

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final LevelManager levelManager;
    private final BulletManager bulmgr;
    private final int[][] lvlData;

    public EnemyManager(LevelManager levelManager, BulletManager bulletManager) {
        this.levelManager = levelManager;
        lvlData = levelManager.getCurLevelData();

        this.bulmgr = bulletManager;
        //enemies.add(new Jumper(100, 100, 40, 40, this.lvlData, this.bulmgr));
        enemies.add(new Shooter(100, 100, 60, 60, this.lvlData, this.bulmgr));
        enemies.add(new Dasher(100, 100, 48, 48, lvlData));
        enemies.add(new BasicEnemy(500, 400, 48, 48, lvlData));
        enemies.add(new Bomber(100, 100, 48, 48, lvlData, bulmgr, enemies));
        enemies.add(new Bomber(100, 100, 48, 48, lvlData, bulmgr, enemies));
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
                e.animate(g2);
            }
        }
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

}
