
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
//TODO: consertar corações que não aparecem, e a vida dos inimigos 

public class Hud {

    private static final int HEART_SIZE = 9;
    private static final int HEART_SCALE = 3;
    private static final int HEART_RENDER = HEART_SIZE * HEART_SCALE;
    private static final int HEART_GAP = 2;
    private static final int HEARTS_MAX = 5;

    private static final int AMMO_BAR_WIDTH = 160;
    private static final int AMMO_BAR_HEIGHT = 14;
    private static final int AMMO_BAR_MARGIN = 16;

    private BufferedImage heartSheet;

    // ── Partículas ──────────────────────────────────────────────────────────────
    private static class HeartParticle {

        double x, y; // posição na tela (espaço HUD)
        double velX, velY; // velocidade em px/frame
        int life; // frames restantes
        final int maxLife;
        final float scale; // tamanho da partícula (fator sobre HEART_RENDER)

        HeartParticle(double x, double y, double velX, double velY, int life, float scale) {
            this.x = x;
            this.y = y;
            this.velX = velX;
            this.velY = velY;
            this.life = this.maxLife = life;
            this.scale = scale;
        }
    }

    private final ArrayList<HeartParticle> particles = new ArrayList<>();

    public Hud() {
        try {
            heartSheet = LoadSave.GetSpriteAtlas("heart_sprites.png");
        } catch (Exception e) {
            System.err.println("Erro ao carregar heart_sprites.png: " + e.getMessage());
        }
    }

    public void draw(Graphics2D g2, int telaLargura, int telaAltura,
            CameraManager camera, Player p, EnemyManager em) {

        // spawna particulas de dano
        if (p.consumirDanoFlag()) {
            spawnHeartParticles(p);
        }

        player_hearts(g2, p);
        updateAndDrawParticles(g2);
        healthbar_inimigos(g2, telaLargura, telaAltura, camera, em);
        ammobar(g2, telaLargura, telaAltura, p);
    }

    void ammobar(Graphics2D g2, int telaLargura, int telaAltura, Player p) {
        int pente = p.getPente();
        int penteMax = p.getPenteMax();
        int municaoTotal = p.getMunicao();

        if (penteMax <= 0) {
            return;
        }

        int barX = telaLargura - AMMO_BAR_WIDTH - AMMO_BAR_MARGIN;
        int barY = telaAltura - AMMO_BAR_HEIGHT - AMMO_BAR_MARGIN;

        g2.setColor(new Color(20, 20, 20, 180));
        g2.fill(new RoundRectangle2D.Double(barX, barY,
                AMMO_BAR_WIDTH, AMMO_BAR_HEIGHT, 6, 6));

        int fillW = (int) ((AMMO_BAR_WIDTH - 4) * (double) pente / penteMax);
        if (fillW > 0) {

            GradientPaint grad = new GradientPaint(
                    barX + 2, barY, new Color(200, 100, 10),
                    barX + 2, barY + AMMO_BAR_HEIGHT, new Color(255, 165, 30));
            Paint prev = g2.getPaint();
            g2.setPaint(grad);
            g2.fill(new RoundRectangle2D.Double(barX + 2, barY + 2,
                    fillW, AMMO_BAR_HEIGHT - 4, 4, 4));
            g2.setPaint(prev);
        }

        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        String texto;
        if (p.isReloading()) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            texto = "Reloading...";
        } else {
            texto = pente + " / " + municaoTotal;
        }
        FontMetrics fm = g2.getFontMetrics();
        int tx = barX + AMMO_BAR_WIDTH - fm.stringWidth(texto) - 4;
        int ty = barY - 4;

        g2.setColor(Color.BLACK);
        g2.drawString(texto, tx + 1, ty + 1);

