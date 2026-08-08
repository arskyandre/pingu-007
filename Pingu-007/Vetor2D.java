public class Vetor2D {
    public double x, y;

    public Vetor2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void normalize() {
        double magnitude = Math.sqrt(x * x + y * y);

        if (magnitude == 0) {
            return;
        }

        x /= magnitude;
        y /= magnitude;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public Vetor2D normalized() {
        double magnitude = magnitude();

        if (magnitude == 0) {
            return new Vetor2D(0, 0);
        }

        double normalized_x = x / magnitude;
        double normalized_y = y / magnitude;
        return new Vetor2D(normalized_x, normalized_y);
    }

    public Vetor2D partiallyNormalized() {
        double magnitude = magnitude();

        if (magnitude == 0) {
            return new Vetor2D(0, 0);
        }

        if (magnitude > 1.0) {
            return normalized();
        }

        return new Vetor2D(x, y);
    }
}
