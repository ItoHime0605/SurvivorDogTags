import java.util.Random;
import java.util.Arrays;
import tw.zonezero.survivordogtags.game.DogTagPaymentPlanner;

/** Deterministic brute-force comparison for overlapping item tags. */
public final class PaymentPlannerFuzzTest {
    private static boolean brute(int[] quantities, boolean[][] accepts, int[] required) {
        boolean[][] possible = new boolean[required[0] + 1][required[1] + 1];
        possible[0][0] = true;
        for (int slot = 0; slot < quantities.length; slot++) {
            boolean[][] next = new boolean[required[0] + 1][required[1] + 1];
            for (int a = 0; a <= required[0]; a++) for (int b = 0; b <= required[1]; b++) {
                if (!possible[a][b]) continue;
                for (int x = 0; x <= quantities[slot] && a + x <= required[0]; x++) {
                    if (x != 0 && !accepts[0][slot]) continue;
                    for (int y = 0; x + y <= quantities[slot] && b + y <= required[1]; y++) {
                        if (y != 0 && !accepts[1][slot]) continue;
                        next[a + x][b + y] = true;
                    }
                }
            }
            possible = next;
        }
        return possible[required[0]][required[1]];
    }

    public static void main(String[] args) {
        Random rng = new Random(0x1504CAFE);
        for (int trial = 0; trial < 5_000; trial++) {
            int[] counts = new int[4];
            boolean[][] accepts = new boolean[2][4];
            for (int slot = 0; slot < 4; slot++) {
                counts[slot] = rng.nextInt(6);
                accepts[0][slot] = rng.nextBoolean();
                accepts[1][slot] = rng.nextBoolean();
            }
            int[] req = {1 + rng.nextInt(7), 1 + rng.nextInt(7)};
            boolean feasible = brute(counts, accepts, req);
            int[] payout = DogTagPaymentPlanner.plan(counts, accepts, req);
            if (feasible != (payout != null)) {
                throw new AssertionError("Mismatched feasibility at " + trial + " for " + Arrays.toString(counts));
            }
            if (payout != null) {
                if (Arrays.stream(payout).sum() != req[0] + req[1]) {
                    throw new AssertionError("Incorrect paid total at " + trial);
                }
                for (int slot = 0; slot < 4; slot++) {
                    if (payout[slot] < 0 || payout[slot] > counts[slot])
                        throw new AssertionError("Overdraft at " + trial);
                }
            }
        }
        System.out.println("PASS: 5,000 randomized material-tag allocations compared with brute force");
    }
}
