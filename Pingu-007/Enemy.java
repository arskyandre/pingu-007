
import java.awt.Color;
import java.awt.Graphics2D;
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
    protected int pathfindingCooldown = 0;
    public boolean podePularBuracos = false;
    public boolean isInvulneravel = false;

    protected int timerPuxado = 0;

    public Enemy(double startX, double startY, double width, double height, int[][] lvlData) {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.lvlData = lvlData;
        this.peso = 1.0;
        this.pathfindingCooldown = (int) (Math.random() * tempoRecalculoAStar);
    }

    public abstract void update(Player player, ArrayList<JumpLink> jumpLinks);

    protected boolean temLinhaDeVisaoLivre(Player player) {
        double x0 = this.x + this.width / 2.0;
        double y0 = this.y + this.height / 2.0;
        double x1 = player.getX() + player.getLargura() / 2.0;
        double y1 = player.getY() + player.getAltura() / 2.0;

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dist = Math.hypot(dx, dy);

        int steps = (int) (dist / 16.0);
        for (int i = 0; i <= steps; i++) {
            double checkX = x0 + (dx * i) / steps;
            double checkY = y0 + (dy * i) / steps;
            int col = (int) (checkX / GameCore.tiles_size);
            int row = (int) (checkY / GameCore.tiles_size);

            if (row >= 0 && row < lvlData.length && col >= 0 && col < lvlData[0].length) {
                int tile = lvlData[row][col];
                if (TileProperties.isSolid(tile) || TileProperties.isHole(tile)) {
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

    protected void atualizarTimersKnockback() {
        if (isPuxado) {
            timerPuxado--;
            if (timerPuxado <= 0) {
                isPuxado = false;
                // Força o recálculo do A* imediatamente após o atordoamento para ele não ir contra a parede
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

        if (isAirborne && Math.abs(velX) < 1.0 && Math.abs(velY) < 1.0) {
            this.isAirborne = false;
        }

        double meuCenterX = this.x + (this.bodyCollider != null ? this.bodyCollider.getOffsetX() + this.bodyCollider.getWidth() / 2.0 : this.width / 2.0);
        double meuCenterY = this.y + (this.bodyCollider != null ? this.bodyCollider.getOffsetY() + this.bodyCollider.getHeight() / 2.0 : this.height / 2.0);
        double playerCenterX = player.getX() + player.getLargura() / 2.0;
        double playerCenterY = player.getY() + player.getAltura() / 2.0;

        double distToPlayer = Math.hypot(playerCenterX - meuCenterX, playerCenterY - meuCenterY);

        if (!isAirborne && distToPlayer < (GameCore.tiles_size * 6) && temLinhaDeVisaoLivre(player)) {
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
            if (aStarDelay >= tempoRecalculoAStar || caminhoAStar == null || currentPathIndex >= caminhoAStar.size()) {
                int startCol = (int) (meuCenterX / GameCore.tiles_size);
                int startRow = (int) (meuCenterY / GameCore.tiles_size);
                int targetCol = (int) (playerCenterX / GameCore.tiles_size);
                int targetRow = (int) (playerCenterY / GameCore.tiles_size);

                caminhoAStar = PathFinder.encontrarCaminho(startCol, startRow, targetCol, targetRow, this.lvlData, this.podePularBuracos ? jumpLinks : null);
                aStarDelay = 0;
                currentPathIndex = 0;
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
        double alvoX = proximoNo.coluna * GameCore.tiles_size + (GameCore.tiles_size / 2.0) - (this.width / 2.0);
        double alvoY = proximoNo.linha * GameCore.tiles_size + (GameCore.tiles_size / 2.0) - (this.height / 2.0);

        double dx = alvoX - this.x;
        double dy = alvoY - this.y;
        double dist = Math.hypot(dx, dy);

        double landingMargin = (GameCore.tiles_size / 2.0) + 5.0;

        if (dist < landingMargin) {
            currentPathIndex++;

            if (currentPathIndex < caminhoAStar.size()) {
                Node proximo = caminhoAStar.get(currentPathIndex);
                if (proximo.requerSalto) {
                    iniciarSaltoAStar(proximo);
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

    protected void iniciarSaltoAStar(Node noDestino) {
        this.isAirborne = true;
        double alvoX = noDestino.coluna * GameCore.tiles_size + (GameCore.tiles_size / 2.0) - (this.width / 2.0);
        double alvoY = noDestino.linha * GameCore.tiles_size + (GameCore.tiles_size / 2.0) - (this.height / 2.0);
        double dx = alvoX - this.x;
        double dy = alvoY - this.y;

        double dist = Math.hypot(dx, dy);

        if (dist > 0) {
            double forcaDoPulo = dist * 0.18;
            this.velocidadeMax = Math.max(this.velocidadeMax, forcaDoPulo + 5.0);
            this.velX = (dx / dist) * forcaDoPulo;
            this.velY = (dy / dist) * forcaDoPulo;
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
        if (this == outro || this.isDead || outro.isDead) {
            return;
        }

        double meuCX = this.x + bodyCollider.getOffsetX() + (bodyCollider.getWidth() / 2.0);
        double meuCY = this.y + bodyCollider.getOffsetY() + (bodyCollider.getHeight() / 2.0);
        double outroCX = outro.x + outro.bodyCollider.getOffsetX() + (outro.bodyCollider.getWidth() / 2.0);
        double outroCY = outro.y + outro.bodyCollider.getOffsetY() + (outro.bodyCollider.getHeight() / 2.0);

        double dx = meuCX - outroCX;
        double dy = meuCY - outroCY;
        double dist = Math.hypot(dx, dy);
        double distMin = (this.bodyCollider.getWidth() / 2.0) + (outro.bodyCollider.getWidth() / 2.0);

        if (dist < distMin) {
            if (dist == 0.0) {
                dx = Math.random() - 0.5;
                dy = Math.random() - 0.5;
                dist = Math.hypot(dx, dy);
            }
            double intensidade = (distMin - dist) / distMin;
            double forcaRepulsao = 0.5;
            this.velX += (dx / dist) * intensidade * forcaRepulsao / this.peso;
            this.velY += (dy / dist) * intensidade * forcaRepulsao / this.peso;
        }
    }
}
