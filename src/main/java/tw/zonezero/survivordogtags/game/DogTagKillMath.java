package tw.zonezero.survivordogtags.game;

/** Pure-Java migration bounds; never sum overlapping historical snapshots. */
public final class DogTagKillMath {
    private DogTagKillMath() {}

    public static int baseline(int legacyCount, int vanillaCount) {
        return Math.max(0, Math.max(legacyCount, vanillaCount));
    }

    public static int combine(int recorded, int legacy, int vanilla, int snapshot) {
        return Math.max(baseline(recorded, legacy), baseline(vanilla, snapshot));
    }
}
