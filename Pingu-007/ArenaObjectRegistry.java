
public final class ArenaObjectRegistry {

    private ArenaObjectRegistry() {
    }

    public static ArenaObject create(TiledObject obj) {
        String tipo = obj.tipo != null ? obj.tipo.toLowerCase().trim() : "";

        return switch (tipo) {
            case "wall", "door" ->
                new DoorObject(obj);
            case "button" ->
                new PressureButton(obj);
            default ->
                null;
        };
    }
}
