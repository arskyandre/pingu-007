
public abstract class Entity {

    protected double x, y;
    protected double velX, velY;

    protected Collider bodyCollider;
    protected Collider hurtbox;
    protected Collider hitbox;

    protected int vidaMaxima;
    protected int vida;
    protected boolean isDead = false;

    protected double aceleracao = 1.0;
    protected double atritoPadrao = 0.85;
    protected double velocidadeMax = 30.0;

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

    protected void aplicarFisicaBasica() {
        velX = Math.max(-velocidadeMax, Math.min(velX, velocidadeMax));
        velY = Math.max(-velocidadeMax, Math.min(velY, velocidadeMax));

        velX *= atritoPadrao;
        velY *= atritoPadrao;

        if (Math.abs(velX) < 0.01) {
            velX = 0;
        }
        if (Math.abs(velY) < 0.01) {
            velY = 0;
        }
    }

    // Lógica universal de colisão com o mapa de Tiles
    protected void moveAndCollideWithMap(int[][] lvlData) {
        double cbX = x + bodyCollider.getOffsetX();
        double cbY = y + bodyCollider.getOffsetY();
        double cbW = bodyCollider.getWidth();
        double cbH = bodyCollider.getHeight();

        double proxX = cbX + velX;
        double proxY = cbY + velY;

        // Movimento Horizontal
        if (!HelpMethods.CanMoveHere(proxX, cbY, cbW, cbH, lvlData)) {
            if (velX > 0) {
                int tileX = (int) ((proxX + cbW) / GameCore.tiles_size);
                x = (tileX * GameCore.tiles_size) - cbW - 0.1 - bodyCollider.getOffsetX();
            } else if (velX < 0) {
                int tileX = (int) (proxX / GameCore.tiles_size);
                x = ((tileX + 1) * GameCore.tiles_size) + 0.1 - bodyCollider.getOffsetX();
            }
            velX = 0;
        } else {
            x += velX;
        }
        // Movimento Vertical
        cbX = x + bodyCollider.getOffsetX();
        if (!HelpMethods.CanMoveHere(cbX, proxY, cbW, cbH, lvlData)) {
            if (velY > 0) {
                int tileY = (int) ((proxY + cbH) / GameCore.tiles_size);
                y = (tileY * GameCore.tiles_size) - cbH - 0.1 - bodyCollider.getOffsetY();
            } else if (velY < 0) {
                int tileY = (int) (proxY / GameCore.tiles_size);
                y = ((tileY + 1) * GameCore.tiles_size) + 0.1 - bodyCollider.getOffsetY();
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
