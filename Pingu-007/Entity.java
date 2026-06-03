
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
        double cbW = bodyCollider.getWidth();
        double cbH = bodyCollider.getHeight();

        // Descobre a maior velocidade que a entidade está tentando atingir neste frame
        double maxVel = Math.max(Math.abs(velX), Math.abs(velY));

        // Dividimos pelo raio do tile para garantir precisão absoluta.
        int steps = (int) Math.ceil(maxVel / (GameCore.tiles_size / 2.0));

        // Se a velocidade for muito baixa, dá pelo menos 1 passo normal
        if (steps == 0) {
            steps = 1;
        }

        double stepX = velX / steps;
        double stepY = velY / steps;

        for (int i = 0; i < steps; i++) {
            double cbX = x + bodyCollider.getOffsetX();
            double cbY = y + bodyCollider.getOffsetY();

            double proxX = cbX + stepX;
            double proxY = cbY + stepY;

            // Movimento Horizontal
            if (!HelpMethods.CanMoveHere(proxX, cbY, cbW, cbH, lvlData)) {
                if (stepX > 0) { // Colisão na parede direita
                    int tileX = (int) ((proxX + cbW) / GameCore.tiles_size);
                    x = (tileX * GameCore.tiles_size) - cbW - 0.1 - bodyCollider.getOffsetX();
                } else if (stepX < 0) { // Colisão na parede esquerda
                    int tileX = (int) (proxX / GameCore.tiles_size);
                    x = ((tileX + 1) * GameCore.tiles_size) + 0.1 - bodyCollider.getOffsetX();
                }

                // Zera as variáveis porque batemos em uma parede
                velX = 0;
                stepX = 0;
            } else {
                x += stepX;
            }

            // Atualiza a posição X real
            cbX = x + bodyCollider.getOffsetX();

            // Movimento Vertical
            if (!HelpMethods.CanMoveHere(cbX, proxY, cbW, cbH, lvlData)) {
                if (stepY > 0) {
                    int tileY = (int) ((proxY + cbH) / GameCore.tiles_size);
                    y = (tileY * GameCore.tiles_size) - cbH - 0.1 - bodyCollider.getOffsetY();
                } else if (stepY < 0) {
                    int tileY = (int) (proxY / GameCore.tiles_size);
                    y = ((tileY + 1) * GameCore.tiles_size) + 0.1 - bodyCollider.getOffsetY();
                }

                // Zera as variáveis porque batemos no chão/teto
                velY = 0;
                stepY = 0;
            } else {
                y += stepY;
            }

            // Se bateu numa quina e as duas velocidades zeraram no meio do loop.
            if (stepX == 0 && stepY == 0) {
                break;
            }
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
