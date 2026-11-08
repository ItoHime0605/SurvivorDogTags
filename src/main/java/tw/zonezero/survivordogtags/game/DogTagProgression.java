package tw.zonezero.survivordogtags.game;

/** All stats are totals at this level. An old level's bonus is NEVER added again. */
public final class DogTagProgression {
    private DogTagProgression() {}
    public record Bonus(int maxHealth, int speedPercent, int physicalDamage) {}
    private static final Bonus[] LEVELS = {
        new Bonus(0, 0, 0),
        new Bonus(2, 0, 0),
        new Bonus(4, 0, 1),
        new Bonus(6, 3, 1),
        new Bonus(8, 4, 1),
        new Bonus(10, 5, 2)
    };
    public static Bonus forLevel(int level) {
        return LEVELS[Math.max(0, Math.min(level, LEVELS.length - 1))];
    }
}
