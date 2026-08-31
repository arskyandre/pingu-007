
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;

public class GameCore extends Canvas implements Runnable {

    // VARIÁVEL DO FPS CAP (0 para ilimitado)
    public int targetFps = 120;
    // TODO: consertar o checkpoint para que a arena ative e desative corretamente
    // TODO: Terminar de converter os objetos do cenario de tiles para objetos
    // TODO: nerfar o jumper
    private static GameState gameState = GameState.MAIN_MENU;
    private static ShopMenu currentShopMenu = null;
    private final MainMenu mainMenu;
    private final PauseMenu pauseMenu;
    private final OptionsMenu optionsMenu;
    private final GameOverScreen gameOverScreen;
    private final KeyBindingsMenu keyBindingsMenu;

    // permite os botoes de teste(debuginputprocessing() e outros). se colocar false
    // o jogo se comporta como versao de "usuario"
    private static boolean debugInputs = true;

    private double checkX, checkY;
    private int checkVida, checkMunicao, checkPente, checkChaves;
    private int chavesColetadasCheckpoint = 0;
    private ArrayList<Integer> checkArenas = new ArrayList<>();
    private boolean hasCheckpoint = false;
    private boolean checkVaraDePesca = false;
    private ArenaManager.EstadoMapa estadoLevel1AntesDaLoja;
    private ArrayList<Item> itensLevel1AntesDaLoja = new ArrayList<>();
    private double retornoLojaX;
    private double retornoLojaY;
    private boolean temRetornoDaLoja = false;

    private boolean introPendente = false;
    private boolean introDialogoAtiva = false;
    private int introTimer = 0;
    private static final int INTRO_DELAY_FRAMES = 120;
    private boolean introPreDelay = false;
    private int introPreDelayTimer = 0;
    private static final int INTRO_PRE_DELAY_FRAMES = 60;

    public static BufferedImage missing_image = LoadSave.GetSpriteAtlas("images/missing_image.png");
    public static BufferedImage cellphone_image = LoadSave.GetSpriteAtlas("images/portrait/cellphone.png");
    public static BufferedImage pingu_portrait = LoadSave.GetSpriteAtlas("images/portrait/pingu_portrait_close.jpg");
    public static BufferedImage pescador_portrait = LoadSave.GetSpriteAtlas("images/portrait/pescador_portrait.png");

    public static Font pixelFont;

    JFrame frame;
    boolean running = true;
    private boolean isFullscreen = false;
    private Rectangle windowedBounds;
    private boolean showFpsCounter = false;
    private boolean avisoPlebeu = true;
    private int currentFps = 240;
    private int fpsFrameCount = 0;
    private long fpsUpdateTimer = 0;

    private boolean musicaDeFightAtiva = false;

    // pro novo ciclo de dia e noite(cores/musica)
    private boolean musicaDeDiaAtiva = true;
    private int lastProcessedDay = 1;
    private long updateDayNightAnteriorNanos = -1L;

    private static double fullDaySeconds = 360.0;
    private static final double horarioinicial = 8.0 / 24.0;

    private static double dayProgress = horarioinicial;
    private static double elapsedGameSeconds = horarioinicial * fullDaySeconds;

    private boolean dayNightClockRunning = true;

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
    private final ScreenTransition screenTransition;
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
    // public final static int game_width = tiles_size * tiles_in_width;
    // public final static int game_height = tiles_size * tiles_in_height;
    public static boolean estaLevel2 = false;
    public static boolean estaDentroLoja = false;

    public boolean estaEmArena() {
        return arenaManager.existeArenaRealAtiva();
    }

    private static final double BASE_ZOOM = 1.25;
    private static final int BASE_HEIGHT = game_height;

