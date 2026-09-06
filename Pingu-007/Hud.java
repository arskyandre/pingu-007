
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class Hud {

    private Font pixelFont;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SCALE = 3;
    private static final int HEART_RENDER = HEART_SIZE * HEART_SCALE;
    private static final int HEART_GAP = 2;
    private static final int HEARTS_MAX = 5;

    private static final int AMMO_BAR_WIDTH = 216;
    private static final int AMMO_BAR_HEIGHT = 3;
    private static final int AMMO_CURRENT_BAR_HEIGHT = 3;
    private static final int AMMO_BAR_GAP = 1;
    private static final int AMMO_MAX_BAR_OFFSET_X = 4;
    private static final int AMMO_ICON_SIZE = 32;
    private static final int AMMO_BAR_MARGIN = 16;
    private static final float AMMO_BAR_OPACITY = 0.5f;
    private static final float AMMO_TEXT_SIZE = 16f;
    private static final float AMMO_RELOADING_TEXT_SIZE = AMMO_TEXT_SIZE - 4f;

    private static final Polygon SETA_POLYGON = new Polygon(
            new int[] { 10,
                    -8, -8 },
            new int[] { 0,
                    -8, 8 },
            3);
    private static final BasicStroke SETA_STROKE = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color COR_SETA_BORDA = new Color(0, 0, 0, 180);
    private static final Color COR_SETA_INTERIOR = new Color(220, 20, 60);

    private boolean animateChave = false;
    private boolean jaPegouChave = false;
    private boolean jaPegouMoeda = false;
    private boolean jaPegouIsca = false;
    private BufferedImage heartSheet = null;
    public static final BufferedImage balaSprite[];
    private BufferedImage moedaSprite[] = null;
    private BufferedImage iscaSprite[] = null;
    private BufferedImage chaveSprite[] = null;

    private static class HeartParticle {

        double x, y;
        double velX, velY;
        double life;
        final double maxLife;
        final float scale;

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

    static {
        BufferedImage[] temp;
        try {
            BufferedImage base = LoadSave.GetSpriteAtlas("images/hud/balasprite.png");
            temp = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                temp[i] = base.getSubimage(i * 16, 0, 16, 16);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar balasprite.png: " + e.getMessage());
            temp = new BufferedImage[2];
        }
        balaSprite = temp;
    }

    public Hud() {
        try {
            heartSheet = LoadSave.GetSpriteAtlas("images/hud/heart_sprites.png");
        } catch (Exception e) {
            System.err.println("Erro ao carregar heart_sprites.png: " + e.getMessage());
        }
        try {
            BufferedImage base = LoadSave.GetSpriteAtlas("images/hud/moedasprite.png");
            moedaSprite = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                moedaSprite[i] = base.getSubimage(i * 16, 0, 16, 16);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar moedasprite.png: " + e.getMessage());
        }
        try {
            BufferedImage base = LoadSave.GetSpriteAtlas("images/hud/chavesprite.png");
            chaveSprite = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                chaveSprite[i] = base.getSubimage(i * 16, 0, 16, 16);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar chavesprite.png: " + e.getMessage());
        }
        try {
            BufferedImage base = LoadSave.GetSpriteAtlas("images/hud/iscasprite.png");
            iscaSprite = new BufferedImage[2];
            for (int i = 0; i < 2; i++) {
                iscaSprite[i] = base.getSubimage(i * 16, 0, 16, 16);
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar iscasprite.png: " + e.getMessage());
        }
        try {
            Font base = Font.createFont(Font.TRUETYPE_FONT, new File("font/PressStart2P-Regular.ttf"));
            pixelFont = base.deriveFont(Font.BOLD, 7f);
        } catch (Exception e) {
            System.err.println("Font not found, falling back");
            pixelFont = new Font("Monospaced", Font.BOLD, 12);
        }
    }

    public void draw(Graphics2D g2, int telaLargura, int telaAltura,
            CameraManager camera, Player p, EnemyManager em, QuestManager questManager, double delta, int offset) {
        if (p.consumirDanoFlag()) {
            spawnHeartParticles(p);
        }
        if (p.consumirNovaChave()) {
            animateChave = true;
        }

        updateAndDrawParticles(g2, delta, offset);
        healthbar_inimigos(g2, telaLargura, telaAltura, camera, em);
        if (GameCore.getGameState() != GameState.CUTSCENE) {
            indicadores_inimigos(g2, telaLargura, telaAltura, camera, em, offset);
            indicador_missao(g2, telaLargura, telaAltura, camera, p, questManager, offset);
        }
    }

    private void drawSeta(Graphics2D g2, double x, double y, double angle, boolean isOffScreen) {
        Composite oldComposite = g2.getComposite();
        if (isOffScreen) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform oldTransform = g2.getTransform();
        AffineTransform transform = new AffineTransform(oldTransform);
        transform.translate(x, y);
        transform.rotate(angle);
        g2.setTransform(transform);
        g2.setColor(COR_SETA_BORDA);
        g2.setStroke(SETA_STROKE);
        g2.drawPolygon(SETA_POLYGON);
        g2.setColor(COR_SETA_INTERIOR);
        g2.fillPolygon(SETA_POLYGON);
        g2.setTransform(oldTransform);
        g2.setComposite(oldComposite);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private void indicador_missao(Graphics2D g2, int telaLargura, int telaAltura,
            CameraManager camera, Player p, QuestManager questManager, int offset) {
        if (p.getArenaManager() == null
                || questManager.getQuestState() != QuestManager.QuestState.ATIVA
                || questManager.isQuestArenaAtiva()) {
            return;
        }

        java.awt.geom.Point2D.Double pontoAlvo = questManager.getQuestTargetPoint();

        if (pontoAlvo == null) {
            return;
        }

        double alvoX = pontoAlvo.x;
        double alvoY = pontoAlvo.y;
        double camX = camera.getX();
        double camY = camera.getY();
        double camZoom = camera.getZoom();
        double centerX = telaLargura / 2.0;
        double centerY = telaAltura / 2.0;
        int marginX = 35;
        int marginY = 35 + offset;
        double limitW = centerX - marginX;
        double limitH = centerY - marginY;
        double screenX = (alvoX - camX) * camZoom;
        double screenY = (alvoY - camY) * camZoom;

        boolean isOffScreen = screenX < 0
                || screenX > telaLargura
                || screenY < offset
                || screenY > telaAltura - offset;

        if (isOffScreen) {
            double dx = screenX - centerX;
            double dy = screenY - centerY;

            if (dx == 0 && dy == 0) {
                return;
            }

            double scaleX = limitW / Math.abs(dx);
            double scaleY = limitH / Math.abs(dy);
            double scale = Math.min(scaleX, scaleY);
            double drawX = centerX + dx * scale;
            double drawY = centerY + dy * scale;
            double angle = Math.atan2(dy, dx);

            drawSetaMissao(g2, drawX, drawY, angle);
        } else {
            double distanciaDoCentro = 24.0;
            double drawX = Math.max(marginX,
                    Math.min(telaLargura - marginX, screenX));
            double drawY = Math.max(offset + marginX,
                    Math.min(telaAltura - offset - marginX,
                            screenY - distanciaDoCentro));

            drawSetaMissao(g2, drawX, drawY, Math.PI / 2.0);
        }
    }

    private void drawSetaMissao(Graphics2D g2, double x, double y, double angle) {
        java.awt.Composite oldComposite = g2.getComposite();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.9f));
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        java.awt.geom.AffineTransform transform = new java.awt.geom.AffineTransform(oldTransform);
        transform.translate(x, y);
        transform.rotate(angle);
        g2.setTransform(transform);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.setStroke(new java.awt.BasicStroke(4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g2.drawPolygon(SETA_POLYGON);
        g2.setColor(new Color(255, 215, 0));
        g2.fillPolygon(SETA_POLYGON);
        g2.setTransform(oldTransform);
        g2.setComposite(oldComposite);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private void indicadores_inimigos(Graphics2D g2, int telaLargura, int telaAltura, CameraManager camera,
            EnemyManager em, int offset) {
        double camX = camera.getX();
        double camY = camera.getY();
        double camzoom = camera.getZoom();
        double centerX = telaLargura / 2.0;
        double centerY = telaAltura / 2.0;
        int marginX = 35;
        int marginY = 35 + offset;
        double limitW = centerX - marginX;
        double limitH = centerY - marginY;
        ArrayList<Enemy> enemies = em.getEnemies();
        for (Enemy enemy : enemies) {
            if (enemy.getVida() <= 0) {
                continue;
            }

            double enCentroX = enemy.getX() + (enemy.getLargura() / 2.0);
            double enCentroY = enemy.getY() + (enemy.getAltura() / 2.0);
            double screenX = (enCentroX - camX) * camzoom;
            double screenY = (enCentroY - camY) * camzoom;
            double leftX = (enemy.getX() - camX) * camzoom;
            double rightX = ((enemy.getX() + enemy.getLargura()) - camX) * camzoom;
            double topY = (enemy.getY() - camY) * camzoom;
            double bottomY = ((enemy.getY() + enemy.getAltura()) - camY) * camzoom;
            boolean isOffScreenVisual = rightX < 0 || leftX > telaLargura
                    || bottomY < offset || topY > (telaAltura - offset);
            if (isOffScreenVisual) {
                double dx = screenX - centerX;
                double dy = screenY - centerY;
                if (dx == 0 && dy == 0) {
                    continue;
                }

                double scaleX = limitW / Math.abs(dx);
                double scaleY = limitH / Math.abs(dy);
                double scale = Math.min(scaleX, scaleY);
                double drawX = centerX + (dx * scale);
                double drawY = centerY + (dy * scale);
                double angle = Math.atan2(dy, dx);
                drawSeta(g2, drawX, drawY, angle, true);
            } else {
                double drawX = screenX;
                double drawY = topY - (20 * camzoom);
                if (drawY < offset + 15) {
                    drawY = offset + 15;
                }

                double angle = Math.PI / 2;
                drawSeta(g2, drawX, drawY, angle, false);
            }
        }
    }

    public void resetJaPegou() {
        chaveAlpha = 0;
        moedaAlpha = 0;
        iscaAlpha = 0.0;
        jaPegouIsca = false;
        jaPegouChave = false;
        jaPegouMoeda = false;
    }

    public void ammobar(Graphics2D g2, int telaLargura, int telaAltura, Player p, int offset) {
        int pente = p.getPente();
        int penteMax = p.getPenteMax();
        int municaoTotal = p.getMunicao();
        if (penteMax <= 0) {
            return;
        }

        int barX = telaLargura - AMMO_BAR_WIDTH - AMMO_BAR_MARGIN;
        int barraPenteMaxY = telaAltura - AMMO_BAR_HEIGHT - AMMO_BAR_MARGIN - offset;
        int barraPenteY = barraPenteMaxY - AMMO_BAR_GAP - AMMO_CURRENT_BAR_HEIGHT;
        Composite compositeAnterior = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, AMMO_BAR_OPACITY));
        // barra clara eh o tamanho do pente total, barra escura eh as balas no pente
        g2.setColor(new Color(137, 137, 137));
        g2.fillRect(barX + AMMO_MAX_BAR_OFFSET_X, barraPenteMaxY, AMMO_BAR_WIDTH, AMMO_BAR_HEIGHT);
        int larguraPente = (int) Math.round(AMMO_BAR_WIDTH * (double) pente / penteMax);
        larguraPente = Math.max(0, Math.min(AMMO_BAR_WIDTH, larguraPente));
        if (larguraPente > 0) {
            g2.setColor(new Color(51, 51, 51));
            g2.fillRect(barX + AMMO_MAX_BAR_OFFSET_X, barraPenteY, larguraPente - AMMO_MAX_BAR_OFFSET_X,
                    AMMO_CURRENT_BAR_HEIGHT);

        }
        g2.setComposite(compositeAnterior);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);

        String texto;
        if (p.isReloading()) {
            texto = "RECARREGANDO...";
        } else {
            texto = pente + "/" + municaoTotal;
        }

        float tamanhoFonte = p.isReloading() ? AMMO_RELOADING_TEXT_SIZE : AMMO_TEXT_SIZE;
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, tamanhoFonte));
        FontMetrics fm = g2.getFontMetrics();
        int iconX = barX - AMMO_ICON_SIZE;
        int textoTopo = barraPenteY - 6 - fm.getHeight();
        int elementoBottom = barraPenteMaxY + AMMO_BAR_HEIGHT;
        int elementoAltura = elementoBottom - textoTopo;
        int iconY = textoTopo + (elementoAltura - AMMO_ICON_SIZE) / 2;

        if (balaSprite[0] != null && balaSprite[1] != null) {
            g2.drawImage(balaSprite[0], iconX + 2, iconY + 2,
                    AMMO_ICON_SIZE, AMMO_ICON_SIZE, null);
            g2.drawImage(balaSprite[1], iconX, iconY,
                    AMMO_ICON_SIZE, AMMO_ICON_SIZE, null);
        }

        int tx = barX + 4;
        int ty = textoTopo + fm.getAscent();

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(texto, tx + 2, ty + 2);

        g2.setColor(new Color(255, 170, 0));
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

    private void updateAndDrawParticles(Graphics2D g2, double delta, int offset) {
        if (heartSheet == null) {
            return;
        }

        BufferedImage halfHeart = heartSheet.getSubimage(1 * HEART_SIZE, 0, HEART_SIZE, HEART_SIZE);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double timeScale = delta * 640.0;

        Iterator<HeartParticle> it = particles.iterator();
        while (it.hasNext()) {
            HeartParticle pt = it.next();

            pt.velY += 0.014 * timeScale;
            pt.velX *= Math.pow(0.97, timeScale);
            pt.x += pt.velX * timeScale;
            pt.y += pt.velY * timeScale;
            pt.life -= timeScale;

            double alpha = pt.life < 60 ? pt.life / 60f : 1f;
            int size = Math.max(1, (int) (HEART_RENDER * pt.scale));

            Composite comp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, (float) alpha)));
            g2.drawImage(halfHeart,
                    (int) (pt.x - size / 2.0),
                    (int) (pt.y - size / 2.0) + offset,
                    size, size, null);
            g2.setComposite(comp);

            if (pt.life <= 0) {
                it.remove();
            }
        }
    }

    public void player_hearts(Graphics2D g2, Player p, int offset) {
        if (heartSheet == null) {
            return;
        }

        int vida = p.getVida();
        int vidaMax = p.getVidaMax();
        float vidaPorCoracao = (float) vidaMax / HEARTS_MAX;
        for (int i = 0; i < HEARTS_MAX; i++) {
            float vidaRestante = vida - i * vidaPorCoracao;
            int spriteIndex;
            if (vidaRestante >= vidaPorCoracao) {
                spriteIndex = 0;
            } else if (vidaRestante >= vidaPorCoracao / 2f) {
                spriteIndex = 1;
            } else {
                spriteIndex = 2;
            }

            BufferedImage sprite = heartSheet.getSubimage(spriteIndex * HEART_SIZE, 0, HEART_SIZE, HEART_SIZE);
            int drawX = 16 + i * (HEART_RENDER + HEART_GAP);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.drawImage(sprite, drawX, 16 + offset, HEART_RENDER, HEART_RENDER, null);
        }
    }

    private double chaveAlpha = 0.0;

    public void desenha_chaves(Graphics2D g2, Player p, int telaLargura, int telaAltura, int offset, double delta) {
        if (chaveSprite == null) {
            return;
        }

        int chaves = p.getChaves();
        if (chaves > 0) {
            jaPegouChave = true;
        }
        double chaveAlphaTarget = (jaPegouChave && !GameCore.isLevel2()) ? 1.0 : 0.0;
        double chave_fade_duracao = 0.75;
        double chaveAlphaFadeSpeed = 1.0 / chave_fade_duracao;
        if (chaveAlpha < chaveAlphaTarget) {
            chaveAlpha = Math.min(chaveAlphaTarget, chaveAlpha + chaveAlphaFadeSpeed * delta);
        } else if (chaveAlpha > chaveAlphaTarget) {
            chaveAlpha = Math.max(chaveAlphaTarget, chaveAlpha - chaveAlphaFadeSpeed * delta);
        }

        if (chaveAlpha <= 0.0) {
            return;
        }

        int chavesMax = 3; // mudar caso exista mais chave no jogo

        int iconSize = 32;
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER,
                (float) chaveAlpha));

        for (int i = 0; i < chavesMax; i++) {
            int drawX = iconSize / 2 + 16 + i * (iconSize + HEART_GAP);

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            // preenche da direita pra esquerda conforme as chaves sao coletadas
            g2.drawImage(chaveSprite[0], drawX + 2, telaAltura - iconSize - 16 - offset + 2, iconSize, iconSize,
                    null);
            if (i < chaves) {
                g2.drawImage(chaveSprite[1], drawX, telaAltura - iconSize - 16 - offset, iconSize, iconSize, null);
            }

        }
        g2.setComposite(oldComposite);
    }

    private double moedaAlpha = 0.0;
    private double iscaAlpha = 0.0;

    public void desenha_moedas_e_isca(Graphics2D g2, Player p, int telaLargura, int telaAltura, int offset,
            double delta) {
        if (moedaSprite == null) {
            return;
        }

        int moedas = p.getMoedas();
        if (moedas > 0) {
            jaPegouMoeda = true;
        }

        double moedaAlphaTarget = (jaPegouMoeda && !GameCore.isLevel2()) ? 1.0 : 0.0;
        double moeda_fade_duracao = 0.75;
        double moedaAlphaFadeSpeed = 1.0 / moeda_fade_duracao;
        if (moedaAlpha < moedaAlphaTarget) {
            moedaAlpha = Math.min(moedaAlphaTarget, moedaAlpha + moedaAlphaFadeSpeed * delta);
        } else if (moedaAlpha > moedaAlphaTarget) {
            moedaAlpha = Math.max(moedaAlphaTarget, moedaAlpha - moedaAlphaFadeSpeed * delta);
        }

        int iscas = p.getIscas();
        if (iscas > 0) {
            jaPegouIsca = true;
        }

        double iscaAlphaTarget = (jaPegouIsca && !GameCore.isLevel2()) ? 1.0 : 0.0;
        double isca_fade_duracao = 0.75;
        double iscaAlphaFadeSpeed = 1.0 / isca_fade_duracao;
        if (iscaAlpha < iscaAlphaTarget) {
            iscaAlpha = Math.min(iscaAlphaTarget, iscaAlpha + iscaAlphaFadeSpeed * delta);
        } else if (iscaAlpha > iscaAlphaTarget) {
            iscaAlpha = Math.max(iscaAlphaTarget, iscaAlpha - iscaAlphaFadeSpeed * delta);
        }

        if (moedaAlpha <= 0.0 && iscaAlpha <= 0.0) {
            return;
        }

        int iconSize = 32;
        int margin = 16;
        int icon_text_distance = 8;
        int group_gap = 24; // espaço entre o grupo de iscas e o grupo de moedas

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setFont(GameCore.pixelFont.deriveFont(Font.PLAIN, 24f));
        String chMoeda = Integer.toString(moedas);
        Rectangle2D textBoundsMoeda = g2.getFontMetrics().getStringBounds(chMoeda, g2);
        int twMoeda = (int) textBoundsMoeda.getWidth();
        int thMoeda = (int) textBoundsMoeda.getHeight();
        int moedaDrawX = telaLargura - iconSize - margin - twMoeda - icon_text_distance;
        if (moedaAlpha > 0.0) {
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) moedaAlpha));
            g2.drawImage(moedaSprite[0], moedaDrawX + 2, margin + offset + 2, iconSize, iconSize, null);
            g2.drawImage(moedaSprite[1], moedaDrawX, margin + offset, iconSize, iconSize, null);
            int textX = moedaDrawX + iconSize + icon_text_distance;
            int textY = margin + 4 + offset + thMoeda;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(chMoeda, textX + 2, textY + 2);
            g2.setColor(new Color(255, 170, 0));
            g2.drawString(chMoeda, textX, textY);
            g2.setComposite(oldComposite);
        }

        if (iscaSprite != null && iscaAlpha > 0.0) {
            String chIsca = Integer.toString(iscas);
            Rectangle2D textBoundsIsca = g2.getFontMetrics().getStringBounds(chIsca, g2);
            int twIsca = (int) textBoundsIsca.getWidth();
            int thIsca = (int) textBoundsIsca.getHeight();
            int iscaDrawX = moedaDrawX - group_gap - iconSize - icon_text_distance - twIsca;
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) iscaAlpha));
            g2.drawImage(iscaSprite[0], iscaDrawX + 2, margin + offset + 2, iconSize, iconSize, null);
            g2.drawImage(iscaSprite[1], iscaDrawX, margin + offset, iconSize, iconSize, null);
            int textX = iscaDrawX + iconSize + icon_text_distance;
            int textY = margin + 4 + offset + thIsca;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(chIsca, textX + 2, textY + 2);
            g2.setColor(new Color(156, 81, 46));
            g2.drawString(chIsca, textX, textY);
            g2.setComposite(oldComposite);
        }
    }

    // Desenha barra de vida para inimigos que nao estao com a vida cheia
    void healthbar_inimigos(Graphics2D g2, int telaLargura, int telaAltura, CameraManager camera, EnemyManager em) {
        double camX = camera.getX();
        double camY = camera.getY();
        double camzoom = camera.getZoom();
        ArrayList<Enemy> enemies = em.getEnemies();
        for (Enemy enemy : enemies) {
            if (enemy instanceof MorsaBoss || enemy instanceof BossMao)
                continue;
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
                    (48 * (1.0 * envida / envidamax) - 2) * camzoom, 4 * camzoom,
                    2, 2));

        }
    }
}
