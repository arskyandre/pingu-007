
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class LoadSave {

    public static final String LEVEL_ATLAS = "images/tile_set.png";
    public static final String LEVEL_1_DATA = "LEVEL_1_DATA.json";
    public static final String LEVEL_2_DATA = "LEVEL_2_DATA.tmj";

    public static BufferedImage GetSpriteAtlas(String filename) {
        try (InputStream is
                = LoadSave.class.getResourceAsStream("/" + filename)) {
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

            int dataIdx = json.indexOf("\"data\":[");
            while (dataIdx != -1) {
                int layerStart = json.lastIndexOf("{", dataIdx);
                int nameIdx = json.indexOf("\"name\":\"", layerStart);
                String name = "unknown";

                if (nameIdx != -1 && nameIdx < dataIdx) {
                    nameIdx += 8;
                    name = json.substring(nameIdx, json.indexOf("\"", nameIdx));
                } else {
                    int endData = json.indexOf("]", dataIdx);
                    nameIdx = json.indexOf("\"name\":\"", endData);
                    if (nameIdx != -1) {
                        nameIdx += 8;
                        name = json.substring(nameIdx, json.indexOf("\"", nameIdx));
                    }
                }

                int endIdx = json.indexOf("]", dataIdx);
                if (endIdx != -1) {
                    String dataString = json.substring(dataIdx + 8, endIdx);
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
                dataIdx = json.indexOf("\"data\":[", dataIdx + 8);
            }

            String[] rawObjects = json.split("\"id\":");
            for (int i = 1; i < rawObjects.length; i++) {
                String objStr = rawObjects[i];
                if (objStr.contains("\"x\"") && objStr.contains("\"y\"")) {
                    TiledObject tObj = new TiledObject();

                    String processStr = objStr;

                    int polyIdx = processStr.indexOf("\"polygon\":[");
                    if (polyIdx == -1) {
                        polyIdx = processStr.indexOf("\"polyline\":[");
                    }

                    if (polyIdx != -1) {
                        tObj.isPolygon = true;
                        int polyEnd = processStr.indexOf("]", polyIdx);
                        String polyStr = processStr.substring(polyIdx, polyEnd + 1);
                        String[] points = polyStr.split("\\}");

                        ArrayList<Double> xs = new ArrayList<>();
                        ArrayList<Double> ys = new ArrayList<>();
                        for (String pt : points) {
                            if (pt.contains("\"x\"")) {
                                xs.add(extractDouble(pt, "\"x\""));
                                ys.add(extractDouble(pt, "\"y\""));
                            }
                        }
                        tObj.polygonXs = new double[xs.size()];
                        tObj.polygonYs = new double[ys.size()];
                        for (int j = 0; j < xs.size(); j++) {
                            tObj.polygonXs[j] = xs.get(j);
                            tObj.polygonYs[j] = ys.get(j);
                        }

                        processStr = processStr.replace(polyStr, "");
                    }

                    tObj.x = extractDouble(processStr, "\"x\"");
                    tObj.y = extractDouble(processStr, "\"y\"");
                    tObj.width = extractDouble(processStr, "\"width\"");
                    tObj.height = extractDouble(processStr, "\"height\"");

                    if (processStr.contains("\"gid\"")) {
                        tObj.gid = (int) extractDouble(processStr, "\"gid\"");
                        tObj.y -= tObj.height;
                    }

                    tObj.tipo = extractStringProp(processStr, "type");
                    if (tObj.tipo.isEmpty()) {
                        tObj.tipo = extractRootString(processStr, "type");
                    }
                    if (tObj.tipo.isEmpty()) {
                        tObj.tipo = extractRootString(processStr, "class");
                    }
                    tObj.acao = extractStringProp(processStr, "acao");
                    tObj.inimigo = extractStringProp(processStr, "enemy");
                    tObj.id_arena = extractIntProp(processStr, "id_arena", -1);
                    tObj.horda = extractIntProp(processStr, "horda", 1);
                    tObj.ativa = extractBoolProp(processStr, "isActive", false);
                    tObj.totalHordas = extractIntProp(processStr, "totalHordas", 1);
                    tObj.destino = extractStringProp(processStr, "destino");

                    // Extração dos novos interativos
                    tObj.colision = extractBoolProp(processStr, "colision", true);
                    //tObj.key = extractIntProp(processStr, "key", 0);
                    tObj.id_button = extractIntProp(processStr, "id_button", -1);
                    tObj.isToggle = extractBoolProp(processStr, "isToggle", false);

                    if (processStr.contains("\"point\":true")) {
                        tObj.isPoint = true;
                        if (tObj.tipo.isEmpty()) {
                            tObj.tipo = "spawner";
                        }
                    }

                    objects.add(tObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MapDATA(layers, objects);
    }

    private static double extractDouble(String text, String key) {
        int idx = text.indexOf(key);
        if (idx == -1) {
            return 0;
        }
        idx = text.indexOf(":", idx) + 1;
        while (idx < text.length() && Character.isWhitespace(text.charAt(idx))) {
            idx++;
        }
        int end = idx;
        while (end < text.length()
                && (Character.isDigit(text.charAt(end))
                || text.charAt(end) == '-'
                || text.charAt(end) == '.')) {
            end++;
        }
        try {
            return Double.parseDouble(text.substring(idx, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String extractRootString(String text, String key) {
        int idx = text.indexOf("\"" + key + "\":\"");
        if (idx == -1) {
            return "";
        }
        int start = idx + key.length() + 4;
        int end = text.indexOf("\"", start);
        if (end == -1) {
            return "";
        }
        return text.substring(start, end);
    }

    private static String extractStringProp(String text, String propName) {
        int idx = text.indexOf("\"name\":\"" + propName + "\"");
        if (idx == -1) {
            return "";
        }
        int valIdx = text.indexOf("\"value\"", idx);
        if (valIdx == -1) {
            return "";
        }
        valIdx = text.indexOf("\"", text.indexOf(":", valIdx)) + 1;
        int end = text.indexOf("\"", valIdx);
        return text.substring(valIdx, end);
    }

    private static int extractIntProp(String text, String propName, int defaultVal) {
        int idx = text.indexOf("\"name\":\"" + propName + "\"");
        if (idx == -1) {
            return defaultVal;
        }
        int valIdx = text.indexOf("\"value\"", idx);
        if (valIdx == -1) {
            return defaultVal;
        }
        valIdx = text.indexOf(":", valIdx) + 1;
        while (valIdx < text.length() && Character.isWhitespace(text.charAt(valIdx))) {
            valIdx++;
        }
        int end = valIdx;
        while (end < text.length()
                && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(text.substring(valIdx, end));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean extractBoolProp(String text, String propName, boolean defaultVal) {
        int idx = text.indexOf("\"name\":\"" + propName + "\"");
        if (idx == -1) {
            return defaultVal;
        }
        int valIdx = text.indexOf("\"value\"", idx);
        if (valIdx == -1) {
            return defaultVal;
        }
        valIdx = text.indexOf(":", valIdx) + 1;
        while (valIdx < text.length() && Character.isWhitespace(text.charAt(valIdx))) {
            valIdx++;
        }
        return text.startsWith("true", valIdx);
    }
}
