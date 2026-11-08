package tw.zonezero.survivordogtags.workbench;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import tw.zonezero.survivordogtags.game.DogTagUpgradePlan;
import tw.zonezero.survivordogtags.game.DogTagPaymentPlanner;

/** Costs are fixed and measured in individual ingots, not stacks. */
public final class DogTagMaterials {
    private DogTagMaterials() {}
    private static final TagKey<Item> ALUMINUM = tag("c:ingots/aluminum");
    private static final TagKey<Item> ALUMINIUM = tag("c:ingots/aluminium");
    private static final TagKey<Item> FORGE_ALUMINUM = tag("forge:ingots/aluminum");
    private static final TagKey<Item> FORGE_ALUMINIUM = tag("forge:ingots/aluminium");
    private static final TagKey<Item> COBALT = tag("c:ingots/cobalt");
    private static final TagKey<Item> FORGE_COBALT = tag("forge:ingots/cobalt");
    private static final TagKey<Item> TUNGSTEN = tag("c:ingots/tungsten");
    private static final TagKey<Item> FORGE_TUNGSTEN = tag("forge:ingots/tungsten");
    private static TagKey<Item> tag(String s) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(s));
    }

    public enum Kind {
        IRON, COPPER, GOLD, ALUMINUM, COBALT, TUNGSTEN;
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            return switch (this) {
                case IRON -> stack.is(Items.IRON_INGOT);
                case COPPER -> stack.is(Items.COPPER_INGOT);
                case GOLD -> stack.is(Items.GOLD_INGOT);
                case ALUMINUM -> stack.is(ALUMINUM) || stack.is(ALUMINIUM) || stack.is(FORGE_ALUMINUM) || stack.is(FORGE_ALUMINIUM);
                case COBALT -> stack.is(COBALT) || stack.is(FORGE_COBALT);
                case TUNGSTEN -> stack.is(TUNGSTEN) || stack.is(FORGE_TUNGSTEN);
            };
        }
    }
    public static boolean isSupportedIngot(ItemStack stack) {
        for (Kind kind : Kind.values()) if (kind.matches(stack)) return true;
        return false;
    }
    public record Cost(Kind kind, int amount) {}
    /** All costs are derived from the same immutable rules tested without Minecraft. */
    public static Cost[] costForNextLevel(int currentLevel) {
        var requirements = DogTagUpgradePlan.forCurrentLevel(currentLevel);
        Cost[] costs = new Cost[requirements.length];
        for (int i = 0; i < requirements.length; i++) {
            var need = requirements[i];
            costs[i] = new Cost(Kind.valueOf(need.material().name()), need.count());
        }
        return costs;
    }
    public static int count(Container inventory, Kind kind) {
        int total = 0;
        for (int slot = 2; slot < 6; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (kind.matches(stack)) total += stack.getCount();
        }
        return total;
    }
    /** A material payment is planned completely before any inventory is changed. */
    private static int[] paymentPlan(Container inventory, Cost[] costs) {
        int[] counts = new int[4];
        boolean[][] accepted = new boolean[costs.length][4];
        int[] needed = new int[costs.length];
        for (int slot = 0; slot < 4; slot++) {
            counts[slot] = inventory.getItem(slot + 2).getCount();
        }
        for (int r = 0; r < costs.length; r++) {
            needed[r] = costs[r].amount();
            for (int slot = 0; slot < 4; slot++) {
                accepted[r][slot] = costs[r].kind().matches(inventory.getItem(slot + 2));
            }
        }
        return DogTagPaymentPlanner.plan(counts, accepted, needed);
    }
    public static boolean canPay(Container inventory, Cost[] costs) {
        return paymentPlan(inventory, costs) != null;
    }
    /** Returns false without removing ANY material if the recipe cannot be paid. */
    public static boolean pay(Container inventory, Cost[] costs) {
        int[] plan = paymentPlan(inventory, costs);
        if (plan == null) return false;
        for (int i = 0; i < plan.length; i++) {
            if (plan[i] != 0) inventory.removeItem(i + 2, plan[i]);
        }
        return true;
    }
}