    BufferedImage cursorImage = new BufferedImage(
            16, 16, BufferedImage.TYPE_INT_ARGB);
    Cursor invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
            cursorImage,
            new Point(0, 0),
            "invisibleCursor");
    private Boolean cursorInvisivelAplicado = null;

    public GameCore() {
        setPreferredSize(new Dimension(game_width, game_height));
        setBackground(Color.BLACK);

        soundManager = new SoundManager();
        bulletmanager = new BulletManager();
        itemManager = new ItemManager();
        input = new InputManager();
        renderer = new Renderer();
        screenTransition = new ScreenTransition();
        renderer.modoDebug = false;
        levelManager = new LevelManager(this, soundManager);
        dialogueManager = new DialogueManager(soundManager);
        hud = new Hud();

        gameOverScreen = new GameOverScreen(soundManager);
        mainMenu = new MainMenu(soundManager);
        pauseMenu = new PauseMenu(soundManager);
        optionsMenu = new OptionsMenu(soundManager);
        keyBindingsMenu = new KeyBindingsMenu(soundManager);

        npcManager = new NPCManager(dialogueManager, itemManager, soundManager);

        player = new Player(380, 500, tiles_size - 1, tiles_size - 1, bulletmanager, soundManager, null);

        camera = new CameraManager(player.getX(), player.getY(), BASE_ZOOM);
        cutsceneManager = new CutsceneManager(this, soundManager, dialogueManager, camera);

        enemyManager = new EnemyManager(levelManager, bulletmanager, soundManager, this, null);
        enemyManager.setItemManager(itemManager);

        arenaManager = new ArenaManager(enemyManager, levelManager, itemManager, npcManager, cutsceneManager, this,
                camera, soundManager);

        player.arenaManager = this.arenaManager;
        enemyManager.arenaManager = this.arenaManager;

        fishingManager = new FishingManager(player, soundManager, itemManager);
        player.setFishingManager(fishingManager);

        levelManager.inicializarPrimeiroNivel();
        player.loadLvlData(levelManager.getCurLevelData());

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

    public static boolean getDebug() {
        return debugInputs;
    }

    public static GameState getGameState() {
        return gameState;
    }

    public static void setShopMenu(ShopMenu menu) {
        currentShopMenu = menu;
    }

    public static ShopMenu getShopMenu() {
        return currentShopMenu;
    }

    public static void setLevel2(boolean set) {
        estaLevel2 = set;
    }

    public static boolean isLevel2() {
        return estaLevel2;
    }

    public static void setDentroLoja(boolean set) {
        estaDentroLoja = set;
    }

    public static boolean getEstaDentroLoja() {
        return estaDentroLoja;
    }

    public int getCurrentDay() {
        return (int) Math.floor(elapsedGameSeconds / fullDaySeconds) + 1;
    }

    public double getInGameTime() {
        return dayProgress;
    }

    public static double getSunAngle() {
        return !estaDentroLoja ? ((dayProgress * Math.PI * 2.0) - (Math.PI / 2.0)) : (1.3 * Math.PI / 2.0);
    }

    private void atualizarMusicaDayNight() {
        if (!LoadSave.LEVEL_1_DATA.equals(
                levelManager.getArquivoNivelAtual()) || arenaManager.existeArenaRealAtiva()) {
            return;
        }

        double horaAtual = dayProgress * 24.0;

        boolean deveTocarMusicaDia = (horaAtual >= 8.0 && horaAtual < 19.0);

        if (deveTocarMusicaDia == musicaDeDiaAtiva) {
            return;
        }

        musicaDeDiaAtiva = deveTocarMusicaDia;

        if (deveTocarMusicaDia) {
            alternarParaMusicaDia(2.5);
        } else {
            alternarParaMusicaNoite(2.5);
        }
    }

    private void atualizarMusicaArena() {
        if (!LoadSave.LEVEL_1_DATA.equals(
                levelManager.getArquivoNivelAtual())) {
            return;
        }

        boolean deveTocarMusicaFight = arenaManager.existeArenaRealAtiva();

        if (deveTocarMusicaFight == musicaDeFightAtiva) {
            return;
        }

        musicaDeFightAtiva = deveTocarMusicaFight;

        if (deveTocarMusicaFight) {
            alternarParaMusicaFight();
        } else {
            if (isDia()) {
                alternarParaMusicaDia(3);
                musicaDeDiaAtiva = true;
            } else {
                alternarParaMusicaNoite(2.25);
                musicaDeDiaAtiva = false;
            }
        }
    }

    private void atualizarCicloDayNight(boolean avancarRelogio) {

        long now = System.nanoTime();

        if (updateDayNightAnteriorNanos < 0L) {
            updateDayNightAnteriorNanos = now;
            return;
        }

        double deltaSeconds = (now - updateDayNightAnteriorNanos)
                / 1_000_000_000.0;

        updateDayNightAnteriorNanos = now;

        if (!dayNightClockRunning || !avancarRelogio) {
            return;
        }

        elapsedGameSeconds += deltaSeconds;

        dayProgress = (elapsedGameSeconds % fullDaySeconds)
                / fullDaySeconds;

        int currentDay = getCurrentDay();

        if (currentDay > lastProcessedDay) {
            lastProcessedDay = currentDay;
            onNovoDia(currentDay);
        }
        atualizarMusicaDayNight();
        atualizarMusicaArena();
    }

    private void alternarParaMusicaDia(double delay) {
        if (soundManager.currentSong() == SoundManager.BGM.LEVEL_1_DAY_INTRO
                || soundManager.currentSong() == SoundManager.BGM.LEVEL_1_DAY_LOOP) {
            return;
        }
        System.out.println("Mudou para musica de dia");
        soundManager.crossfadeBGM(SoundManager.BGM.LEVEL_1_DAY_INTRO, SoundManager.BGM.LEVEL_1_DAY_LOOP, 5000, delay,
                true);
    }

    private void alternarParaMusicaNoite(double delay) {
        if (soundManager.currentSong() == SoundManager.BGM.LEVEL_1_NIGHT_INTRO
                || soundManager.currentSong() == SoundManager.BGM.LEVEL_1_NIGHT_LOOP) {
            return;
        }
        System.out.println("Mudou para musica de noite");
        soundManager.crossfadeBGM(SoundManager.BGM.LEVEL_1_NIGHT_INTRO, SoundManager.BGM.LEVEL_1_NIGHT_LOOP, 5000,
                delay,
                false);
    }

    private void alternarParaMusicaFight() {
        System.out.println("Mudou para musica de fight");
        soundManager.crossfadeBGM(SoundManager.BGM.ARENA_INTRO, SoundManager.BGM.ARENA_LOOP, 2000, 0.5, true);
    }

    private void onNovoDia(int day) {
        System.out.println("Novo dia: " + day);
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

    public boolean isRenderShadows() {
        return Renderer.isRenderShadows();
    }

    public void toggleRenderShadows() {
        Renderer.toggleRenderShadows();
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

    private void updateCursorVisibility() {
        boolean deveSerInvisivel = gameState == GameState.PLAYING || gameState == GameState.CUTSCENE
                || input.isMouseBloqueado();
        if (cursorInvisivelAplicado != null && cursorInvisivelAplicado == deveSerInvisivel) {
            return;
        }

        setCursor(deveSerInvisivel ? invisibleCursor : Cursor.getDefaultCursor());
        cursorInvisivelAplicado = deveSerInvisivel;
    }

    public void update() {
        // System.out.println("Player position MapaUpdate (" + player.getX() + ", " +
        // player.getY() + ")");
        if (input.isKeyJustPressed(KeyEvent.VK_F11)) {
            toggleFullscreen();
        }
        input.atualizarBloqueioMouse();
        camera.adjustForViewportResize(getWidth(), getHeight(), calculateBaseZoom(getHeight()));
        updateCursorVisibility();

        if (screenTransition.isAtivo()) {
            screenTransition.update();
            if (screenTransition.deveBloquearAtualizacaoDaCena()) {
                input.update();
                updateCursorVisibility();
                return;
            }
        }

        boolean avancarRelogio = (gameState == GameState.PLAYING
                || gameState == GameState.CUTSCENE) && !getEstaDentroLoja();

        atualizarCicloDayNight(avancarRelogio);

        switch (gameState) {
            case MAIN_MENU -> {
                GameState next = mainMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.MAIN_MENU);
                }
                if (next == GameState.PLAYING) {
                    screenTransition.start(this::iniciarJogoDoMenu);
                    next = GameState.MAIN_MENU;
                }
                gameState = next;
            }
            case PLAYING -> {
                if (!getDebug()) {
                    if (introPreDelay) {
                        introPreDelayTimer--;
                        if (introPreDelayTimer <= 0) {
                            introPreDelay = false;
                            introPendente = true;
                            introTimer = INTRO_DELAY_FRAMES;
                            player.setBlockInputs(true);
                            soundManager.playSFX(SoundManager.SFX.CALL_RING);
                            player.setTemporarySpriteOverride(0, introTimer);
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
                }
                if (dialogueManager.isAtivo()) {
                    dialogueManager.atualizar(input);
                    atualizarCameraSemInput();
                } else {
                    if (introDialogoAtiva) {
                        introDialogoAtiva = false;
                        player.setBlockInputs(false);
                    }
                    updateGame();
                }
                // DESCOMENTAR BLOCO NO JOGO FINAL
                // (nao mais, agora eh controlado pela flag de debug)
            }
            case SHOP -> {
                ShopMenu shop = getShopMenu();
                if (shop != null) {
                    shop.update(input, getWidth(), getHeight());
                }
            }
            case GAME_OVER -> {
                GameState next = gameOverScreen.update(input, getWidth(), getHeight());
                if (next == GameState.MAIN_MENU) {
                    screenTransition.start(this::voltarAoMenuPrincipalImediato);
                    next = GameState.GAME_OVER;
                }
                if (next == GameState.PLAYING) {
                    carregarCheckpoint();
                    if (!arenaManager.existeArenaRealAtiva()) {
                        setCinematicBorderAnimation(Renderer.BorderState.OUT);
                    }

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
                }
            }
            case PAUSED -> {
                GameState next = pauseMenu.update(input, getWidth(), getHeight());
                if (next == GameState.OPTIONS) {
                    optionsMenu.setReturnState(GameState.PAUSED);
                }
                if (next == GameState.MAIN_MENU) {
                    screenTransition.start(this::voltarAoMenuPrincipalImediato);
                    next = GameState.PAUSED;
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
            case QUIT -> {
                input.shutdown();
                System.exit(0);
            }
            case CREDITS -> {

            }
        }
        updateCursorVisibility();
        input.update();
        updateCursorVisibility();
    }

    private void iniciarSequenciaIntro() {
        introPreDelay = true;
        introPendente = false;
        introDialogoAtiva = false;
        introPreDelayTimer = INTRO_PRE_DELAY_FRAMES;
    }

    private void iniciarJogoDoMenu() {
        updateDayNightAnteriorNanos = -1L;
        elapsedGameSeconds = horarioinicial * fullDaySeconds;
        dayProgress = horarioinicial;
        lastProcessedDay = 1;
        soundManager.playBGM(SoundManager.BGM.LEVEL_1_DAY_INTRO, SoundManager.BGM.LEVEL_1_DAY_LOOP);
        player.setShootCooldownTimer(30);
        iniciarSequenciaIntro();
        configurarCameraDoMapaAtual();
        gameState = GameState.PLAYING;
    }

    private void voltarAoMenuPrincipalImediato() {
        resetarJogoCompleto();
        soundManager.playBGM(SoundManager.BGM.MAIN_MENU);
        gameState = GameState.MAIN_MENU;
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
                    new BufferedImage[]{
                        pingu_portrait,
                        cellphone_image,
                        pingu_portrait,
                        cellphone_image
                    });
            dialogueManager.setAoTerminarDialogo(() -> {
                ToastNotifications.RequestNotification("Use as setas para selecionar a opção e ENTER para confirmar.",
                        10.0);
                DialogueCatalogo.loopDialogoInicial(dialogueManager, soundManager);
            });
        }
    }

    public void debugInputProcessing() {

        if (input.isKeyJustPressed(KeyEvent.VK_P)) {
            System.out.println("Coordenadas do player: (X, Y) = (" + player.getX() / GameCore.tiles_size + ", "
                    + player.getY() / GameCore.tiles_size + ")");
        }
        if (input.isKeyJustPressed(KeyEvent.VK_F)) {
            toggleFpsCounter();
        }
        if (input.isKeyJustPressed(KeyEvent.VK_I)) {
            setCinematicBorderAnimation(Renderer.BorderState.IN);
        }
        if (input.isKeyJustPressed(KeyEvent.VK_O)) {
            setCinematicBorderAnimation(Renderer.BorderState.OUT);
        }

        if (debugSpawnCooldown > 0) {
            debugSpawnCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_L) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            enemyManager.adicionarInimigo("lobo", mouseXWorld, mouseYWorld, 0, -1);
            System.out.println("DEBUG: Inimigo spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 15;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_K) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            soundManager.playSFX(SoundManager.SFX.KEY_SPAWN);
            itemManager.spawn(new KeyItem(mouseXWorld, mouseYWorld));
            System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 15;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_H) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            itemManager.spawn(new HealthPackItem(mouseXWorld, mouseYWorld));
            System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 15;
        }
        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_M) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            itemManager.spawn(new MoedaItem(mouseXWorld, mouseYWorld, 100));
            System.out.println("DEBUG: moeda milionaria spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 15;
        }
        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_B)) {
            System.out.println("deu 10 iscas");
            player.addIscas(10);
        }
        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_V) && debugSpawnCooldown <= 0) {
            double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
            double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
            itemManager.spawn(new FishingRodItem(mouseXWorld, mouseYWorld));
            System.out.println("DEBUG: Item spawnado na posição: " + mouseXWorld + ", " + mouseYWorld);
            debugSpawnCooldown = 15;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_F10) && debugSpawnCooldown <= 0) {
            renderer.modoDebug = !renderer.modoDebug;
            toggleFpsCounter();
            if (renderer.modoDebug) {
                System.out.println("DEBUG: Visão dos Triggers e Objetos Ativada");
            } else {
                System.out.println("DEBUG: Visão dos Triggers e Objetos Desativada");
            }

            debugSpawnCooldown = 15;
        }
        if (mapLoadCooldown > 0) {
            mapLoadCooldown--;
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_F1) && mapLoadCooldown <= 0) {
            System.out.println("Voltando para o Mapa 1...");
            if (LoadSave.CASA_VENDEDOR.equals(levelManager.getArquivoNivelAtual())) {
                sairCasaVendedor();
            } else {
                screenTransition.start(this::carregarNivel1DebugImediato);
            }
        }

        if (input.isKeyJustPressed(KeyEvent.VK_F6)) {
            elapsedGameSeconds = (fullDaySeconds / 24.0) * 18.65;
            dayProgress = 18.65 / 24.0;
        }

        if (input.isKeyJustPressed(KeyEvent.VK_F5)) {
            elapsedGameSeconds = (fullDaySeconds / 24.0) * 7.65;
            dayProgress = 7.65 / 24.0;
        }

        if (input.isKeyJustPressed(KeyEvent.VK_N)) {
            player.no_clip = !player.no_clip;
            if (player.no_clip) {
                System.out.println("DEBUG: NO-CLIP ATIVADO!");
            } else {
                System.out.println("DEBUG: NO-CLIP DESATIVADO!");
            }
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_F2) && mapLoadCooldown <= 0) {
            System.out.println("Indo para o Mapa 2 de Testes...");
            entrarNivelBoss();
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_F4) && mapLoadCooldown <= 0) {
            System.out.println("Indo para o Mapa 4 de Testes...");
            entrarNivelTest();
        }

        if (input.isKeyPressed(java.awt.event.KeyEvent.VK_F3) && mapLoadCooldown <= 0) {
            System.out.println("Indo para o Mapa 3 de Testes...");
            entrarCasaVendedor();
        }

    }

    public SoundManager.BGM getLevel1MusicaIntro() {
        return isDia() ? SoundManager.BGM.LEVEL_1_DAY_INTRO
                : SoundManager.BGM.LEVEL_1_NIGHT_INTRO;
    }

    public SoundManager.BGM getLevel1MusicaLoop() {
        return isDia() ? SoundManager.BGM.LEVEL_1_DAY_LOOP
                : SoundManager.BGM.LEVEL_1_NIGHT_LOOP;
    }

    public boolean isDia() {
        return (dayProgress * 24.0 >= 8.0 && dayProgress * 24.0 <= 19.0);
    }

    public void entrarCasaVendedor() {
        screenTransition.start(this::entrarCasaVendedorImediato);
    }

    private void entrarCasaVendedorImediato() {
        if (LoadSave.LEVEL_1_DATA.equals(levelManager.getArquivoNivelAtual())) {
            estadoLevel1AntesDaLoja = arenaManager.capturarEstadoMapa();
            itensLevel1AntesDaLoja = new ArrayList<>(itemManager.getItems());
            retornoLojaX = player.getX();
            retornoLojaY = player.getY();
            temRetornoDaLoja = true;
        } else {
            estadoLevel1AntesDaLoja = null;
            itensLevel1AntesDaLoja.clear();
            temRetornoDaLoja = false;
        }

        levelManager.carregarNivel(LoadSave.CASA_VENDEDOR);
        mapLoadCooldown = 60;
        configurarCameraDoMapaAtual();
        soundManager.crossfadeBGM(SoundManager.BGM.INSIDE_INTRO, SoundManager.BGM.INSIDE_LOOP, 2000, 1.25, false);
        setDentroLoja(true);
    }

    public void sairCasaVendedor() {
        screenTransition.start(this::sairCasaVendedorImediato);
    }

    private void sairCasaVendedorImediato() {
        levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
        // System.out.println("Player position levelManager.carregarNivel (" +
        // player.getX() + ", " + player.getY() + ")");

        if (estadoLevel1AntesDaLoja != null) {
            arenaManager.restaurarEstadoMapa(estadoLevel1AntesDaLoja, player, itemManager);
        }
        // System.out.println("Player position arenaManager.restaurarEstadoMapa (" +
        // player.getX() + ", " + player.getY() + ")");

        if (!itensLevel1AntesDaLoja.isEmpty()) {
            itemManager.getItems().addAll(itensLevel1AntesDaLoja);
        }

        if (temRetornoDaLoja) {
            player.setX(retornoLojaX);
            player.setY(retornoLojaY);
        }

        // System.out.println("Player position retorno (" + player.getX() + ", " +
        // player.getY() + ")");
        configurarCameraDoMapaAtual();
        setCinematicBorderAnimation(Renderer.BorderState.OUT);
        if (isDia()) {
            alternarParaMusicaDia(2.5);
        } else {
            alternarParaMusicaNoite(2.0);
        }
        mapLoadCooldown = 60;

        salvarCheckpoint();

        estadoLevel1AntesDaLoja = null;
        itensLevel1AntesDaLoja.clear();
        temRetornoDaLoja = false;
        setDentroLoja(false);
    }

    public void entrarNivelBoss() {
        screenTransition.start(this::entrarNivelBossImediato);
    }

    private void entrarNivelBossImediato() {
        levelManager.carregarNivel(LoadSave.LEVEL_2_DATA);
        arenaManager.setFirstArenaFlag(false);
        mapLoadCooldown = 60;
        configurarCameraDoMapaAtual();
    }

    public void entrarNivelTest() {
        screenTransition.start(this::entrarNivelTestImediato);
    }

    private void entrarNivelTestImediato() {
        levelManager.carregarNivel(LoadSave.LEVEL_YSORT);
        arenaManager.setFirstArenaFlag(false);
        mapLoadCooldown = 60;
        configurarCameraDoMapaAtual();
    }

    public void transicionarMapa(String mapaDestino) {
        if (mapaDestino == null || mapaDestino.isEmpty()) {
            return;
        }

        screenTransition.start(() -> {
            levelManager.carregarNivel(mapaDestino);
            configurarCameraDoMapaAtual();
            mapLoadCooldown = 60;
        });
    }

    private void carregarNivel1DebugImediato() {
        levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
        setCinematicBorderAnimation(Renderer.BorderState.OUT);
        configurarCameraDoMapaAtual();
        if (soundManager.currentSong() != SoundManager.BGM.LEVEL_1_DAY_LOOP
                && soundManager.currentSong() != SoundManager.BGM.LEVEL_1_NIGHT_LOOP
                && soundManager.currentSong() != SoundManager.BGM.LEVEL_1_DAY_INTRO
                && soundManager.currentSong() != SoundManager.BGM.LEVEL_1_NIGHT_INTRO) {
            soundManager.crossfadeBGM(getLevel1MusicaLoop(), 2000, true);
        }
        mapLoadCooldown = 60;
    }

    public void toggleAntiAliasing() {
        renderer.toggleAntiAliasing();
    }

    public boolean isAntiAliasingEnabled() {
        return renderer.useAntiAliasing;
    }

    public void updateGame() {
        if (avisoPlebeu && currentFps < targetFps * 4 / 5) {
            avisoPlebeu = false;
            ToastNotifications.RequestNotification("plebeu detectado, desligando sombras");
            System.out.println("plebeu detectado, desligando sombras\nFPS: " + currentFps);
            Renderer.setRenderShadows(false);
        }
        if (input.isKeyJustPressed(KeyEvent.VK_ESCAPE) || input.isButtonJustPressed(InputManager.GamepadButton.START)) {
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
                player, enemyManager.getEnemies(), arenaManager.getObjetosDeCenario());

        levelManager.update();

        updateCombatTarget();
        if (dialogueManager.isAtivo()) {
            atualizarCameraSemInput();
        } else {
            atualizarCamera();
        }

        if (input.isKeyJustPressed(java.awt.event.KeyEvent.VK_E)
                || input.isButtonJustPressed(InputManager.GamepadButton.Y)) {
            arenaManager.interagir(player, player.getChaves());
            System.out.println("Apertou E");
        }
        if (getDebug()) {
            debugInputProcessing();
        }

    }

    public void updateCutscene() {
        cutsceneManager.update();
        if (cutsceneManager.isBossIntroAtiva()) {
            MorsaBoss morsa = enemyManager.getMorsaBoss();
            if (morsa != null) {
                morsa.atualizarCutsceneIntro(); // só cuida do rugido/tremida — sem alvo, sem ataque, sem BossMao
            }
        }
        if (dialogueManager.isAtivo()) {
            atualizarCameraSemInput();
        } else {
            atualizarCamera();
        }
        if (!cutsceneManager.isAtiva()) {
            player.setBlockInputs(false);
            gameState = GameState.PLAYING;
        }

    }

    private void atualizarCamera() {
        camera.update(player, input, getWidth(), getHeight());
        fishingManager.syncToCamera(camera, getWidth(), getHeight());
    }

    private void atualizarCameraSemInput() {
        camera.updateSemNovoInput(player, getWidth(), getHeight());
        fishingManager.syncToCamera(camera, getWidth(), getHeight());
    }

    private void updateCombatTarget() {
        MorsaBoss morsa = enemyManager.getMorsaBoss();

        if (morsa == null || morsa.isDead() || !morsa.isPodeRugir()) {
            camera.clearCombatTarget();
            return;
        }

        Rectangle2D.Double limites = new Rectangle2D.Double(
                morsa.getX(),
                morsa.getY(),
                morsa.getLargura(),
                morsa.getAltura());

        for (Enemy enemy : arenaManager.getCombatCameraEnemies()) {
            Rectangle2D.Double retangulo = new Rectangle2D.Double(
                    enemy.getX(),
                    enemy.getY(),
                    enemy.getLargura(),
                    enemy.getAltura());

            double minX = Math.min(limites.x, retangulo.x);
            double minY = Math.min(limites.y, retangulo.y);
            double maxX = Math.max(limites.x + limites.width, retangulo.x + retangulo.width);
            double maxY = Math.max(limites.y + limites.height, retangulo.y + retangulo.height);

            limites = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        }

        camera.setCombatTargetBounds(limites);
    }

    private double calculateBaseZoom(int height) {
        return BASE_ZOOM * (height / (double) BASE_HEIGHT);
    }

    private void configurarCameraDoMapaAtual() {
        int telaLargura = getWidth();
        int telaAltura = getHeight();

        camera.resetCameraState(player.getX(), player.getY(), player.getLargura(), player.getAltura(),
                telaLargura, telaAltura);

        Rectangle2D.Double cameraFocus = levelManager.getCameraFocus();
        if (cameraFocus != null) {
            camera.focarEmRectTeleport(cameraFocus, telaLargura, telaAltura);
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

                if (obj.hitbox != null) {
                    AffineTransform scaleTransform = new AffineTransform();
                    scaleTransform.scale(GameCore.scale, GameCore.scale);
                    obj.hitbox = scaleTransform.createTransformedShape(obj.hitbox);
                }

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
                case "spawn_npc" -> {
                    NPC npc = NPCRegistry.create(obj, camera, soundManager);
                    if (npc != null) {
                        npcManager.spawn(npc);
                        System.out.println("Spawnou NPC '" + obj.npc_nome + "' em: " + obj.x + ", " + obj.y);
                    }
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
        musicaDeDiaAtiva = true;
        musicaDeFightAtiva = false;
        updateDayNightAnteriorNanos = -1L;
        elapsedGameSeconds = horarioinicial * fullDaySeconds;
        dayProgress = horarioinicial;
        hasCheckpoint = false;
        checkArenas.clear();
        estadoLevel1AntesDaLoja = null;
        itensLevel1AntesDaLoja.clear();
        temRetornoDaLoja = false;
        camera.setModoCombate(false);
        camera.desfocarCamera();
        arenaManager.setFirstArenaFlag(true);
        arenaManager.setFezCutscene(false);
        FishingManager.setPlayerHasKey(false);
        fishingManager.cancelFishing();
        fishingManager.setfirstFlag(true);
        npcManager.clearAll();
        chavesColetadasCheckpoint = 0;
        player.resetarProgresso();
        hud.resetJaPegou();
        renderer.setBorderProgress(0.0);
        setCinematicBorderAnimation(Renderer.BorderState.IDLE);
        introPreDelay = false;
        introPreDelayTimer = 0;
        introPendente = false;
        introDialogoAtiva = false;
        introTimer = 0;
        levelManager.carregarNivel(LoadSave.LEVEL_1_DATA);
        configurarCameraDoMapaAtual();
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

    private void drawLateHudElements(Graphics2D g2, double delta) {
        hud.desenha_moedas_e_isca(g2, player, getWidth(), getHeight(), renderer.getOffset(), delta);
        hud.player_hearts(g2, player, renderer.getOffset());
        hud.ammobar(g2, getWidth(), getHeight(), player, renderer.getOffset());
        hud.desenha_chaves(g2, player, getWidth(), getHeight(), renderer.getOffset(), delta);

        if (dialogueManager != null && dialogueManager.isAtivo()) {
            dialogueManager.renderizar(g2, getWidth(), getHeight());
        }
    }

    public void render(BufferStrategy bs, double delta) {
        do {
            do {
                Graphics2D g2 = (Graphics2D) bs.getDrawGraphics();
                if (gameState == GameState.PLAYING || gameState == GameState.SHOP || gameState == GameState.PAUSED
                        || gameState == GameState.CUTSCENE) {
                    ToastNotifications.update(delta);

                }
                switch (gameState) {
                    case MAIN_MENU -> {
                        mainMenu.render(g2, getWidth(), getHeight());
                    }
                    case PLAYING -> {

                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, !estaDentroLoja, dayProgress, delta,
                                true, true);

                    }
                    case SHOP -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, !estaDentroLoja, dayProgress, delta,
                                false, false);
                        // renderizar os elementos de venda por cima
                        ShopMenu shop = getShopMenu();
                        if (shop != null) {
                            shop.render(g2, getWidth(), getHeight());
                        }
                    }
                    case GAME_OVER -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, !estaDentroLoja, dayProgress, delta,
                                true, false);

                        gameOverScreen.render(g2, getWidth(), getHeight());
                    }
                    case PAUSED -> {
                        renderer.renderizar(g2, camera, player, input,
                                getWidth(), getHeight(),
                                levelManager, bulletmanager, itemManager,
                                enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                cutsceneManager, !estaDentroLoja, dayProgress, delta,
                                true, false);

                        pauseMenu.render(g2, getWidth(), getHeight());

                    }
                    case CUTSCENE -> {
                        {
                            renderer.renderizar(g2, camera, player, input,
                                    getWidth(), getHeight(),
                                    levelManager, bulletmanager, itemManager,
                                    enemyManager, arenaManager, hud, dialogueManager, fishingManager, npcManager,
                                    cutsceneManager, !estaDentroLoja, dayProgress, delta,
                                    true, false);

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
                if (gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.CUTSCENE
                        || gameState == GameState.GAME_OVER || gameState == GameState.SHOP) {
                    drawLateHudElements(g2, delta);
                }
                screenTransition.draw(g2, getWidth(), getHeight());
                if (gameState == GameState.PLAYING && showFpsCounter) {
                    drawFpsCounter(g2);
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
        if (rects == null || rects.length == 0) {
            return null;
        }
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax) {
                Xmax = x;
            } else if (x < Xmax) {
                Xmax = x;
            }
            if (y > Ymax) {
                Ymax = y;
            } else if (y < Ymin) {
                Ymin = y;
            }
        }
        return new Rectangle2D.Double(Xmin, Ymin, Xmax - Xmin, Ymax - Ymin);
    }

    public static Rectangle2D getRetanguloComum(Rectangle2D[] rects, double margin) {
        if (rects == null || rects.length == 0) {
            return null;
        }
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax) {
                Xmax = x;
            } else if (x < Xmax) {
                Xmax = x;
            }
            if (y > Ymax) {
                Ymax = y;
            } else if (y < Ymin) {
                Ymin = y;
            }
        }
        return new Rectangle2D.Double(Xmin - margin, Ymin - margin, Xmax - Xmin + 2 * margin, Ymax - Ymin + 2 * margin);
    }

    public static Rectangle2D getRetanguloComum(Rectangle2D[] rects, double marginX, double marginY) {
        if (rects == null || rects.length == 0) {
            return null;
        }
        double Ymin, Ymax;
        double Xmin, Xmax;
        Ymin = rects[0].getY();
        Xmin = rects[0].getX();
        Ymax = Ymin;
        Xmax = Xmin;
        for (Rectangle2D rect : rects) {
            double x = rect.getX();
            double y = rect.getY();
            if (x > Xmax) {
                Xmax = x;
            } else if (x < Xmax) {
                Xmax = x;
            }
            if (y > Ymax) {
                Ymax = y;
            } else if (y < Ymin) {
                Ymin = y;
            }
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
        System.out.println("se aparecer algum erro de libusb.dll ignore ");
        GameCore game = new GameCore();
        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        game.frame = new JFrame("Pingu 007");
        game.frame.setIconImage(
                pingu_portrait.getScaledInstance(64, 64,
                        Image.SCALE_SMOOTH));
        game.frame.add(game);
        game.frame.pack();
        game.frame.setMinimumSize(new Dimension(800, 500));
        game.frame.setLocationRelativeTo(null);
        game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        game.frame.setResizable(true);
        game.optionsMenu.repositionElements(game.getWidth(), game.getHeight(), game);
        game.keyBindingsMenu.repositionElements(game.getWidth(), game.getHeight());
        game.frame.setVisible(true);
        game.start();
    }
}
