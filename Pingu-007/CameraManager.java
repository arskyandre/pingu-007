import java.awt.geom.Rectangle2D;

public class CameraManager {
    private double x, y;
    private double zoom;
    public double pesoOffset = 0.25;
    // Margem da tela (em pixels) para o player não chegar perto da borda
    public double margemX = 180;
    public double margemY = 120;

    private double shakeIntensidade = 0;
    private int shakeTimer = 0;
    private int shakeDuracaoTotal = 0;
    private double shakeOffsetX = 0, shakeOffsetY = 0;

    private double focoAlvoX, focoAlvoY;
    private int focoTimer = 0;
    private double focoVelocidade = 0.18;
    private boolean foco_indefinido = false;

    private double zoomBase; // zoom "normal" desejado pelo jogo
    private double zoomFocoAlvo; // zoom alvo enquanto o foco estiver ativo

    // para o boss
    private boolean combatModeAtivo = false;
    private double combatX, combatY, combatW, combatH;
    private double combatVelocidade = 0.08;
    private double combatPadding = GameCore.tiles_size * 0.75;
    private double zoomMinimoCombate = 0.4;
    private boolean letterboxAtivo = false;
    private double letterboxAspect = 21.0 / 9.0;

    public CameraManager(double x, double y, double zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.zoomBase = zoom;
        this.zoomFocoAlvo = zoom;
    }

    public void setModoCombate(boolean set) {
        combatModeAtivo = set;
    }

    public void update(Player player, InputManager input, int telaLargura, int telaAltura) {
        double centroTelaX = telaLargura / 2.0;
        double centroTelaY = telaAltura / 2.0;

        double targetX;
        double targetY;
        double velocidade;

        if (focoTimer > 0 || foco_indefinido) {
            // Cutscene em andamento: ignora player e mouse, mira direto no alvo do foco
            targetX = focoAlvoX - (centroTelaX / zoom);
            targetY = focoAlvoY - (centroTelaY / zoom);
            velocidade = focoVelocidade;

            zoom += (zoomFocoAlvo - zoom) * velocidade;

        } else if (combatModeAtivo) {
            double minX = Math.min(player.getX(), combatX);
            double maxX = Math.max(player.getX() + player.getLargura(), combatX + combatW);
            double minY = Math.min(player.getY(), combatY);
            double maxY = Math.max(player.getY() + player.getAltura(), combatY + combatH);

            double rectW = maxX - minX;
            double rectH = maxY - minY;
            double centerX = (minX + maxX) / 2.0;
            double centerY = (minY + maxY) / 2.0;

            double alturaVisivel = getAlturaVisivel(telaLargura, telaAltura);

            double neededZoomW = telaLargura / (rectW + combatPadding * 2);
            double neededZoomH = alturaVisivel / (rectH + combatPadding * 2);
            double fitZoom = Math.min(neededZoomW, neededZoomH);

            double targetZoom = Math.min(fitZoom, zoomBase);
            targetZoom = Math.max(targetZoom, zoomMinimoCombate);

            targetX = centerX - (centroTelaX / zoom);
            targetY = centerY - (centroTelaY / zoom);
            velocidade = combatVelocidade;

            zoom += (targetZoom - zoom) * velocidade;

        } else {

            // distancia do mouse para o centro da tela
            double distMouseX = input.getMouseX() - centroTelaX;
            double distMouseY = input.getMouseY() - centroTelaY;

            // offset baseado no peso
            double telaOffsetX = distMouseX * pesoOffset;
            double telaOffsetY = distMouseY * pesoOffset;

            // limite maximo do offset baseado na margem
            double maxOffsetX = Math.max(0, centroTelaX - margemX);
            double maxOffsetY = Math.max(0, centroTelaY - margemY);

            // clamp do offset
            telaOffsetX = Math.max(-maxOffsetX, Math.min(telaOffsetX, maxOffsetX));
            telaOffsetY = Math.max(-maxOffsetY, Math.min(telaOffsetY, maxOffsetY));

            // centro do player no mundo
            double playerCentroX = player.getX() + (player.getLargura() / 2.0);
            double playerCentroY = player.getY() + (player.getAltura() / 2.0);

            targetX = playerCentroX - (centroTelaX / zoom) + (telaOffsetX / zoom);
            targetY = playerCentroY - (centroTelaY / zoom) + (telaOffsetY / zoom);
            velocidade = 0.1;

            if (Math.abs(zoom - zoomBase) > 0.0005) {
                zoom += (zoomBase - zoom) * 0.1;
            } else {
                zoom = zoomBase;
            }
        }
        focoTimer--;
        if (focoTimer == Integer.MIN_VALUE) {
            focoTimer = -1;
        }
        // suavização
        x += (targetX - x) * velocidade;
        y += (targetY - y) * velocidade;

        atualizarTremida();
    }

    private void atualizarTremida() {
        if (shakeTimer > 0) {
            // A força da tremida decai conforme o tempo restante acaba, em vez de
            // parar bruscamente
            double forcaAtual = shakeIntensidade * (shakeTimer / (double) shakeDuracaoTotal);
            shakeOffsetX = (Math.random() * 2 - 1) * forcaAtual;
            shakeOffsetY = (Math.random() * 2 - 1) * forcaAtual;
            shakeTimer--;
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }
    }

