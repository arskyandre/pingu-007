
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public abstract class Entity implements Renderable {

    private static final double SWEEP_EPSILON = 0.01;
    private static final int SWEEP_SEARCH_ITERATIONS = 24;

    protected double x, y;
    protected double velX, velY;

    protected Collider bodyCollider;
    protected Collider hurtbox;
    protected Collider hitbox;

    protected int vidaMaxima;
    protected int vida;
    protected boolean isDead = false;

    protected double aceleracao = 1.0;
    protected double atritoAtual = 0.85;
    protected double velocidadeAndar = 5.0;
    protected double velocidadeMax = 30.0;
    protected double peso = 1.0;

    protected boolean isAirborne = false;
    protected boolean isPuxado = false;
    protected boolean isSlippery = false;
    protected boolean isCaindo = false;
    protected boolean podeAtravessarObjetosTransparentes = false;
    protected int timerQueda = 0;
    protected int timerLedgeSnap = 0;
    protected int timerDano = 0;
    protected int tempoDano = 12;

    protected double ultimoXSeguro;
    protected double ultimoYSeguro;

    public boolean no_clip = false;

    public SoundManager soundManager;
    public ArenaManager arenaManager;
    protected int snowFootstepTimer = 0;
    protected int snowFootstepInterval = 22;
    protected int iceFootstepTimer = 0;
    protected int iceFootstepInterval = 50;

    public Entity(ArenaManager arenaManager, SoundManager sound) {
        this.arenaManager = arenaManager;
        this.soundManager = sound;
    }

    public void receberDano(int dano) {
        vida -= dano;
        if (vida <= 0) {
            vida = 0;
            isDead = true;
        }
        timerDano = tempoDano;
    }

    public boolean isDead() {
        return isDead;
    }

    protected void updateFootsteps(SoundManager sound, int[][] lvlData) {
        if (lvlData == null || bodyCollider == null) {
            return;
        }

        int centroCol = (int) ((x + bodyCollider.getOffsetX() + bodyCollider.getWidth() / 2.0) / GameCore.tiles_size);
        int centroRow = (int) ((y + bodyCollider.getOffsetY() + bodyCollider.getHeight() / 2.0) / GameCore.tiles_size);

        if (centroRow < 0 || centroRow >= lvlData.length || centroCol < 0 || centroCol >= lvlData[0].length) {
            return;
        }

        int tile = lvlData[centroRow][centroCol];

        snowFootstepTimer--;
        iceFootstepTimer--;
        if (snowFootstepTimer <= 0) {
            if (!TileProperties.isHole(tile) && !TileProperties.isIce(tile)) {
                sound.playRandomSnowStep();
            }
            snowFootstepTimer = snowFootstepInterval;
        }
        if (iceFootstepTimer <= 0) {

            if (!TileProperties.isHole(tile) && TileProperties.isIce(tile)) {
                sound.playRandomIceStep();
            }
            iceFootstepTimer = iceFootstepInterval;
        }
    }

    @Override
    public abstract void draw(Graphics2D g2, double delta);

    public void dmgCheck() {
        if (timerDano > 0) {
            timerDano--;
        }
    }

    protected void aplicarFisicaBasica() {

        double mult = no_clip ? 4.0 : 1.0;
        double limiteAndar = velocidadeAndar * mult;
        double limiteMaximo = velocidadeMax * mult;

        if (!isAirborne && !isPuxado) {
            limitarMagnitudeVelocidade(limiteAndar);
        }

        limitarMagnitudeVelocidade(limiteMaximo);

        velX *= atritoAtual;
        velY *= atritoAtual;

        double threshold = isSlippery ? 0.001 : 0.01;
        if (Math.abs(velX) < threshold) {
            velX = 0;
        }
        if (Math.abs(velY) < threshold) {
            velY = 0;
        }
    }

    protected void limitarMagnitudeVelocidade(double limite) {
        if (limite <= 0) {
            velX = 0;
            velY = 0;
            return;
        }

        double magnitude = Math.hypot(velX, velY);
        if (magnitude > limite) {
            double escala = limite / magnitude;
            velX *= escala;
            velY *= escala;
        }
    }

    protected boolean colideComObjetos(double nextX, double nextY, double width, double height, ArrayList<MapObject> objetos) {
        if (objetos == null || objetos.isEmpty()) {
            return false;
        }

        Rectangle2D.Double hitboxFutura = new Rectangle2D.Double(nextX, nextY, width, height);
        Rectangle2D.Double hitboxAtual = new Rectangle2D.Double(
                x + bodyCollider.getOffsetX(),
                y + bodyCollider.getOffsetY(),
                bodyCollider.getWidth(),
                bodyCollider.getHeight());

        for (MapObject obj : objetos) {
            if (obj == null || !obj.isSolid() || obj.getHitbox() == null) {
                continue;
            }

            if (podeAtravessarObjetosTransparentes && obj.isTransparent()
                    && (isAirborne || obj.getHitbox().intersects(hitboxAtual))) {
                continue;
            }

            if (obj.getHitbox().intersects(hitboxFutura)) {
                return true;
            }
        }
        return false;
    }

    private boolean deveIgnorarObjetoNoSweep(
            MapObject obj,
            Rectangle2D.Double hitboxAtual) {

        if (obj == null || !obj.isSolid() || obj.getHitbox() == null) {
            return true;
        }

        if (obj.getHitbox().intersects(hitboxAtual)) {
            return true;
        }

        return podeAtravessarObjetosTransparentes
                && obj.isTransparent()
                && isAirborne;
    }

    private double limitarDeslocamentoHorizontal(
            double cbX, double cbY,
            double cbW, double cbH,
            double deslocamento,
            ArrayList<MapObject> objetos) {

        if (deslocamento == 0.0 || objetos == null || objetos.isEmpty()) {
            return deslocamento;
        }

        Rectangle2D.Double hitboxAtual = new Rectangle2D.Double(cbX, cbY, cbW, cbH);
        double distancia = Math.abs(deslocamento);
        boolean movendoDireita = deslocamento > 0.0;
        double inicioCorredor = movendoDireita ? cbX + cbW : cbX - distancia;
        Rectangle2D.Double corredor = new Rectangle2D.Double(
                inicioCorredor, cbY, distancia, cbH);
        double menorFracao = 1.0;

        for (MapObject obj : objetos) {
            if (deveIgnorarObjetoNoSweep(obj, hitboxAtual)
                    || !obj.getHitbox().intersects(corredor)) {
                continue;
            }

            double inicio = 0.0;
            double fim = 1.0;

            for (int i = 0; i < SWEEP_SEARCH_ITERATIONS; i++) {
                double meio = (inicio + fim) / 2.0;
                double largura = distancia * meio;
                double xCorredor = movendoDireita
                        ? cbX + cbW
                        : cbX - largura;
                Rectangle2D.Double corredorParcial = new Rectangle2D.Double(
                        xCorredor, cbY, largura, cbH);

                if (obj.getHitbox().intersects(corredorParcial)) {
                    fim = meio;
                } else {
                    inicio = meio;
                }
            }

            menorFracao = Math.min(menorFracao, fim);
        }

        if (menorFracao >= 1.0) {
            return deslocamento;
        }

        double distanciaPermitida = Math.max(
                0.0,
                distancia * menorFracao - SWEEP_EPSILON);
        return movendoDireita ? distanciaPermitida : -distanciaPermitida;
    }

    private double limitarDeslocamentoVertical(
            double cbX, double cbY,
            double cbW, double cbH,
            double deslocamento,
            ArrayList<MapObject> objetos) {

        if (deslocamento == 0.0 || objetos == null || objetos.isEmpty()) {
            return deslocamento;
        }

        Rectangle2D.Double hitboxAtual = new Rectangle2D.Double(cbX, cbY, cbW, cbH);
        double distancia = Math.abs(deslocamento);
        boolean movendoBaixo = deslocamento > 0.0;
        double inicioCorredor = movendoBaixo ? cbY + cbH : cbY - distancia;
        Rectangle2D.Double corredor = new Rectangle2D.Double(
                cbX, inicioCorredor, cbW, distancia);
        double menorFracao = 1.0;

        for (MapObject obj : objetos) {
            if (deveIgnorarObjetoNoSweep(obj, hitboxAtual)
                    || !obj.getHitbox().intersects(corredor)) {
                continue;
            }

            double inicio = 0.0;
            double fim = 1.0;

            for (int i = 0; i < SWEEP_SEARCH_ITERATIONS; i++) {
                double meio = (inicio + fim) / 2.0;
                double altura = distancia * meio;
                double yCorredor = movendoBaixo
                        ? cbY + cbH
                        : cbY - altura;
                Rectangle2D.Double corredorParcial = new Rectangle2D.Double(
                        cbX, yCorredor, cbW, altura);

                if (obj.getHitbox().intersects(corredorParcial)) {
                    fim = meio;
                } else {
                    inicio = meio;
                }
            }

            menorFracao = Math.min(menorFracao, fim);
        }

        if (menorFracao >= 1.0) {
            return deslocamento;
        }

        double distanciaPermitida = Math.max(
                0.0,
                distancia * menorFracao - SWEEP_EPSILON);
        return movendoBaixo ? distanciaPermitida : -distanciaPermitida;
    }

    protected void moveAndCollideWithMap(int[][] lvlData, ArrayList<MapObject> objetosDeCenario) {
        if (no_clip) {
            this.x += velX;
            this.y += velY;
            return;
        }

        if (this.isAirborne) {
            this.timerLedgeSnap = isSlippery ? 18 : 10;
        } else if (this.timerLedgeSnap > 0) {
            this.timerLedgeSnap--;
        }

        limitarMagnitudeVelocidade(velocidadeMax);

        double cbW = bodyCollider.getWidth();
        double cbH = bodyCollider.getHeight();
        double maxVel = Math.hypot(velX, velY);

        double maxCollisionStep = Math.max(1.0, GameCore.tiles_size / 2.0);
        int steps = (int) Math.ceil(maxVel / maxCollisionStep);
        if (steps < 1) {
            steps = 1;
        }

        double stepX = velX / steps;
        double stepY = velY / steps;

        for (int i = 0; i < steps; i++) {
            double cbX = x + bodyCollider.getOffsetX();
            double cbY = y + bodyCollider.getOffsetY();

            double movimentoPermitidoX = limitarDeslocamentoHorizontal(
                    cbX, cbY, cbW, cbH, stepX, objetosDeCenario);
            double proxX = cbX + stepX;

            // HORIZONTAL
            boolean colidiuTileX = !canMoveHere(proxX, cbY, cbW, cbH, lvlData);
            boolean colidiuObjX = Math.abs(movimentoPermitidoX - stepX) > 0.000001;

            if (colidiuTileX || colidiuObjX) {
                if (colidiuObjX && !colidiuTileX) {
                    x += movimentoPermitidoX;
                } else if (colidiuTileX && !colidiuObjX) {
                    if (stepX > 0) {
                        int tileX = (int) ((proxX + cbW - 0.1) / GameCore.tiles_size);
                        x = tileX * GameCore.tiles_size - cbW - 0.1 - bodyCollider.getOffsetX();
                    } else if (stepX < 0) {
                        int tileX = (int) (proxX / GameCore.tiles_size);
                        x = (tileX + 1) * GameCore.tiles_size + 0.1 - bodyCollider.getOffsetX();
                    }
                }

                velX = 0;
                stepX = 0;
                if (isAirborne) {
                    isAirborne = false;
                    renovarTimerLedgeSnapPosAterragem(lvlData);
                }
            } else {
                x += stepX;
            }
            cbX = x + bodyCollider.getOffsetX();
            cbY = y + bodyCollider.getOffsetY();

            double movimentoPermitidoY = limitarDeslocamentoVertical(
                    cbX, cbY, cbW, cbH, stepY, objetosDeCenario);
            double proxY = cbY + stepY;

            // VERTICAL
            boolean colidiuTileY = !canMoveHere(cbX, proxY, cbW, cbH, lvlData);
            boolean colidiuObjY = Math.abs(movimentoPermitidoY - stepY) > 0.000001;

            if (colidiuTileY || colidiuObjY) {
                if (colidiuObjY && !colidiuTileY) {
                    y += movimentoPermitidoY;
                } else if (colidiuTileY && !colidiuObjY) {
                    if (stepY > 0) {
                        int tileY = (int) ((proxY + cbH - 0.1) / GameCore.tiles_size);
                        y = tileY * GameCore.tiles_size - cbH - 0.1 - bodyCollider.getOffsetY();
                    } else if (stepY < 0) {
                        int tileY = (int) (proxY / GameCore.tiles_size);
                        y = (tileY + 1) * GameCore.tiles_size + 0.1 - bodyCollider.getOffsetY();
                    }
                }

                velY = 0;
                stepY = 0;
                if (isAirborne) {
                    isAirborne = false;
                    renovarTimerLedgeSnapPosAterragem(lvlData);
                }
            } else {
                y += stepY;
            }

            if (stepX == 0 && stepY == 0) {
                break;
            }
        }

        // BURACOS
        if (!isAirborne && !isCaindo) {
            double cbX = x + bodyCollider.getOffsetX();
            double cbY = y + bodyCollider.getOffsetY();
            double centroX = cbX + bodyCollider.getWidth() / 2.0;
            double centroY = cbY + bodyCollider.getHeight() / 2.0;

            int colAtual = (int) (centroX / GameCore.tiles_size);
            int rowAtual = (int) (centroY / GameCore.tiles_size);

            if (rowAtual >= 0 && rowAtual < lvlData.length && colAtual >= 0 && colAtual < lvlData[0].length) {
                if (TileProperties.isHole(lvlData[rowAtual][colAtual])) {
                    boolean salvoPelaBorda = false;
                    if (this.timerLedgeSnap > 0) {
                        salvoPelaBorda = verificarEApplicarLedgeSnap(
                                lvlData, objetosDeCenario);
                    }
                    if (!salvoPelaBorda) {
                        this.isCaindo = true;
                    }
                }
            }
        }

        atualizarFisicaDoChao(lvlData);
    }

    protected boolean canMoveHere(double nextX, double nextY, double width, double height, int[][] lvlData) {
        int leftCol = (int) (nextX / GameCore.tiles_size);
        int rightCol = (int) ((nextX + width - 0.1) / GameCore.tiles_size);
        int topRow = (int) (nextY / GameCore.tiles_size);
        int bottomRow = (int) ((nextY + height - 0.1) / GameCore.tiles_size);

        if (leftCol < 0 || rightCol >= lvlData[0].length || topRow < 0 || bottomRow >= lvlData.length) {
            return false;
        }

        return !isBlocked(lvlData[topRow][leftCol])
                && !isBlocked(lvlData[topRow][rightCol])
                && !isBlocked(lvlData[bottomRow][leftCol])
                && !isBlocked(lvlData[bottomRow][rightCol]);
    }

    private boolean isBlocked(int tileID) {
        if (TileProperties.isSolid(tileID)) {
            return true;
        }
        if (TileProperties.isSemiSolid(tileID)) {
            if (isAirborne || isPuxado || isCaindo) {
                return false;
            }
            return !(isSlippery && TileProperties.isHole(tileID));
        }
        return false;
    }

    private boolean destinoDoLedgeSnapEstaLivre(
            double destinoX,
            double destinoY,
            ArrayList<MapObject> objetosDeCenario) {

        if (objetosDeCenario == null || objetosDeCenario.isEmpty()) {
            return true;
        }

        double cbX = x + bodyCollider.getOffsetX();
        double cbY = y + bodyCollider.getOffsetY();
        double cbW = bodyCollider.getWidth();
        double cbH = bodyCollider.getHeight();
        double destinoCbX = destinoX + bodyCollider.getOffsetX();
        double destinoCbY = destinoY + bodyCollider.getOffsetY();

        double deslocamentoX = destinoCbX - cbX;
        double permitidoX = limitarDeslocamentoHorizontal(
                cbX, cbY, cbW, cbH, deslocamentoX, objetosDeCenario);
        if (Math.abs(permitidoX - deslocamentoX) > 0.000001) {
            return false;
        }

        double deslocamentoY = destinoCbY - cbY;
        double permitidoY = limitarDeslocamentoVertical(
                destinoCbX, cbY, cbW, cbH, deslocamentoY, objetosDeCenario);
        if (Math.abs(permitidoY - deslocamentoY) > 0.000001) {
            return false;
        }

        Rectangle2D.Double hitboxDestino = new Rectangle2D.Double(
                destinoCbX, destinoCbY, cbW, cbH);

        for (MapObject obj : objetosDeCenario) {
            if (obj == null || !obj.isSolid() || obj.getHitbox() == null) {
                continue;
            }

            if (podeAtravessarObjetosTransparentes
                    && obj.isTransparent()
                    && isAirborne) {
                continue;
            }

            if (obj.getHitbox().intersects(hitboxDestino)) {
                return false;
            }
        }

        return true;
    }

    protected boolean verificarEApplicarLedgeSnap(
            int[][] lvlData,
            ArrayList<MapObject> objetosDeCenario) {
        if (this.bodyCollider == null || this.isDead) {
            return false;
        }

        double colX = this.x + this.bodyCollider.getOffsetX();
        double colY = this.y + this.bodyCollider.getOffsetY();
        double colW = this.bodyCollider.getWidth();
        double colH = this.bodyCollider.getHeight();

        double areaTotal = colW * colH;

        int startCol = (int) (colX / GameCore.tiles_size);
        int endCol = (int) ((colX + colW) / GameCore.tiles_size);
        int startRow = (int) (colY / GameCore.tiles_size);
        int endRow = (int) ((colY + colH) / GameCore.tiles_size);

        double areaSeguraAcumulada = 0;
        double melhorDestinoX = Double.NaN;
        double melhorDestinoY = Double.NaN;
        double menorDistanciaCentro = Double.MAX_VALUE;

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (r >= 0 && r < lvlData.length && c >= 0 && c < lvlData[0].length) {
                    int tile = lvlData[r][c];

                    if (!TileProperties.isHole(tile) && !TileProperties.isSolid(tile)) {

                        double tileX = c * GameCore.tiles_size;
                        double tileY = r * GameCore.tiles_size;

                        double interX = Math.max(colX, tileX);
                        double interY = Math.max(colY, tileY);
                        double interW = Math.min(colX + colW, tileX + GameCore.tiles_size) - interX;
                        double interH = Math.min(colY + colH, tileY + GameCore.tiles_size) - interY;

                        if (interW > 0 && interH > 0) {
                            areaSeguraAcumulada += (interW * interH);

                            double centroTileX = tileX + GameCore.tiles_size / 2.0;
                            double centroTileY = tileY + GameCore.tiles_size / 2.0;

                            if (isSlippery) {
                                centroTileX -= Math.signum(velX)
                                        * Math.min(Math.abs(velX) * 1.5, GameCore.tiles_size * 0.15);
                                centroTileY -= Math.signum(velY)
                                        * Math.min(Math.abs(velY) * 1.5, GameCore.tiles_size * 0.15);
                            }

                            double destinoX = centroTileX
                                    - (bodyCollider.getOffsetX() + colW / 2.0);
                            double destinoY = centroTileY
                                    - (bodyCollider.getOffsetY() + colH / 2.0);

                            if (!destinoDoLedgeSnapEstaLivre(
                                    destinoX, destinoY, objetosDeCenario)) {
                                continue;
                            }

                            double centroEntidadeX = colX + colW / 2.0;
                            double centroEntidadeY = colY + colH / 2.0;

                            double dist = Math.hypot(centroTileX - centroEntidadeX, centroTileY - centroEntidadeY);

                            if (dist < menorDistanciaCentro) {
                                menorDistanciaCentro = dist;
                                melhorDestinoX = destinoX;
                                melhorDestinoY = destinoY;
                            }
                        }
                    }
                }
            }
        }

        double limiarSnap = isSlippery ? 0.30 : 0.40;
        if (isSlippery && (Math.abs(velX) > 0.5 || Math.abs(velY) > 0.5)) {
            limiarSnap = 0.22;
        }

        if ((areaSeguraAcumulada / areaTotal) >= limiarSnap
                && !Double.isNaN(melhorDestinoX)) {
            this.x = melhorDestinoX;
            this.y = melhorDestinoY;

            this.velX = 0;
            this.velY = 0;
            this.isAirborne = false;
            this.isCaindo = false;

            return true;
        }

        return false;
    }

    protected void renovarTimerLedgeSnapPosAterragem(int[][] lvlData) {
        int centroCol = (int) ((x + bodyCollider.getOffsetX() + bodyCollider.getWidth() / 2.0) / GameCore.tiles_size);
        int centroRow = (int) ((y + bodyCollider.getOffsetY() + bodyCollider.getHeight() / 2.0) / GameCore.tiles_size);

        boolean aterrouNoGelo = false;
        if (centroRow >= 0 && centroRow < lvlData.length && centroCol >= 0 && centroCol < lvlData[0].length) {
            aterrouNoGelo = TileProperties.isIce(lvlData[centroRow][centroCol]);
        }

        this.timerLedgeSnap = aterrouNoGelo ? 20 : 12;
    }

    protected double[] preverDeslocamentoGelo(int frames) {
        double projX = velX;
        double projY = velY;
        for (int i = 0; i < frames; i++) {
            projX *= atritoAtual;
            projY *= atritoAtual;
        }
        return new double[]{projX * frames * 0.35, projY * frames * 0.35};
    }

    protected void atualizarFisicaDoChao(int[][] lvlData) {
        int centroCol = (int) ((x + bodyCollider.getOffsetX() + bodyCollider.getWidth() / 2.0) / GameCore.tiles_size);
        int centroRow = (int) ((y + bodyCollider.getOffsetY() + bodyCollider.getHeight() / 2.0) / GameCore.tiles_size);

        if (centroCol < 0 || centroCol >= lvlData[0].length || centroRow < 0 || centroRow >= lvlData.length) {
            isCaindo = true;
        } else {
            int tileAtual = lvlData[centroRow][centroCol];

            if (TileProperties.isIce(tileAtual)) {
                isSlippery = true;
                atritoAtual = 0.98;
            } else if (!TileProperties.isHole(tileAtual)) {
                isSlippery = false;
                atritoAtual = 0.85;
            }

            if (TileProperties.isHole(tileAtual)) {
                if (!isAirborne) {
                    isCaindo = true;
                }
            } else {
                if (!isAirborne && !isPuxado && !isCaindo) {
                    ultimoXSeguro = x;
                    ultimoYSeguro = y;
                } else if (!TileProperties.isHole(tileAtual) && isCaindo) {
                    isCaindo = false;
                    timerQueda = 0;
                }
            }
        }

        if (isCaindo) {
            timerQueda++;
            velX *= 0.6;
            velY *= 0.6;

            if (timerQueda >= 15) {
                processarQuedaNoAbismo();
                isCaindo = false;
                timerQueda = 0;
            }
        }
    }

    protected void processarQuedaNoAbismo() {
        velX = 0;
        velY = 0;

        if (this instanceof Player) {
            receberDano(10);
            if (this.vida <= 0) {
                this.vida = 0;
                this.isDead = true;
            }
            x = ultimoXSeguro;
            y = ultimoYSeguro;
        } else {
            vida = 0;
            isDead = true;
        }
    }

    public boolean checkAreaCollision(Entity other) {
        if (hitbox == null || other.hurtbox == null) {
            return false;
        }
        return hitbox.intersects(x, y, other.hurtbox, other.x, other.y);
    }

    public void setX(double x) {
        this.x = x;
        this.ultimoXSeguro = x;
    }

    public void setY(double y) {
        this.y = y;
        this.ultimoYSeguro = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getVida() {
        return vida;
    }

    public int getVidaMax() {
        return vidaMaxima;
    }

    public Collider getHitbox() {
        return hitbox;
    }

    public Collider getHurtbox() {
        return hurtbox;
    }

    public Collider getBodyCollider() {
        return bodyCollider;
    }

    @Override
    public double getProfundidade() {
        if (bodyCollider != null) {
            return this.y + bodyCollider.getOffsetY() + bodyCollider.getHeight();
        }
        return this.y;
    }
}
