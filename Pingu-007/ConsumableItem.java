public abstract class ConsumableItem extends Item {

    public ConsumableItem(double x, double y, double largura, double altura) {
        super(x, y, largura, altura);
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.CONSUMABLE;
    }
}
