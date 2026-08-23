
import java.awt.Shape;

public interface DebugRenderable {

    // Os dados originais do Tiled (tipo, posição, etc.) usados para rotular o debug.
    TiledObject getDadosTiled();

    Shape getHitboxAtual();
}
