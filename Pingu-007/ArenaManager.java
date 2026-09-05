
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ArenaManager {
    private final LevelManager levelManager;
    private final EnemyManager enemyManager;
    private final ItemManager itemManager;
    private final ArenaContext context;
    private final NPCManager npcManager;
    private final CutsceneManager cutsceneManager;
    private CameraManager camera;
    private final SoundManager soundManager;
    private final GameCore gameCore;

    private ArrayList<MapObject> objetosDeCenario = new ArrayList<>();
    private final ArrayList<InteractiveMapObject> interactives = new ArrayList<>();
    // Trigger e spawner não viram uma classe própria, então não tem como implementar DebugRenderable.
    private final ArrayList<TiledObject> triggersESpawnersParaDebug = new ArrayList<>();

    // public boolean flagArena16Ativada = false;
    private boolean chave14_15_spawnada = false;
    private boolean cutscene_vendedor = false;
    private boolean cutsceneBossSolicitada = false;
    private boolean fezCutscene = false;
    private boolean la_ele = false;
    private boolean isFirstArena = true;

    public static class Arena {

        int id;
        boolean ativa = false;
        boolean concluida = false;
        int hordaAtual = 0;
        int totalHordas = 0;
        ArrayList<TiledObject> spawners = new ArrayList<>();
        ArrayList<Enemy> inimigosVivos = new ArrayList<>();
        TiledObject trigger;

        boolean geraRecompensaPadrao = true;
        boolean randomSpawns = false;
    }

    public static final class EstadoMapa {

        private final ArrayList<Integer> arenasConcluidas;
        private final boolean chave14_15_spawnada;
        private final boolean cutscene_vendedor;
        private final boolean cutsceneBossSolicitada;
        private final boolean fezCutscene;
        private final boolean la_ele;
        private final boolean isFirstArena;

        private EstadoMapa(ArrayList<Integer> arenasConcluidas,
                boolean chave14_15_spawnada,
                boolean cutscene_vendedor,
                boolean cutsceneBossSolicitada,
                boolean fezCutscene,
                boolean la_ele,
                boolean isFirstArena) {
            this.arenasConcluidas = new ArrayList<>(arenasConcluidas);
            this.chave14_15_spawnada = chave14_15_spawnada;
            this.cutscene_vendedor = cutscene_vendedor;
            this.cutsceneBossSolicitada = cutsceneBossSolicitada;
            this.fezCutscene = fezCutscene;
            this.la_ele = la_ele;
            this.isFirstArena = isFirstArena;
        }
    }

    private final ArrayList<Arena> arenas = new ArrayList<>();
    private final ArrayList<DoorObject> doors = new ArrayList<>();
    private final ArrayList<PressureButton> buttons = new ArrayList<>();
    private final ArrayList<ArenaObject> allObjects = new ArrayList<>();
    private final java.util.Set<Integer> arenasConcluidas = new java.util.HashSet<>();

    public ArenaManager(EnemyManager enemyManager, LevelManager levelManager, ItemManager itemManager,
            NPCManager npcm, CutsceneManager CM, GameCore gc, CameraManager cameraMgr, SoundManager soundMgr) {
        this.cutsceneManager = CM;
        camera = cameraMgr;
        this.npcManager = npcm;
        this.soundManager = soundMgr;
        this.enemyManager = enemyManager;
        this.levelManager = levelManager;
        this.itemManager = itemManager;
        this.context = new ArenaContext(levelManager, enemyManager);
        this.gameCore = gc;
    }

    public interface ObservadorArenas {
        void arenaCriada(Arena arena);
        void hordasCarregadas(int idArena, int totalHordas);
        void combateConcluido(int idArena);
    }

    private ObservadorArenas observadorArenas;

    void observarArenas(ObservadorArenas observador) {
        this.observadorArenas = observador;
    }

    java.util.Set<Integer> getHistoricoArenasConcluidas() {
        return java.util.Collections.unmodifiableSet(arenasConcluidas);
    }

    public void limparHistoricoArenasConcluidas() {
        arenasConcluidas.clear();
    }

    public void configurarSpawns(Arena arena, boolean aleatorios, boolean recompensaPadrao) {
        arena.randomSpawns = aleatorios;
        arena.geraRecompensaPadrao = recompensaPadrao;
    }

    public void prepararArenaParaRepeticao(int id) {
        arenasConcluidas.remove(id);
        for (Arena arena : arenas) {
            if (arena.id == id) {
                configurarSpawns(arena, true, false);
                desmarcarArenaConcluida(arena);
                arena.ativa = false;
                arena.hordaAtual = 0;
            }
        }
    }

    public void restaurarArenaParaRepeticao(int id, Player player) {
        Arena arena = getOuCriarArena(id);
        arena.concluida = false;
        arena.ativa = false;
        configurarSpawns(arena, true, false);
        setWallState(id, false, player);
    }

    private void marcarArenaConcluida(Arena arena) {
        arena.concluida = true;
        arenasConcluidas.add(arena.id);
    }

    private void desmarcarArenaConcluida(Arena arena) {
        arena.concluida = false;
        arenasConcluidas.remove(arena.id);
    }

    public void carregarObjetos(ArrayList<TiledObject> objetos) {
        npcManager.clearAll();
        arenas.clear();
        doors.clear();
        interactives.clear();
        buttons.clear();
        allObjects.clear();
        objetosDeCenario.clear();
        triggersESpawnersParaDebug.clear();
        // flagArena16Ativada = false;
        chave14_15_spawnada = false;
        cutscene_vendedor = false;
        cutsceneBossSolicitada = false;
        la_ele = false;

        for (TiledObject obj : objetos) {
            String tipo = obj.tipo != null ? obj.tipo.toLowerCase().trim() : "";
            String acao = obj.acao != null ? obj.acao.toLowerCase().trim() : "";

            switch (tipo) {
                case "trigger", "arena_trigger", "level_trigger" -> {
                    Arena arena = getOuCriarArena(obj.id_arena);
                    arena.trigger = obj;
                    arena.totalHordas = obj.totalHordas > 0 ? obj.totalHordas : obj.horda;
                    if (arena.totalHordas > 0) {
                        if (observadorArenas != null) {
                            observadorArenas.hordasCarregadas(arena.id, arena.totalHordas);
                        }
                    }
                    if (tipo.equals("level_trigger")) {
                        arena.trigger.destino = obj.destino;
                    }
                    triggersESpawnersParaDebug.add(obj);
                }
                case "spawner", "spawn_player", "spawn_npc" -> {
                    if (tipo.equals("spawner") && obj.id_arena >= 0) {
                        getOuCriarArena(obj.id_arena).spawners.add(obj);
                    }
                    triggersESpawnersParaDebug.add(obj);
                }
                case "map_object", "interativo", "colision" -> {
                    boolean isInteract = obj.isInteractive || "interativo".equals(tipo) || "pescar".equals(acao) || "trocar_mapa".equalsIgnoreCase(acao);

                    if (isInteract) {
                        InteractiveMapObject interativo = new InteractiveMapObject(obj, gameCore.getDialogueManager(), gameCore);
                        objetosDeCenario.add(interativo);
                        interactives.add(interativo);
                    } else if ("colision".equals(tipo)) {
                        CollisionPolygon block = new CollisionPolygon(obj);
                        objetosDeCenario.add(block);
                    } else {
                        MapObject novoObjeto = new MapObject(obj);
                        objetosDeCenario.add(novoObjeto);
                    }
                }
                default -> {
                    ArenaObject arenaObject = ArenaObjectRegistry.create(obj);
                    if (arenaObject != null) {
                        allObjects.add(arenaObject);
                        registerTypedObject(arenaObject);
                        arenaObject.onLoad(context);
                    }
                }
            }
        }

        setWallState(3, true, null);
        setWallState(101, true, null);
        setWallState(102, true, null);
    }

    public void setFezCutscene(boolean set) {
        fezCutscene = set;
    }

    public boolean getFezCutscene() {
        return fezCutscene;
    }

    public EstadoMapa capturarEstadoMapa() {
        return new EstadoMapa(
                getArenasConcluidas(),
                chave14_15_spawnada,
                cutscene_vendedor,
                cutsceneBossSolicitada,
                fezCutscene,
                la_ele,
                isFirstArena);
    }

    public void restaurarEstadoMapa(EstadoMapa estado, Player player, ItemManager itemManager) {
        if (estado == null) {
            return;
        }

        // Estas flags precisam ser restauradas antes das arenas para que
        // verificarDesativacaoParedes nao repita cutscenes nem recompensas.
        chave14_15_spawnada = estado.chave14_15_spawnada;
        cutscene_vendedor = estado.cutscene_vendedor;
        // cutsceneBossSolicitada = estado.cutsceneBossSolicitada;
        fezCutscene = estado.fezCutscene;
        isFirstArena = estado.isFirstArena;

        restaurarArenas(getArenasConcluidas(), player, itemManager);
        la_ele = estado.la_ele;
    }

    private Rectangle2D.Double getCombinedWallRect(int idArena) {
        Rectangle2D.Double combined = null;
        for (DoorObject door : doors) {
            if (door.getArenaId() == idArena && door.isClosed()) {
                TiledObject d = door.getData();
                Rectangle2D.Double r = new Rectangle2D.Double(d.x, d.y, d.width, d.height);
                combined = (combined == null) ? r : (Rectangle2D.Double) combined.createUnion(r);
            }
        }
        return combined;
    }

    public void setFirstArenaFlag(boolean fg) {
        isFirstArena = fg;
    }

    private void registerTypedObject(ArenaObject arenaObject) {
        switch (arenaObject) {
            case DoorObject door ->
                doors.add(door);
            case PressureButton button ->
                buttons.add(button);
            default -> {
            }
        }
    }

    // private int debugCooldown = 60;
    public void update(Player player, CameraManager camera, SoundManager sound) {
        for (InteractiveMapObject interactive : interactives) {
            interactive.update(player);
        }
        atualizarBotoes(player);
        for (int i = 0; i < arenas.size(); i++) {
            Arena arena = arenas.get(i);
            /*
             * if (arena.ativa && !arena.concluida && !arena.inimigosVivos.isEmpty()) {
             * if (debugCooldown <= 0) {
             * System.out.print("[ARENA " + arena.id + "] Fantasmas: ");
             * for (Enemy e : arena.inimigosVivos) {
             * System.out.print(e.getClass().getSimpleName() + " (X:" + (int) e.getX() +
             * ", Y:" + (int) e.getY() + ", Vida:" + e.getVida() + ", Caindo:" + e.isCaindo
             * + ") | ");
             * }
             * System.out.println();
             * }
             * }
             */

            if (!arena.ativa && !arena.concluida && arena.trigger != null) {
                if (!player.no_clip && ArenaTriggers.collides(arena.trigger, player)) {
                    ativarArena(arena.id, player, camera, cutsceneManager, sound);
                    return;
                }
            }
            if (arena.ativa && !arena.concluida) {
                arena.inimigosVivos.removeIf(e -> e.isDead() || e.isCaindo);
                if (arena.inimigosVivos.isEmpty()) {
                    if (arena.hordaAtual < arena.totalHordas) {
                        arena.hordaAtual++;
                        spawnHorda(arena);
                    } else {
                        marcarArenaConcluida(arena);
                        verificarDesativacaoParedes(arena.id, player);
                        if (observadorArenas != null) {
                            observadorArenas.combateConcluido(arena.id);
                        }

                        if (!existeCombateAtivo()) {
                            gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                        }
                        /*
                         * if (!existeArenaAtiva()) {
                         * gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                         * }
                         */
                        if (arena.id == 7 || arena.id == 9 || arena.id == 10 || arena.id == 13 || arena.id == 14 || arena.id == 15
                                || arena.id == 16) {
                            player.solicitarCheckpoint();
                            System.out.println("Checkpoint garantido após vencer a arena " + arena.id);
                        }
                    }
                }
            }
        }
        /*
         * debugCooldown--;
         * if (debugCooldown < 0) {
         * debugCooldown = 60;
         * }
         */
    }

    public boolean existeCombateAtivo() {
        for (Arena arena : arenas) {
            if (arena.ativa && !arena.concluida) {
                if (!arena.inimigosVivos.isEmpty() || arena.hordaAtual < arena.totalHordas) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Enemy> getCombatCameraEnemies() {
        ArrayList<Enemy> inimigos = new ArrayList<>();

        for (Arena arena : arenas) {
            if (!arena.ativa || arena.concluida) {
                continue;
            }

            for (Enemy enemy : arena.inimigosVivos) {
                if (enemy != null && !enemy.isDead() && !enemy.isCaindo && enemy.includedInCombatCamera()) {
                    inimigos.add(enemy);
                }
            }
        }

        return inimigos;
    }

    public void registrarInimigoNaArena(int idArena, Enemy enemy) {
        if (enemy == null) {
            return;
        }

        Arena arena = getOuCriarArena(idArena);
        if (!arena.inimigosVivos.contains(enemy)) {
            arena.inimigosVivos.add(enemy);
        }
    }

    public boolean existeArenaAtiva() {
        for (Arena arena : arenas) {
            if (arena.ativa && !arena.concluida) {
                return true;
            }
        }
        return false;
    }

    public boolean existeArenaRealAtiva() {
        for (Arena arena : arenas) {
            if (arena.ativa && !arena.concluida && arena.id != 0) {
                return true;
            }
        }
        return false;
    }

    public void interagir(Player player, int chavesDoPlayer) {
        for (InteractiveMapObject interactive : interactives) {
            if (interactive.tryInteract(player, chavesDoPlayer)) {
                TiledObject data = interactive.getData();

                if (data != null && "trocar_mapa".equalsIgnoreCase(data.acao)) {
                    System.out.println(data.destino);
                    iniciarTransicaoDeFase(data.destino);
                }
                return;
            }
        }
    }

    public void drawOverlays(java.awt.Graphics2D g2) {
        for (ArenaObject object : allObjects) {
            object.drawOverlay(g2);
        }
    }

    public ArrayList<ArenaObject> getArenaObjects() {
        return allObjects;
    }

    private void atualizarBotoes(Player player) {
        PressureButton botaoLightsOutAcionado = null;
        boolean temBotaoNormal = false;
        boolean todosNormaisAtivados = true;
        int idArenaNormal = -1;

        for (PressureButton button : buttons) {
            boolean pisavaAntes = button.isPlayerPisando();
            button.update(context, player);

            if (button.isToggleMode()) {
                if (button.isPlayerPisando() && !pisavaAntes) {
                    botaoLightsOutAcionado = button;
                }
            } else {
                temBotaoNormal = true;
                idArenaNormal = button.getData().id_arena;
                if (!button.isPressed()) {
                    todosNormaisAtivados = false;
                }
            }
        }

        if (temBotaoNormal && idArenaNormal != -1) {
            Arena arenaNormal = getOuCriarArena(idArenaNormal);
            if (!arenaNormal.concluida && todosNormaisAtivados) {
                System.out.println("=== PUZZLE DOS BOTÕES NORMAIS DA ARENA " + idArenaNormal + " CONCLUÍDO! ===");
                marcarArenaConcluida(arenaNormal);

                // Descomente e ajuste os IDs quando for usar o puzzle normal
                // setWallState(idArenaNormal, false, player);
                // itemManager.spawn(new KeyItem(X, Y));
            }
        }
        if (botaoLightsOutAcionado != null) {
            soundManager.playSFX(SoundManager.SFX.CLICK);
            alternarLightsOut(botaoLightsOutAcionado, player);
        }
    }

    private void alternarLightsOut(PressureButton origem, Player player) {
        origem.toggle(context);

        double espacamentoGrade = GameCore.tiles_size * 3;
        double origemX = origem.getData().x;
        double origemY = origem.getData().y;

        for (PressureButton vizinho : buttons) {
            if (vizinho == origem || !vizinho.isToggleMode()
                    || vizinho.getData().id_arena != origem.getData().id_arena) {
                continue;
            }

            double dx = Math.abs(origemX - vizinho.getData().x);
            double dy = Math.abs(origemY - vizinho.getData().y);

            if ((Math.abs(dx - espacamentoGrade) < 2 && dy < 2) || (Math.abs(dy - espacamentoGrade) < 2 && dx < 2)) {
                vizinho.toggle(context);
            }
        }
        verificarVitoriaLightsOut(origem.getData().id_arena, player);
    }

    private void verificarVitoriaLightsOut(int idArena, Player player) {
        Arena arena = getOuCriarArena(idArena);
        if (arena.concluida) {
            return;
        }

        boolean todosLightsOutAtivados = true;
        boolean temBotaoLightsOut = false;

        for (PressureButton btn : buttons) {
            if (btn.isToggleMode() && btn.getData().id_arena == idArena) {
                temBotaoLightsOut = true;
                if (!btn.isPressed()) {
                    todosLightsOutAtivados = false;
                    break;
                }
            }
        }

        if (temBotaoLightsOut && todosLightsOutAtivados) {
            System.out.println("=== PUZZLE LIGHTS OUT DA ARENA " + idArena + " CONCLUÍDO! ===");
            marcarArenaConcluida(arena);
            setWallState(idArena, false, player);
            camera.focarEm(5029 + 16, 4200 + 16, 90, false);
            soundManager.playSFX(SoundManager.SFX.KEY_SPAWN);
            itemManager.spawn(new KeyItem(5029, 4200));
        }
    }

    private void setWallState(int idArena, boolean closed, Player player) {
        for (DoorObject door : doors) {
            if (door.getArenaId() == idArena) {
                door.setClosed(context, closed, player);
            }
        }
    }

    private void ativarArena(int id, Player player, CameraManager camera, CutsceneManager cutsceneManager,
            SoundManager sound) {
        System.out.println("Ativou arena: " + id);

        Arena arena = getOuCriarArena(id);
        arena.ativa = true;

        if (arena.trigger != null && arena.trigger.destino != null && !arena.trigger.destino.isEmpty()) {
            iniciarTransicaoDeFase(arena.trigger.destino);
            marcarArenaConcluida(arena);
            return;
        }

        if (id == 0) {
            player.setTemporarySpriteOverride(7, 2);
        }
        if (id != 67 && id != 102) {
            System.out.println("CUTSCENE setado em ArenaManager, id = " + id);
            GameCore.setGameState(GameState.CUTSCENE);
        }
        if (id != 102) {
            sound.playSFX(SoundManager.SFX.ARENA_ENTER);
            gameCore.setCinematicBorderAnimation(Renderer.BorderState.IN);
        }

        if (arena.totalHordas > 0) {
            arena.hordaAtual = 1;
            spawnHorda(arena);
        }

        switch (id) {
            case 0 -> {
                setWallState(0, true, player);
                marcarArenaConcluida(arena);
                player.solicitarCheckpoint();
            }
            case 1 -> {
                if (arena.totalHordas == 0) {
                    marcarArenaConcluida(arena);
                }
                setWallState(1, true, player);
                player.solicitarCheckpoint();
            }
            case 2 -> {
                setWallState(2, true, player);
            }
            case 4, 5 -> {
                setWallState(4, true, player);
            }
            case 6 -> {
                if (arena.totalHordas == 0) {
                    marcarArenaConcluida(arena);
                }
                setWallState(6, true, player);
                player.solicitarCheckpoint();
            }
            case 9, 10 -> {
                if (arena.totalHordas == 0) {
                    marcarArenaConcluida(arena);
                }
                if (id == 9) {
                    player.solicitarCheckpoint();
                }
                setWallState(9, true, player);
                setWallState(10, true, player);
            }
            case 14, 15 -> {
                if (arena.totalHordas == 0) {
                    marcarArenaConcluida(arena);
                }
                if (id == 14) {
                    player.solicitarCheckpoint();
                }
                setWallState(14, true, player);
                setWallState(15, true, player);
            }
            case 16 -> {
                player.solicitarCheckpoint();
            }
            case 67 -> {
                setWallState(67, true, player);

                MorsaBoss morsa = enemyManager.getMorsaBoss();
                if (morsa != null && camera != null && cutsceneManager != null) {

                    morsa.setEnemyManager(enemyManager);
                    Arena arenaAtiva = getOuCriarArena(67);
                    for (TiledObject spawner : arenaAtiva.spawners) {
                        if (spawner.inimigo != null && !spawner.inimigo.toLowerCase().contains("morsa")) {
                            morsa.adicionarPontoDeSpawn(spawner.x, spawner.y);
                        }
                    }

                    if (morsa.getMaoEsquerda() != null && !arenaAtiva.inimigosVivos.contains(morsa.getMaoEsquerda())) {
                        arenaAtiva.inimigosVivos.add(morsa.getMaoEsquerda());
                    }
                    if (morsa.getMaoDireita() != null && !arenaAtiva.inimigosVivos.contains(morsa.getMaoDireita())) {
                        arenaAtiva.inimigosVivos.add(morsa.getMaoDireita());
                    }

                    morsa.vincularCamera(camera);

                    if (!cutsceneBossSolicitada) {
                        cutsceneManager.iniciarBossIntro(camera, player, morsa.getCenterX(), morsa.getCenterY(),
                                enemyManager);
                        cutsceneBossSolicitada = true;
                    } else {
                        morsa.setPodeRugir(true);
                        player.setBlockInputs(false);
                    }
                } else {
                    System.out.println("ERRO: Morsa não encontrada na Arena 67!");
                }
            }
            case 102 -> {
                setWallState(102, false, player);
                marcarArenaConcluida(arena);
                player.solicitarCheckpoint();
            }
            default -> {
                if (arena.totalHordas == 0) {
                    marcarArenaConcluida(arena);
                }
                setWallState(id, true, player);
            }
        }

        Rectangle2D.Double wallRect = getCombinedWallRect(id);

        if (wallRect != null) {
            if (isFirstArena || id == 0) {
                cutsceneManager.iniciarWallRevealComCamera(wallRect, camera, player);
                if (id != 67) {
                    GameCore.setGameState(GameState.CUTSCENE);
                }
                isFirstArena = false;
            } else {
                cutsceneManager.iniciarWallFade(wallRect);
            }
        }
        if (id == 0) {
            isFirstArena = true;
        }
        // pra rodar a cutscene em uma arena de verdade
    }

    private void verificarDesativacaoParedes(int id, Player player) {
        switch (id) {
            case 0 -> {
            }
            case 2, 3 -> {
                if (id == 2 && !la_ele) {
                    if (!fezCutscene) {
                        camera.focarEm(20.5 * GameCore.tiles_size, 45.3 * GameCore.tiles_size, 60, false);
                        fezCutscene = true;
                    }
                    npcManager.spawn(new PescadorNPC(20.5 * GameCore.tiles_size, 45.3 * GameCore.tiles_size, camera,
                            soundManager));
                    System.out.printf("Spawnou pescador em: %f, %f\n", 20.5 * GameCore.tiles_size,
                            45.7 * GameCore.tiles_size);
                    la_ele = true;
                }
                if (isArenaConcluida(2) && isArenaConcluida(3)) {
                    setWallState(2, false, player);
                    setWallState(3, false, player);
                }
            }
            case 4, 5 -> {
                if (isArenaConcluida(4) && isArenaConcluida(5)) {
                    setWallState(4, false, player);
                }
            }
            case 9, 10 -> {
                if (isArenaConcluida(9) && isArenaConcluida(10)) {
                    setWallState(9, false, player);
                    setWallState(10, false, player);
                    if (!cutscene_vendedor) {
                        camera.focarEm((double) (250.5 * GameCore.tiles_size), (double) (58.3 * GameCore.tiles_size),
                                120, false);
                        cutscene_vendedor = true;
                    }
                }
            }
            case 14, 15 -> {
                if (isArenaConcluida(14) && isArenaConcluida(15)) {
                    setWallState(14, false, player);
                    setWallState(15, false, player);

                    if (!chave14_15_spawnada) {
                        camera.focarEm(12839 + 16, 4870 + 16, 90, false);
                        soundManager.playSFX(SoundManager.SFX.KEY_SPAWN);
                        itemManager.spawn(new KeyItem(12839, 4870));
                        chave14_15_spawnada = true;
                    }
                }
            }
            case 16, 101, 102, 999 -> {
            }
            default ->
                setWallState(id, false, player);
        }
    }

    private void iniciarTransicaoDeFase(String mapaDestino) {
        System.out.println(">>> Carregando mapa: " + mapaDestino + " <<<");

        if (mapaDestino != null && !mapaDestino.isEmpty()) {
            if (LoadSave.CASA_VENDEDOR.equals(mapaDestino)) {
                if (cutscene_vendedor) {
                    gameCore.entrarCasaVendedor();
                }
                return;
            }
            if (LoadSave.LEVEL_1_DATA.equals(mapaDestino)
                    && LoadSave.CASA_VENDEDOR.equals(levelManager.getArquivoNivelAtual())) {
                gameCore.sairCasaVendedor();
                return;
            }
            if (LoadSave.LEVEL_2_DATA.equals(mapaDestino)) {
                gameCore.entrarNivelBoss();
                return;
            }
            gameCore.transicionarMapa(mapaDestino);
        }
    }

    private void spawnHorda(Arena arena) {
        for (TiledObject spawner : arena.spawners) {
            if (spawner.horda == arena.hordaAtual) {
                double spawnY = spawner.y;
                if (spawner.height > 0) {
                    spawnY -= spawner.height;
                } else if (spawner.gid > 0) {
                    spawnY -= GameCore.tiles_size;
                }

                // Aplica a configuração de spawns da arena.
                String tipoSorteado = spawner.inimigo;
                if (!arena.geraRecompensaPadrao && arena.randomSpawns) {
                    String[] inimigosPossiveis = {"lobo", "shooter", "bomber", "jumper"};
                    tipoSorteado = inimigosPossiveis[(int) (Math.random() * inimigosPossiveis.length)];
                }

                Enemy inimigo = enemyManager.adicionarE_RetornarInimigo(
                        tipoSorteado, spawner.x, spawnY, spawner.horda, arena.id);

                if (inimigo != null) {
                    /*
                     * if (arena.id == 16 || arena.id == 2) {
                     * inimigo.podePularBuracos = false;
                     * }
                     */

                    if (!arena.geraRecompensaPadrao) {
                        inimigo.geraRecompensaPadrao = false;
                        // inimigo.podeDropar = false;
                    }

                    arena.inimigosVivos.add(inimigo);
                }
            }
        }
    }

    // Reseta uma arena para o estado "não concluída"
    public void resetarArena(int id, Player player) {
        Arena arena = getOuCriarArena(id);
        for (Enemy e : arena.inimigosVivos) {
            e.marcarLootProcessado();
            enemyManager.removerSemEfeitos(e);
        }
        arena.inimigosVivos.clear();
        desmarcarArenaConcluida(arena);
        arena.ativa = false;
        arena.hordaAtual = 0;
        setWallState(id, false, player);
    }

    public void restaurarArenas(ArrayList<Integer> salvas, Player player, ItemManager itemManager) {
        System.out.println("[DEBUG ARENA] Restaurando arenas. Salvas no Checkpoint: " + salvas);
        boolean rebobinouAlgumPuzzle = false;
        arenasConcluidas.clear();
        arenasConcluidas.addAll(salvas);
        // impedindo de spawnar varas infinitamente (vixi la ele)
        la_ele = false;
        if (npcManager != null) {
            npcManager.getNpcs().removeIf(npc -> npc instanceof PescadorNPC);
        }
        // mudado para apagar todos os itens EXCETO KeyItem, mudar caso dê problema dps
        // itemManager.getItems().removeIf(item -> item instanceof FishingRodItem);
        itemManager.getItems().removeIf(item -> !(item instanceof KeyItem));

        for (Arena arena : arenas) {
            for (Enemy e : arena.inimigosVivos) {
                e.marcarLootProcessado();
                enemyManager.removerSemEfeitos(e);
            }
            arena.inimigosVivos.clear();

            if (salvas.contains(arena.id)) {
                marcarArenaConcluida(arena);
                arena.ativa = false;
                verificarDesativacaoParedes(arena.id, player);
            } else {
                if (arena.concluida) {
                    rebobinouAlgumPuzzle = true;
                }

                desmarcarArenaConcluida(arena);
                arena.ativa = false;
                arena.hordaAtual = 0;

                if (arena.id == 3 || arena.id == 101 || arena.id == 102) {
                    setWallState(arena.id, true, player);
                } else {
                    setWallState(arena.id, false, player);
                }

                for (PressureButton btn : buttons) {
                    if (btn.getData().id_arena == arena.id) {
                        btn.setPlayerPisando(false);
                        btn.setPressed(context, btn.getData().ativa);
                    }
                }
                if (arena.id == 10) {
                    cutscene_vendedor = false;
                }
                if (arena.id == 14 || arena.id == 15) {
                    chave14_15_spawnada = false;
                }
            }
        }

        if (rebobinouAlgumPuzzle) {
            itemManager.getItems().removeIf(item -> item instanceof KeyItem);
            System.out.println(">>> Puzzles rebobinados e chaves soltas deletadas.");
        }
    }

    public Arena getOuCriarArena(int id) {
        for (Arena arena : arenas) {
            if (arena.id == id) {
                return arena;
            }
        }
        Arena nova = new Arena();
        nova.id = id;
        if (observadorArenas != null) {
            observadorArenas.arenaCriada(nova);
        }
        arenas.add(nova);
        return nova;
    }

    private boolean isArenaConcluida(int id) {
        for (Arena arena : arenas) {
            if (arena.id == id) {
                return arena.concluida;
            }
        }
        return false;
    }

    public String getHordaInfo() {
        for (Arena arena : arenas) {
            if (arena.ativa && !arena.concluida && arena.totalHordas > 0) {
                return "Wave " + arena.hordaAtual + " / " + arena.totalHordas;
            }
        }
        return "";
    }

    public ArrayList<Integer> getArenasConcluidas() {
        ArrayList<Integer> ids = new ArrayList<>(arenasConcluidas);
        ids.sort(Integer::compareTo);
        return ids;
    }

    public ArrayList<MapObject> getObjetosDeCenario() {
        return objetosDeCenario;
    }

    public List<TiledObject> getTriggersESpawnersParaDebug() {
        return triggersESpawnersParaDebug;
    }

    public List<DebugRenderable> getObjetosInstanciadosParaDebug() {
        List<DebugRenderable> combinados = new ArrayList<>(objetosDeCenario.size() + allObjects.size());
        combinados.addAll(objetosDeCenario);
        combinados.addAll(allObjects);
        return combinados;
    }
}
