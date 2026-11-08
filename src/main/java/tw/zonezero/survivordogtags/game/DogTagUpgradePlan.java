package tw.zonezero.survivordogtags.game;

/** Pure Java, server-authoritative upgrade requirements. -1 means creation of Lv.0. */
public final class DogTagUpgradePlan {
    private DogTagUpgradePlan() {}
    public enum Material { IRON, COPPER, GOLD, ALUMINUM, COBALT, TUNGSTEN }
    public record Need(Material material, int count) {
        public Need {
            if (count <= 0) throw new IllegalArgumentException("Ingredient count must be positive");
        }
    }
    public static Need[] forCurrentLevel(int level) {
        return switch (level) {
            case -1 -> new Need[] {new Need(Material.IRON, 1), new Need(Material.COPPER, 1)};
            case 0 -> new Need[] {new Need(Material.IRON, 40)};
            case 1 -> new Need[] {new Need(Material.IRON, 80), new Need(Material.ALUMINUM, 40)};
            case 2 -> new Need[] {new Need(Material.GOLD, 40), new Need(Material.ALUMINUM, 80)};
            case 3 -> new Need[] {new Need(Material.COBALT, 6), new Need(Material.GOLD, 4)};
            case 4 -> new Need[] {new Need(Material.TUNGSTEN, 80), new Need(Material.COBALT, 80)};
            default -> new Need[0];
        };
    }
}
