public record RawInputEvent(Type type, int code, int x, int y) {
    public enum Type {
        KEY_PRESSED,
        KEY_RELEASED,
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_MOVED,
        FOCUS_GAINED,
        FOCUS_LOST
    }

    public static RawInputEvent key(Type type, int keyCode) {
        return new RawInputEvent(type, keyCode, 0, 0);
    }

    public static RawInputEvent mouse(Type type, int button, int x, int y) {
        return new RawInputEvent(type, button, x, y);
    }

    public static RawInputEvent focus(Type type) {
        return new RawInputEvent(type, 0, 0, 0);
    }
}
