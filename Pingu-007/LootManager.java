
import java.awt.Graphics2D;
import java.util.ArrayList;

public class LootManager {
    // Essa classe gerencia o loot dropado no chao(municao, possiveis curas etc)

    private final ArrayList<Loot> loots = new ArrayList<>();

    public void spawn(Loot loot) {
        loots.add(loot);
    }

    public void update(Player player) {
        for (Loot l : loots) {
            if (l.isAtivo()) {
                l.update(player);
            }
        }
        loots.removeIf(l -> !l.isAtivo());
    }

    /**
     * Desenha todos os itens ativos.
     */
    public void draw(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        for (Loot l : loots) {
            // Só desenha se estiver dentro da visão da câmera
            if (l.isAtivo() && camera.onScreen(l.getX(), l.getY(), l.getLargura(), l.getAltura(), telaLargura, telaAltura)) {
                l.draw(g2);
            }
        }
    }

    public ArrayList<Loot> getLoots() {
        return loots;
    }

    public int getCount() {
        return loots.size();
    }

}
