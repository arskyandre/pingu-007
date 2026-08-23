
import java.awt.Graphics2D;

public abstract class NPC implements Renderable {

    protected double x, y;
    protected double largura, altura;
    protected boolean active = true;

    protected static double INTERACT_RANGE = GameCore.tiles_size * 2;

    public NPC(double x, double y, double largura, double altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
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

    @Override
    public double getProfundidade() {
        return y + altura;
    }

    public boolean playerNearby(Player player) {
        double dx = (player.getX() + player.getLargura() / 2.0)
                - (x + largura / 2.0);

        double dy = (player.getY() + player.getAltura() / 2.0)
                - (y + altura / 2.0);

        return Math.hypot(dx, dy) <= INTERACT_RANGE;
    }

    public abstract void update(
            Player player,
            InputManager input,
            DialogueManager dialogueManager,
            SoundManager soundManager,
            ItemManager itemManager);

    @Override
    public abstract void draw(Graphics2D g2, double delta);
}
