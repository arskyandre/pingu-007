
public class Node {

    public int coluna;
    public int linha;

    public int gCost;
    public int hCost;
    public int fCost;

    public Node parent;
    public boolean requerSalto;

    public int distanciaTiles;

    public Node(int coluna, int linha) {
        this.coluna = coluna;
        this.linha = linha;
        this.requerSalto = false;
        this.distanciaTiles = 1;
    }

    public void calcularFCost() {
        this.fCost = this.gCost + this.hCost;
    }
}
