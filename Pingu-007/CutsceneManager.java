/**
 * Classe para fazer cutscenes(arena, possivelmente boss, etc)
 */
public class CutsceneManager {
    public enum Phase {
        NONE, OPENING, CLOSING
    }

    private Phase phase = Phase.NONE;
    private int timer = 0;
    private static final int DURATION = 100;

    private static final int BLACK_BORDER_HEIGHT = 80;;
}