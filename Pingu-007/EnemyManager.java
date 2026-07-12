
import java.util.ArrayList;

public class EnemyManager {

    private final ArrayList<Enemy> enemies = new ArrayList<>();
private final GameCore gameCore;
    private final BulletManager bulmgr;
    private final LevelManager levelManager;
    private ItemManager itemManager;
    private int[][] lvlData;
    private SoundManager soundManager;
    private MorsaBoss morsaAtual;

    public EnemyManager(LevelManager lm, BulletManager bm, SoundManager sound, GameCore GC) {
        gameCore = GC;
        levelManager = lm;
        bulmgr = bm;
        soundManager = sound;
        lvlData = lm.getMapData().getMainLayer();
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    public void setLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
        for (Enemy enemy : enemies) {
            enemy.setLvlData(lvlData);
        }
    }

    public void update(Player player, ArrayList<JumpLink> links) {

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e1 = enemies.get(i);

            if (!e1.isDead()) {
                e1.update(player, links);
                e1.dmgCheck();
            }

            if (e1.isDead()) {
                e1.playDeathSound();
                if (itemManager != null && !e1.isLootProcessado() && e1.podeDropar) {
                    itemManager.gerarDropDeInimigo(e1);
                }
                enemies.remove(i);
                continue;
            }

            double e1CX = e1.getX() + e1.getLargura() / 2.0;
            double e1CY = e1.getY() + e1.getAltura() / 2.0;
            double maxSepDistSq = (GameCore.tiles_size * 3.0) * (GameCore.tiles_size * 3.0);

            for (int j = i - 1; j >= 0; j--) {
                Enemy e2 = enemies.get(j);

                if (e2.isDead()) {
                    continue;
                }
                double dx = e1CX - (e2.getX() + e2.getLargura() / 2.0);
                double dy = e1CY - (e2.getY() + e2.getAltura() / 2.0);

                if (dx * dx + dy * dy <= maxSepDistSq) {
                    e1.separarEmpilhamento(e2);
                }
            }
        }
    }

    public void adicionarInimigo(String tipo, double x, double y, int horda, int arena) {
        adicionarE_RetornarInimigo(tipo, x, y, horda, arena);
    }

    public Enemy adicionarE_RetornarInimigo(String tipo, double x, double y, int horda, int arena) {
        System.out.println("-> ENGINE TENTOU CRIAR O INIMIGO: " + tipo + " nas coordenadas X: " + x + " Y: " + y);
        Enemy novo = null;
        switch (tipo.toLowerCase()) {
            case "lobo" ->
                novo = new BasicEnemy(x, y, 48, 48, lvlData, soundManager);
            case "jumper" ->
                novo = new Jumper(x, y, 48, 48, lvlData, bulmgr, soundManager);
            case "shooter" ->
                novo = new Shooter(x, y, 60, 60, lvlData, bulmgr, soundManager);
            case "dasher" ->
                novo = new Dasher(x, y, 48, 48, lvlData, soundManager);
            case "bomber" ->
                novo = new Bomber(x, y, 48, 48, lvlData, bulmgr, enemies, soundManager);
            case "morsa" -> {
                MorsaBoss morsaInstancia = new MorsaBoss(x, y, lvlData, bulmgr, soundManager, gameCore);

                this.morsaAtual = morsaInstancia;

                BossMao maoEsq = new BossMao(x - 140, y + 64, lvlData, soundManager, morsaInstancia);
                BossMao maoDir = new BossMao(x + (GameCore.tiles_size * 6) + 44, y + 64, lvlData, soundManager, morsaInstancia);

                morsaInstancia.vincularMaos(maoEsq, maoDir);

                enemies.add(maoEsq);
                enemies.add(maoDir);

                novo = morsaInstancia;
            }
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

    public MorsaBoss getMorsaBoss(){
      return morsaAtual;
    }
}
