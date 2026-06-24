
import java.util.ArrayList;

public class ArenaManager {

    private final LevelManager levelManager;
    private final EnemyManager enemyManager;
    private final ItemManager itemManager;
    private final ArenaContext context;

    //public boolean flagArena16Ativada = false;
    public boolean puzzleBotoesConcluido = false;

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

    public ArenaManager(EnemyManager enemyManager, LevelManager levelManager, ItemManager itemManager) {
        this.enemyManager = enemyManager;
        this.levelManager = levelManager;
        this.itemManager = itemManager;
        this.context = new ArenaContext(levelManager, enemyManager);
    }

    public void carregarObjetos(ArrayList<TiledObject> objetos) {
        arenas.clear();
        doors.clear();
        interactives.clear();
        buttons.clear();
        collisionBlocks.clear();
        allObjects.clear();
        //flagArena16Ativada = false;
        puzzleBotoesConcluido = false;

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

    public void update(Player player) {
        atualizarBotoes(player);
        for (int i = 0; i < arenas.size(); i++) {
            Arena arena = arenas.get(i);
            if (!arena.ativa && !arena.concluida && arena.trigger != null) {
                if (ArenaTriggers.collides(arena.trigger, player)) {
                    ativarArena(arena.id, player);
                    if (arenas.isEmpty()) {
                        return;
                    }
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
        //TODO: FAZER PUZZLE DOS BOTÕES
        if (puzzleBotoesConcluido) {
            return;
        }

        boolean todosApertados = true;

        for (PressureButton button : buttons) {
            button.update(context, player);
            if (!button.isPressed()) {
                todosApertados = false;
            }
        }

        if (!buttons.isEmpty() && todosApertados) {
            puzzleBotoesConcluido = true;
            System.out.println("=== PUZZLE DOS BOTÕES CONCLUÍDO! ===");
            resolverPuzzle101(player);
        }
    }

    private void setWallState(int idArena, boolean closed, Player player) {
        for (DoorObject door : doors) {
            if (door.getArenaId() == idArena) {
                door.setClosed(context, closed, player);
            }
        }
    }

    private void ativarArena(int id, Player player) {
        Arena arena = getOuCriarArena(id);
        arena.ativa = true;
        switch (id) {
            case 0 -> {
                setWallState(0, true, player);
                arena.concluida = true;
            }
            case 2 -> {
                setWallState(2, true, player);
            }
            case 4, 5 -> {
                setWallState(4, true, player);
            }
            case 9, 10 -> {
                setWallState(9, true, player);
                setWallState(10, true, player);
            }
            case 14, 15 -> {
                setWallState(14, true, player);
                setWallState(15, true, player);
            }
            /*case 16 ->
                flagArena16Ativada = true;*/
            case 102 -> {
                setWallState(102, false, player);
                arena.concluida = true;
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
                    itemManager.spawn(new KeyItem(12839, 4870));
                }
            }
            case 16, 101, 102, 999 -> {
            }
            default ->
                setWallState(id, false, player);
        }
    }

    public void resolverPuzzle101(Player player) {
        itemManager.spawn(new KeyItem(5029, 4200));
        setWallState(101, false, player);
        Arena arena = getOuCriarArena(101);
        arena.concluida = true;
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
                    /*if (arena.id == 16 || arena.id == 2) {
                        inimigo.podePularBuracos = false;
                    }*/
                    arena.inimigosVivos.add(inimigo);
                }
            }
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
}
