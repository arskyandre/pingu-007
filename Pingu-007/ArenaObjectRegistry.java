public final class ArenaObjectRegistry {

    private ArenaObjectRegistry() {
    }

    public static ArenaObject create(TiledObject obj) {
        String tipo = obj.tipo != null ? obj.tipo.toLowerCase().trim() : "";
        String acao = obj.acao != null ? obj.acao.toLowerCase().trim() : "";

        if (acao.equals("pescar")) {
            tipo = "interativo";
        }

        return switch (tipo) {
            case "wall", "door" -> new DoorObject(obj);
            case "interativo" -> new InteractiveObject(obj);
            case "colision" -> new CollisionBlock(obj);
            case "button" -> new PressureButton(obj);
            default -> null;
        };
    }
}
