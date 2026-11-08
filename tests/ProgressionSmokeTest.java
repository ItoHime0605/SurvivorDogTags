import tw.zonezero.survivordogtags.game.DogTagProgression;

/** Pure JDK smoke test of level stats. Does not require Minecraft to run. */
public final class ProgressionSmokeTest {
    public static void main(String[] args) {
        int[][] expected = {
            {0, 0, 0},
            {2, 0, 0},
            {4, 0, 1},
            {6, 3, 1},
            {8, 4, 1},
            {10, 5, 2}
        };
        for (int level = 0; level < expected.length; level++) {
            var b = DogTagProgression.forLevel(level);
            if (b.maxHealth() != expected[level][0] ||
                b.speedPercent() != expected[level][1] ||
                b.physicalDamage() != expected[level][2]) {
                throw new AssertionError("Wrong stats at Lv." + level + ": " + b);
            }
        }
        if (!DogTagProgression.forLevel(-1).equals(DogTagProgression.forLevel(0)) ||
            !DogTagProgression.forLevel(100).equals(DogTagProgression.forLevel(5))) {
            throw new AssertionError("Level clamping failed");
        }
        System.out.println("PASS: 6 level bonuses and bounds");
    }
}
