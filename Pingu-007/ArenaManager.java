
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class ArenaManager {

    private LevelManager levelManager;
    private EnemyManager enemyManager;

    public boolean flagArena16Ativada = false;
    public boolean puzzleBotoesConcluido = false;

    public class Arena {

        int id;
        boolean ativa = false;
        boolean concluida = false;
        int hordaAtual = 0;
        int totalHordas = 0;
        ArrayList<TiledObject> spawners = new ArrayList<>();
        ArrayList<Enemy> inimigosVivos = new ArrayList<>();
        TiledObject trigger;
    }

    public class ArenaWall {

        int id_arena;
        TiledObject data;
        boolean ativa = false;
        int[][] tilesFisicosOriginais;
        int[][] tilesVisuaisOriginais;

        public ArenaWall(TiledObject data) {
            this.id_arena = data.id_arena;
            this.data = data;
        }
    }

    public class ArenaButton {

        TiledObject data;
        boolean isPressed = false;
        int timer = 0;
        int[][] tilesVisuaisOriginais;

        public ArenaButton(TiledObject data) {
            this.data = data;
        }
    }

    public class ArenaColision {

        TiledObject data;
        boolean ativa = false;
        int[][] tilesFisicosOriginais;

        public ArenaColision(TiledObject data) {
            this.data = data;
        }
    }

    public class ArenaInteractive {

        TiledObject data;
        int[][] tilesVisuaisOriginais;

        public ArenaInteractive(TiledObject data) {
            this.data = data;
        }
    }

    private ArrayList<Arena> arenas = new ArrayList<>();
    private ArrayList<ArenaWall> walls = new ArrayList<>();
    private ArrayList<ArenaInteractive> interativos = new ArrayList<>();
    private ArrayList<ArenaButton> botoes = new ArrayList<>();
    private ArrayList<ArenaColision> bloqueiosColisao = new ArrayList<>();

    public ArenaManager(EnemyManager enemyManager, LevelManager levelManager) {
        this.enemyManager = enemyManager;
        this.levelManager = levelManager;
    }

    public void carregarObjetos(ArrayList<TiledObject> objetos) {
        arenas.clear();
        walls.clear();
        interativos.clear();
        botoes.clear();
        bloqueiosColisao.clear();
        flagArena16Ativada = false;
        puzzleBotoesConcluido = false;

        for (TiledObject obj : objetos) {
            String tipo = obj.tipo != null ? obj.tipo.toLowerCase().trim() : "";
            String acao = obj.acao != null ? obj.acao.toLowerCase().trim() : "";

            if (acao.equals("pescar")) {
                tipo = "interativo";
            }

            switch (tipo) {
                case "trigger", "arena_trigger", "level_trigger" -> {
                    Arena a = getOuCriarArena(obj.id_arena);
                    a.trigger = obj;
                    a.totalHordas = obj.totalHordas > 0 ? obj.totalHordas : obj.horda;
                }
                case "spawner" -> {
                    if (obj.id_arena >= 0) {
                        Arena a = getOuCriarArena(obj.id_arena);
                        a.spawners.add(obj);
                    }
                }
                case "wall", "door" -> {
                    walls.add(new ArenaWall(obj));
                }
                case "interativo" -> {
                    if (acao.equals("pescar")) {
                        obj.gid = 11; // 10 + 1 do Tiled
                    }
                    ArenaInteractive inter = new ArenaInteractive(obj);
                    interativos.add(inter);
                    setInteractiveState(inter, true);
                }
                case "colision" -> {
                    ArenaColision col = new ArenaColision(obj);
                    bloqueiosColisao.add(col);
                    setColisionState(col, true, null); // Inicia trancada fisicamente
                }
                case "button" -> {
                    ArenaButton btn = new ArenaButton(obj);
                    botoes.add(btn);
                    setButtonState(btn, false); // Inicia não pressionado
                }
            }
        }

        setWallState(3, true, null);
        setWallState(101, true, null);
        setWallState(102, true, null);
    }

    public void update(Player player) {
        atualizarBotoes(player);

        for (Arena a : arenas) {
            if (!a.ativa && !a.concluida && a.trigger != null) {
                if (checkTriggerCollision(a.trigger, player)) {
                    ativarArena(a.id, player);
                }
            }

            if (a.ativa && !a.concluida) {
                a.inimigosVivos.removeIf(Enemy::isDead);
                if (a.inimigosVivos.isEmpty()) {
                    if (a.hordaAtual < a.totalHordas) {
                        a.hordaAtual++;
                        spawnHorda(a);
                    } else {
                        a.concluida = true;
                        verificarDesativacaoParedes(a.id, player);
                    }
                }
            }
        }
    }

    private void setInteractiveState(ArenaInteractive inter, boolean state) {
        if (inter.data.gid <= 0) {
            return;
        }

        int idTileVisual = state ? inter.data.gid : 0;
        int[][] matrizFisica = levelManager.getMapData().getMainLayer();
        int[][] matrizVisual = null;

        for (MapDATA.TileLayer layer : levelManager.getMapData().layers) {
            if (layer.name.equals("bWall")) {
                matrizVisual = layer.data;
                break;
            }
        }
        if (matrizVisual == null) {
            matrizVisual = new int[matrizFisica.length][matrizFisica[0].length];
            levelManager.getMapData().layers.add(new MapDATA.TileLayer("bWall", matrizVisual));
        }

        int startCol = (int) (inter.data.x / GameCore.tiles_size);
        int startRow = (int) (inter.data.y / GameCore.tiles_size);
        int endCol = (int) ((inter.data.x + inter.data.width) / GameCore.tiles_size);
        int endRow = (int) ((inter.data.y + inter.data.height) / GameCore.tiles_size);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < matrizVisual.length && c >= 0 && c < matrizVisual[0].length) {
                    if (state) {
                        if (inter.tilesVisuaisOriginais == null) {
                            inter.tilesVisuaisOriginais = new int[endRow - startRow][endCol - startCol];
                            inter.tilesVisuaisOriginais[r - startRow][c - startCol] = matrizVisual[r][c];
                        }
                        matrizVisual[r][c] = idTileVisual;

                        // Limpa as pedras em cima para o interativo não ficar escondido
                        for (MapDATA.TileLayer layer : levelManager.getMapData().layers) {
                            if (layer.name.equals("bStone") || layer.name.equals("tStone")) {
                                layer.data[r][c] = 0;
                            }
                        }
                    } else {
                        if (inter.tilesVisuaisOriginais != null) {
                            matrizVisual[r][c] = inter.tilesVisuaisOriginais[r - startRow][c - startCol];
                        }
                    }
                }
            }
        }
    }

    private void setButtonState(ArenaButton btn, boolean state) {
        btn.isPressed = state;
        int idTileVisual = state ? 53 : 52;

        int[][] matrizFisica = levelManager.getMapData().getMainLayer();
        int[][] matrizVisual = null;
        for (MapDATA.TileLayer layer : levelManager.getMapData().layers) {
            if (layer.name.equals("bWall")) {
                matrizVisual = layer.data;
                break;
            }
        }
        if (matrizVisual == null) {
            matrizVisual = new int[matrizFisica.length][matrizFisica[0].length];
            levelManager.getMapData().layers.add(new MapDATA.TileLayer("bWall", matrizVisual));
        }

        int startCol = (int) (btn.data.x / GameCore.tiles_size);
        int startRow = (int) (btn.data.y / GameCore.tiles_size);
        int endCol = (int) ((btn.data.x + btn.data.width) / GameCore.tiles_size);
        int endRow = (int) ((btn.data.y + btn.data.height) / GameCore.tiles_size);

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < matrizVisual.length && c >= 0 && c < matrizVisual[0].length) {
                    if (btn.tilesVisuaisOriginais == null) {
                        btn.tilesVisuaisOriginais = new int[endRow - startRow][endCol - startCol];
                        btn.tilesVisuaisOriginais[r - startRow][c - startCol] = matrizVisual[r][c];
                    }
                    matrizVisual[r][c] = idTileVisual;

                    // Limpa as pedras em cima para o botão não ficar escondido
                    for (MapDATA.TileLayer layer : levelManager.getMapData().layers) {
                        if (layer.name.equals("bStone") || layer.name.equals("tStone")) {
                            layer.data[r][c] = 0;
                        }
                    }
                }
            }
        }
    }

    private void setColisionState(ArenaColision col, boolean state, Player player) {
        if (!col.data.colision) {
            return;
        }

        col.ativa = state;
        int idTileParede = 125;
        int[][] matrizFisica = levelManager.getMapData().getMainLayer();

        int startCol = (int) (col.data.x / GameCore.tiles_size);
        int startRow = (int) (col.data.y / GameCore.tiles_size);
        int endCol = (int) ((col.data.x + col.data.width) / GameCore.tiles_size);
        int endRow = (int) ((col.data.y + col.data.height) / GameCore.tiles_size);

        if (state) {
            Rectangle2D.Double wallRect = new Rectangle2D.Double(col.data.x, col.data.y, col.data.width, col.data.height);
            if (player != null) {
                Rectangle2D.Double playerRect = new Rectangle2D.Double(player.getX(), player.getY(), player.getLargura(), player.getAltura());
                if (wallRect.intersects(playerRect)) {
                    double pushX = (player.getX() + player.getLargura() / 2.0) - (col.data.x + col.data.width / 2.0);
                    double pushY = (player.getY() + player.getAltura() / 2.0) - (col.data.y + col.data.height / 2.0);
                    if (Math.abs(pushX) > Math.abs(pushY)) {
                        player.setX(player.getX() + Math.signum(pushX) * GameCore.tiles_size);
                    } else {
                        player.setY(player.getY() + Math.signum(pushY) * GameCore.tiles_size);
                    }
                }
            }
        }

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                if (r >= 0 && r < matrizFisica.length && c >= 0 && c < matrizFisica[0].length) {
                    if (state) {
                        if (col.tilesFisicosOriginais == null) {
                            col.tilesFisicosOriginais = new int[endRow - startRow][endCol - startCol];
                        }
                        col.tilesFisicosOriginais[r - startRow][c - startCol] = matrizFisica[r][c];
                        matrizFisica[r][c] = idTileParede;
                    } else {
                        if (col.tilesFisicosOriginais != null) {
                            matrizFisica[r][c] = col.tilesFisicosOriginais[r - startRow][c - startCol];
                        } else {
                            matrizFisica[r][c] = 0;
                        }
                    }
                }
            }
        }
    }

    public void interagir(Player player, int chavesDoPlayer) {
        for (ArenaInteractive inter : interativos) {
            if (checkTriggerCollision(inter.data, player)) {
                if ("pescar".equalsIgnoreCase(inter.data.acao)) {
                    System.out.println("Iniciando minigame de pesca no tile ID 10!");
                    return;
                }
            }
        }

        for (ArenaColision col : bloqueiosColisao) {
            if (col.ativa && checkTriggerCollision(col.data, player)) {
                if (chavesDoPlayer >= 3) {
                    desativarPuzzleDeColisao(col);
                    return;
                } else {
                    System.out.println("Você precisa de 3 chaves para abrir isso!");
                }
            }
        }
    }

    private void atualizarBotoes(Player player) {
        if (puzzleBotoesConcluido) {
            return;
        }

        boolean todosApertados = true;

        for (ArenaButton btn : botoes) {
            boolean playerEmCima = checkTriggerCollision(btn.data, player);

            if (playerEmCima && !btn.isPressed) {
                setButtonState(btn, true);
                btn.timer = 0;
                System.out.println("Botão " + btn.data.id_button + " pressionado!");
            } else if (!playerEmCima && btn.isPressed) {
                btn.timer++;
                if (btn.timer >= 360) {
                    setButtonState(btn, false);
                    btn.timer = 0;
                    System.out.println("Botão " + btn.data.id_button + " desarmou por falta de peso!");
                }
            }

            if (!btn.isPressed) {
                todosApertados = false;
            }
        }

        if (!botoes.isEmpty() && todosApertados) {
            puzzleBotoesConcluido = true;
            System.out.println("=== PUZZLE DOS BOTÕES CONCLUÍDO! ===");
        }
    }

    private void desativarPuzzleDeColisao(ArenaColision col) {
        setColisionState(col, false, null);
        System.out.println("Colisão destrancada! Mudando os tiles das pedras...");

        MapDATA mapData = levelManager.getMapData();
        for (MapDATA.TileLayer layer : mapData.layers) {
            if (layer.name.equals("bStone") || layer.name.equals("tStone")) {
                for (int r = 0; r < layer.data.length; r++) {
                    for (int c = 0; c < layer.data[0].length; c++) {
                        int t = layer.data[r][c];
                        if (t == 112) {
                            layer.data[r][c] = 116;
                        } else if (t == 113 || t == 114 || t == 99 || t == 100 || t == 85 || t == 86) {
                            layer.data[r][c] = 23;
                        } else if (t == 115) {
                            layer.data[r][c] = 117;
                        } else if (t == 98) {
                            layer.data[r][c] = 102;
                        } else if (t == 84) {
                            layer.data[r][c] = 88;
                        } else if (t == 101) {
                            layer.data[r][c] = 103;
                        } else if (t == 87) {
                            layer.data[r][c] = 89;
                        }
                    }
                }
            }
        }
    }

    private void setWallState(int id_arena, boolean state, Player player) {
        int idTileParede = 125;
        int[][] matrizFisica = levelManager.getMapData().getMainLayer();
        int[][] matrizVisual = null;
        for (MapDATA.TileLayer layer : levelManager.getMapData().layers) {
            if (layer.name.equals("bWall")) {
                matrizVisual = layer.data;
                break;
            }
        }
        if (matrizVisual == null) {
            matrizVisual = new int[matrizFisica.length][matrizFisica[0].length];
            levelManager.getMapData().layers.add(new MapDATA.TileLayer("bWall", matrizVisual));
        }

        for (ArenaWall w : walls) {
            if (w.id_arena == id_arena && w.ativa != state) {
                w.ativa = state;

                if (state) {
                    Rectangle2D.Double wallRect = new Rectangle2D.Double(w.data.x, w.data.y, w.data.width, w.data.height);

                    if (player != null) {
                        Rectangle2D.Double playerRect = new Rectangle2D.Double(player.getX(), player.getY(), player.getLargura(), player.getAltura());
                        if (wallRect.intersects(playerRect)) {
                            double pushX = (player.getX() + player.getLargura() / 2.0) - (w.data.x + w.data.width / 2.0);
                            double pushY = (player.getY() + player.getAltura() / 2.0) - (w.data.y + w.data.height / 2.0);
                            if (Math.abs(pushX) > Math.abs(pushY)) {
                                player.setX(player.getX() + Math.signum(pushX) * GameCore.tiles_size);
                            } else {
                                player.setY(player.getY() + Math.signum(pushY) * GameCore.tiles_size);
                            }
                        }
                    }

                    for (Enemy e : enemyManager.getEnemies()) {
                        Rectangle2D.Double eRect = new Rectangle2D.Double(e.getX(), e.getY(), e.getLargura(), e.getAltura());
                        if (wallRect.intersects(eRect)) {
                            double pushX = (e.getX() + e.getLargura() / 2.0) - (w.data.x + w.data.width / 2.0);
                            double pushY = (e.getY() + e.getAltura() / 2.0) - (w.data.y + w.data.height / 2.0);
                            if (Math.abs(pushX) > Math.abs(pushY)) {
                                e.setX(e.getX() + Math.signum(pushX) * GameCore.tiles_size);
                            } else {
                                e.setY(e.getY() + Math.signum(pushY) * GameCore.tiles_size);
                            }
                        }
                    }
                }

                int startCol = (int) (w.data.x / GameCore.tiles_size);
                int startRow = (int) (w.data.y / GameCore.tiles_size);
                int endCol = (int) ((w.data.x + w.data.width) / GameCore.tiles_size);
                int endRow = (int) ((w.data.y + w.data.height) / GameCore.tiles_size);

                for (int r = startRow; r < endRow; r++) {
                    for (int c = startCol; c < endCol; c++) {
                        if (r >= 0 && r < matrizFisica.length && c >= 0 && c < matrizFisica[0].length) {
                            if (state) {
                                if (w.tilesFisicosOriginais == null) {
                                    w.tilesFisicosOriginais = new int[endRow - startRow][endCol - startCol];
                                    w.tilesVisuaisOriginais = new int[endRow - startRow][endCol - startCol];
                                }
                                w.tilesFisicosOriginais[r - startRow][c - startCol] = matrizFisica[r][c];
                                w.tilesVisuaisOriginais[r - startRow][c - startCol] = matrizVisual[r][c];

                                matrizFisica[r][c] = idTileParede;
                                matrizVisual[r][c] = idTileParede;
                            } else {
                                if (w.tilesFisicosOriginais != null) {
                                    matrizFisica[r][c] = w.tilesFisicosOriginais[r - startRow][c - startCol];
                                    matrizVisual[r][c] = w.tilesVisuaisOriginais[r - startRow][c - startCol];
                                } else {
                                    matrizFisica[r][c] = 0;
                                    matrizVisual[r][c] = 0;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void ativarArena(int id, Player player) {
        Arena a = getOuCriarArena(id);
        a.ativa = true;

        switch (id) {
            case 0 -> {
                setWallState(0, true, player);
                a.concluida = true;
            }
            case 2 ->
                setWallState(2, true, player);
            case 4, 5 ->
                setWallState(4, true, player);
            case 9, 10 -> {
                setWallState(9, true, player);
                setWallState(10, true, player);
            }
            case 14, 15 -> {
                setWallState(14, true, player);
                setWallState(15, true, player);
            }
            case 16 ->
                flagArena16Ativada = true;
            case 102 -> {
                setWallState(102, false, player);
                a.concluida = true;
            }
            case 999 -> {
                iniciarTransicaoDeFase(a.trigger.destino);
                a.concluida = true;
            }
            default -> {
                if (a.totalHordas == 0) {
                    a.concluida = true;
                }
                setWallState(id, true, player);
            }
        }

        if (!a.concluida && a.totalHordas > 0) {
            a.hordaAtual = 1;
            spawnHorda(a);
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
                }
            }
            case 16, 101, 102, 999 -> {
            }
            default ->
                setWallState(id, false, player);
        }
    }

    public void resolverPuzzle101(Player player) {
        setWallState(101, false, player);
        Arena a = getOuCriarArena(101);
        a.concluida = true;
    }

    private void iniciarTransicaoDeFase(String mapaDestino) {
        System.out.println(">>> Carregando mapa: " + mapaDestino + " <<<");
        if (mapaDestino != null && !mapaDestino.isEmpty()) {
            levelManager.carregarNivel(mapaDestino);
        }
    }

    private void spawnHorda(Arena a) {
        for (TiledObject spawner : a.spawners) {
            if (spawner.horda == a.hordaAtual) {
                double spawnY = spawner.y;
                if (spawner.height > 0) {
                    spawnY -= spawner.height;
                } else if (spawner.gid > 0) {
                    spawnY -= GameCore.tiles_size;
                }

                Enemy inimigo = enemyManager.adicionarE_RetornarInimigo(
                        spawner.inimigo, spawner.x, spawnY, spawner.horda, a.id);
                if (inimigo != null) {
                    a.inimigosVivos.add(inimigo);
                }
            }
        }
    }

    private boolean checkTriggerCollision(TiledObject trigger, Player player) {
        if (trigger == null) {
            return false;
        }
        if (trigger.isPolygon) {
            return trigger.getPolygon().contains(player.getX(), player.getY());
        } else {
            Rectangle2D.Double rect = new Rectangle2D.Double(trigger.x, trigger.y, trigger.width, trigger.height);
            return rect.intersects(player.getX(), player.getY(), player.getLargura(), player.getAltura());
        }
    }

    private Arena getOuCriarArena(int id) {
        for (Arena a : arenas) {
            if (a.id == id) {
                return a;
            }
        }
        Arena nova = new Arena();
        nova.id = id;
        arenas.add(nova);
        return nova;
    }

    private boolean isArenaConcluida(int id) {
        for (Arena a : arenas) {
            if (a.id == id) {
                return a.concluida;
            }
        }
        return false;
    }

    public String getHordaInfo() {
        for (Arena a : arenas) {
            if (a.ativa && !a.concluida && a.totalHordas > 0) {
                return "Wave " + a.hordaAtual + " / " + a.totalHordas;
            }
        }
        return "";
    }
}
