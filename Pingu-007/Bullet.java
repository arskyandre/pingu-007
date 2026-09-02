import java.awt.image.BufferedImage;

public class Bullet {

    private double x, y;
    private final double startX, startY;
    private final BulletOwner owner;
    private final double largura, altura;
    private final double velX, velY;
    final private double speed = 10.0;
    private boolean active = true;

    private double KnockbackForce = 20;
    private final double maxDistancia = 600.0;

    private final Collider collider;
    private final int dano = 10;

    private final boolean damageFalloff;
    private static final double DANO_MINIMO_FRACAO = 0.15;
    private double distanciaPercorrida = 0;
    private BufferedImage sprite;

    Bullet(double x, double y, double dirX, double dirY, BulletOwner owner) {
        this(x, y, dirX, dirY, owner, false);
    }

    Bullet(double x, double y, double dirX, double dirY, BulletOwner owner, boolean damageFalloff) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.owner = owner;
        this.largura = 8;
        this.altura = 8;
        this.damageFalloff = damageFalloff;

        this.collider = new Collider(0, 0, largura / 2.0);

        double len = Math.hypot(dirX, dirY);
        if (len < 0.0001) {
            this.velX = 0;
            this.velY = -speed;
        } else {
            this.velX = (dirX / len) * speed;
            this.velY = (dirY / len) * speed;
        }

        sprite = LoadSave.GetSpriteAtlas("images/bala.png");

    }

    public void update(CameraManager camera, int telaLargura, int telaAltura) {
        x += velX;
        y += velY;

        distanciaPercorrida = Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        if (distanciaPercorrida > maxDistancia) {
            desativar();
        }
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

    public Collider getCollider() {
        return collider;
    }

    public BulletOwner getOwner() {
        return owner;
    }

    public int getDano() {
        if (!damageFalloff) {
            return dano;
        }
        double fracaoPercorrida = Math.min(1.0, distanciaPercorrida / maxDistancia);
        double multiplicador = 1.0 - (fracaoPercorrida * (1.0 - DANO_MINIMO_FRACAO));
        return (int) Math.round(dano * multiplicador);
    }

    public double getKnockback() {
        return KnockbackForce;
    }

    public BufferedImage getSprite(){
        return sprite;
    }
}