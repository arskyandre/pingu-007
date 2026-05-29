
public class Collider {

    public enum Type {
        RECTANGLE,
        CIRCLE
    }

    private final Type type;
    private double offsetX, offsetY;
    private double width, height;
    private double radius;

    public Collider(double offsetX, double offsetY, double width, double height) {
        this.type = Type.RECTANGLE;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
    }

    public Collider(double offsetX, double offsetY, double radius) {
        this.type = Type.CIRCLE;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.radius = radius;
    }

    // Checa colisão contra outro Collider (Entity vs Entity / Area vs Area)
    public boolean intersects(double myX, double myY, Collider other, double otherX, double otherY) {
        if (this.type == Type.RECTANGLE && other.type == Type.RECTANGLE) {
            return rectVsRect(myX, myY, other, otherX, otherY);
        } else if (this.type == Type.CIRCLE && other.type == Type.CIRCLE) {
            return circleVsCircle(myX, myY, other, otherX, otherY);
        } else {
            // Um é círculo e o outro é retângulo
            if (this.type == Type.CIRCLE) {
                return circleVsRect(myX, myY, this, otherX, otherY, other);
            } else {
                return circleVsRect(otherX, otherY, other, myX, myY, this);
            }
        }
    }

    private boolean rectVsRect(double x1, double y1, Collider c2, double x2, double y2) {
        return (x1 + offsetX < x2 + c2.offsetX + c2.width
                && x1 + offsetX + width > x2 + c2.offsetX
                && y1 + offsetY < y2 + c2.offsetY + c2.height
                && y1 + offsetY + height > y2 + c2.offsetY);
    }

    private boolean circleVsCircle(double x1, double y1, Collider c2, double x2, double y2) {
        double c1X = x1 + offsetX + radius;
        double c1Y = y1 + offsetY + radius;
        double c2X = x2 + c2.offsetX + c2.radius;
        double c2Y = y2 + c2.offsetY + c2.radius;

        double dx = c1X - c2X;
        double dy = c1Y - c2Y;
        double distanceSquared = (dx * dx) + (dy * dy);
        double radiusSum = this.radius + c2.radius;

        return distanceSquared < (radiusSum * radiusSum);
    }

    private boolean circleVsRect(double cx, double cy, Collider circle, double rx, double ry, Collider rect) {
        double circleCenterX = cx + circle.offsetX + circle.radius;
        double circleCenterY = cy + circle.offsetY + circle.radius;

        double rectLeft = rx + rect.offsetX;
        double rectRight = rectLeft + rect.width;
        double rectTop = ry + rect.offsetY;
        double rectBottom = rectTop + rect.height;

        // Encontra o ponto no retângulo mais próximo do centro do círculo
        double closestX = Math.max(rectLeft, Math.min(circleCenterX, rectRight));
        double closestY = Math.max(rectTop, Math.min(circleCenterY, rectBottom));

        double dx = circleCenterX - closestX;
        double dy = circleCenterY - closestY;

        return (dx * dx + dy * dy) < (circle.radius * circle.radius);
    }

    // Getters
    public Type getType() {
        return type;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
