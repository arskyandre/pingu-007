
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.util.ArrayList;
import java.util.EventListener;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    // VARIÁVEL DO FPS CAP (0 para ilimitado)
    public int targetFps = 120;

    private static GameState gameState = GameState.MAIN_MENU;
    private final MainMenu mainMenu;
    private final PauseMenu pauseMenu;
    private final OptionsMenu optionsMenu;
    private final GameOverScreen gameOverScreen;
    private final KeyBindingsMenu keyBindingsMenu;

    private double checkX, checkY;
    private int checkVida, checkMunicao, checkPente, checkChaves;
    private int chavesColetadasCheckpoint = 0;
    private ArrayList<Integer> checkArenas = new ArrayList<>();
    private boolean hasCheckpoint = false;
    private boolean checkVaraDePesca = false;

    private boolean introPendente = false;
    private boolean introDialogoAtiva = false;
    private int introTimer = 0;
    private static final int INTRO_DELAY_FRAMES = 120;
    private boolean introPreDelay = false;
    private int introPreDelayTimer = 0;
    private static final int INTRO_PRE_DELAY_FRAMES = 60;

    public static BufferedImage cellphone_image = LoadSave.GetSpriteAtlas("images/portrait/cellphone.png");
    public static BufferedImage pingu_portrait = LoadSave.GetSpriteAtlas("pingu_portrait_close.jpg");
    public static BufferedImage pescador_portrait = LoadSave.GetSpriteAtlas("images/portrait/pescador_portrait.png");

    public static Font pixelFont;

    JFrame frame;
    boolean running = true;
    private boolean isFullscreen = false;
    private Rectangle windowedBounds;
    private boolean showFpsCounter = false;
    private int currentFps = 0;
    private int fpsFrameCount = 0;
    private long fpsUpdateTimer = 0;

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
    private NPCManager npcManager;
    private FishingManager fishingManager;
    private int debugSpawnCooldown = 0;
    private int mapLoadCooldown = 0;

    public final static int tiles_default_size = 16;
    public final static float scale = 3f;
    public final static int tiles_in_width = 26;
    public final static int tiles_in_height = 14;
    public final static int tiles_size = (int) (tiles_default_size * scale);
    public final static int game_width = tiles_size * tiles_in_width;
    public final static int game_height = tiles_size * tiles_in_height;
    public static boolean estaLevel2 = false;

    private static final double BASE_ZOOM = 1.25;
    private static final int BASE_HEIGHT = game_height;

    public GameCore() {
        setPreferredSize(new Dimension(game_width, game_height));
        setBackground(Color.BLACK);
        soundManager = new SoundManager();
        gameOverScreen = new GameOverScreen(soundManager);
        mainMenu = new MainMenu(soundManager);
        pauseMenu = new PauseMenu(soundManager);
        optionsMenu = new OptionsMenu(soundManager);
        keyBindingsMenu = new KeyBindingsMenu(soundManager);
        bulletmanager = new BulletManager();
        itemManager = new ItemManager();
        input = new InputManager();
        player = new Player(380, 500, tiles_size - 1, tiles_size - 1, bulletmanager, soundManager);
        camera = new CameraManager(player.getX(), player.getY(), BASE_ZOOM);
        renderer = new Renderer();
        renderer.modoDebug = false;
        levelManager = new LevelManager(this, soundManager);

        dialogueManager = new DialogueManager(soundManager);
        itemManager = new ItemManager();
        fishingManager = new FishingManager(player, soundManager, itemManager);
        enemyManager = new EnemyManager(levelManager, bulletmanager, soundManager, this);
        enemyManager.setItemManager(itemManager);
        npcManager = new NPCManager(dialogueManager, itemManager);
        cutsceneManager = new CutsceneManager(this, soundManager, dialogueManager, camera);
        arenaManager = new ArenaManager(enemyManager, levelManager, itemManager, npcManager, cutsceneManager, this);
        hud = new Hud();

        levelManager.inicializarPrimeiroNivel();

        player.loadLvlData(levelManager.getCurLevelData());
        System.out.println(player.getX() + ", " + player.getY());

        addKeyListener(input);
        addMouseMotionListener(input);
        addMouseListener(input);
        setFocusable(true);
        requestFocus();
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.BOLD, 32f);
        } catch (Exception e) {
            System.err.println("Font not found, falling back");
            pixelFont = new Font("Monospaced", Font.BOLD, 12);
        }
    }

    public static GameState getGameState() {
        return gameState;
    }

    public static void setLevel2(boolean set) {
        estaLevel2 = set;
    }

    public static boolean isLevel2() {
        return estaLevel2;
    }

    public void toggleFullscreen() {
        if (!isFullscreen) {
            System.out.println("Alternando para Fullscreen");
            windowedBounds = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setVisible(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            isFullscreen = true;
        } else {
            System.out.println("Alternando para Modo Janela");
            frame.dispose();
            frame.setUndecorated(false);
            frame.setExtendedState(JFrame.NORMAL);
            frame.setBounds(windowedBounds);
            frame.setVisible(true);
            isFullscreen = false;
        }
        requestFocusInWindow();
    }

    public boolean isShowFpsCounter() {
        return showFpsCounter;
    }

    public void toggleFpsCounter() {
        showFpsCounter = !showFpsCounter;
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setTargetFps(int targ) {
        targetFps = targ;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    public void update() {
        if (input.isKeyJustPressed(KeyEvent.VK_F11)) {
            toggleFullscreen();
        }
        switch (gameState) {
            case MAIN_MENU -> {
                GameState next = mainMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    soundManager.playBGM(SoundManager.BGM.LEVEL_1_INTRO, SoundManager.BGM.LEVEL_1_LOOP);
                    player.setShootCooldownTimer(30);
                    iniciarSequenciaIntro();
                }
                gameState = next;
            }
            case PLAYING -> {
                if (introPreDelay) {
                    introPreDelayTimer--;
                    if (introPreDelayTimer <= 0) {
                        introPreDelay = false;
                        introPendente = true;
                        introTimer = INTRO_DELAY_FRAMES;
                        player.setBlockInputs(true);
                        soundManager.playSFX(SoundManager.SFX.CALL_RING);
                    }
                }
                if (introPendente) {
                    introTimer--;
                    if (introTimer <= 0) {
                        introPendente = false;
                        introDialogoAtiva = true;
                        triggerDialogoInicial();
                    }
                }
                if (dialogueManager.isAtivo()) {
                    dialogueManager.atualizar(input);
                } else {
                    if (introDialogoAtiva) {
                        introDialogoAtiva = false;
                        player.setBlockInputs(false);
                    }
                    updateGame();
                }
            }
            case GAME_OVER -> {
                GameState next = gameOverScreen.update(input, getWidth(), getHeight());
                if (next == GameState.MAIN_MENU) {
                    resetarJogoCompleto();
                    soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    carregarCheckpoint();

                    player.setShootCooldownTimer(30);
                }
                gameState = next;
            }
            case CUTSCENE -> {
                updateCutscene();
                updatePlayerMovement();
                player.setEmDash(false);
                if (dialogueManager.isAtivo()) {
                    dialogueManager.atualizar(input);
                    // camera.update(player, input, getWidth(), getHeight()); dar um jeito de fazer
                    // a camera nao seguir o mouse no update, apenas para arrumar a posicao da
                    // camera caso o tamanho da tela mude durante o dialogo
                }
            }
            case PAUSED -> {
                GameState next = pauseMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.PAUSED);
                }
                if (next == GameState.MAIN_MENU) {
                    resetarJogoCompleto();
                    soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    player.setShootCooldownTimer(15);
                }
                gameState = next;
            }
            case OPTIONS ->
                gameState = optionsMenu.update(input, getWidth(), getHeight(), this);
            case KEYBINDINGS ->
                gameState = keyBindingsMenu.update(input, getWidth(), getHeight());
            case QUIT ->
                System.exit(0);
        }
        input.update();
    }

    private void iniciarSequenciaIntro() {
        introPreDelay = true;
        introPendente = false;
        introDialogoAtiva = false;
        introPreDelayTimer = INTRO_PRE_DELAY_FRAMES;
    }

    public void updatePlayerMovement() {
        player.updatePlayerMovement();
    }

    public void setCinematicBorderAnimation(Renderer.BorderState state) {
        renderer.setCinematicBorderAnimation(state);
    }

    public void shakeCamera(double intensidade, int duracaoFrames) {
        if (gameState == GameState.PLAYING || gameState == GameState.CUTSCENE) {
            camera.tremer(intensidade, duracaoFrames);
        }
    }

    public void triggerDialogoInicial() {
        if (!dialogueManager.isAtivo()) {
            dialogueManager.iniciarDialogo(DialogueCatalogo.TextoInicialRadio, DialogueCatalogo.FalaInicialRadio,
                    new BufferedImage[] {
                            pingu_portrait,
                            cellphone_image,
                            pingu_portrait,
                            cellphone_image
                    }, true);
        }
    }

    public void debugInputProcessing() {

        if (input.isKeyJustPressed(KeyEvent.VK_F)) {
            toggleFpsCounter();
        }
        if (input.isKeyJustPressed(KeyEvent.VK_I)) {
            setCinematicBorderAnimation(Renderer.BorderState.IN);
        }
        if (input.isKeyJustPressed(KeyEvent.VK_O)) {
            setCinematicBorderAnimation(Renderer.BorderState.OUT);
        }
        if (input.isKeyJustPressed(KeyEvent.VK_N)) {
            player.setX(20.5 * GameCore.tiles_size);
            player.setY(48.0 * GameCore.tiles_size);
        }

        if (debugSpawnCooldown > 0) {
            debugSpawnCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_K) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            enemyManager.adicionarInimigo("lobo", mouseXWorld, mouseYWorld, 0, -1);
            System.out.println("DEBUG: Inimigo spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
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

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_H) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            // itemManager.spawn(new KeyItem(12839, 4870));
            itemManager.spawn(new HealthPackItem(mouseXWorld, mouseYWorld));
            System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 30;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_V) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            itemManager.spawn(new FishingRodItem(mouseXWorld, mouseYWorld));
            System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 30;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_0) && debugSpawnCooldown <= 0) {
            renderer.modoDebug = !renderer.modoDebug;
            toggleFpsCounter();
            if (renderer.modoDebug) {
                System.out.println("DEBUG: Visão dos Triggers e Objetos Ativada");
            } else {
                System.out.println("DEBUG: Visão dos Triggers e Objetos Desativada");
            }

            debugSpawnCooldown = 30;
        }
        if (mapLoadCooldown > 0) {
            mapLoadCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_1) && mapLoadCooldown <= 0) {
            System.out.println("Voltando para o Mapa 1...");
            levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
            if (soundManager.currentSong() != SoundManager.BGM.LEVEL_1_LOOP)
                soundManager.playBGM(SoundManager.BGM.LEVEL_1_INTRO, SoundManager.BGM.LEVEL_1_LOOP);
            mapLoadCooldown = 60;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_2) && mapLoadCooldown <= 0) {
            System.out.println("Indo para o Mapa 2 de Testes...");
            // Substitua pelo nome exato do seu arquivo JSON de teste
            entrarNivelBoss();
        }

        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_3)) {
            renderer.toggleAntiAliasing();
        }
    }

    public void entrarNivelBoss() {
        levelManager.carregarNivel(LoadSave.LEVEL_2_DATA);
        arenaManager.setFirstArenaFlag(false);
        mapLoadCooldown = 60;
    }

    public void toggleAntiAliasing() {
        renderer.toggleAntiAliasing();
    }

    public boolean isAntiAliasingEnabled() {
        return renderer.useAntiAliasing;
    }

    public void updateGame() {
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE)) {
            gameState = GameState.PAUSED;
            return;
        }
        cutsceneManager.update();

        // Intercepta a morte e carrega o save
        if (player.isDead()) {
            gameState = GameState.GAME_OVER;
            return;
        }

        // Cria o save se pegou chave nova OU passou em um trigger de checkpoint
        if (player.getTotalChavesColetadas() > chavesColetadasCheckpoint || player.isCheckpointSolicitado()) {
            salvarCheckpoint();
            chavesColetadasCheckpoint = player.getTotalChavesColetadas();
            player.limparSolicitacaoCheckpoint();
        }

        fishingManager.update(input, camera, levelManager.getCurLevelData(), getWidth(), getHeight());

        player.update(input, getWidth(), getHeight(), camera, enemyManager.getEnemies());
        npcManager.update(player, input);
        itemManager.update(player);

        ArrayList<JumpLink> linksAtuais = levelManager.getJumpLinks();
        enemyManager.update(player, linksAtuais);

        arenaManager.update(player, camera, soundManager);

        bulletmanager.update(camera, getWidth(), getHeight(),
                player, enemyManager.getEnemies());

        levelManager.update();
        double dynamicZoom = BASE_ZOOM * (getHeight() / (double) BASE_HEIGHT);
        camera.setBaseZoom(dynamicZoom);
        camera.update(player, input, getWidth(), getHeight());
        fishingManager.syncToCamera(camera, getWidth(), getHeight());

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_E) && debugSpawnCooldown <= 0) {
            arenaManager.interagir(player, player.getChaves());
        }

        debugInputProcessing();

    }

    public void updateCutscene() {
        cutsceneManager.update();
        double dynamicZoom = BASE_ZOOM * (getHeight() / (double) BASE_HEIGHT);
        camera.setBaseZoom(dynamicZoom);
        if (cutsceneManager.isBossIntroAtiva()) {
            MorsaBoss morsa = enemyManager.getMorsaBoss();
            if (morsa != null) {
                morsa.atualizarCutsceneIntro(); // só cuida do rugido/tremida — sem alvo, sem ataque, sem BossMao
            }
        }
        camera.update(player, input, getWidth(), getHeight());
        if (!cutsceneManager.isAtiva()) {
            player.setBlockInputs(false);
            gameState = GameState.PLAYING;
        }

    }

    public static void setGameState(GameState state) {
        gameState = state;
    }

    public void processarNovoMapa(ArrayList<TiledObject> objetosDoMapa) {
        enemyManager.limparTudo();
        bulletmanager.limparTudo();
        itemManager.limparTudo();
        npcManager.clearAll();
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
        checkVaraDePesca = player.hasFishingRod();
        System.out.println(">>> CHECKPOINT SALVO! <<<");
    }

    public void carregarCheckpoint() {
        if (!hasCheckpoint) {
            return;
        }
        System.out.println(">>> CARREGANDO CHECKPOINT... <<<");
        player.respawn(checkX, checkY, checkVida, checkMunicao, checkPente, checkChaves);
        player.setFishingRod(checkVaraDePesca);
        bulletmanager.limparTudo();
        itemManager.limparConsumiveis();
        arenaManager.restaurarArenas(checkArenas, player, itemManager);
    }

    public void resetarJogoCompleto() {
        hasCheckpoint = false;
        checkArenas.clear();
        arenaManager.setFirstArenaFlag(true);
        fishingManager.setPlayerHasKey(false);
        npcManager.clearAll();
        chavesColetadasCheckpoint = 0;
        player.resetarProgresso();
        renderer.setBorderProgress(0.0);
        setCinematicBorderAnimation(Renderer.BorderState.IDLE);
        introPreDelay = false;
        introPreDelayTimer = 0;
        introPendente = false;
        introDialogoAtiva = false;
        introTimer = 0;
        levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
    }

    private void drawFpsCounter(Graphics2D g2) {
        int MARGIN = 12;
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        String text = "FPS: " + currentFps;
        Rectangle2D textbounds = g2.getFontMetrics().getStringBounds(text, g2);
        int tw = (int) textbounds.getWidth();
        int th = (int) textbounds.getHeight();
        g2.setColor(Color.GRAY);
        g2.fillRect(getWidth() - tw - MARGIN - 4, MARGIN + 1, tw + 4, th + 2);
        int textX = getWidth() - tw - MARGIN - 2;
        int textY = th + MARGIN - 2;
        g2.setColor(Color.BLACK);
        g2.drawString(text, textX + 1, textY + 1);
        g2.setColor(Color.GREEN);
        g2.drawString(text, textX, textY);
    }

    public void render(BufferStrategy bs, double delta) {
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                switch (gameState) {
                    case MAIN_MENU -> {
                        mainMenu.render(g2, getWidth(), getHeight());
                    }
                    case PLAYING -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, delta,
                                true, true);
                        if (showFpsCounter) {
                            drawFpsCounter(g2);
                        }
                    }
                    case GAME_OVER -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, delta,
                                true, false);
                        gameOverScreen.render(g2, getWidth(), getHeight());
                    }
                    case PAUSED -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, delta,
                                true, false);
                        pauseMenu.render(g2, getWidth(), getHeight());
                    }
                    case CUTSCENE -> {
                        {
                            renderer.renderizar(g2, camera, player, input,
                                    getWidth(), getHeight(),
                                    levelManager, bulletmanager, itemManager,
                                    enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                    cutsceneManager, delta,
                                    true, false);

                            if (showFpsCounter) {
                                drawFpsCounter(g2);
                            }
                        }
                    }
                    case OPTIONS -> {
                        optionsMenu.render(g2, getWidth(), getHeight());
                    }
                    case KEYBINDINGS -> {
                        keyBindingsMenu.render(g2, getWidth(), getHeight());
                    }
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

    public boolean isFullscreen() {
        return isFullscreen;
    }

    public static Rectangle2D getRetanguloComum(Rectangle2D[] rects) {
        if (rects == null || rects.length == 0)
            return null;
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax)
                Xmax = x;
            else if (x < Xmax)
                Xmax = x;
            if (y > Ymax)
                Ymax = y;
            else if (y < Ymin)
                Ymin = y;
        }
        return new Rectangle2D.Double(Xmin, Ymin, Xmax - Xmin, Ymax - Ymin);
    }

    public static Rectangle2D getRetanguloComum(Rectangle2D[] rects, double margin) {
        if (rects == null || rects.length == 0)
            return null;
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax)
                Xmax = x;
            else if (x < Xmax)
                Xmax = x;
            if (y > Ymax)
                Ymax = y;
            else if (y < Ymin)
                Ymin = y;
        }
        return new Rectangle2D.Double(Xmin - margin, Ymin - margin, Xmax - Xmin + 2 * margin, Ymax - Ymin + 2 * margin);
    }

    public static Rectangle2D getRetanguloComum(Rectangle2D[] rects, double marginX, double marginY) {
        if (rects == null || rects.length == 0)
            return null;
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax)
                Xmax = x;
            else if (x < Xmax)
                Xmax = x;
            if (y > Ymax)
                Ymax = y;
            else if (y < Ymin)
                Ymin = y;
        }
        return new Rectangle2D.Double(Xmin - marginX, Ymin - marginY, Xmax - Xmin + 2 * marginX,
                Ymax - Ymin + 2 * marginY);
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerUpdate = 1_000_000_000.0 / 60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            long frameTime = now - lastTime;
            lastTime = now;

            delta += frameTime / nsPerUpdate;

            while (delta >= 1) {
                update();
                delta--;
            }

            BufferStrategy bs = getBufferStrategy();
            if (bs == null) {
                createBufferStrategy(3);
                continue;
            }

            double deltaTimeRender = frameTime / 1_000_000_000.0;
            render(bs, deltaTimeRender);

            fpsFrameCount++;
            fpsUpdateTimer += frameTime;
            if (fpsUpdateTimer >= 1_000_000_000L) {
                currentFps = fpsFrameCount;
                fpsFrameCount = 0;
                fpsUpdateTimer -= 1_000_000_000L;
            }

            if (targetFps > 0) {
                long optimalTime = 1_000_000_000L / targetFps;
                while (System.nanoTime() - now < optimalTime) {
                    long timeLeft = optimalTime - (System.nanoTime() - now);

                    if (timeLeft > 2_000_000) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Thread.yield();
                    }
                }
            }
        }
    }

    public void start() {
        new Thread(this).start();
        soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
    }

    public static void main(String[] args) {
        GameCore game = new GameCore();
        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        game.frame = new JFrame("Pingu 007 (ALPHA)");
        game.frame.setIconImage(
                LoadSave.GetSpriteAtlas("pingu_portrait_close.jpg").getScaledInstance(64, 64, Image.SCALE_SMOOTH));
        game.frame.add(game);
        game.frame.pack();
        game.frame.setLocationRelativeTo(null);
        game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.frame.setResizable(true);
        game.optionsMenu.repositionElements(game.getWidth(), game.getHeight(), game);
        game.keyBindingsMenu.repositionElements(game.getWidth(), game.getHeight());
        game.frame.setVisible(true);
        game.start();
    }
}
