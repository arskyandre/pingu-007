import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;

public class Ammo extends Loot {

    private final int quantidade;

    private static final Color COR_CAIXA = new Color(210, 160, 40); // Dourado

    // Cria um drop de munição na posição dada com quantidade customizada.
    public Ammo(double x, double y, int quantidade) {
        super(x, y, 24, 24);
        this.quantidade = quantidade;
    }

    // padrao de 15
    public Ammo(double x, double y) {
        this(x, y, 15);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!ativo)
            return;

        double drawY = getVisualY(); // Y com efeito de bob

        // Sombra suave embaixo do item
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new Ellipse2D.Double(x - 2, y + altura - 4, largura + 4, 8));

        // Corpo da caixinha de munição
        g2.setColor(COR_CAIXA);
        g2.fill(new RoundRectangle2D.Double(x, drawY, largura, altura, 5, 5));

    }

    @Override
    protected void onCollected(Player player) {
        player.addMunicao(quantidade);
    }

    public int getQuantidade() {
        return quantidade;
    }
}
