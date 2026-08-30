
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class LoadSave {

    public static final long FLIPPED_HORIZONTALLY_FLAG = 0x80000000L;
    public static final long FLIPPED_VERTICALLY_FLAG = 0x40000000L;
    public static final long FLIPPED_DIAGONALLY_FLAG = 0x20000000L;

    public double x, y, width, height;
    public int gid = 0;

    public BufferedImage sprite;
    public Shape hitbox;

    public static final String LEVEL_ATLAS = "images/tile_set.png";
    public static final String LEVEL_1_DATA = "LEVEL_1_DATA_converted.json";
    public static final String LEVEL_2_DATA = "LEVEL_2_DATA.tmj";
    public static final String LEVEL_YSORT = "teste_ysort.tmj";
    public static final String CASA_VENDEDOR = "CASA_VENDEDOR.tmj";

    public static ArrayList<TilesetData> currentTilesets = new ArrayList<>();
    private static java.util.HashMap<Integer, java.util.HashMap<String, String>> tsxCache = new java.util.HashMap<>();

    public static BufferedImage GetSpriteAtlas(String filename) {
        try (InputStream is = LoadSave.class.getResourceAsStream("/" + filename)) {
            if (is == null) {
                throw new RuntimeException("Arquivo não encontrado: " + filename);
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar imagem: " + filename, e);
        }
    }

    public static MapDATA GetMapData(String filename) {
        ArrayList<MapDATA.TileLayer> layers = new ArrayList<>();
        ArrayList<TiledObject> objects = new ArrayList<>();

        InputStream is = LoadSave.class.getResourceAsStream("/" + filename);
        if (is == null) {
            throw new RuntimeException("Arquivo não encontrado: " + filename);
        }

        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            String json = scanner.useDelimiter("\\A").next();

            int width = (int) extractDouble(json, "\"width\"");
            int height = (int) extractDouble(json, "\"height\"");
            if (width <= 0) {
                width = GameCore.tiles_in_width;
            }
            if (height <= 0) {
                height = GameCore.tiles_in_height;
            }

            currentTilesets.clear();
            int tsIdx = findJsonArrayStart(json, "tilesets", 0);
            if (tsIdx != -1) {
                int tsEnd = json.indexOf("],", tsIdx);
                if (tsEnd == -1) {
                    tsEnd = json.indexOf("]", tsIdx);
                }
                String tsString = json.substring(tsIdx, tsEnd);

                String[] tsBlocks = tsString.split("\\{");
                for (int i = 1; i < tsBlocks.length; i++) {
                    int firstGid = (int) extractDouble(tsBlocks[i], "\"firstgid\"");
                    String sourceTSX = extractRootString(tsBlocks[i], "source");

                    if (!sourceTSX.isEmpty()) {
                        currentTilesets.add(loadTSX(sourceTSX, firstGid));
                    } else {
                        TilesetData embeddedData = new TilesetData();
                        embeddedData.firstGid = firstGid;

                        double tw = extractDouble(tsBlocks[i], "\"tilewidth\"");
                        if (tw > 0) {
                            embeddedData.tileWidth = (int) tw;
                        }

                        double th = extractDouble(tsBlocks[i], "\"tileheight\"");
                        if (th > 0) {
                            embeddedData.tileHeight = (int) th;
                        }

                        double cols = extractDouble(tsBlocks[i], "\"columns\"");
                        if (cols > 0) {
                            embeddedData.columns = (int) cols;
                        }

                        String imgPath = extractRootString(tsBlocks[i], "image");
                        if (imgPath.isEmpty()) {
                            imgPath = extractStringProp(tsBlocks[i], "image");
                        }
                        if (!imgPath.isEmpty()) {
                            if (imgPath.contains("/")) {
                                imgPath = imgPath.substring(imgPath.lastIndexOf("/") + 1);
                            }
                            embeddedData.texture = GetSpriteAtlas("images/" + imgPath);
                        }
                        currentTilesets.add(embeddedData);
                    }
                }
            }

            int dataIdx = findJsonArrayStart(json, "data", 0);
            while (dataIdx != -1) {
                int layerStart = json.lastIndexOf("{", dataIdx);
                String name = extractRootString(json.substring(layerStart, dataIdx), "name");

                int endIdx = json.indexOf("]", dataIdx);
                if (endIdx != -1) {
                    if (name.isEmpty()) {
                        int layerEnd = json.indexOf("}", endIdx);
                        if (layerEnd != -1) {
                            name = extractRootString(json.substring(endIdx + 1, layerEnd), "name");
                        }
                    }
                    if (name.isEmpty()) {
                        name = "unknown";
                    }
                    String dataString = json.substring(dataIdx + 1, endIdx);
                    String[] stringValues = dataString.split(",");
                    int[][] lvlData = new int[height][width];
                    int index = 0;
                    for (int j = 0; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            if (index < stringValues.length) {
                                lvlData[j][i] = Integer.parseInt(stringValues[index].trim());
                                index++;
                            }
                        }
                    }
                    layers.add(new MapDATA.TileLayer(name, lvlData));
                }
                dataIdx = findJsonArrayStart(json, "data", dataIdx + 1);
            }

            int objectsIdx = json.indexOf("\"objects\"");
            int countEsperado = 0;
            int countCarregado = 0;
            ArrayList<String> falhas = new ArrayList<>();

            while (objectsIdx != -1) {
                int arrayStart = json.indexOf("[", objectsIdx);
                if (arrayStart == -1) {
                    break;
                }

                int arrayEnd = findClosingBracket(json, arrayStart, '[', ']');
                if (arrayEnd == -1) {
                    break;
                }

                String objectsArrayStr = json.substring(arrayStart, arrayEnd + 1);
                ArrayList<String> objStrings = extractJsonObjects(objectsArrayStr);
                countEsperado += objStrings.size();

                for (int idxObj = 0; idxObj < objStrings.size(); idxObj++) {
                    String objStr = objStrings.get(idxObj);
                    try {
                        TiledObject tObj = parseObjeto(objStr);
                        objects.add(tObj);
                        countCarregado++;
                    } catch (Exception e) {
                        String idHint = "desconhecido";
                        try {
                            int rawId = (int) extractDouble(objStr, "\"id\"");
                            if (rawId > 0) {
                                idHint = String.valueOf(rawId);
                            }
                        } catch (Exception ignored) {
                        }
                        String msg = "objeto #" + (idxObj + 1) + " do array (id Tiled=" + idHint + "): "
                                + e.getClass().getSimpleName() + " - " + e.getMessage();
                        falhas.add(msg);
                    }
                }
                objectsIdx = json.indexOf("\"objects\"", arrayEnd);
            }

            if (!falhas.isEmpty()) {
                System.err.println("[MAP LOAD] " + filename + ": " + countCarregado + "/" + countEsperado
                        + " objetos carregados. Falhas:");
                for (String f : falhas) {
                    System.err.println("  - " + f);
                }
            }

            if (countEsperado > 0 && countCarregado < countEsperado * 0.5) {
                throw new RuntimeException("Falha crítica ao carregar '" + filename + "': apenas "
                        + countCarregado + "/" + countEsperado + " objetos válidos. Abortando o load do mapa.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Falha ao carregar mapa: " + filename, e);
        }
        return new MapDATA(layers, objects);
    }

    private static TiledObject parseObjeto(String objStr) {
        TiledObject tObj = new TiledObject();

        String baseStr = objStr;
        Map<String, String> props = new HashMap<>();

        int propsIdx = objStr.indexOf("\"properties\"");
        if (propsIdx != -1) {
            int propsArrayStart = objStr.indexOf("[", propsIdx);
            if (propsArrayStart != -1) {
                int propsEnd = findClosingBracket(objStr, propsArrayStart, '[', ']');
                if (propsEnd != -1) {
                    baseStr = objStr.substring(0, propsIdx) + objStr.substring(propsEnd + 1);
                    props = parsePropertiesMap(objStr.substring(propsArrayStart, propsEnd + 1));
                }
            }
        }

        long rawGid = (long) extractDouble(baseStr, "\"gid\"");
        if (rawGid > 0) {
            tObj.flipH = (rawGid & FLIPPED_HORIZONTALLY_FLAG) != 0;
            tObj.flipV = (rawGid & FLIPPED_VERTICALLY_FLAG) != 0;
            tObj.flipDiagonal = (rawGid & FLIPPED_DIAGONALLY_FLAG) != 0;
            tObj.gid = (int) (rawGid
                    & ~(FLIPPED_HORIZONTALLY_FLAG | FLIPPED_VERTICALLY_FLAG | FLIPPED_DIAGONALLY_FLAG));

            if (tsxCache.containsKey(tObj.gid)) {
                java.util.HashMap<String, String> herdadas = tsxCache.get(tObj.gid);
                for (Map.Entry<String, String> entry : herdadas.entrySet()) {
                    props.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }

        tObj.acao = props.getOrDefault("acao", "");
        tObj.tipo = props.getOrDefault("type", props.getOrDefault("class", ""));
        tObj.inimigo = props.getOrDefault("enemy", "");
        tObj.id_arena = parseIntOr(props.get("id_arena"), -1);
        tObj.horda = parseIntOr(props.get("horda"), 1);
        tObj.ativa = parseBoolOr(props.get("isactive"), false);
        tObj.totalHordas = parseIntOr(props.get("totalhordas"), 1);
        tObj.destino = props.getOrDefault("destino", "");
        tObj.npc_nome = props.getOrDefault("npc_nome", "");

        tObj.collision = parseBoolOr(props.get("colisao"), tObj.tipo.equals("colision"));
        tObj.solidoPorPadrao = parseBoolOr(props.get("solido"), true);
        tObj.isTransparent = parseBoolOr(props.get("istransparent"), false);
        tObj.isInteractive = parseBoolOr(props.get("isinteractive"), false);
        tObj.castsShadow = parseBoolOr(props.get("castsshadow"), false);
        tObj.isToggle = parseBoolOr(props.get("istoggle"), false);
        tObj.id_button = parseIntOr(props.get("id_button"), -1);

        int polyIdx = baseStr.indexOf("\"polygon\"");
        boolean isPolyline = false;
        if (polyIdx == -1) {
            polyIdx = baseStr.indexOf("\"polyline\"");
            isPolyline = polyIdx != -1;
        }
        if (polyIdx != -1) {
            int polyArrayStart = baseStr.indexOf("[", polyIdx);
            if (polyArrayStart != -1) {
                int polyEnd = findClosingBracket(baseStr, polyArrayStart, '[', ']');
                if (polyEnd != -1) {
                    tObj.isPolygon = true;
                    String polyStr = baseStr.substring(polyArrayStart, polyEnd + 1);

                    ArrayList<String> pointsStr = extractJsonObjects(polyStr);
                    tObj.polygonXs = new double[pointsStr.size()];
                    tObj.polygonYs = new double[pointsStr.size()];
                    for (int i = 0; i < pointsStr.size(); i++) {
                        String pt = pointsStr.get(i);
                        tObj.polygonXs[i] = extractDouble(pt, "\"x\"");
                        tObj.polygonYs[i] = extractDouble(pt, "\"y\"");
                    }

                    int keyStart = isPolyline ? baseStr.indexOf("\"polyline\"") : baseStr.indexOf("\"polygon\"");
                    baseStr = baseStr.substring(0, keyStart) + baseStr.substring(polyEnd + 1);
                }
            }
        }

        tObj.id = (int) extractDouble(baseStr, "\"id\"");
        tObj.x = extractDouble(baseStr, "\"x\"");
        tObj.y = extractDouble(baseStr, "\"y\"");
        tObj.width = extractDouble(baseStr, "\"width\"");
        tObj.height = extractDouble(baseStr, "\"height\"");
        tObj.rotation = extractDouble(baseStr, "\"rotation\"");

        if (tObj.tipo.isEmpty()) {
            tObj.tipo = extractRootString(baseStr, "type");
        }
        if (tObj.tipo.isEmpty()) {
            tObj.tipo = extractRootString(baseStr, "class");
        }

        if (baseStr.contains("\"point\":true") || baseStr.contains("\"point\": true")) {
            tObj.isPoint = true;
            if (tObj.tipo.isEmpty()) {
                tObj.tipo = "spawner";
            }
        }

        if (rawGid > 0) {
            tObj.y -= tObj.height;
            if (tObj.tipo.isEmpty()) {
                tObj.tipo = "map_object";
            }
        }

        if (tObj.gid > 0 && !tObj.isPolygon) {
            applyGidData(tObj);
        }

        return tObj;
    }

    private static Map<String, String> parsePropertiesMap(String propsArrayStr) {
        Map<String, String> map = new HashMap<>();
        for (String p : extractJsonObjects(propsArrayStr)) {
            String name = extractRootString(p, "name");
            if (name.isEmpty()) {
                continue;
            }
            String rawValue = extractRawValue(p, "value");
            map.put(name.toLowerCase(), rawValue);
        }
        return map;
    }

    private static String extractRawValue(String text, String key) {
        int idx = text.indexOf("\"" + key + "\"");
        if (idx == -1) {
            return "";
        }
        int colonIdx = text.indexOf(":", idx);
        if (colonIdx == -1) {
            return "";
        }
        int start = colonIdx + 1;
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        if (start < text.length() && text.charAt(start) == '"') {
            int quote2 = text.indexOf("\"", start + 1);
            return quote2 == -1 ? "" : text.substring(start + 1, quote2);
        }
        int end = start;
        while (end < text.length() && text.charAt(end) != ',' && text.charAt(end) != '}') {
            end++;
        }
        return text.substring(start, end).trim();
    }

    private static int findJsonArrayStart(String text, String key, int fromIndex) {
        String quotedKey = "\"" + key + "\"";
        int searchFrom = Math.max(0, fromIndex);
        while (searchFrom < text.length()) {
            int keyIndex = text.indexOf(quotedKey, searchFrom);
            if (keyIndex == -1) {
                return -1;
            }

            int cursor = keyIndex + quotedKey.length();
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
            if (cursor >= text.length() || text.charAt(cursor) != ':') {
                searchFrom = cursor;
                continue;
            }
            cursor++;
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
            if (cursor < text.length() && text.charAt(cursor) == '[') {
                return cursor;
            }
            searchFrom = cursor;
        }
        return -1;
    }

    private static int parseIntOr(String raw, int defaultVal) {
        if (raw == null || raw.isEmpty()) {
            return defaultVal;
        }
        try {
            return (int) Double.parseDouble(raw);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean parseBoolOr(String raw, boolean defaultVal) {
        if (raw == null || raw.isEmpty()) {
            return defaultVal;
        }
        return raw.equalsIgnoreCase("true");
    }

    public static void applyGidData(TiledObject tObj) {
        if (tObj.gid <= 0) {
            return;
        }

        TilesetData tsAtual = tilesetParaGid(tObj.gid);
        if (tsAtual == null) {
            return;
        }

        int localId = tObj.gid - tsAtual.firstGid;

        if (tsAtual.tileHeight > 0 && tsAtual.tileWidth > 0) {
            tObj.height = tsAtual.tileHeight;
            tObj.width = tsAtual.tileWidth;
        }

        if (tsAtual.texture != null) {
            tObj.sprite = tsAtual.sprites.get(localId);
            if (tObj.sprite == null) {
                int col = localId % tsAtual.columns;
                int row = localId / tsAtual.columns;
                int sx = col * tsAtual.tileWidth;
                int sy = row * tsAtual.tileHeight;

                if (sx + tsAtual.tileWidth <= tsAtual.texture.getWidth()
                        && sy + tsAtual.tileHeight <= tsAtual.texture.getHeight()) {
                    tObj.sprite = tsAtual.texture.getSubimage(sx, sy,
                            tsAtual.tileWidth, tsAtual.tileHeight);
                } else {
                    tObj.sprite = tsAtual.texture;
                }
                tsAtual.sprites.put(localId, tObj.sprite);
            }
        } else {
            tObj.sprite = null;
        }

        tObj.castsShadow = tsAtual.castsShadows.getOrDefault(localId, false);
        tObj.hitbox = recalcularHitboxDeGid(tObj);
        tObj.collision = tObj.hitbox != null;
    }

    public static Shape recalcularHitboxDeGid(TiledObject tObj) {
        if (tObj.gid <= 0) {
            return null;
        }
        TilesetData tsAtual = tilesetParaGid(tObj.gid);
        if (tsAtual == null) {
            return null;
        }
        int localId = tObj.gid - tsAtual.firstGid;
        Shape localShape = tsAtual.collisions.get(localId);
        if (localShape == null) {
            return null;
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(tObj.x, tObj.y);
        if (tObj.flipH) {
            transform.translate(tObj.width, 0);
            transform.scale(-1, 1);
        }
        if (tObj.flipV) {
            transform.translate(0, tObj.height);
            transform.scale(1, -1);
        }
        return transform.createTransformedShape(localShape);
    }

    private static TilesetData tilesetParaGid(int gid) {
        for (int j = currentTilesets.size() - 1; j >= 0; j--) {
            if (gid >= currentTilesets.get(j).firstGid) {
                return currentTilesets.get(j);
            }
        }
        return null;
    }

    private static double extractDouble(String text, String key) {
        int idx = text.indexOf(key);
        if (idx == -1) {
            return 0;
        }

        idx += key.length();
        while (idx < text.length() && (Character.isWhitespace(text.charAt(idx)) || text.charAt(idx) == ':')) {
            idx++;
        }
        int end = idx;
        while (end < text.length()
                && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-' || text.charAt(end) == '.'
                || text.charAt(end) == 'e' || text.charAt(end) == 'E' || text.charAt(end) == '+')) {
            end++;
        }
        try {
            return Double.parseDouble(text.substring(idx, end));
        } catch (Exception e) {
            return 0;
        }
    }

    public static int findClosingBracket(String text, int openPos, char openChar, char closeChar) {
        int depth = 0;
        boolean inQuotes = false;
        for (int i = openPos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == openChar) {
                    depth++;
                } else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static ArrayList<String> extractJsonObjects(String jsonArray) {
        ArrayList<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inQuotes = false;

        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '"' && (i == 0 || jsonArray.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        objects.add(jsonArray.substring(start, i + 1));
                    }
                }
            }
        }
        return objects;
    }

    private static String extractRootString(String text, String key) {
        int idx = text.indexOf("\"" + key + "\"");
        if (idx == -1) {
            return "";
        }
        int colonIdx = text.indexOf(":", idx);
        if (colonIdx == -1) {
            return "";
        }
        int quote1 = text.indexOf("\"", colonIdx);
        if (quote1 == -1) {
            return "";
        }
        int quote2 = text.indexOf("\"", quote1 + 1);
        if (quote2 == -1) {
            return "";
        }
        return text.substring(quote1 + 1, quote2);
    }

    private static String extractStringProp(String text, String propName) {
        ArrayList<String> props = extractJsonObjects(text);
        String lowerProp = "\"" + propName.toLowerCase() + "\"";

        for (String p : props) {
            if (p.toLowerCase().contains(lowerProp)) {
                return extractRootString(p, "value");
            }
        }
        return "";
    }

    public static class TilesetData {

        public int firstGid;
        public BufferedImage texture;
        public int tileWidth = 16;
        public int tileHeight = 16;
        public int columns = 1;
        public Map<Integer, Shape> collisions = new HashMap<>();
        public Map<Integer, Boolean> castsShadows = new HashMap<>();
        public Map<Integer, BufferedImage> sprites = new HashMap<>();
    }

    private static TilesetData loadTSX(String tsxName, int firstGid) {
        TilesetData data = new TilesetData();
        data.firstGid = firstGid;

        if (tsxName.contains("/")) {
            tsxName = tsxName.substring(tsxName.lastIndexOf("/") + 1);
        }

        InputStream is = LoadSave.class.getResourceAsStream("/images/" + tsxName);
        if (is == null) {
            System.out.println("ERRO: TSX nao encontrado no classpath em /images/" + tsxName
                    + " -- confira se o arquivo esta em /images/ e foi incluido no build.");
            return data;
        }

        try (Scanner scanner = new Scanner(is, "UTF-8")) {

            String xml = scanner.useDelimiter("\\A").next();
            carregarPropriedadesTSX(xml, firstGid);

            double tw = extractXMLDouble(xml, "tilewidth");
            if (tw > 0) {
                data.tileWidth = (int) tw;
            }
            double th = extractXMLDouble(xml, "tileheight");
            if (th > 0) {
                data.tileHeight = (int) th;
            }
            double cols = extractXMLDouble(xml, "columns");
            if (cols > 0) {
                data.columns = (int) cols;
            }

            int imgIdx = xml.indexOf("<image source=\"");
            if (imgIdx != -1) {
                int start = imgIdx + 15;
                int end = xml.indexOf("\"", start);
                String imgName = xml.substring(start, end);

                if (imgName.contains("/")) {
                    imgName = imgName.substring(imgName.lastIndexOf("/") + 1);
                }
                data.texture = GetSpriteAtlas("images/" + imgName);
            }

            String[] tiles = xml.split("<tile id=\"");
            for (int i = 1; i < tiles.length; i++) {
                String tileBlock = tiles[i];
                int idEnd = tileBlock.indexOf("\"");
                int localId = Integer.parseInt(tileBlock.substring(0, idEnd));

                int castsShadowIdx = tileBlock.indexOf("<property name=\"castsShadow\"");
                if (castsShadowIdx != -1) {
                    int valueIdx = tileBlock.indexOf("value=\"", castsShadowIdx);
                    if (valueIdx != -1) {
                        int valueStart = valueIdx + 7;
                        int valueEnd = tileBlock.indexOf("\"", valueStart);
                        if (valueEnd != -1) {
                            data.castsShadows.put(localId,
                                    Boolean.parseBoolean(tileBlock.substring(valueStart, valueEnd)));
                        }
                    }
                }

                Area areaDoTile = new Area();
                boolean temColisao = false;

                int objIdx = tileBlock.indexOf("<object ");
                while (objIdx != -1) {
                    int bracketIdx = tileBlock.indexOf(">", objIdx);
                    if (bracketIdx == -1) {
                        break;
                    }
                    String objTag;
                    int indexParaProximaBusca;

                    if (tileBlock.charAt(bracketIdx - 1) == '/') {
                        indexParaProximaBusca = bracketIdx + 1;
                        objTag = tileBlock.substring(objIdx, indexParaProximaBusca);
                    } else {
                        int endObjIdx = tileBlock.indexOf("</object>", bracketIdx);
                        if (endObjIdx == -1) {
                            break;
                        }
                        indexParaProximaBusca = endObjIdx + 9;
                        objTag = tileBlock.substring(objIdx, indexParaProximaBusca);
                    }

                    double cx = extractXMLDouble(objTag, "x");
                    double cy = extractXMLDouble(objTag, "y");
                    double cw = extractXMLDouble(objTag, "width");
                    double ch = extractXMLDouble(objTag, "height");

                    Shape shape;
                    int polyIdx2 = objTag.indexOf("<polygon points=\"");
                    if (polyIdx2 != -1) {
                        int startPts = polyIdx2 + 17;
                        int endPts = objTag.indexOf("\"", startPts);
                        String[] points = objTag.substring(startPts, endPts).split(" ");

                        Path2D.Double path = new Path2D.Double();
                        boolean first = true;
                        for (String pt : points) {
                            if (pt.trim().isEmpty()) {
                                continue;
                            }
                            String[] coords = pt.split(",");
                            double px = Double.parseDouble(coords[0]);
                            double py = Double.parseDouble(coords[1]);
                            if (first) {
                                path.moveTo(cx + px, cy + py);
                                first = false;
                            } else {
                                path.lineTo(cx + px, cy + py);
                            }
                        }
                        path.closePath();
                        shape = path;
                    } else if (objTag.contains("<ellipse")) {
                        shape = new Ellipse2D.Double(cx, cy, cw, ch);
                    } else {
                        shape = new Rectangle2D.Double(cx, cy, cw, ch);
                    }
                    areaDoTile.add(new Area(shape));
                    temColisao = true;

                    objIdx = tileBlock.indexOf("<object ", indexParaProximaBusca);
                }
                if (temColisao) {
                    data.collisions.put(localId, areaDoTile);
                }
            }
        } catch (Exception e) {
            System.out.println("ERRO: Falha ao ler/processar o TSX externo: " + tsxName
                    + " -- " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }

    private static double extractXMLDouble(String text, String key) {
        int idx = text.indexOf(key + "=\"");
        if (idx == -1) {
            return 0;
        }
        int start = idx + key.length() + 2;
        int end = text.indexOf("\"", start);
        try {
            return Double.parseDouble(text.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    public static void carregarPropriedadesTSX(String tsxContent, int firstGid) {
        String[] tiles = tsxContent.split("<tile id=\"");

        for (int i = 1; i < tiles.length; i++) {
            String tileBlock = tiles[i];

            int quoteIdx = tileBlock.indexOf("\"");
            int localId = Integer.parseInt(tileBlock.substring(0, quoteIdx));
            int globalGid = firstGid + localId;

            java.util.HashMap<String, String> props = new java.util.HashMap<>();

            String[] linhas = tileBlock.split("<property ");
            for (int j = 1; j < linhas.length; j++) {
                String linha = linhas[j];
                String name = extrairAtributoXML(linha, "name");
                String value = extrairAtributoXML(linha, "value");

                if (!name.isEmpty() && !value.isEmpty()) {
                    props.put(name.toLowerCase(), value);
                }
            }

            if (!props.isEmpty()) {
                tsxCache.put(globalGid, props);
            }
        }
    }

    private static String extrairAtributoXML(String linha, String atributo) {
        String busca = atributo + "=\"";
        int start = linha.indexOf(busca);
        if (start == -1) {
            return "";
        }
        start += busca.length();
        int end = linha.indexOf("\"", start);
        if (end == -1) {
            return "";
        }
        return linha.substring(start, end);
    }
}
