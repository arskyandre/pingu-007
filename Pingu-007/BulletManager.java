
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class BulletManager {

    private ArrayList<Bullet> Bullets = new ArrayList<>();
    private boolean atualizando;
    private boolean limpezaSolicitada;
    // Matriz do mapa para as balas baterem nas paredes
    private int[][] lvlData;

    public void setLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
    }

    public void shoot(double startX, double startY, double dirX, double dirY, BulletOwner owner) {
        shoot(startX, startY, dirX, dirY, owner, false);
    }

    public void shoot(double startX, double startY, double dirX, double dirY, BulletOwner owner,
            boolean damageFalloff) {
        Bullets.add(new Bullet(startX, startY, dirX, dirY, owner, damageFalloff));
    }

    public void update(CameraManager camera, int telaLargura, int telaAltura, Player player,
            ArrayList<Enemy> enemies, ArrayList<MapObject> objetosDeCenario) {
        Bullets.removeIf(b -> !b.isActive());
        atualizando = true;
        try {
            for (Bullet b : Bullets) {
                b.update(camera, telaLargura, telaAltura);
                if (!b.isActive()) {
                    continue;
                }

                // Hit Registration na PAREDE do Mapa (tiles)
                if (lvlData != null) {
                    int tileX = (int) (b.getX() / GameCore.tiles_size);
                    int tileY = (int) (b.getY() / GameCore.tiles_size);
                    if (tileX >= 0 && tileX < lvlData[0].length && tileY >= 0 && tileY < lvlData.length) {
                        if (TileProperties.isSolid(lvlData[tileY][tileX])) {
                            b.desativar();
                            continue;
                        }
                    }
                }

                // Hit Registration nos MapObjects
                if (objetosDeCenario != null) {
                    boolean atingiuObjeto = false;
                    for (MapObject mo : objetosDeCenario) {
                        if (mo.isTransparent() || !mo.isSolid()) {
                            continue;
                        }
                        Shape hitbox = mo.getHitbox();
                        if (hitbox == null) {
                            continue;
                        }
                        if (b.getCollider().intersects(b.getX(), b.getY(), hitbox)) {
                            b.desativar();
                            atingiuObjeto = true;
                            break;
                        }
                    }
                    if (atingiuObjeto) {
                        continue;
                    }
                }

                // Hit Registration nos Inimigos / Player
                if (b.getOwner() == BulletOwner.PLAYER) {
                    for (Enemy e : enemies) {
                        if (e.isInvulneravel) {
                            continue;
                        }

                        if (e.getHurtbox() != null) {
                            if (b.getCollider().intersects(b.getX(), b.getY(), e.getHurtbox(), e.getX(), e.getY())) {
                                e.receberDano(b.getDano(), b.getX(), b.getY(), b.getKnockback());
                                b.desativar();
                                break;
                            }
                        }
                    }
                } else if (b.getOwner() == BulletOwner.ENEMY) {
                    if (player.getHurtbox() != null) {
                        if (b.getCollider().intersects(b.getX(), b.getY(), player.getHurtbox(), player.getX(),
                                player.getY())) {
                            player.receberDano(b.getDano());
                            b.desativar();
                        }
                    }
                }
            }
        } finally {
            atualizando = false;
            if (limpezaSolicitada) {
                Bullets.clear();
                limpezaSolicitada = false;
            }
        }
    }

    public void draw(Graphics2D g2, CameraManager camera, int telaLargura, int telaAltura) {
        g2.setColor(Color.RED);
        for (Bullet b : Bullets) {
            if (camera.onScreen(b.getX(), b.getY(), b.getLargura(), b.getAltura(), telaLargura, telaAltura)) {
                g2.fill(new Ellipse2D.Double(b.getX() - 4, b.getY() - 4, 8, 8));
                g2.drawImage(b.getSprite(), (int) b.getX() - 24, (int) b.getY() - 24, 48, 48, null);
            }

        }
    }

    public void limparTudo() {
        if (atualizando) {
            for (Bullet bullet : Bullets) {
                bullet.desativar();
            }
            limpezaSolicitada = true;
        } else {
            Bullets.clear();
        }
    }

    public ArrayList<Bullet> getBullets() {
        return Bullets;
    }
}
