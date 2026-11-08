import tw.zonezero.survivordogtags.game.DogTagDamageMath;

public final class DamageMathSmokeTest {
    private static void check(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (float headshot : new float[] {0.25f, 0.5f, 1f, 1.5f, 2f, 3f, 8f}) {
            for (int bonus : new int[] {1, 2}) {
                float base = 12.25f;
                float adjusted = DogTagDamageMath.boostedBase(base, bonus, headshot);
                check(Math.abs((adjusted - base) * headshot - bonus) < 0.00005,
                    "flat damage must not be amplified by headshot multiplier");
            }
        }
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f, -1f}) {
            check(Float.isNaN(DogTagDamageMath.boostedBase(10f, 2, invalid)), "invalid multiplier must be ignored");
            check(Float.isNaN(DogTagDamageMath.boostedBase(invalid, 2, 1f)), "invalid base must be ignored");
        }
        check(Float.isNaN(DogTagDamageMath.boostedBase(Float.MAX_VALUE, 2, 1f)) == false,
             "very large finite hits must remain finite when float rounding is safe");
        check(Float.isNaN(DogTagDamageMath.boostedBase(10f, 0, 1f)), "level 0/1 adds no damage");
        System.out.println("PASS: flat +1/+2 TaCZ hit bonus survives headshot multipliers and invalid values");
    }
}
