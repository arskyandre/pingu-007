
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

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
    private final ArrayList<InteractiveObject> interactives = new ArrayList<>();
    private final ArrayList<PressureButton> buttons = new ArrayList<>();
    private final ArrayList<CollisionBlock> collisionBlocks = new ArrayList<>();
    private final ArrayList<ArenaObject> allObjects = new ArrayList<>();

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

    public void carregarObjetos(ArrayList<TiledObject> objetos) {
        npcManager.clearAll();
        arenas.clear();
        doors.clear();
        interactives.clear();
        buttons.clear();
        collisionBlocks.clear();
        allObjects.clear();
        // flagArena16Ativada = false;
        chave14_15_spawnada = false;
        cutscene_vendedor = false;
        cutsceneBossSolicitada = false;
        la_ele = false;

        for (TiledObject obj : objetos) {
            String tipo = obj.tipo != null ? obj.tipo.toLowerCase().trim() : "";

            switch (tipo) {
                case "trigger", "arena_trigger", "level_trigger" -> {
                    Arena arena = getOuCriarArena(obj.id_arena);
                    arena.trigger = obj;
                    arena.totalHordas = obj.totalHordas > 0 ? obj.totalHordas : obj.horda;
                    if (tipo.equals("level_trigger")) {
                        arena.trigger.destino = obj.destino;
                    }
                }
                case "spawner" -> {
                    if (obj.id_arena >= 0) {
                        getOuCriarArena(obj.id_arena).spawners.add(obj);
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

        restaurarArenas(estado.arenasConcluidas, player, itemManager);
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
            case InteractiveObject interactive ->
                interactives.add(interactive);
            case PressureButton button ->
                buttons.add(button);
            case CollisionBlock block -> {
                block.setDialogueManager(gameCore.getDialogueManager());
                collisionBlocks.add(block);
            }
            default -> {
            }
        }
    }

    // private int debugCooldown = 60;
    public void update(Player player, CameraManager camera, SoundManager sound) {
        for (InteractiveObject interactive : interactives) {
            interactive.update(context, player);
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
                        arena.concluida = true;
                        verificarDesativacaoParedes(arena.id, player);
                        if (!existeCombateAtivo()) {
                            gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                        }
                        /*
                         * if (!existeArenaAtiva()) {
                         * gameCore.setCinematicBorderAnimation(Renderer.BorderState.OUT);
                         * }
                         */
                        if (arena.id == 9 || arena.id == 10 || arena.id == 13 || arena.id == 14 || arena.id == 15
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

    public boolean existeArenaAtiva() {
        for (Arena arena : arenas) {
            if (arena.ativa && !arena.concluida) {
                return true;
            }
        }
        return false;
    }

    public void interagir(Player player, int chavesDoPlayer) {
        for (InteractiveObject interactive : interactives) {
            if (interactive.tryInteract(context, player, chavesDoPlayer)) {
                TiledObject data = interactive.getData();

                if ("trocar_mapa".equalsIgnoreCase(data.acao)) {
                    iniciarTransicaoDeFase(data.destino);
                }
                return;
            }
        }

        for (CollisionBlock block : collisionBlocks) {
            if (block.tryInteract(context, player, chavesDoPlayer)) {
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
                arenaNormal.concluida = true;

                // Descomente e ajuste os IDs quando for usar o puzzle normal
                // setWallState(idArenaNormal, false, player);
                // itemManager.spawn(new KeyItem(X, Y));
            }
        }
        if (botaoLightsOutAcionado != null) {
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
            arena.concluida = true;
            setWallState(idArena, false, player);
            // TODO: fazer a camera focar na chave
            camera.focarEm(5029 + 16, 4200 + 16, 90, false);
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
            arena.concluida = true;
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
                arena.concluida = true;
                player.solicitarCheckpoint();
            }
            case 1 -> {
                if (arena.totalHordas == 0) {
                    arena.concluida = true;
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
                    arena.concluida = true;
                }
                setWallState(6, true, player);
                player.solicitarCheckpoint();
            }
            case 9, 10 -> {
                if (arena.totalHordas == 0) {
                    arena.concluida = true;
                }
                if (id == 9) {
                    player.solicitarCheckpoint();
                }
                setWallState(9, true, player);
                setWallState(10, true, player);
            }
            case 14, 15 -> {
                if (arena.totalHordas == 0) {
                    arena.concluida = true;
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
                        cutsceneManager.iniciarBossIntro(camera, player, morsa.getCenterX(), morsa.getCenterY(), enemyManager);
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
                arena.concluida = true;
                player.solicitarCheckpoint();
            }
            default -> {
                if (arena.totalHordas == 0) {
                    arena.concluida = true;
                }
                setWallState(id, true, player);
            }
        }

        Rectangle2D.Double wallRect = getCombinedWallRect(id);

        if (wallRect != null) {
            if (isFirstArena) {
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
            levelManager.carregarNivel(mapaDestino);
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

                Enemy inimigo = enemyManager.adicionarE_RetornarInimigo(
                        spawner.inimigo, spawner.x, spawnY, spawner.horda, arena.id);

                if (inimigo != null) {
                    /*
                     * if (arena.id == 16 || arena.id == 2) {
                     * inimigo.podePularBuracos = false;
                     * }
                     */
                    arena.inimigosVivos.add(inimigo);
                }
            }
        }
    }

    public void restaurarArenas(ArrayList<Integer> salvas, Player player, ItemManager itemManager) {
        boolean rebobinouAlgumPuzzle = false;
        // impedindo de spawnar varas infinitamente (vixi la ele)
        la_ele = false;
        if (npcManager != null) {
            npcManager.getNpcs().removeIf(npc -> npc instanceof PescadorNPC);
        }
        // mudado para apagar todos os itens EXCETO KeyItem, mudar caso dê problema dps
        //itemManager.getItems().removeIf(item -> item instanceof FishingRodItem);
        itemManager.getItems().removeIf(item -> !(item instanceof KeyItem));

        for (Arena arena : arenas) {
            for (Enemy e : arena.inimigosVivos) {
                e.marcarLootProcessado();
                e.receberDano(99999);
            }
            arena.inimigosVivos.clear();

            if (salvas.contains(arena.id)) {
                arena.concluida = true;
                arena.ativa = false;
                verificarDesativacaoParedes(arena.id, player);
            } else {
                if (arena.concluida) {
                    rebobinouAlgumPuzzle = true;
                }

                arena.concluida = false;
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

    private Arena getOuCriarArena(int id) {
        for (Arena arena : arenas) {
            if (arena.id == id) {
                return arena;
            }
        }
        Arena nova = new Arena();
        nova.id = id;
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
        ArrayList<Integer> concluidas = new ArrayList<>();
        for (Arena arena : arenas) {
            if (arena.concluida) {
                concluidas.add(arena.id);
            }
        }
        return concluidas;
    }
}
