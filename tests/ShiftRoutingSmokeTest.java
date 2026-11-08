import tw.zonezero.survivordogtags.game.DogTagShiftRouting;

/** Pure Java safety regression for inventory transfers; no Minecraft runtime. */
public final class ShiftRoutingSmokeTest {
    private static void check(boolean expected, String message) {
        if (!expected) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var kind = DogTagShiftRouting.InputType.INGOT;
        for (int index : new int[] {-1, 6, 43, Integer.MAX_VALUE}) {
            check(DogTagShiftRouting.destination(index, kind) == null,
                "must deny result or out-of-range slot: " + index);
        }
        for (int index = 0; index < 6; index++) {
            var p = DogTagShiftRouting.destination(index, kind);
            check(p != null && p.first() == 7 && p.afterLast() == 43 && p.reverse(),
                "workbench input must return to player without touching result");
        }
        for (int index = 7; index < 43; index++) {
            var tag = DogTagShiftRouting.destination(index, DogTagShiftRouting.InputType.DOG_TAG);
            var photo = DogTagShiftRouting.destination(index, DogTagShiftRouting.InputType.PHOTOGRAPH);
            var ingot = DogTagShiftRouting.destination(index, kind);
            check(tag != null && tag.first() == 0 && tag.afterLast() == 1, "tag to slot 0");
            check(photo != null && photo.first() == 1 && photo.afterLast() == 2, "photo to slot 1");
            check(ingot != null && ingot.first() == 2 && ingot.afterLast() == 6, "ingots to four slots 2..5");
            check(DogTagShiftRouting.destination(index, DogTagShiftRouting.InputType.OTHER) == null,
                "unrelated items must not enter the workbench");
        }
        System.out.println("PASS: result cannot shift-transfer; 36 player slots route correctly; 4 ingot slots supported");
    }
}
