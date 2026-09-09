import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class MapScreen {

    private BufferedImage miniatura;
    private double larguraMundo;
    private double alturaMundo;
    private static final Color COR_MISSAO = new Color(255, 215, 65);

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
            QuestManager questManager,
            NPCManager npcManager) {
        Graphics2D g = (Graphics2D) original.create();

        try {
            g.setColor(new Color(0, 0, 0, 200));
            g.fillRect(0, 0, larguraTela, alturaTela);

            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            g.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 22f));
            g.setColor(Color.WHITE);
            g.drawString("MAPA", 24, 32);

            g.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 12f));
            g.drawString(GameCore.getDebug()
                    ? "Tab: voltar  |  Esc: fechar  |  F12: testar missão"
                    : "Tab: voltar  |  Esc: fechar", 24, 55);

            if (miniatura == null) {
                return;
            }

            int margem = 24;
            String statusMissao = switch (questManager.getQuestState()) {
                case ATIVA -> "Missão do vendedor: derrote os inimigos na arena marcada.";
                case PRONTA_PARA_ENTREGAR -> "Missão concluída! Volte ao vendedor para receber o prêmio.";
                case NENHUMA -> "Nenhuma missão ativa.";
            };
            g.setColor(COR_MISSAO);
            g.drawString(statusMissao, margem, 76);

            int topo = 94;

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

            // O objetivo é consultado a cada render, sem ficar gravado na miniatura.
            // As coordenadas das arenas do vendedor pertencem ao mapa principal.
            if (LoadSave.LEVEL_1_DATA.equals(levelManager.getArquivoNivelAtual())) {
                Shape alvo = questManager.getQuestTargetShape();
                if (alvo != null) {
                    marcarMissao(g, alvo, mapaX, mapaY, larguraMapa, alturaMapa, escala);
                }
            }

            for (NPCManager.MarcadorMapa npc : npcManager.getMarcadoresMapa(
                    levelManager.getArquivoNivelAtual(), levelManager.getMapData().objects)) {
                marcar(g, npc.x(), npc.y(), mapaX, mapaY, escala, Color.CYAN, npc.nome());
            }

            Collider corpo = player.getBodyCollider();

            double centroX = player.getX() + corpo.getOffsetX() + corpo.getWidth() / 2.0;
            double centroY = player.getY() + corpo.getOffsetY() + corpo.getHeight() / 2.0;

            marcar(g, centroX, centroY, mapaX, mapaY, escala, Color.GREEN, "Você");

            g.setColor(Color.WHITE);
            g.drawString("Verde: Pingu    Azul: NPCs    ! Amarelo: arena da missão", 24, alturaTela - 18);
        } finally {
            g.dispose();
        }
    }

    private void marcarMissao(Graphics2D original, Shape arena, int mapaX, int mapaY,
            int larguraMapa, int alturaMapa, double escala) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.clipRect(mapaX, mapaY, larguraMapa, alturaMapa);
            AffineTransform transform = AffineTransform.getTranslateInstance(mapaX, mapaY);
            transform.scale(escala, escala);
            Shape area = transform.createTransformedShape(arena);

            g.setColor(new Color(255, 215, 65, 65));
            g.fill(area);
            g.setStroke(new BasicStroke(2f));
            g.setColor(COR_MISSAO);
            g.draw(area);

            Rectangle2D bounds = area.getBounds2D();
            int x = (int) Math.round(bounds.getCenterX());
            int y = (int) Math.round(bounds.getCenterY());
            // O anel continua visível quando o Pingu está no centro do objetivo.
            g.setColor(Color.BLACK);
            g.fillOval(x - 11, y - 11, 22, 22);
            g.setColor(COR_MISSAO);
            g.drawOval(x - 10, y - 10, 20, 20);
            g.drawString("!", x - g.getFontMetrics().stringWidth("!") / 2,
                    y + (g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);

            String texto = "Missão do vendedor";
            int largura = g.getFontMetrics().stringWidth(texto) + 12;
            int altura = g.getFontMetrics().getHeight() + 6;
            int labelX = Math.max(mapaX + 2,
                    Math.min(x - largura / 2, mapaX + larguraMapa - largura - 2));
            int labelY = Math.max(mapaY + 2,
                    Math.min(y + 16, mapaY + alturaMapa - altura - 2));
            g.setColor(new Color(15, 20, 25, 235));
            g.fillRoundRect(labelX, labelY, largura, altura, 6, 6);
            g.setColor(COR_MISSAO);
            g.drawString(texto, labelX + 6, labelY + 3 + g.getFontMetrics().getAscent());
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
