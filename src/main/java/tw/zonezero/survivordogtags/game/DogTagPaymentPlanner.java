package tw.zonezero.survivordogtags.game;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Pure-Java atomic material planner. A stack may match multiple material tags,
 * but every physical ingot can be spent only once.
 *
 * We solve a tiny capacitated bipartite matching problem (up to four input
 * slots and two requirements) rather than using a greedy first-match rule.
 * The latter can falsely reject valid recipes when another mod assigns an
 * ingot to overlapping ore tags.
 */
public final class DogTagPaymentPlanner {
    private DogTagPaymentPlanner() {}

    /**
     * @param counts    available item counts, one per slot
     * @param accepted  accepted[requirement][slot], true if the item matches that cost
     * @param required  required ingots per material
     * @return total quantity to remove from each slot, or null if unaffordable
     */
    public static int[] plan(int[] counts, boolean[][] accepted, int[] required) {
        if (counts == null || accepted == null || required == null ||
            counts.length == 0 || required.length == 0 || accepted.length != required.length) {
            return null;
        }
        final int slots = counts.length;
        final int costs = required.length;
        final int source = 0;
        final int requirementStart = 1 + slots;
        final int sink = requirementStart + costs;
        final int n = sink + 1;
        final int[][] residual = new int[n][n];
        long neededTotal = 0;
        long availableTotal = 0;

        for (int i = 0; i < slots; i++) {
            if (counts[i] < 0) return null;
            residual[source][i + 1] = counts[i];
            availableTotal += counts[i];
        }
        for (int j = 0; j < costs; j++) {
            if (required[j] <= 0 || accepted[j] == null || accepted[j].length != slots) {
                return null;
            }
            residual[requirementStart + j][sink] = required[j];
            neededTotal += required[j];
            for (int i = 0; i < slots; i++) {
                if (accepted[j][i]) {
                    // A single slot can contribute at most its own item count.
                    residual[i + 1][requirementStart + j] = counts[i];
                }
            }
        }
        if (neededTotal > availableTotal) return null;

        long paid = 0;
        int[] previous = new int[n];
        while (paid < neededTotal) {
            Arrays.fill(previous, -1);
            previous[source] = source;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty() && previous[sink] == -1) {
                int u = queue.removeFirst();
                for (int v = 0; v < n; v++) {
                    if (previous[v] == -1 && residual[u][v] > 0) {
                        previous[v] = u;
                        queue.addLast(v);
                    }
                }
            }
            if (previous[sink] == -1) return null;
            int augment = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = previous[v]) {
                augment = Math.min(augment, residual[previous[v]][v]);
            }
            for (int v = sink; v != source; v = previous[v]) {
                int u = previous[v];
                residual[u][v] -= augment;
                residual[v][u] += augment;
            }
            paid += augment;
        }
        int[] take = new int[slots];
        for (int i = 0; i < slots; i++) {
            take[i] = counts[i] - residual[source][i + 1];
        }
        return take;
    }
}
