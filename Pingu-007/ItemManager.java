
import java.util.ArrayList;

public class ItemManager {

    private final ArrayList<Item> items = new ArrayList<>();
    private int[][] lvlData;

    public void setLvlData(int[][] lvlData) {
        this.lvlData = lvlData;
    }

    public void spawn(Item item) {
        if (lvlData != null) {
            double[] posSegura = encontrarPosicaoSegura(
                    item.getX(), item.getY(), item.getLargura(), item.getAltura(), lvlData);
            item.setPosicao(posSegura[0], posSegura[1]);
        }
        items.add(item);
    }

    public void gerarDropDeInimigo(Enemy enemy) {
        if (lvlData == null || enemy.isLootProcessado()) {
            return;
        }
        enemy.marcarLootProcessado();

        double dropX = enemy.getX() + enemy.getLargura() / 2.0 - 12;
        double dropY = enemy.getY() + enemy.getAltura() / 2.0 - 12;

        if (enemy instanceof Shooter) {
            spawn(new AmmoPackItem(dropX, dropY));
            if (Math.random() < 0.05) {
                spawn(new MoedaItem(dropX, dropY, 10));
            }
        } else if (enemy instanceof Dasher || enemy instanceof Jumper
                || enemy instanceof BasicEnemy || enemy instanceof Bomber) {
            if (Math.random() < 0.75) {
                spawn(new HealthPackItem(dropX, dropY));
            }
            if ((enemy instanceof Bomber || enemy instanceof Jumper) && Math.random() < 0.66) {
                spawn(new AmmoPackItem(dropX + 16, dropY + 8));
            }
        }
    }

    public void update(Player player) {
        for (Item item : items) {
            if (item.isAtivo()) {
                item.update(player);
            }
        }
        items.removeIf(item -> !item.isAtivo());
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public int getCount() {
        return items.size();
    }

    public void limparTudo() {
        items.clear();
    }

    public void limparConsumiveis() {
        items.removeIf(item -> item instanceof AmmoPackItem || item instanceof HealthPackItem);
        System.out.println("Limpeza de consumíveis do chão concluída.");
    }

    public static double[] encontrarPosicaoSegura(double x, double y, double largura, double altura, int[][] lvlData) {
        int centerCol = (int) ((x + largura / 2.0) / GameCore.tiles_size);
        int centerRow = (int) ((y + altura / 2.0) / GameCore.tiles_size);

        int maxRadius = Math.max(lvlData.length, lvlData[0].length);
        double melhorDist = Double.MAX_VALUE;
        int melhorCol = centerCol;
        int melhorRow = centerRow;

        for (int raio = 0; raio <= maxRadius; raio++) {
            boolean encontrou = false;
            for (int dr = -raio; dr <= raio; dr++) {
                for (int dc = -raio; dc <= raio; dc++) {
                    if (Math.abs(dr) != raio && Math.abs(dc) != raio) {
                        continue;
                    }

                    int row = centerRow + dr;
                    int col = centerCol + dc;
                    if (!tileValido(row, col, lvlData)) {
                        continue;
                    }

                    double spawnX = col * GameCore.tiles_size + (GameCore.tiles_size - largura) / 2.0;
                    double spawnY = row * GameCore.tiles_size + (GameCore.tiles_size - altura) / 2.0;

                    if (!podePosicionarItem(spawnX, spawnY, largura, altura, lvlData)) {
                        continue;
                    }

                    double dist = Math.hypot(col - centerCol, row - centerRow);
                    if (dist < melhorDist) {
                        melhorDist = dist;
                        melhorCol = col;
                        melhorRow = row;
                        encontrou = true;
                    }
                }
            }
            if (encontrou) {
                break;
            }
        }

        double finalX = melhorCol * GameCore.tiles_size + (GameCore.tiles_size - largura) / 2.0;
        double finalY = melhorRow * GameCore.tiles_size + (GameCore.tiles_size - altura) / 2.0;
        return new double[] { finalX, finalY };
    }

    private static boolean tileValido(int row, int col, int[][] lvlData) {
        return row >= 0 && row < lvlData.length && col >= 0 && col < lvlData[0].length;
    }

    private static boolean podePosicionarItem(double spawnX, double spawnY, double largura, double altura,
            int[][] lvlData) {
        int leftCol = (int) (spawnX / GameCore.tiles_size);
        int rightCol = (int) ((spawnX + largura - 0.1) / GameCore.tiles_size);
        int topRow = (int) (spawnY / GameCore.tiles_size);
        int bottomRow = (int) ((spawnY + altura - 0.1) / GameCore.tiles_size);

        if (leftCol < 0 || rightCol >= lvlData[0].length || topRow < 0 || bottomRow >= lvlData.length) {
            return false;
        }

        int[][] corners = {
                { topRow, leftCol },
                { topRow, rightCol },
                { bottomRow, leftCol },
                { bottomRow, rightCol }
        };

        for (int[] corner : corners) {
            int tile = lvlData[corner[0]][corner[1]];
            if (TileProperties.isHole(tile) || TileProperties.isSolid(tile)) {
                return false;
            }
        }

        int centerCol = (int) ((spawnX + largura / 2.0) / GameCore.tiles_size);
        int centerRow = (int) ((spawnY + altura / 2.0) / GameCore.tiles_size);
        if (!tileValido(centerRow, centerCol, lvlData)) {
            return false;
        }

        int tileCentro = lvlData[centerRow][centerCol];
        return !TileProperties.isHole(tileCentro) && !TileProperties.isSolid(tileCentro);
    }
}
