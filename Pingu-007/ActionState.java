public record ActionState(boolean down, boolean pressed, boolean released) {
    public static final ActionState IDLE = new ActionState(false, false, false);
}
