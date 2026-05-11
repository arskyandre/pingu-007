
public class Quadrado {

    private double x, y;
    final private double largura, altura;

    public Quadrado(double x, double y, double largura, double altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
    }

    // Getters para o desenho
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getLargura() {
        return largura;
    }

    public double getAltura() {
        return altura;
    }

    public void setPosicao(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
