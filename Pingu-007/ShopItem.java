import java.awt.image.BufferedImage;

public class ShopItem {

    public final String nome;
    public final String descricao;
    public final BufferedImage icone;
    public final int preco;
    public final Runnable aoComprar;

    public ShopItem(String nome, String descricao, BufferedImage icone, int preco, Runnable aoComprar) {
        this.nome = nome;
        this.descricao = descricao;
        this.icone = icone;
        this.preco = preco;
        this.aoComprar = aoComprar;
    }
}