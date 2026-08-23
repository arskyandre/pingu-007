
import java.awt.Graphics2D;

public class CollisionPolygon extends MapObject {

    public CollisionPolygon(TiledObject tObj) {
        super(tObj);

        boolean estadoInicial = tObj.id_arena >= 0 ? tObj.ativa : tObj.solidoPorPadrao;

        setSolid(estadoInicial);
        setSprite(null);
    }

    public void setActive(boolean state) {
        setSolid(state);
    }

    public void toggleActive() {
        setSolid(!isSolid());
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        // --- DEBUG DE COLISÃO ---
        // Descomente o bloco abaixo quando for testar para ver o retângulo 
        // pintado de vermelho exatamente em cima do tronco da árvore!
        // if (getHitbox() != null && isSolid()) {
        //     g2.setColor(new java.awt.Color(255, 0, 0, 150));
        //     g2.fill(getHitbox());
        // }
        super.draw(g2, delta);
    }
}
