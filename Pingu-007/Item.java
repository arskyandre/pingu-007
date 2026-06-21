import java.awt.Graphics2D;

public abstract class Item {

    protected double x, y;
    protected double largura, altura;
    protected Collider bodyCollider;
    protected boolean ativo = true;

    private double bobOffset = 0;
    private double bobVel = 0.08;
    private double bobAmplitude = 4.0;
    private double bobTempo = 0;

    public Item(double x, double y, double largura, double altura) {
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.bodyCollider = new Collider(0, 0, largura, altura);
    }

    public abstract ItemCategory getCategory();

    protected abstract void aplicarEfeito(Player player);

    public abstract void draw(Graphics2D g2);

    public void update(Player player) {
        if (!ativo) {
            return;
        }

        bobTempo += bobVel;
        bobOffset = Math.sin(bobTempo) * bobAmplitude;

        if (colideComPlayer(player)) {
            aplicarEfeito(player);
            ativo = false;
        }
    }

    public double getVisualY() {
        return y + bobOffset;
    }

    public double getSortBaseY() {
        if (bodyCollider != null) {
            return y + bodyCollider.getOffsetY() + bodyCollider.getHeight();
        }
        return y + altura;
    }

    private boolean colideComPlayer(Player player) {
        double lootLeft = x;
        double lootRight = x + largura;
        double lootTop = y;
        double lootBottom = y + altura;

        double playerLeft = player.getX();
        double playerRight = player.getX() + player.getLargura();
        double playerTop = player.getY();
        double playerBottom = player.getY() + player.getAltura();

        return lootRight > playerLeft
                && lootLeft < playerRight
                && lootBottom > playerTop
                && lootTop < playerBottom;
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

    public Collider getBodyCollider() {
        return bodyCollider;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setPosicao(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
