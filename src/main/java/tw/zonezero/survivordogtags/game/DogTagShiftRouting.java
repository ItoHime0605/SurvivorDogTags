package tw.zonezero.survivordogtags.game;

/**
 * Side-independent quick-transfer policy. The actual movement stays in the
 * Minecraft menu, but these bounds and denied result transfers are tested
 * with plain javac so later GUI changes cannot silently route a result into
 * a normal slot (which would bypass the dedicated payment transaction).
 */
public final class DogTagShiftRouting {
    private DogTagShiftRouting() {}

    public static final int RESULT_SLOT = 6;
    public static final int PLAYER_START = 7;
    public static final int PLAYER_END = 43;

    public enum InputType { DOG_TAG, PHOTOGRAPH, INGOT, OTHER }
    public record Destination(int first, int afterLast, boolean reverse) {}

    public static Destination destination(int sourceSlot, InputType type) {
        if (sourceSlot < 0 || sourceSlot >= PLAYER_END || sourceSlot == RESULT_SLOT)
            return null;
        if (sourceSlot < RESULT_SLOT)
            return new Destination(PLAYER_START, PLAYER_END, true);
        return switch (type) {
            case DOG_TAG -> new Destination(0, 1, false);
            case PHOTOGRAPH -> new Destination(1, 2, false);
            case INGOT -> new Destination(2, RESULT_SLOT, false);
            case OTHER -> null;
        };
    }
}
