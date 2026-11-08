package tw.zonezero.survivordogtags.game;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import tw.zonezero.survivordogtags.SurvivorDogTags;
import tw.zonezero.survivordogtags.network.OpenDogTagPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DogTagItem extends Item implements ICurioItem {
    public DogTagItem(Properties properties) { super(properties); }

    // Curios adds these only when the item is equipped, and removes them when unequipped.
    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slot, ResourceLocation slotUniqueId, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = LinkedHashMultimap.create();
        if (!DogTagData.isBound(stack) || !"necklace".equals(slot.identifier()) || slot.cosmetic()
                || !isFirstActiveNecklaceTag(slot)) return map;
        int lv = DogTagData.level(stack);
        var bonus = DogTagProgression.forLevel(lv);
        int hp = bonus.maxHealth();
        int speed = bonus.speedPercent();
        if (hp > 0) {
            map.put(Attributes.MAX_HEALTH, new AttributeModifier(
                slotUniqueId, hp, AttributeModifier.Operation.ADD_VALUE));
        }
        if (speed > 0) {
            map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                slotUniqueId, speed / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        return map;
    }

    /** Extra necklace slots supplied by other mods must never stack duplicate dog tag buffs. */
    private static boolean isFirstActiveNecklaceTag(SlotContext context) {
        if (context.entity() == null || context.index() < 0) return true;
        return CuriosApi.getCuriosInventory(context.entity()).map(handler -> {
            var necklaces = handler.getStacksHandler("necklace");
            if (necklaces.isEmpty()) return true;
            var stacks = necklaces.get().getStacks();
            for (int index = 0; index < stacks.getSlots(); index++) {
                if (!handler.isSlotActive("necklace", index)) continue;
                if (DogTagData.isBound(stacks.getStackInSlot(index))) {
                    return index == context.index();
                }
            }
            return true;
        }).orElse(true);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack candidate) {
        if (!"necklace".equals(context.identifier())) return false;
        if (context.entity() == null || context.cosmetic()) return true;
        // Do not permit a second functional dog tag into extra necklace slots.
        return CuriosApi.getCuriosInventory(context.entity()).map(handler -> {
            var maybeSlot = handler.getStacksHandler("necklace");
            if (maybeSlot.isEmpty()) return true;
            var stacks = maybeSlot.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (i == context.index()) continue;
                if (stacks.getStackInSlot(i).is(SurvivorDogTags.DOG_TAG.get())) return false;
            }
            return true;
        }).orElse(true);
    }

    @Override
    public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) { return false; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer server && DogTagData.isBound(stack)) {
            int kills = DogTagTracker.getKills(server, DogTagData.owner(stack), DogTagData.lastKills(stack));
            // Only refresh the snapshot when held by its owner, not by another player.
            if (DogTagData.owner(stack).equals(server.getUUID())) DogTagData.withKillsSnapshot(stack, kills);
            PacketDistributor.sendToPlayer(server, new OpenDogTagPayload(
                DogTagData.owner(stack), DogTagData.ownerName(stack), DogTagData.level(stack),
                kills, DogTagData.photo(stack, server.registryAccess())));
        } else if (player instanceof ServerPlayer server) {
            server.displayClientMessage(Component.translatable("message.survivordogtags.unbound"), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tips, TooltipFlag flag) {
        if (!DogTagData.isBound(stack)) {
            tips.add(Component.translatable("tooltip.survivordogtags.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        tips.add(Component.literal(DogTagData.ownerName(stack)).withStyle(ChatFormatting.GOLD));
        tips.add(Component.translatable("tooltip.survivordogtags.level", DogTagData.level(stack))
            .withStyle(ChatFormatting.GRAY));
        int lv = DogTagData.level(stack);
        if (lv == 0) {
            tips.add(Component.translatable("tooltip.survivordogtags.identity_only")
                .withStyle(ChatFormatting.GRAY));
        } else {
            tips.add(Component.translatable("tooltip.survivordogtags.health", DogTagProgression.forLevel(lv).maxHealth())
                .withStyle(ChatFormatting.GREEN));
        }
        if (lv >= 2) {
            tips.add(Component.translatable("tooltip.survivordogtags.damage", DogTagProgression.forLevel(lv).physicalDamage())
                .withStyle(ChatFormatting.RED));
        }
        if (lv >= 3) {
            tips.add(Component.translatable("tooltip.survivordogtags.speed", DogTagProgression.forLevel(lv).speedPercent())
                .withStyle(ChatFormatting.AQUA));
        }
    }
}
