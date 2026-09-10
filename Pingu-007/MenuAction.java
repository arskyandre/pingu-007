import java.util.Objects;
import java.util.function.Supplier;

/** An application action owned by a menu entry rather than by a visual widget. */
@FunctionalInterface
public interface MenuAction {

    GameState execute();

    static MenuAction navigateTo(GameState state) {
        return () -> Objects.requireNonNull(state, "state");
    }

    static MenuAction stay(GameState state, Runnable sideEffect) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(sideEffect, "sideEffect");
        return () -> {
            sideEffect.run();
            return state;
        };
    }

    static MenuAction returnTo(Supplier<GameState> stateSupplier) {
        Objects.requireNonNull(stateSupplier, "stateSupplier");
        return () -> Objects.requireNonNull(stateSupplier.get(), "state");
    }
}
