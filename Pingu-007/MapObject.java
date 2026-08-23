
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

public class MapObject implements Renderable, DebugRenderable {

    protected TiledObject data;

    private double x, y;
    private BufferedImage sprite;
    private boolean flipH, flipV;
    private double width, height;
    private Shape hitboxNoMundo;
    private double profundidade;

    private boolean temColisao;
    private boolean isTransparent;

    public MapObject(TiledObject tObj) {
        this.data = tObj;

        this.x = tObj.x;
        this.y = tObj.y;
        this.width = tObj.width;
        this.height = tObj.height;

        this.sprite = tObj.sprite;
        this.flipH = tObj.flipH;
        this.flipV = tObj.flipV;
        this.temColisao = tObj.collision;
        this.isTransparent = tObj.isTransparent;

        if (tObj.hitbox != null) {
            this.hitboxNoMundo = tObj.hitbox;
        } else if (tObj.isPolygon) {
            this.hitboxNoMundo = tObj.getPolygonShape();
        } else if (tObj.width > 0 && tObj.height > 0 && this.temColisao) {
            this.hitboxNoMundo = new java.awt.geom.Rectangle2D.Double(tObj.x, tObj.y, tObj.width, tObj.height);
        } else {
            this.hitboxNoMundo = null;
        }

        if (this.hitboxNoMundo != null) {
            this.profundidade = this.hitboxNoMundo.getBounds2D().getMaxY();
        } else {
            this.profundidade = this.y + this.height;
        }
    }

    @Override
    public double getProfundidade() {
        return profundidade;
    }

    @Override
    public void draw(Graphics2D g2, double delta) {
        if (sprite != null) {
            int dx = (int) x;
            int dy = (int) y;

            int drawW = (int) Math.round(sprite.getWidth() * GameCore.scale);
            int drawH = (int) Math.round(sprite.getHeight() * GameCore.scale);

            if (flipH) {
                dx += drawW;
                drawW = -drawW;
            }
            if (flipV) {
                dy += drawH;
                drawH = -drawH;
            }
            g2.drawImage(sprite, dx, dy, drawW, drawH, null);
        }

        // --- DEBUG DE COLISÃO ---
        // if (hitboxNoMundo != null && temColisao) {
        //     g2.setColor(new Color(255, 0, 0, 150));
        //     g2.fill(hitboxNoMundo);
        // }
    }

    public void updateTransformFromData() {
        this.x = this.data.x;
        this.y = this.data.y;
        this.width = this.data.width;
        this.height = this.data.height;

        if (this.data.isPolygon) {
            this.hitboxNoMundo = this.data.getPolygonShape();
        } else if (this.data.gid > 0) {
            this.hitboxNoMundo = LoadSave.recalcularHitboxDeGid(this.data);
        } else if (this.data.width > 0 && this.data.height > 0 && this.temColisao) {
            this.hitboxNoMundo = new java.awt.geom.Rectangle2D.Double(this.x, this.y, this.width, this.height);
        }

        if (this.hitboxNoMundo != null) {
            this.profundidade = this.hitboxNoMundo.getBounds2D().getMaxY();
        } else {
            this.profundidade = this.y + this.height;
        }
    }

    public TiledObject getData() {
        return data;
    }

    @Override
    public TiledObject getDadosTiled() {
        return data;
    }

    @Override
    public Shape getHitboxAtual() {
        return hitboxNoMundo;
    }

    public void setSolid(boolean solido) {
        this.temColisao = solido;
    }

    public void setSprite(BufferedImage novoSprite) {
        this.sprite = novoSprite;
    }

    public void setHitboxNoMundo(Shape shape) {
        this.hitboxNoMundo = shape;
    }

    public Shape getHitbox() {
        return hitboxNoMundo;
    }

    public boolean isSolid() {
        return temColisao;
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getLargura() {
        return width;
    }

    public double getAltura() {
        return height;
    }
}
