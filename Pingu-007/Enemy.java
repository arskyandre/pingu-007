
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public abstract class Enemy extends Entity {

    protected double width;
    protected double height;
    protected int[][] lvlData;
    protected int danoContato = 10;
    protected Color cor = Color.MAGENTA;

    protected ArrayList<Node> caminhoAStar;
    protected int currentPathIndex = 0;
    protected double aStarDelay = 0;
    protected double tempoRecalculoAStar = 30;

    protected boolean calculandoCaminho = false;

    public boolean podePularBuracos = false;
    public boolean isInvulneravel = false;

    public boolean isHooked = false;
    public boolean podeSerPuxado = true;

    public double raioDeteccao = GameCore.tiles_size * 8.0;
    protected boolean aggroPermanente = false;
    protected boolean lootProcessado = false;
    public boolean podeDropar = true;

    protected boolean emSaltoCinematico = false;
    protected double lerpStartX, lerpStartY, lerpTargetX, lerpTargetY;
    protected int lerpFramesMax = 0;
    protected int lerpFrameAtual = 0;
    protected Node pendingJumpNode = null;
    public boolean geraRecompensaPadrao = true;

    protected int timerPuxado = 0;

    /**
     * TODO: atribuir efeito sonoro de morte do inimigo pelo enum SFX do
     * SoundManager(falta: Jumper, Dasher, Shooter, Maos da Morsa(?)) o som toca
     * para todo inimigo morto pelo EnemyManager
     */
    protected SoundManager.SFX deathSFX = null;

    public Enemy(double startX, double startY, double width, double height, int[][] lvlData, SoundManager soundManager, ArenaManager arenaManager) {
        super(arenaManager, soundManager);
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.lvlData = lvlData;
        this.peso = 1.0;
        this.aStarDelay = (int) (Math.random() * tempoRecalculoAStar);
    }

    public void setLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
        this.caminhoAStar = null;
        this.currentPathIndex = 0;
    }

    public boolean temAggro() {
        return aggroPermanente;
    }

    public boolean includedInCombatCamera() {
        return temAggro();
    }

    public boolean isLootProcessado() {
        return lootProcessado;
    }

    public void marcarLootProcessado() {
        lootProcessado = true;
    }

    public void playDeathSound() {
        if (soundManager != null && deathSFX != null) {
            soundManager.playSFX(deathSFX);
        }
    }

    protected boolean atualizarAggro(Player player) {
        if (aggroPermanente) {
            return true;
        }

        double meuCenterX = this.x + (this.bodyCollider != null
                ? this.bodyCollider.getOffsetX() + this.bodyCollider.getWidth() / 2.0
                : this.width / 2.0);
        double meuCenterY = this.y + (this.bodyCollider != null
                ? this.bodyCollider.getOffsetY() + this.bodyCollider.getHeight() / 2.0
                : this.height / 2.0);
        double playerCenterX = player.getX() + player.getLargura() / 2.0;
        double playerCenterY = player.getY() + player.getAltura() / 2.0;

        double dist = Math.hypot(playerCenterX - meuCenterX, playerCenterY - meuCenterY);
        if (dist <= raioDeteccao && temLinhaDeVisaoLivre(player)) {
            aggroPermanente = true;
        }
        return aggroPermanente;
    }

    protected void aplicarComportamentoIdle() {
        velX *= 0.85;
        velY *= 0.85;
    }

    public abstract void update(Player player, ArrayList<JumpLink> jumpLinks);

    public void drawGroundTelegraph(Graphics2D g2, double delta) {
    }

    protected boolean temLinhaDeVisaoLivre(Player player) {
        double x0 = this.x + this.width / 2.0;
        double y0 = this.y + this.height / 2.0;
        double x1 = player.getX() + player.getLargura() / 2.0;
        double y1 = player.getY() + player.getAltura() / 2.0;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dist = Math.hypot(dx, dy);

        int steps = Math.max(1, (int) Math.ceil(dist / 16.0));
        for (int i = 0; i <= steps; i++) {
            double checkX = x0 + (dx * i) / steps;
            double checkY = y0 + (dy * i) / steps;
            int col = (int) (checkX / GameCore.tiles_size);
            int row = (int) (checkY / GameCore.tiles_size);

            if (row >= 0 && row < lvlData.length && col >= 0 && col < lvlData[0].length) {
                int tile = lvlData[row][col];
                if (TileProperties.isOpaque(tile)) {
                    return false;
                }
            }
        }

        if (arenaManager != null && arenaManager.getObjetosDeCenario() != null) {
            java.awt.geom.Line2D.Double linhaDeVisao = new java.awt.geom.Line2D.Double(x0, y0, x1, y1);

            java.awt.geom.Rectangle2D segBounds = new java.awt.geom.Rectangle2D.Double(
                    Math.min(x0, x1), Math.min(y0, y1),
                    Math.abs(dx), Math.abs(dy));

            for (MapObject obj : arenaManager.getObjetosDeCenario()) {
                if (obj == null || !obj.isSolid() || obj.isTransparent()) {
                    continue;
                }

                Shape hb = obj.getHitbox();
                if (hb == null) {
                    continue;
                }

                java.awt.geom.Rectangle2D bounds = hb.getBounds2D();

                if (!bounds.intersects(segBounds)) {
                    continue;
                }

                if (hb instanceof java.awt.geom.Rectangle2D rect) {
                    if (linhaDeVisao.intersects(rect)) {
                        return false;
                    }
                } else if (hb.intersects(segBounds)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void aplicarFreioDePreparacao(double intensidade) {
        this.velX *= intensidade;
        this.velY *= intensidade;
    }

    public void serPuxado(double originX, double originY, double forcaBaseCalculada) {
        if (!podeSerPuxado) {
            return;
        }

        this.isPuxado = true;
        this.timerPuxado = 20;
        this.aStarDelay = tempoRecalculoAStar;

        this.isAirborne = true;

        double meuCX = this.x
                + (this.bodyCollider != null ? this.bodyCollider.getOffsetX() + (this.bodyCollider.getWidth() / 2.0)
                        : this.width / 2.0);
        double meuCY = this.y
                + (this.bodyCollider != null ? this.bodyCollider.getOffsetY() + (this.bodyCollider.getHeight() / 2.0)
                        : this.height / 2.0);

        double dx = originX - meuCX;
        double dy = originY - meuCY;
        double dist = Math.hypot(dx, dy);

        if (dist > 0) {
            double forcaFinal = forcaBaseCalculada / this.peso;
            this.velX = (dx / dist) * forcaFinal;
            this.velY = (dy / dist) * forcaFinal;
        }
    }

    protected void atualizarTimersKnockback() {
        if (isPuxado) {
            timerPuxado--;

            int col = (int) ((this.x + this.width / 2.0) / GameCore.tiles_size);
            int row = (int) ((this.y + this.height / 2.0) / GameCore.tiles_size);

            if (row >= 0 && row < lvlData.length && col >= 0 && col < lvlData[0].length) {
                int tileAtual = lvlData[row][col];

                if (TileProperties.isHole(tileAtual)) {
                    double velocidadeAtual = Math.hypot(this.velX, this.velY);

                    if (velocidadeAtual < 3.0) {
                        this.isAirborne = false;
                        this.isCaindo = true;
                        this.isDead = true;

                        this.velX = 0;
                        this.velY = 0;
                        this.timerPuxado = 0;

                        playDeathSound();
                    }
                }
            }

            if (timerPuxado <= 0 && !isCaindo) {
                isPuxado = false;
                this.isAirborne = false;
                aStarDelay = tempoRecalculoAStar;
            }
        }
    }

    protected void seguirCaminhoAStar(Player player, ArrayList<JumpLink> jumpLinks) {

        if (isPuxado || isCaindo) {
            if (isCaindo) {
                velX *= 0.2;
                velY *= 0.2;
            }
            return;
        }

        if (!atualizarAggro(player)) {
            aplicarComportamentoIdle();
            return;
        }

        if (isAirborne && Math.abs(velX) < 1.0 && Math.abs(velY) < 1.0) {
            this.isAirborne = false;
        }

        double meuCenterX = this.x
                + (this.bodyCollider != null ? this.bodyCollider.getOffsetX() + this.bodyCollider.getWidth() / 2.0
                        : this.width / 2.0);
        double meuCenterY = this.y
                + (this.bodyCollider != null ? this.bodyCollider.getOffsetY() + this.bodyCollider.getHeight() / 2.0
                        : this.height / 2.0);
        double playerCenterX = player.getX() + player.getLargura() / 2.0;
        double playerCenterY = player.getY() + player.getAltura() / 2.0;

        double distToPlayer = Math.hypot(playerCenterX - meuCenterX, playerCenterY - meuCenterY);

        if (!isAirborne && distToPlayer < raioDeteccao && temLinhaDeVisaoLivre(player)) {
            double dx = player.getX() - this.x;
            double dy = player.getY() - this.y;
            double distP = Math.hypot(dx, dy);

            if (distP > 0) {
                this.velX += (dx / distP) * aceleracao;
                this.velY += (dy / distP) * aceleracao;
            }
            return;
        }

        if (!isAirborne) {
            aStarDelay++;

            if (!calculandoCaminho && (aStarDelay >= tempoRecalculoAStar || caminhoAStar == null || currentPathIndex >= caminhoAStar.size())) {

                int startCol = (int) (meuCenterX / GameCore.tiles_size);
                int startRow = (int) (meuCenterY / GameCore.tiles_size);
                int targetCol = (int) (playerCenterX / GameCore.tiles_size);
                int targetRow = (int) (playerCenterY / GameCore.tiles_size);

                calculandoCaminho = true;
                aStarDelay = 0;

                PathFinder.solicitarCaminhoAsync(startCol, startRow, targetCol, targetRow, this.lvlData,
                        this.podePularBuracos ? jumpLinks : null,
                        arenaManager.getObjetosDeCenario(),
                        (novoCaminho) -> {
                            this.caminhoAStar = novoCaminho;
                            this.currentPathIndex = 0;
                            this.calculandoCaminho = false;
                        });
            }
        }

        if (caminhoAStar == null || currentPathIndex >= caminhoAStar.size()) {
            if (!isAirborne) {
                double dx = player.getX() - this.x;
                double dy = player.getY() - this.y;
                double dist = Math.hypot(dx, dy);

                if (dist > 0) {
                    this.velX += (dx / dist) * aceleracao;
                    this.velY += (dy / dist) * aceleracao;
                }
            }
            return;
        }

        Node proximoNo = caminhoAStar.get(currentPathIndex);
        double alvoX = getPathTargetX(proximoNo);
        double alvoY = getPathTargetY(proximoNo);

        double dx = alvoX - this.x;
        double dy = alvoY - this.y;
        double dist = Math.hypot(dx, dy);

        double landingMargin = (GameCore.tiles_size / 2.0) + 5.0;

        if (dist < landingMargin) {
            currentPathIndex++;

            if (currentPathIndex < caminhoAStar.size()) {
                Node proximo = caminhoAStar.get(currentPathIndex);
                if (proximo.requerSalto) {
                    prepararSaltoAStar(proximo);
                } else {
                    this.isAirborne = false;
                }
            } else {
                this.isAirborne = false;
            }
            return;
        }

        if (!isAirborne) {
            this.velX += (dx / dist) * aceleracao;
            this.velY += (dy / dist) * aceleracao;
        }
    }

    protected void prepararSaltoAStar(Node noDestino) {
    }

    private double getPathTargetX(Node no) {
        double centroLocal = bodyCollider != null
                ? bodyCollider.getOffsetX() + bodyCollider.getWidth() / 2.0
                : width / 2.0;
        return no.coluna * GameCore.tiles_size
                + GameCore.tiles_size / 2.0
                - centroLocal;
    }

    private double getPathTargetY(Node no) {
        double centroLocal = bodyCollider != null
                ? bodyCollider.getOffsetY() + bodyCollider.getHeight() / 2.0
                : height / 2.0;
        return no.linha * GameCore.tiles_size
                + GameCore.tiles_size / 2.0
                - centroLocal;
    }

    protected void iniciarSaltoCinematico(Node noDestino, int durationFrames) {
        this.emSaltoCinematico = true;
        this.lerpFrameAtual = 0;
        this.lerpFramesMax = durationFrames;
        this.lerpStartX = this.x;
        this.lerpStartY = this.y;

        double alvoX = getPathTargetX(noDestino);
        double alvoY = getPathTargetY(noDestino);
        this.lerpTargetX = alvoX;
        this.lerpTargetY = alvoY;

        this.velX = 0;
        this.velY = 0;
    }

    protected void executarSaltoCinematico() {
        if (!emSaltoCinematico) {
            return;
        }

        lerpFrameAtual++;
        double t = (double) lerpFrameAtual / lerpFramesMax;

        if (t >= 1.0) {
            t = 1.0;
            this.emSaltoCinematico = false;
            this.x = lerpTargetX;
            this.y = lerpTargetY;
            this.velX = 0;
            this.velY = 0;
            this.isAirborne = false;
            this.isCaindo = false;
            this.timerLedgeSnap = 0;
        } else {
            this.x = lerpStartX + (lerpTargetX - lerpStartX) * t;
            this.y = lerpStartY + (lerpTargetY - lerpStartY) * t;
        }
    }

    public void receberDano(int dano, double sourceX, double sourceY, double knockbackForce) {
        super.receberDano(dano);

        this.isPuxado = true;
        this.timerPuxado = 15;

        double meuCX = this.x + bodyCollider.getOffsetX() + (bodyCollider.getWidth() / 2.0);
        double meuCY = this.y + bodyCollider.getOffsetY() + (bodyCollider.getHeight() / 2.0);
        double dx = meuCX - sourceX;
        double dy = meuCY - sourceY;
        double dist = Math.hypot(dx, dy);

        if (dist > 0) {
            double forcaReal = knockbackForce / this.peso;
            this.velX += (dx / dist) * forcaReal;
            this.velY += (dy / dist) * forcaReal;
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(cor);
        g2.fill(new Rectangle2D.Double(x, y, width, height));
    }

    public double getLargura() {
        return width;
    }

    public double getAltura() {
        return height;
    }

    public void separarEmpilhamento(Entity outro) {
        if (this == outro || this.isDead || outro.isDead
                || this.bodyCollider == null || outro.bodyCollider == null) {
            return;
        }

        double meuLeft = this.x + bodyCollider.getOffsetX();
        double meuTop = this.y + bodyCollider.getOffsetY();
        double meuRight = meuLeft + bodyCollider.getWidth();
        double meuBottom = meuTop + bodyCollider.getHeight();

        double outroLeft = outro.x + outro.bodyCollider.getOffsetX();
        double outroTop = outro.y + outro.bodyCollider.getOffsetY();
        double outroRight = outroLeft + outro.bodyCollider.getWidth();
        double outroBottom = outroTop + outro.bodyCollider.getHeight();

        double overlapX = Math.min(meuRight, outroRight) - Math.max(meuLeft, outroLeft);
        double overlapY = Math.min(meuBottom, outroBottom) - Math.max(meuTop, outroTop);

        if (overlapX <= 0 || overlapY <= 0) {
            return;
        }

        double meuCX = (meuLeft + meuRight) / 2.0;
        double meuCY = (meuTop + meuBottom) / 2.0;
        double outroCX = (outroLeft + outroRight) / 2.0;
        double outroCY = (outroTop + outroBottom) / 2.0;
        double forcaRepulsao = 0.5 / Math.max(0.1, this.peso);

        if (overlapX < overlapY) {
            double direcao = meuCX < outroCX ? -1.0 : 1.0;
            this.velX += direcao * Math.min(1.0, overlapX / bodyCollider.getWidth()) * forcaRepulsao;
        } else {
            double direcao = meuCY < outroCY ? -1.0 : 1.0;
            this.velY += direcao * Math.min(1.0, overlapY / bodyCollider.getHeight()) * forcaRepulsao;
        }
    }

    public Boolean isMoving() {
        return (velX > 0.2 || velX < -0.2) || (velY > 0.2 || velY < -0.2);
    }
}