        g2.setColor(new Color(255, 200, 80));
        g2.drawString(texto, tx, ty);
    }

    private void spawnHeartParticles(Player p) {
        if (heartSheet == null) {
            return;
        }

        int vida = p.getVida();
        int vidaMax = p.getVidaMax();
        float vidaPorCoracao = (float) vidaMax / HEARTS_MAX;

        int coracaoAfetado = 0;
        for (int i = 0; i < HEARTS_MAX; i++) {
            float vidaRestante = vida - i * vidaPorCoracao;
            if (vidaRestante > 0) {
                coracaoAfetado = i + 1;
            }
        }

        double origemX = 16 + coracaoAfetado * (HEART_RENDER + HEART_GAP) + HEART_RENDER / 2.0;
        double origemY = 16 + HEART_RENDER / 2.0;

        int qtd = 7;
        double speed = 0.8;
        int life = 120;
        double minAngle = Math.PI * 7 / 6; // 210°
        double maxAngle = Math.PI * 11 / 6; // 330°
        double passo = (maxAngle - minAngle) / (qtd - 1);

        for (int i = 0; i < qtd; i++) {
            double angle = minAngle + i * passo;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            vy -= 0.2;
            float scale = 0.4f + (float) Math.random() * 0.4f;

            particles.add(new HeartParticle(origemX, origemY, vx, vy, life, scale));
        }
    }

    private void updateAndDrawParticles(Graphics2D g2) {
        if (heartSheet == null) {
            return;
        }

        // Sprite de meio-coração (índice 1)
        BufferedImage halfHeart = heartSheet.getSubimage(1 * HEART_SIZE, 0, HEART_SIZE, HEART_SIZE);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        Iterator<HeartParticle> it = particles.iterator();
        while (it.hasNext()) {
            HeartParticle pt = it.next();

            // Física
            pt.velY += 0.014;
            pt.velX *= 0.97;
            pt.x += pt.velX;
            pt.y += pt.velY;
            pt.life--;

            float alpha = pt.life < 60 ? pt.life / 60f : 1f;
            int size = Math.max(1, (int) (HEART_RENDER * pt.scale));

            Composite comp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            g2.drawImage(halfHeart,
                    (int) (pt.x - size / 2.0),
                    (int) (pt.y - size / 2.0),
                    size, size, null);
            g2.setComposite(comp);

            // Remove só depois de desenhar
            if (pt.life <= 0) {
                it.remove();
            }
        }
    }

    void player_hearts(Graphics2D g2, Player p) {
        if (heartSheet == null) {
            return;
        }

        int vida = p.getVida();
        int vidaMax = p.getVidaMax();

        // Quantos pontos de vida cada coração representa
        // Ex: vidaMax=100, HEARTS_MAX=5 → cada coração = 20 pontos
        float vidaPorCoracao = (float) vidaMax / HEARTS_MAX;

        for (int i = 0; i < HEARTS_MAX; i++) {
            // Vida restante após os corações anteriores
            float vidaRestante = vida - i * vidaPorCoracao;

            int spriteIndex;
            if (vidaRestante >= vidaPorCoracao) {
                spriteIndex = 0; // coração cheio
            } else if (vidaRestante >= vidaPorCoracao / 2f) {
                spriteIndex = 1; // meio coração
            } else {
                spriteIndex = 2; // coração vazio
            }

            BufferedImage sprite = heartSheet.getSubimage(spriteIndex * HEART_SIZE, 0, HEART_SIZE, HEART_SIZE);
            int drawX = 16 + i * (HEART_RENDER + HEART_GAP);

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.drawImage(sprite, drawX, 16, HEART_RENDER, HEART_RENDER, null);
        }
    }

    // Desenha barra de vida para inimigos que nao estao com a vida cheia
    void healthbar_inimigos(Graphics2D g2, int telaLargura, int telaAltura, CameraManager camera, EnemyManager em) {
        double camX = camera.getX();
        double camY = camera.getY();
        double camzoom = camera.getZoom();
        ArrayList<Enemy> enemies = em.getEnemies();
        for (Enemy enemy : enemies) {
            int envida = enemy.getVida();
            int envidamax = enemy.getVidaMax();
            if (envida == envidamax) {
                continue;
            }
            double enX = enemy.getX();
            double enY = enemy.getY();

            double screenX = (enX - camX) * camzoom;
            double screenY = (enY - 32 - camY) * camzoom;

            g2.setColor(Color.BLACK);
            g2.fill(new RoundRectangle2D.Double(screenX, screenY, 48 * camzoom, 8 * camzoom, 4, 4));
            g2.setColor(Color.RED);
            g2.fill(new RoundRectangle2D.Double(screenX + 1 * camzoom, screenY + 2 * camzoom,
                    (48 * (1.0 * envida / envidamax) - 2) * camzoom, 4 * camzoom, 2, 2));

        }
    }
}
