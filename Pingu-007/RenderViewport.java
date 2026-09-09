import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Dimensoes e projecoes usadas por uma frame de renderizacao.
 *
 * O objeto e imutavel para que a thread de entrada sempre observe um conjunto
 * coerente de dimensoes durante uma troca de fullscreen ou de superficie.
 */
public final class RenderViewport {

    private final int larguraCanvasFisico;
    private final int alturaCanvasFisico;
    private final int larguraCenaInterna;
    private final int alturaCenaInterna;
    private final Rectangle retanguloDestinoFisico;
    private final double escalaCenaParaTelaX;
    private final double escalaCenaParaTelaY;
    private final boolean cenaEmBaixaResolucao;
    private final boolean interfaceEmBaixaResolucao;
    private final int larguraInterface;
    private final int alturaInterface;

    public RenderViewport(int larguraCanvasFisico, int alturaCanvasFisico,
            int larguraCenaInterna, int alturaCenaInterna,
            boolean cenaEmBaixaResolucao, boolean interfaceEmBaixaResolucao) {
        this.larguraCanvasFisico = dimensaoSegura(larguraCanvasFisico);
        this.alturaCanvasFisico = dimensaoSegura(alturaCanvasFisico);
        this.larguraCenaInterna = dimensaoSegura(larguraCenaInterna);
        this.alturaCenaInterna = dimensaoSegura(alturaCenaInterna);
        this.retanguloDestinoFisico = new Rectangle(0, 0,
                this.larguraCanvasFisico, this.alturaCanvasFisico);
        this.escalaCenaParaTelaX = this.retanguloDestinoFisico.getWidth() / this.larguraCenaInterna;
        this.escalaCenaParaTelaY = this.retanguloDestinoFisico.getHeight() / this.alturaCenaInterna;
        this.cenaEmBaixaResolucao = cenaEmBaixaResolucao;
        this.interfaceEmBaixaResolucao = interfaceEmBaixaResolucao && cenaEmBaixaResolucao;
        this.larguraInterface = this.interfaceEmBaixaResolucao
                ? this.larguraCenaInterna
                : this.larguraCanvasFisico;
        this.alturaInterface = this.interfaceEmBaixaResolucao
                ? this.alturaCenaInterna
                : this.alturaCanvasFisico;
    }

    public static RenderViewport identidade(int largura, int altura) {
        int larguraSegura = dimensaoSegura(largura);
        int alturaSegura = dimensaoSegura(altura);
        return new RenderViewport(larguraSegura, alturaSegura,
                larguraSegura, alturaSegura, false, false);
    }

    private static int dimensaoSegura(int valor) {
        return Math.max(1, valor);
    }

    public int getLarguraCanvasFisico() {
        return larguraCanvasFisico;
    }

    public int getAlturaCanvasFisico() {
        return alturaCanvasFisico;
    }

    public int getLarguraCenaInterna() {
        return larguraCenaInterna;
    }

    public int getAlturaCenaInterna() {
        return alturaCenaInterna;
    }

    public Rectangle getRetanguloDestinoFisico() {
        return new Rectangle(retanguloDestinoFisico);
    }

    public double getEscalaCenaParaTelaX() {
        return escalaCenaParaTelaX;
    }

    public double getEscalaCenaParaTelaY() {
        return escalaCenaParaTelaY;
    }

    public boolean isCenaEmBaixaResolucao() {
        return cenaEmBaixaResolucao;
    }

    public boolean isInterfaceEmBaixaResolucao() {
        return interfaceEmBaixaResolucao;
    }

    public int getLarguraInterface() {
        return larguraInterface;
    }

    public int getAlturaInterface() {
        return alturaInterface;
    }

    public int screenToSceneX(double xFisico) {
        double xClamped = clamp(xFisico,
                retanguloDestinoFisico.getMinX(), retanguloDestinoFisico.getMaxX() - 1.0);
        return clampInt((int) Math.floor((xClamped - retanguloDestinoFisico.getX())
                / escalaCenaParaTelaX), 0, larguraCenaInterna - 1);
    }

    public int screenToSceneY(double yFisico) {
        double yClamped = clamp(yFisico,
                retanguloDestinoFisico.getMinY(), retanguloDestinoFisico.getMaxY() - 1.0);
        return clampInt((int) Math.floor((yClamped - retanguloDestinoFisico.getY())
                / escalaCenaParaTelaY), 0, alturaCenaInterna - 1);
    }

    public int screenX(double xFisico) {
        return clampInt((int) Math.floor(xFisico), 0, larguraCanvasFisico - 1);
    }

    public int screenY(double yFisico) {
        return clampInt((int) Math.floor(yFisico), 0, alturaCanvasFisico - 1);
    }

    public int screenToInterfaceX(double xFisico) {
        return interfaceEmBaixaResolucao ? screenToSceneX(xFisico) : screenX(xFisico);
    }

    public int screenToInterfaceY(double yFisico) {
        return interfaceEmBaixaResolucao ? screenToSceneY(yFisico) : screenY(yFisico);
    }

    public double sceneToScreenX(double xCena) {
        return retanguloDestinoFisico.getX() + xCena * escalaCenaParaTelaX;
    }

    public double sceneToScreenY(double yCena) {
        return retanguloDestinoFisico.getY() + yCena * escalaCenaParaTelaY;
    }

    public Point2D.Double sceneToScreen(double xCena, double yCena) {
        return new Point2D.Double(sceneToScreenX(xCena), sceneToScreenY(yCena));
    }

    public Point2D.Double screenToScene(double xFisico, double yFisico) {
        return new Point2D.Double(screenToSceneX(xFisico), screenToSceneY(yFisico));
    }

    public Point sceneToScreenPoint(double xCena, double yCena) {
        return new Point((int) Math.round(sceneToScreenX(xCena)),
                (int) Math.round(sceneToScreenY(yCena)));
    }

    public int clampSceneX(int x) {
        return clampInt(x, 0, larguraCenaInterna - 1);
    }

    public int clampSceneY(int y) {
        return clampInt(y, 0, alturaCenaInterna - 1);
    }

    private static double clamp(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private static int clampInt(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }
}
