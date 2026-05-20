import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public abstract class Loot {

    protected double x, y;
    protected double largura, altura;
    protected boolean ativo = true;

    // Efeito de "bob" (flutuar para cima e para baixo)
    private double bobOffset = 0;
    private double bobVel = 0.08;
    private double bobAmplitude = 4.0;
    private double bobTempo = 0;

    public Loot(double x, double y, double largura, double altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
    }

    public void update(Player player) {
        if (!ativo)
            return;

        // Animação de flutuação suave
        bobTempo += bobVel;
        bobOffset = Math.sin(bobTempo) * bobAmplitude;

        // Checagem de colisão (AABB) com o player
        if (colideComPlayer(player)) {
            onCollected(player);
            ativo = false;
        }
    }

    // Desenha o item. Subclasses devem sobrescrever para definir a aparência.
    // A posição Y já inclui o offset de bob.

    public abstract void draw(Graphics2D g2);

    protected abstract void onCollected(Player player);

    // Retorna a posição Y visual do item, já com o efeito de bob aplicado.

    public double getVisualY() {
        return y + bobOffset;
    }

    private boolean colideComPlayer(Player player) {
        // Bounding box do loot (sem bob para colisão ser consistente)
        double lootLeft = x;
        double lootRight = x + largura;
        double lootTop = y;
        double lootBottom = y + altura;

        double playerLeft = player.getX();
        double playerRight = player.getX() + player.getLargura();
        double playerTop = player.getY();
        double playerBottom = player.getY() + player.getAltura();

        return lootRight > playerLeft &&
                lootLeft < playerRight &&
                lootBottom > playerTop &&
                lootTop < playerBottom;
    }

    // --- Getters ---

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

    public boolean isAtivo() {
        return ativo;
    }
}