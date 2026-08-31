import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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

    protected void drawSpriteWithShadow(Graphics2D g2, BufferedImage sprite,
            int drawX, int drawY, int drawW, int drawH) {
        if (sprite == null || drawW == 0 || drawH == 0) {
            return;
        }

        ProjectedShadow.VisualAnchor anchor = ProjectedShadow.getVisualGroundAnchor(sprite,
                drawX, drawY, drawW, drawH);
        double referenceHeight = anchor.hasVisiblePixels()
                ? Math.max(1.0, anchor.getVisibleHeight())
                : Math.max(1.0, Math.abs((double) drawH));
        double feetX = anchor.hasVisiblePixels() ? anchor.getX() : drawX + drawW / 2.0;
        double feetY = anchor.hasVisiblePixels() ? anchor.getY() : drawY + Math.abs((double) drawH);

        ProjectedShadow.drawAtGroundAnchor(g2, feetX, feetY, referenceHeight,
                ProjectedShadow.shadowLengthForReferenceHeight(referenceHeight),
                ProjectedShadow.DEFAULT_SHADOW_OPACITY,
                new ProjectedShadow.Part(sprite, drawX, drawY, drawW, drawH));
    }

    public abstract void update(
            Player player,
            DialogueManager dialogueManager,
            SoundManager soundManager,
            ItemManager itemManager);

    public abstract boolean tryInteract(
            Player player,
            DialogueManager dialogueManager,
            SoundManager soundManager,
            ItemManager itemManager);

    @Override
    public abstract void draw(Graphics2D g2, double delta);
}
