package tw.zonezero.survivordogtags.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.HolderLookup;
import java.util.UUID;
import tw.zonezero.survivordogtags.SurvivorDogTags;
import tw.zonezero.survivordogtags.workbench.ExposurePhotoValidator;

/** ItemStack-backed identity data. Never changes owner UUID during upgrade/photo replacement. */
public final class DogTagData {
    public static final String OWNER_ID = "OwnerUUID";
    public static final String OWNER_NAME = "OwnerName";
    public static final String LEVEL = "Level";
    public static final String PHOTO = "PhotoItem";
    public static final String LAST_KILLS = "LastKnownZombieKills";
    private DogTagData() {}

    public static boolean isBound(ItemStack stack) {
        return stack.is(SurvivorDogTags.DOG_TAG.get()) && data(stack).hasUUID(OWNER_ID);
    }
    public static UUID owner(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.hasUUID(OWNER_ID) ? tag.getUUID(OWNER_ID) : new UUID(0L, 0L);
    }
    public static String ownerName(ItemStack stack) {
        return DogTagProfileSafety.safeName(data(stack).getString(OWNER_NAME));
    }
    public static int level(ItemStack stack) {
        return DogTagProfileSafety.safeLevel(data(stack).getInt(LEVEL));
    }
    public static int lastKills(ItemStack stack) {
        return DogTagProfileSafety.safeKills(data(stack).getInt(LAST_KILLS));
    }
    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    private static void save(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static ItemStack create(UUID id, String name, ItemStack photo, HolderLookup.Provider registry) {
        ItemStack result = new ItemStack(SurvivorDogTags.DOG_TAG.get());
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER_ID, id);
        tag.putString(OWNER_NAME, DogTagProfileSafety.safeName(name));
        tag.putInt(LEVEL, 0);
        tag.putInt(LAST_KILLS, 0);
        if (ExposurePhotoValidator.isValid(photo)) tag.put(PHOTO, photo.copyWithCount(1).save(registry));
        save(result, tag);
        return result;
    }
    public static ItemStack photo(ItemStack stack, HolderLookup.Provider registry) {
        CompoundTag tag = data(stack);
        if (!tag.contains(PHOTO, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        try {
            ItemStack photo = ItemStack.parseOptional(registry, tag.getCompound(PHOTO));
            return ExposurePhotoValidator.isValid(photo) ? photo.copyWithCount(1) : ItemStack.EMPTY;
        } catch (RuntimeException malformedPhotoData) {
            // Old/corrupt third-party item NBT must not crash a card inspection.
            return ItemStack.EMPTY;
        }
    }
    public static void withLevel(ItemStack stack, int level) {
        CompoundTag tag = data(stack);
        tag.putInt(LEVEL, DogTagProfileSafety.safeLevel(level));
        save(stack, tag);
    }
    public static void withPhoto(ItemStack stack, ItemStack photo, HolderLookup.Provider registry) {
        CompoundTag tag = data(stack);
        if (photo.isEmpty()) tag.remove(PHOTO);
        else if (ExposurePhotoValidator.isValid(photo)) tag.put(PHOTO, photo.copyWithCount(1).save(registry));
        save(stack, tag);
    }
    public static void withKillsSnapshot(ItemStack stack, int kills) {
        CompoundTag tag = data(stack);
        tag.putInt(LAST_KILLS, DogTagProfileSafety.safeKills(kills));
        save(stack, tag);
    }
}
