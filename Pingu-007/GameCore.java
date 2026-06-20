
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    JFrame frame;
    boolean running = true;

    private final CameraManager camera;
    private EnemyManager enemyManager;
    private final Player player;
    private final Hud hud;
    private BulletManager bulletmanager;
    private LootManager lootmanager;
    private final InputManager input;
    private final Renderer renderer;
    private final LevelManager levelManager;
    private ArenaManager arenaManager;

    private int debugSpawnCooldown = 0;
    private int mapLoadCooldown = 0;

    public final static int tiles_default_size = 16;
    public final static float scale = 3f;
    public final static int tiles_in_width = 26;
    public final static int tiles_in_height = 14;
    public final static int tiles_size = (int) (tiles_default_size * scale);
    public final static int game_width = tiles_size * tiles_in_width;
    public final static int game_height = tiles_size * tiles_in_height;

    public GameCore() {
        setPreferredSize(new Dimension(game_width, game_height));
        setBackground(Color.BLACK);

        bulletmanager = new BulletManager();
        lootmanager = new LootManager();
        input = new InputManager();
        player = new Player(380, 500, tiles_size - 1, tiles_size - 1, bulletmanager);

        renderer = new Renderer();
        renderer.modoDebug = false;
        levelManager = new LevelManager(this);

        enemyManager = new EnemyManager(levelManager, bulletmanager);
        arenaManager = new ArenaManager(enemyManager, levelManager);
        camera = new CameraManager(player.getX(), player.getY(), 1.25);
        hud = new Hud();

        levelManager.inicializarPrimeiroNivel();

        player.loadLvlData(levelManager.getCurLevelData());

        addKeyListener(input);
        addMouseMotionListener(input);
        addMouseListener(input);
        setFocusable(true);
        requestFocus();
    }

    public void update() {
        player.testemunicao(input, getWidth(), getHeight(), lootmanager, camera);

        player.update(input, getWidth(), getHeight(), camera);
        lootmanager.update(player);

        ArrayList<JumpLink> linksAtuais = levelManager.getJumpLinks();
        enemyManager.update(player, linksAtuais);

        arenaManager.update(player);

        bulletmanager.update(camera, getWidth(), getHeight(),
                player, enemyManager.getEnemies());

        levelManager.update();
        camera.update(player, input, getWidth(), getHeight());

        // FUNÇÕES DE DEBUG
        if (debugSpawnCooldown > 0) {
            debugSpawnCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_K) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            enemyManager.adicionarInimigo("jumper", mouseXWorld, mouseYWorld, 0, -1);
            System.out.println("DEBUG: Dasher spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 30;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_E) && debugSpawnCooldown <= 0) {
            //arenaManager.interagir(player, chavesQueOPlayerTem);
        }

        if (mapLoadCooldown > 0) {
            mapLoadCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_1) && mapLoadCooldown <= 0) {
            System.out.println("Voltando para o Mapa 1...");
            levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
            mapLoadCooldown = 60;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_2) && mapLoadCooldown <= 0) {
            System.out.println("Indo para o Mapa 2 de Testes...");
            // Substitua pelo nome exato do seu arquivo JSON de teste
            levelManager.carregarNivel(LoadSave.LEVEL_2_DATA);
            mapLoadCooldown = 60;
        }
    }

    public void processarNovoMapa(ArrayList<TiledObject> objetosDoMapa) {
        enemyManager.limparTudo();
        bulletmanager.limparTudo();

        for (TiledObject obj : objetosDoMapa) {
            if (!obj.isScaled) {
                obj.x *= GameCore.scale;
                obj.y *= GameCore.scale;
                obj.width *= GameCore.scale;
                obj.height *= GameCore.scale;

                if (obj.isPolygon && obj.polygonXs != null) {
                    for (int i = 0; i < obj.polygonXs.length; i++) {
                        obj.polygonXs[i] *= GameCore.scale;
                        obj.polygonYs[i] *= GameCore.scale;
                    }
                }
                obj.isScaled = true;
            }
        }

        arenaManager.carregarObjetos(objetosDoMapa);

        for (TiledObject obj : objetosDoMapa) {
            String tipoSeguro = obj.tipo != null ? obj.tipo.toLowerCase() : "";
            switch (tipoSeguro) {
                case "spawn_player" -> {
                    player.setX(obj.x);
                    player.setY(obj.y);
                }

                case "spawner" -> {
                    if (obj.id_arena < 0) {
                        enemyManager.adicionarInimigo(
                                obj.inimigo, obj.x, obj.y,
                                obj.horda, obj.id_arena);
                    }
                }
            }
        }

        player.loadLvlData(levelManager.getCurLevelData());
    }

    public void render(BufferStrategy bs) {
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                renderer.renderizar(g2, camera, player, input,
                        getWidth(), getHeight(),
                        levelManager, bulletmanager, lootmanager,
                        enemyManager, arenaManager, hud);
                g2.dispose();
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
        Toolkit.getDefaultToolkit().sync();
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    @Override
    public void run() {
        createBufferStrategy(3);
        BufferStrategy bs = getBufferStrategy();
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            render(bs);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public static void main(String[] args) {
        GameCore game = new GameCore();
        game.frame = new JFrame("Pingu 007 (ALPHA)");
        game.frame.add(game);
        game.frame.pack();
        game.frame.setLocationRelativeTo(null);
        game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.frame.setResizable(false);
        game.frame.setVisible(true);
        game.start();
    }
}
