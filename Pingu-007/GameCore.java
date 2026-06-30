
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    private GameState gameState = GameState.MAIN_MENU;
    private final MainMenu mainMenu;
    private final PauseMenu pauseMenu;
    private final OptionsMenu optionsMenu;
    private final GameOverScreen gameOverScreen;

    private double checkX, checkY;
    private int checkVida, checkMunicao, checkPente, checkChaves;
    private int chavesColetadasCheckpoint = 0;
    private ArrayList<Integer> checkArenas = new ArrayList<>();
    private boolean hasCheckpoint = false;

    JFrame frame;
    boolean running = true;

    private final CameraManager camera;
    private CutsceneManager cutsceneManager;
    private EnemyManager enemyManager;
    private final Player player;
    private final Hud hud;
    private BulletManager bulletmanager;
    private ItemManager itemManager;
    private final InputManager input;
    private final Renderer renderer;
    private final LevelManager levelManager;
    private ArenaManager arenaManager;
    private DialogueManager dialogueManager;
    private SoundManager soundManager;

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
        soundManager = new SoundManager();
        gameOverScreen = new GameOverScreen(soundManager);
        mainMenu = new MainMenu(soundManager);
        pauseMenu = new PauseMenu(soundManager);
        optionsMenu = new OptionsMenu(soundManager);
        bulletmanager = new BulletManager();
        itemManager = new ItemManager();
        input = new InputManager();
        player = new Player(380, 500, tiles_size - 1, tiles_size - 1, bulletmanager, soundManager);

        renderer = new Renderer();
        renderer.modoDebug = false;
        levelManager = new LevelManager(this);

        itemManager = new ItemManager();
        enemyManager = new EnemyManager(levelManager, bulletmanager, soundManager);
        enemyManager.setItemManager(itemManager);
        arenaManager = new ArenaManager(enemyManager, levelManager, itemManager);
        dialogueManager = new DialogueManager();
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
        switch (gameState) {
            case MAIN_MENU -> {
                GameState next = mainMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    soundManager.playBGM(SoundManager.BGM.LEVEL_1);
                    player.setShootCooldownTimer(30);
                }
                gameState = next;
            }
            case PLAYING ->
                updateGame();
            case GAME_OVER -> {
                GameState next = gameOverScreen.update(input, getWidth(), getHeight());
                if (next == GameState.MAIN_MENU) {
                    carregarCheckpoint();
                    soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    carregarCheckpoint();

                    player.setShootCooldownTimer(30);
                }
                gameState = next;
            }
            case CUTSCENE -> {

            }
            case PAUSED -> {
                GameState next = pauseMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.PAUSED);
                }
                if (next == GameState.MAIN_MENU) {
                    soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    player.setShootCooldownTimer(15);
                }
                gameState = next;
            }
            case OPTIONS ->
                gameState = optionsMenu.update(input, getWidth(), getHeight());
            case QUIT ->
                System.exit(0);
        }
        input.update();
    }

    public void updateGame() {
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            gameState = GameState.PAUSED;
            return;
        }

        if (input.isKeyPressed(KeyEvent.VK_T)) {
            if (!dialogueManager.isAtivo()) {
                soundManager.playBGM(SoundManager.BGM.OS_CRIA);
                dialogueManager.iniciarDialogo(new String[] {
                        "PINGU: Entrando na base de operações.",
                        "RADIO: Cuidado, 007. Os lobos estão em alerta máximo.",
                        "PINGU: Eles não vão nem ver de onde veio."
                });
            }
        }

        if (dialogueManager.isAtivo()) {
            dialogueManager.atualizar(input);
        } else {
            // 1. Intercepta a morte e carrega o save
            if (player.isDead()) {
                gameState = GameState.GAME_OVER;
                return;
            }

            // 2. Cria o save se pegou chave nova OU passou em um trigger de checkpoint
            if (player.getTotalChavesColetadas() > chavesColetadasCheckpoint || player.isCheckpointSolicitado()) {
                salvarCheckpoint();
                chavesColetadasCheckpoint = player.getTotalChavesColetadas();
                player.limparSolicitacaoCheckpoint();
            }

            player.testemunicao(input, getWidth(), getHeight(), itemManager, camera);

            player.update(input, getWidth(), getHeight(), camera);
            itemManager.update(player);

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

            if (input.isKeyPressed(java.awt.event.KeyEvent.VK_L) && debugSpawnCooldown <= 0) {
                double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
                double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
                // itemManager.spawn(new KeyItem(12839, 4870));
                itemManager.spawn(new KeyItem(mouseXWorld, mouseYWorld));
                System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
                debugSpawnCooldown = 30;
            }

            if (input.isKeyPressed(java.awt.event.KeyEvent.VK_0) && debugSpawnCooldown <= 0) {
                renderer.modoDebug = !renderer.modoDebug;
                if (renderer.modoDebug) {
                    System.out.println("DEBUG: Visão dos Triggers e Objetos Ativada");
                } else {
                    System.out.println("DEBUG: Visão dos Triggers e Objetos Desativada");
                }

                debugSpawnCooldown = 30;
            }

            if (input.isKeyPressed(java.awt.event.KeyEvent.VK_E) && debugSpawnCooldown <= 0) {
                arenaManager.interagir(player, player.getChaves());
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
    }

    public void processarNovoMapa(ArrayList<TiledObject> objetosDoMapa) {
        enemyManager.limparTudo();
        bulletmanager.limparTudo();
        itemManager.limparTudo();

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

        int[][] levelData = levelManager.getCurLevelData();
        player.loadLvlData(levelData);
        enemyManager.setLvlData(levelData);
        bulletmanager.setLvlData(levelData);
        itemManager.setLvlData(levelData);

        chavesColetadasCheckpoint = player.getTotalChavesColetadas();
        salvarCheckpoint();
    }

    public void salvarCheckpoint() {
        checkX = player.getX();
        checkY = player.getY();
        checkVida = player.getVida();
        checkMunicao = player.getMunicao();
        checkPente = player.getPente();
        checkChaves = player.getChaves();
        checkArenas = arenaManager.getArenasConcluidas();
        hasCheckpoint = true;
        System.out.println(">>> CHECKPOINT SALVO! <<<");
    }

    public void carregarCheckpoint() {
        if (!hasCheckpoint) {
            return;
        }
        System.out.println(">>> CARREGANDO CHECKPOINT... <<<");
        player.respawn(checkX, checkY, checkVida, checkMunicao, checkPente, checkChaves);
        bulletmanager.limparTudo();
        arenaManager.restaurarArenas(checkArenas, player);
    }

    public void render(BufferStrategy bs, double delta) {
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                switch (gameState) {
                    case MAIN_MENU ->
                        mainMenu.render(g2, getWidth(), getHeight());

                    case PLAYING ->
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, delta);
                    case GAME_OVER -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, delta);
                        gameOverScreen.render(g2, getWidth(), getHeight());
                    }
                    case PAUSED -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, delta);
                        pauseMenu.render(g2, getWidth(), getHeight());
                    }
                    case CUTSCENE -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, delta);
                    }
                    case OPTIONS ->
                        optionsMenu.render(g2, getWidth(), getHeight());
                    case QUIT -> {
                    }
                }
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
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            while (delta >= 1) {
                update();
                delta--;
            }
            render(bs, deltaTime);
        }
    }

    public void start() {
        new Thread(this).start();
        soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
    }

    public static void main(String[] args) {
        GameCore game = new GameCore();
        game.frame = new JFrame("Pingu 007 (ALPHA)");
        game.frame.setIconImage(
                LoadSave.GetSpriteAtlas("pingu_portrait_close.jpg").getScaledInstance(64, 64, Image.SCALE_SMOOTH));
        game.frame.add(game);
        game.frame.pack();
        game.frame.setLocationRelativeTo(null);
        game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.frame.setResizable(false);
        game.frame.setVisible(true);
        game.start();
    }
}
