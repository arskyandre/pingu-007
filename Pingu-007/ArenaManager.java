
import java.util.ArrayList;

public class ArenaManager {

    private final LevelManager levelManager;
    private final EnemyManager enemyManager;
    private final ItemManager itemManager;
    private final ArenaContext context;
    private NPCManager npcManager;

    // public boolean flagArena16Ativada = false;
    private boolean chave14_15_spawnada = false;
    private boolean cutsceneBossSolicitada = false;
    private boolean pesqueiro_spawnado = false;

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

    private final ArrayList<Arena> arenas = new ArrayList<>();
    private final ArrayList<DoorObject> doors = new ArrayList<>();
    private final ArrayList<InteractiveObject> interactives = new ArrayList<>();
    private final ArrayList<PressureButton> buttons = new ArrayList<>();
    private final ArrayList<CollisionBlock> collisionBlocks = new ArrayList<>();
    private final ArrayList<ArenaObject> allObjects = new ArrayList<>();

    public ArenaManager(EnemyManager enemyManager, LevelManager levelManager, ItemManager itemManager,
            NPCManager npcm) {
        this.npcManager = npcm;
        this.enemyManager = enemyManager;
        this.levelManager = levelManager;
        this.itemManager = itemManager;
        this.context = new ArenaContext(levelManager, enemyManager);
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

    private void registerTypedObject(ArenaObject arenaObject) {
        switch (arenaObject) {
            case DoorObject door ->
                doors.add(door);
            case InteractiveObject interactive ->
                interactives.add(interactive);
            case PressureButton button ->
                buttons.add(button);
            case CollisionBlock block ->
                collisionBlocks.add(block);
            default -> {
            }
        }
    }

    public void update(Player player, CameraManager camera, CutsceneManager cutsceneManager) {
        atualizarBotoes(player);
        for (int i = 0; i < arenas.size(); i++) {
            Arena arena = arenas.get(i);
            if (!arena.ativa && !arena.concluida && arena.trigger != null) {
                if (ArenaTriggers.collides(arena.trigger, player)) {
                    ativarArena(arena.id, player, camera, cutsceneManager);

                    return;

                }
            }
            if (arena.ativa && !arena.concluida) {
                arena.inimigosVivos.removeIf(Enemy::isDead);
                if (arena.inimigosVivos.isEmpty()) {
                    if (arena.hordaAtual < arena.totalHordas) {
                        arena.hordaAtual++;
                        spawnHorda(arena);
                    } else {
                        arena.concluida = true;
                        verificarDesativacaoParedes(arena.id, player);

                        if (arena.id == 9 || arena.id == 10 || arena.id == 13 || arena.id == 14 || arena.id == 15
                                || arena.id == 16) {
                            player.solicitarCheckpoint();
                            System.out.println("Checkpoint garantido após vencer a arena " + arena.id);
                        }
                    }
                }
            }
        }
    }

    public void interagir(Player player, int chavesDoPlayer) {
        for (InteractiveObject interactive : interactives) {
            if (interactive.tryInteract(context, player, chavesDoPlayer)) {
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

    private void ativarArena(int id, Player player, CameraManager camera, CutsceneManager cutsceneManager) {
        Arena arena = getOuCriarArena(id);
        arena.ativa = true;
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
                // trigger do level 2
                setWallState(67, true, player);
                arena.concluida = true;
                // player.solicitarCheckpoint();
                //
                //
                //
                MorsaBoss morsa = enemyManager.getMorsaBoss();
                if (morsa != null && camera != null && cutsceneManager != null) {
                    morsa.vincularCamera(camera);
                    morsa.iniciarCutsceneEntrada(CutsceneManager.getDuracaoTotal());
                    cutsceneManager.iniciar("Morsa Gigante, o terror do Ártico");
                    player.setBlockInputs(true);
                    cutsceneBossSolicitada = true;
                }
            }
            case 102 -> {
                setWallState(102, false, player);
                arena.concluida = true;
                player.solicitarCheckpoint();
            }
            case 999 -> {
                iniciarTransicaoDeFase(arena.trigger.destino);
                arena.concluida = true;
            }
            default -> {
                if (arena.totalHordas == 0) {
                    arena.concluida = true;
                }
                setWallState(id, true, player);
            }
        }

        if (!arena.concluida && arena.totalHordas > 0) {
            arena.hordaAtual = 1;
            spawnHorda(arena);
        }
    }

    private void verificarDesativacaoParedes(int id, Player player) {
        switch (id) {
            case 0 -> {
            }
            case 2, 3 -> {
                if (isArenaConcluida(2) && isArenaConcluida(3)) {
                    setWallState(2, false, player);
                    setWallState(3, false, player);

                    // spawna o pesqueiro
                    npcManager.spawn(new PescadorNPC(20.5 * GameCore.tiles_size, 45.7 * GameCore.tiles_size));
                    System.out.printf("Spawnou pesqueiro em: %f, %f\n", 20.5 * GameCore.tiles_size,
                            45.7 * GameCore.tiles_size);
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
                }
            }
            case 14, 15 -> {
                if (isArenaConcluida(14) && isArenaConcluida(15)) {
                    setWallState(14, false, player);
                    setWallState(15, false, player);

                    if (!chave14_15_spawnada) {
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

    public boolean consumirSolicitacaoCutsceneBoss() {
        if (cutsceneBossSolicitada) {
            cutsceneBossSolicitada = false;
            return true;
        }
        return false;
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
