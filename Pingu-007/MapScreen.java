import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class MapScreen {

    private BufferedImage miniatura;
    private double larguraMundo;
    private double alturaMundo;

    public static boolean apertouMapa(InputManager input) {
        return input.isKeyJustPressed(KeyEvent.VK_TAB) || input.isButtonJustPressed(InputManager.GamepadButton.BACK);
    }

    public void abrir(LevelManager levelManager) {
        int[][] terreno = levelManager.getCurLevelData();

        larguraMundo = terreno[0].length * (double) GameCore.tiles_size;
        alturaMundo = terreno.length * (double) GameCore.tiles_size;

        miniatura = levelManager.criarMiniaturaMapa();
    }

    public GameState update(InputManager input) {
        if (apertouMapa(input) || input.isKeyJustPressed(KeyEvent.VK_ESCAPE) || input.isButtonJustPressed(InputManager.GamepadButton.B)) {
            return GameState.PLAYING;
        }
        return GameState.MAP;
    }

    public void render(
            Graphics2D original,
            int larguraTela,
            int alturaTela,
            Player player,
            LevelManager levelManager,
            QuestManager questManager) {
        Graphics2D g = (Graphics2D) original.create();

        try {
            g.setColor(new Color(12, 22, 32));
            g.fillRect(0, 0, larguraTela, alturaTela);

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString("MAPA", 24, 32);

            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("Tab / Back: voltar  |  Esc / B: fechar", 24, 55);

            if (miniatura == null) {
                return;
            }

            int margem = 24;
            int topo = 76;

            int larguraDisponivel = Math.max(1, larguraTela - margem * 2);
            int alturaDisponivel = Math.max(1, alturaTela - topo - 48);

            double escala = Math.min(larguraDisponivel / larguraMundo, alturaDisponivel / alturaMundo);

            int larguraMapa = Math.max(1, (int) Math.round(larguraMundo * escala));
            int alturaMapa = Math.max(1, (int) Math.round(alturaMundo * escala));

            int mapaX = (larguraTela - larguraMapa) / 2;
            int mapaY = topo + (alturaDisponivel - alturaMapa) / 2;

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g.drawImage(miniatura, mapaX, mapaY, larguraMapa, alturaMapa, null);

            g.setColor(new Color(140, 175, 195));
            g.drawRect(mapaX, mapaY, larguraMapa, alturaMapa);

            for (TiledObject obj : levelManager.getMapData().objects) {
                if (!"spawn_npc".equalsIgnoreCase(obj.tipo) || obj.npc_nome == null) {
                    continue;
                }

                String nome = obj.npc_nome;

                if (!nome.equalsIgnoreCase("pescador") && !nome.equalsIgnoreCase("vendedor")) {
                    continue;
                }

                marcar(g, obj.x, obj.y, mapaX, mapaY, escala, Color.CYAN, nome);
            }

            boolean mapaPrincipal = LoadSave.LEVEL_1_DATA.equals(levelManager.getArquivoNivelAtual());

            if (mapaPrincipal) {
                Point2D.Double alvo = questManager.getQuestTargetPoint();

                if (alvo != null) {
                    marcar(g, alvo.x, alvo.y, mapaX, mapaY, escala, Color.YELLOW, "Missão");
                }
            }
            Collider corpo = player.getBodyCollider();

            double centroX = player.getX() + corpo.getOffsetX() + corpo.getWidth() / 2.0;
            double centroY = player.getY() + corpo.getOffsetY() + corpo.getHeight() / 2.0;

            marcar(g, centroX, centroY, mapaX, mapaY, escala, Color.GREEN, "Você");

            g.setColor(Color.WHITE);
            g.drawString("Verde: Pingu    Azul: NPCs    Amarelo: missão", 24, alturaTela - 18);
        } finally {
            g.dispose();
        }
    }

    private void marcar(Graphics2D g, double mundoX, double mundoY, int mapaX, int mapaY, double escala, Color cor, String texto) {
        int x = mapaX + (int) Math.round(mundoX * escala);
        int y = mapaY + (int) Math.round(mundoY * escala);

        g.setColor(Color.BLACK);
        g.fillOval(x - 6, y - 6, 12, 12);

        g.setColor(cor);
        g.fillOval(x - 4, y - 4, 8, 8);

        g.setColor(Color.BLACK);
        g.drawString(texto, x + 10, y - 7);

        g.setColor(Color.WHITE);
        g.drawString(texto, x + 9, y - 8);
    }
}
