import java.awt.image.BufferedImage;

public class ShopItem {

    public final String nome;
    public final String descricao;
    public final BufferedImage icone;
    public final int preco;
    public final Runnable aoComprar;
    public boolean disponivel;
    public boolean compra_unica;

    public ShopItem(String nome, String descricao, BufferedImage icone, int preco, Runnable aoComprar,
            boolean disponivel, boolean compra_unica) {
        this.nome = nome;
        this.disponivel = disponivel;
        this.descricao = descricao;
        this.icone = icone;
        this.preco = preco;
        this.aoComprar = aoComprar;
        this.compra_unica = compra_unica;
    }
}