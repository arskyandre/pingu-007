
public class Bullet {

    private double x, y;
    private final double startX, startY;
    private final BulletOwner owner;
    private final double largura, altura;
    private final double velX, velY;
    final private double speed = 10.0;
    private boolean active = true;
    private final double KnockbackForce = 20;

    private final double maxDistancia = 600.0;

    private final Collider collider;
    private final int dano = 10;

    Bullet(double x, double y, double dirX, double dirY, BulletOwner owner) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.owner = owner;
        this.largura = 8;
        this.altura = 8;

        this.collider = new Collider(0, 0, largura / 2.0);

        double len = Math.sqrt(dirX * dirX + dirY * dirY);
        this.velX = (dirX / len) * speed;
        this.velY = (dirY / len) * speed;
    }

    public void update(CameraManager camera, int telaLargura, int telaAltura) {
        x += velX;
        y += velY;

        double distPercorrida = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        if (distPercorrida > maxDistancia) {
            desativar();
        }

        /*if (!camera.onScreen(x, y, largura, altura, telaLargura, telaAltura)) {
            desativar();
        }*/
    }

    public void desativar() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

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

    // Getters para Hit Registration
    public Collider getCollider() {
        return collider;
    }

    public BulletOwner getOwner() {
        return owner;
    }

    public int getDano() {
        return dano;
    }

    public double getKnockback() {
        return KnockbackForce;
    }
}
