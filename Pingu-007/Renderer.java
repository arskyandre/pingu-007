
import java.awt.*;
import java.awt.geom.*;

public class Renderer {

    // Controle para ativar/desativar os visuais de teste
    public boolean modoDebug = true;

    public void renderizar(Graphics2D g2, CameraManager camera, Player quadrado, InputManager input, int telaLargura, int telaAltura,
            LevelManager lm, BulletManager bulletmanager, LootManager lootmanager, EnemyManager enemyManager) {
        // Limpa tela / Background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, telaLargura, telaAltura);
        // Suavização
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform originalTransform = g2.getTransform();
        g2.scale(camera.getZoom(), camera.getZoom());
        g2.translate(-camera.getX(), -camera.getY());

        // A ordem das chamadas define o Z-index
        renderMap(g2, lm, camera, telaLargura, telaAltura);
        renderBullets(g2, bulletmanager, camera, telaLargura, telaAltura);
        renderLoot(g2, lootmanager, camera, telaLargura, telaAltura);
        renderDashEffect(g2, quadrado);
        renderPlayer(g2, quadrado);
        renderEnemies(g2, enemyManager, camera, telaLargura, telaAltura);

        if (modoDebug) {
            renderDebug(g2, camera, quadrado, input);
        }
        g2.setTransform(originalTransform);
        renderMouse(g2, input);
    }

    private void renderLoot(Graphics2D g2, LootManager lootmanager, CameraManager camera, int telaLargura, int telaAltura) {
        lootmanager.draw(g2, camera, telaLargura, telaAltura);
    }

    private void renderBullets(Graphics2D g2, BulletManager bulletmanager, CameraManager camera, int telaLargura, int telaAltura) {
        bulletmanager.draw(g2, camera, telaLargura, telaAltura);
    }

    private void renderMap(Graphics2D g2, LevelManager lm, CameraManager camera, int telaLargura, int telaAltura) {
        // FIXED?: Renderizar o mapa do jeito certinho
        // TODO: Alex, em teoria eu dei uma consertada na renderização do mapa, checa depois
        lm.draw(g2, camera, telaLargura, telaAltura);
    }

    private void renderEnemies(Graphics2D g2, EnemyManager enemyManager, CameraManager camera, int telaLargura, int telaAltura) {
        enemyManager.draw(g2, camera, telaLargura, telaAltura);
    }

    private void renderDashEffect(Graphics2D g2, Player quadrado) {
        // TODO: implementar efeitos no dash
    }

    private void renderPlayer(Graphics2D g2, Player quadrado) {
        // TODO: implementar sistema de sprites
        Direction dir = quadrado.getDirection();
        Color cor = Color.YELLOW;

        if (dir != null) {
            cor = switch (dir) {
                case UP ->
                    Color.BLUE;
                case DOWN ->
                    Color.RED;
                case LEFT ->
                    Color.GREEN;
                default ->
                    Color.YELLOW;
            };
        }

        g2.setColor(cor);
        g2.fill(new Rectangle2D.Double(
                quadrado.getX(),
                quadrado.getY(),
                quadrado.getLargura(),
                quadrado.getAltura()));
    }

    private void renderDebug(Graphics2D g2, CameraManager camera, Player quadrado, InputManager input) {
        double centerX = quadrado.getX() + quadrado.getLargura() / 2.0;
        double centerY = quadrado.getY() + quadrado.getAltura() / 2.0;

        // Debug Dash (Polígono Rosa)
        if (quadrado.isEmDash()) {
            double dashDirX = quadrado.getDashDirX();
            double dashDirY = quadrado.getDashDirY();

            double comprimento = 80;
            double larguraBase = 25;

            double tipX = centerX + dashDirX * comprimento;
            double tipY = centerY + dashDirY * comprimento;

            double perpX = -dashDirY;
            double perpY = dashDirX;

            double baseX = centerX - dashDirX * 20;
            double baseY = centerY - dashDirY * 20;

            double leftX = baseX + perpX * larguraBase;
            double leftY = baseY + perpY * larguraBase;

            double rightX = baseX - perpX * larguraBase;
            double rightY = baseY - perpY * larguraBase;

            Polygon triangle = new Polygon();
            triangle.addPoint((int) tipX, (int) tipY);
            triangle.addPoint((int) leftX, (int) leftY);
            triangle.addPoint((int) rightX, (int) rightY);

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Color.PINK);
            g2.fillPolygon(triangle);
            g2.setComposite(old);
        }

        // Debug linha do mouse
        double mouseXWorld = (input.getMouseX() / camera.getZoom()) + camera.getX();
        double mouseYWorld = (input.getMouseY() / camera.getZoom()) + camera.getY();
        g2.setColor(Color.WHITE);
        g2.drawLine((int) centerX, (int) centerY, (int) mouseXWorld, (int) mouseYWorld);
    }

    private void renderMouse(Graphics2D g2, InputManager input) {
        int mouseX = input.getMouseX();
        int mouseY = input.getMouseY();

        // Mira Teste
        g2.setColor(Color.RED);
        g2.fill(new Ellipse2D.Double(mouseX - 10, mouseY - 10, 20, 20));
    }
}
