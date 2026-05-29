
public class CameraManager {

    private double x, y;
    private double zoom;

    public double pesoOffset = 0.25;

    // Margem da tela (em pixels) para o player não chegar perto da borda
    public double margemX = 180;
    public double margemY = 120;

    public CameraManager(double x, double y, double zoom) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
    }

    public void update(Player player, InputManager input, int telaLargura, int telaAltura) {
        // centraliza a camera no player
        double centroTelaX = telaLargura / 2.0;
        double centroTelaY = telaAltura / 2.0;

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

        double targetX = playerCentroX - (centroTelaX / zoom) + (telaOffsetX / zoom);
        double targetY = playerCentroY - (centroTelaY / zoom) + (telaOffsetY / zoom);

        // suavização
        x += (targetX - x) * 0.1;
        y += (targetY - y) * 0.1;
    }

    public boolean onScreen(double objX, double objY, double objW, double objH, int telaLargura, int telaAltura) {
        // Calcula os limites reais da visão da câmera no mundo, considerando o zoom
        double viewLeft = this.x;
        double viewTop = this.y;
        double viewRight = this.x + (telaLargura / this.zoom);
        double viewBottom = this.y + (telaAltura / this.zoom);

        // Retorna true se o retângulo do objeto colidir com o retângulo da câmera
        // Isso cobre tanto objetos totalmente dentro quanto parcialmente dentro
        return (objX < viewRight
                && objX + objW > viewLeft
                && objY < viewBottom
                && objY + objH > viewTop);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
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

}