    public void tremer(double intensidade, int duracaoFrames) {
        if (duracaoFrames <= 0)
            return;

        if (shakeTimer <= 0 || intensidade >= shakeIntensidade) {
            this.shakeIntensidade = intensidade;
            this.shakeDuracaoTotal = duracaoFrames;
            this.shakeTimer = duracaoFrames;
        }
    }

    public void setLetterboxAtivo(boolean ativo) {
        this.letterboxAtivo = ativo;
    }

    public void setLetterboxAspect(double aspect) {
        this.letterboxAspect = aspect;
    }

    private double getAlturaVisivel(int telaLargura, int telaAltura) {
        if (!letterboxAtivo)
            return telaAltura;
        double alturaLetterbox = telaLargura / letterboxAspect;
        return Math.min(telaAltura, alturaLetterbox);
    }

    public void focarEm(double worldX, double worldY, int duracaoFrames, boolean tempo_indefinido) {
        this.foco_indefinido = tempo_indefinido;
        this.focoAlvoX = worldX;
        this.focoAlvoY = worldY;
        this.focoTimer = duracaoFrames;
        this.zoomFocoAlvo = zoomBase; // mantém o zoom normal
    }

    /** overload, foca ate rodar desfocarCamera() */
    public void focarEm(double worldX, double worldY) {
        this.foco_indefinido = true;
        this.focoAlvoX = worldX;
        this.focoAlvoY = worldY;
        this.focoTimer = 67;
        this.zoomFocoAlvo = zoomBase; // mantém o zoom normal
    }

    public void focarEmRect(Rectangle2D.Double rect, int duracaoFrames, int telaLargura, int telaAltura,
            boolean tempo_indefinido) {
        this.foco_indefinido = tempo_indefinido;
        double centerX = rect.x + rect.width / 2.0;
        double centerY = rect.y + rect.height / 2.0;

        this.focoAlvoX = centerX;
        this.focoAlvoY = centerY;
        this.focoTimer = duracaoFrames;

        double padding = GameCore.tiles_size * 2;
        double neededZoomW = telaLargura / (rect.width + padding * 2);
        double neededZoomH = telaAltura / (rect.height + padding * 2);
        double fitZoom = Math.min(neededZoomW, neededZoomH);

        this.zoomFocoAlvo = Math.min(fitZoom, zoomBase);
    }

    public void desfocarCamera() {
        foco_indefinido = false;
        focoTimer = 0;
    }

    public void resetCameraState(double playerX, double playerY, double playerWidth, double playerHeight,
            int telaLargura, int telaAltura) {
        combatModeAtivo = false;
        foco_indefinido = false;
        focoTimer = 0;
        letterboxAtivo = false;
        shakeTimer = 0;
        shakeOffsetX = 0;
        shakeOffsetY = 0;
        clearCombatTarget();
        zoom = zoomBase;
        zoomFocoAlvo = zoomBase;

        double centroTelaX = telaLargura / 2.0;
        double centroTelaY = telaAltura / 2.0;
        double playerCentroX = playerX + playerWidth / 2.0;
        double playerCentroY = playerY + playerHeight / 2.0;

        this.x = playerCentroX - (centroTelaX / zoom);
        this.y = playerCentroY - (centroTelaY / zoom);
    }

    // Útil para, por exemplo, travar o input do player enquanto a cutscene roda.
    public boolean emFoco() {
        return focoTimer > 0;
    }

    public void setCombatTarget(double bossX, double bossY, double bossWidth, double bossHeight) {
        this.combatModeAtivo = true;
        this.combatX = bossX;
        this.combatY = bossY;
        this.combatW = bossWidth;
        this.combatH = bossHeight;
    }

    public void clearCombatTarget() {
        this.combatModeAtivo = false;
    }

    public boolean isCombatModeAtivo() {
        return combatModeAtivo;
    }

    public void setZoomMinimoCombate(double zoomMinimo) {
        this.zoomMinimoCombate = zoomMinimo;
    }

    public void setCombatPadding(double padding) {
        this.combatPadding = padding;
    }

    public boolean onScreen(double objX, double objY, double objW, double objH, int telaLargura, int telaAltura) {
        // Calcula os limites reais da visão da câmera no mundo, considerando o zoom
        // e a tremida atual
        double viewLeft = getX();
        double viewTop = getY();
        double viewRight = viewLeft + (telaLargura / this.zoom);
        double viewBottom = viewTop + (telaAltura / this.zoom);

        // Retorna true se o retângulo do objeto colidir com o retângulo da câmera
        // Isso cobre tanto objetos totalmente dentro quanto parcialmente dentro
        return (objX < viewRight
                && objX + objW > viewLeft
                && objY < viewBottom
                && objY + objH > viewTop);
    }

    public double getX() {
        return x + shakeOffsetX;
    }

    public double getY() {
        return y + shakeOffsetY;
    }

    public double getZoom() {
        return zoom;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    // Chamado todo frame pelo GameCore com o zoom "desejado" (baseado na
    // resolução da janela). Não força o zoom instantaneamente — apenas
    // atualiza o alvo que update() persegue suavemente.
    public void setBaseZoom(double zoom) {
        this.zoomBase = zoom;
    }
}