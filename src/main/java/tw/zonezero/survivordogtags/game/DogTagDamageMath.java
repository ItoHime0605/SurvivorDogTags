package tw.zonezero.survivordogtags.game;

/**
 * TaCZ applies its headshot multiplier after the Pre event. To give a flat
 * +1 / +2 final direct-hit bonus, add bonus / multiplier to the base amount.
 * Pure Java so invalid and extreme damage packets can be regression-tested.
 */
public final class DogTagDamageMath {
    private DogTagDamageMath() {}

    /** Returns NaN to mean "do not modify this hit". */
    public static float boostedBase(float base, int bonus, float multiplier) {
        if (!Float.isFinite(base) || base <= 0 || bonus <= 0 ||
            !Float.isFinite(multiplier) || multiplier <= 0) return Float.NaN;
        double result = (double) base + (double) bonus / multiplier;
        if (result > Float.MAX_VALUE || !Double.isFinite(result)) return Float.NaN;
        return (float) result;
    }
}
