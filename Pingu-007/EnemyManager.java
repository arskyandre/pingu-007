
import java.util.ArrayList;

public class EnemyManager {

    private final ArrayList<Enemy> enemies = new ArrayList<>();

    private final BulletManager bulmgr;
    private final LevelManager levelManager;
    private int[][] lvlData;

    public EnemyManager(LevelManager lm, BulletManager bm) {
        levelManager = lm;
        bulmgr = bm;
        lvlData = lm.getMapData().getMainLayer();
    }

    public void update(Player player, ArrayList<JumpLink> links) {
        enemies.removeIf(Enemy::isDead);

        for (int i = 0; i < enemies.size(); i++) {
            Enemy e1 = enemies.get(i);

            e1.update(player, links);

            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy e2 = enemies.get(j);
                e1.separarEmpilhamento(e2);
            }
        }
    }

    public void adicionarInimigo(String tipo, double x, double y, int horda, int arena) {
        adicionarE_RetornarInimigo(tipo, x, y, horda, arena);
    }

    public Enemy adicionarE_RetornarInimigo(String tipo, double x, double y, int horda, int arena) {
        Enemy novo = null;
        switch (tipo.toLowerCase()) {
            case "lobo" ->
                novo = new BasicEnemy(x, y, 48, 48, lvlData);
            case "jumper" ->
                novo = new Jumper(x, y, 48, 48, lvlData, bulmgr);
            case "shooter" ->
                novo = new Shooter(x, y, 60, 60, lvlData, bulmgr);
            case "dasher" ->
                novo = new Dasher(x, y, 48, 48, lvlData);
            case "bomber" ->
                novo = new Bomber(x, y, 48, 48, lvlData, bulmgr, enemies);
        }
        if (novo != null) {
            enemies.add(novo);
        }
        return novo;
    }

    public void limparTudo() {
        enemies.clear();
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }
}
