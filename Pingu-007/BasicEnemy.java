
public class BasicEnemy extends Enemy {

    public BasicEnemy(double startX, double startY, double width, double height, int[][] lvlData) {
        super(startX, startY, width, height, lvlData);
        this.vidaMaxima = 30;
        this.vida = this.vidaMaxima;

        // Customizando a física herdada
        this.velocidadeMax = 2.0;
        this.aceleracao = 0.5;

        this.bodyCollider = new Collider(0, height / 2.0, width, height / 2.0);
        this.hurtbox = new Collider(0, 0, width, height);
        this.hitbox = new Collider(4, 4, width - 8, height - 8);
    }

    @Override
    public void update(Player player) {
        // anda direto pro player
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 0) {
            velX += (dx / dist) * aceleracao;
            velY += (dy / dist) * aceleracao;
        }

        aplicarFisicaBasica();
        moveAndCollideWithMap(lvlData);

        // Checa dano Melee
        if (this.hitbox != null && player.getHurtbox() != null) {
            if (this.hitbox.intersects(this.x, this.y, player.getHurtbox(), player.getX(), player.getY())) {
                player.receberDano(danoContato);
            }
        }
    }
}
