public interface GamepadInputSource extends AutoCloseable {
    GamepadSnapshot poll();

    @Override
    void close();
}
