import tw.zonezero.survivordogtags.game.DogTagUpgradePlan;

/** Dependency-free checks for all six recipes, including quantities above 64. */
public final class UpgradePlanSmokeTest {
    private static void assertNeeds(int atLevel, DogTagUpgradePlan.Need... expected) {
        var actual = DogTagUpgradePlan.forCurrentLevel(atLevel);
        if (!java.util.Arrays.equals(actual, expected)) {
            throw new AssertionError("Expected " + java.util.Arrays.toString(expected) +
                " but found " + java.util.Arrays.toString(actual) + " for currentLevel=" + atLevel);
        }
    }
    public static void main(String[] args) {
        assertNeeds(-1, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.IRON, 1),
            new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.COPPER, 1));
        assertNeeds(0, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.IRON, 40));
        assertNeeds(1, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.IRON, 80),
            new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.ALUMINUM, 40));
        assertNeeds(2, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.GOLD, 40),
            new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.ALUMINUM, 80));
        assertNeeds(3, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.COBALT, 6),
            new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.GOLD, 4));
        assertNeeds(4, new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.TUNGSTEN, 80),
            new DogTagUpgradePlan.Need(DogTagUpgradePlan.Material.COBALT, 80));
        assertNeeds(5);
        assertNeeds(-2);
        System.out.println("PASS: 6 exact ingredient requirements and no higher-level recipes");
    }
}
