
public abstract class Entity {

    protected double x, y;
    protected double velX, velY;

    protected Collider bodyCollider;
    protected Collider hurtbox;
    protected Collider hitbox;

    protected int vidaMaxima;
    protected int vida;
    protected boolean isDead = false;

    public void receberDano(int dano) {
        vida -= dano;
        if (vida <= 0) {
            vida = 0;
            isDead = true;
        }
    }

    public boolean isDead() {
        return isDead;
    }

    // Lógica universal de colisão com o mapa de Tiles
    protected void moveAndCollideWithMap(int[][] lvlData) {
        double cbX = x + bodyCollider.getOffsetX();
        double cbY = y + bodyCollider.getOffsetY();
        double cbW = bodyCollider.getWidth();
        double cbH = bodyCollider.getHeight();

        int proxX = (int) (cbX + velX);
        int proxY = (int) (cbY + velY);

        // Movimento Horizontal
        if (!HelpMethods.CanMoveHere(proxX, cbY, cbW, cbH, lvlData)) {
            if (velX > 0) {
                x = (proxX - ((proxX + cbW) % GameCore.tiles_size) - 1) - bodyCollider.getOffsetX();
            } else if (velX < 0) {
                x = (proxX + (GameCore.tiles_size - (proxX % GameCore.tiles_size))) - bodyCollider.getOffsetX();
            }
            velX = 0;
        } else {
            x += velX;
        }

        // Movimento Vertical
        cbX = x + bodyCollider.getOffsetX();
        if (!HelpMethods.CanMoveHere(cbX, proxY, cbW, cbH, lvlData)) {
            if (velY > 0) {
                y = (proxY - ((proxY + cbH) % GameCore.tiles_size) - 1) - bodyCollider.getOffsetY();
            } else if (velY < 0) {
                y = (proxY + (GameCore.tiles_size - (proxX % GameCore.tiles_size))) - bodyCollider.getOffsetY();
            }
            velY = 0;
        } else {
            y += velY;
        }
    }

    public boolean checkAreaCollision(Entity other) {
        if (this.hitbox == null || other.hurtbox == null) {
            return false;
        }
        return this.hitbox.intersects(this.x, this.y, other.hurtbox, other.x, other.y);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
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
}
