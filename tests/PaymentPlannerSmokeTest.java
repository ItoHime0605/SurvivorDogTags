import java.util.Arrays;
import tw.zonezero.survivordogtags.game.DogTagPaymentPlanner;

public class PaymentPlannerSmokeTest {
    private static void ok(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    private static void expect(int[] counts, boolean[][] matches, int[] required, int... amount) {
        int[] original = counts.clone();
        int[] got = DogTagPaymentPlanner.plan(counts, matches, required);
        ok(Arrays.equals(got, amount), "payment mismatch: " + Arrays.toString(got));
        ok(Arrays.equals(counts, original), "planner changed inventory before commit");
    }
    public static void main(String[] args) {
        expect(new int[]{1,1,0,0}, new boolean[][]{{true,false,false,false},{false,true,false,false}},
            new int[]{1,1}, 1,1,0,0);
        expect(new int[]{64,16,40,0}, new boolean[][]{{true,true,false,false},{false,false,true,false}},
            new int[]{80,40}, 64,16,40,0);
        expect(new int[]{64,16,64,16}, new boolean[][]{{true,true,false,false},{false,false,true,true}},
            new int[]{80,80}, 64,16,64,16);
        ok(DogTagPaymentPlanner.plan(new int[]{64,15,64,16},
            new boolean[][]{{true,true,false,false},{false,false,true,true}}, new int[]{80,80}) == null,
            "79 ingots must not pay an 80-ingot requirement");
        ok(DogTagPaymentPlanner.plan(new int[]{64,0,0,0},
            new boolean[][]{{true,false,false,false},{true,false,false,false}}, new int[]{40,40}) == null,
            "same stack cannot be charged twice using overlapping tags");
        expect(new int[]{64,40,0,0}, new boolean[][]{{true,true,false,false},{true,true,false,false}},
            new int[]{40,40}, 64,16,0,0);
        // A stack shared by both tags must not be allocated greedily to the
        // first requirement when that would leave a later requirement short.
        // The allocation may vary, but it must charge 160 total ingots
        // and never charge any slot more than it actually contains.
        int[] flexible = DogTagPaymentPlanner.plan(new int[]{64,64,64,0},
            new boolean[][]{{true,true,false,false},{true,false,true,false}},
            new int[]{80,80});
        ok(flexible != null && Arrays.stream(flexible).sum() == 160,
            "overlapping tags must permit a valid reallocation");
        for (int k = 0; k < flexible.length; k++) {
            ok(flexible[k] >= 0 && flexible[k] <= (k < 3 ? 64 : 0),
                "material charged more than its stack contains");
        }
        ok(DogTagPaymentPlanner.plan(new int[]{64,0,0,0},
            new boolean[][]{{true,false,false,false}}, new int[]{0}) == null,
            "zero requirement invalid");
        System.out.println("PASS: 4-slot material transactions are planned atomically, including 80+80");
    }
}
