import java.awt.geom.Rectangle2D;

public class CameraManager {
    private double x, y;
    private double zoom;
    public double pesoOffset = 0.25;
    // Margem da tela (em pixels) para o player não chegar perto da borda
    public double margemX = 180;
    public double margemY = 120;

    // --- Tremida de câmera (avisos de ataque, impactos, etc.) ---
    private double shakeIntensidade = 0;
    private int shakeTimer = 0;
    private int shakeDuracaoTotal = 0;
    private double shakeOffsetX = 0, shakeOffsetY = 0;

    // --- Foco temporário de câmera (cutscene de entrada na arena, etc.) ---
    private double focoAlvoX, focoAlvoY;
    private int focoTimer = 0;
    private double focoVelocidade = 0.18; // mais rápido que o acompanhamento normal do player (0.1)

    // --- Zoom dinâmico (normal vs. foco com zoom-out para caber um retângulo) ---
    private double zoomBase; // zoom "normal" desejado pelo jogo (atualizado todo frame pelo GameCore)
    private double zoomFocoAlvo; // zoom alvo enquanto o foco estiver ativo

    public CameraManager(double x, double y, double zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.zoomBase = zoom;
        this.zoomFocoAlvo = zoom;
    }

    public void update(Player player, InputManager input, int telaLargura, int telaAltura) {
        double centroTelaX = telaLargura / 2.0;
        double centroTelaY = telaAltura / 2.0;

        double targetX;
        double targetY;
        double velocidade;

        if (focoTimer > 0) {
            // Cutscene em andamento: ignora player e mouse, mira direto no alvo do foco
            targetX = focoAlvoX - (centroTelaX / zoom);
            targetY = focoAlvoY - (centroTelaY / zoom);
            velocidade = focoVelocidade;

            zoom += (zoomFocoAlvo - zoom) * velocidade;

            focoTimer--;
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

            // fora do foco, zoom acompanha o zoomBase; se ainda estiver "voltando"
            // de um foco com zoom-out, suaviza o retorno em vez de saltar
            if (Math.abs(zoom - zoomBase) > 0.0005) {
                zoom += (zoomBase - zoom) * 0.1;
            } else {
                zoom = zoomBase;
            }
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

    // Dispara uma tremida de câmera. Se já tem uma tremida mais forte em andamento,
    // essa chamada é ignorada para não "suavizar" um impacto grande com um menor.
    public void tremer(double intensidade, int duracaoFrames) {
        if (duracaoFrames <= 0)
            return;

        if (shakeTimer <= 0 || intensidade >= shakeIntensidade) {
            this.shakeIntensidade = intensidade;
            this.shakeDuracaoTotal = duracaoFrames;
            this.shakeTimer = duracaoFrames;
        }
    }

    // Faz a câmera parar de seguir o player e focar rapidamente em um ponto do
    // mundo por um tempo (cutscene de entrada na arena do boss, por exemplo).
    // Ao acabar a duração, ela volta a seguir o player normalmente e suavemente.
    // Não altera o zoom.
    public void focarEm(double worldX, double worldY, int duracaoFrames) {
        this.focoAlvoX = worldX;
        this.focoAlvoY = worldY;
        this.focoTimer = duracaoFrames;
        this.zoomFocoAlvo = zoomBase; // mantém o zoom normal
    }

    // Igual a focarEm, mas também calcula (e aplica com zoom-out suave) o zoom
    // necessário para que todo o retângulo dado caiba na tela, com uma margem.
    // Nunca faz zoom-in além do zoom normal — só reduz o zoom se o retângulo
    // for maior do que a área visível atual.
    public void focarEmRect(Rectangle2D.Double rect, int duracaoFrames, int telaLargura, int telaAltura) {
        double centerX = rect.x + rect.width / 2.0;
        double centerY = rect.y + rect.height / 2.0;

        this.focoAlvoX = centerX;
        this.focoAlvoY = centerY;
        this.focoTimer = duracaoFrames;

        double padding = GameCore.tiles_size * 2; // margem ao redor da(s) parede(s)
        double neededZoomW = telaLargura / (rect.width + padding * 2);
        double neededZoomH = telaAltura / (rect.height + padding * 2);
        double fitZoom = Math.min(neededZoomW, neededZoomH);

        // só reduz o zoom quando necessário; nunca ultrapassa o zoom normal
        this.zoomFocoAlvo = Math.min(fitZoom, zoomBase);
    }

    // Útil para, por exemplo, travar o input do player enquanto a cutscene roda.
    public boolean emFoco() {
        return focoTimer > 0;
    }

    public boolean onScreen(double objX, double objY, double objW, double objH, int telaLargura, int telaAltura) {
        // Calcula os limites reais da visão da câmera no mundo, considerando o zoom
        // e a tremida atual (usando getX()/getY() em vez do campo direto)
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