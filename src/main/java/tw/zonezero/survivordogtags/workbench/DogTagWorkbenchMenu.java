package tw.zonezero.survivordogtags.workbench;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import tw.zonezero.survivordogtags.SurvivorDogTags;
import tw.zonezero.survivordogtags.game.DogTagData;
import tw.zonezero.survivordogtags.game.DogTagShiftRouting;

/**
 * Input: [0] existing tag, [1] actual Exposure photograph, [2..5] ingot stacks.
 * Output: [6].  Server decides which item can be taken, and consumes inputs once.
 */
public final class DogTagWorkbenchMenu extends AbstractContainerMenu {
    public static final int MODE_REFINE = 0;
    public static final int MODE_PHOTO = 1;
    private enum Operation { NONE, CREATE, UPGRADE, REPLACE_PHOTO }

    private final Inventory playerInventory;
    private final ContainerLevelAccess access;
    private final ContainerData settings = new SimpleContainerData(1);
    private boolean initialized;
    private boolean processing;
    private Operation preview = Operation.NONE;
    private int previewPreviousLevel = -1;
    private DogTagMaterials.Cost[] previewCosts = new DogTagMaterials.Cost[0];
    private final Container input = new SimpleContainer(6) {
        @Override public void setChanged() {
            super.setChanged();
            if (initialized && !processing) updateResult();
        }
    };
    private final ResultContainer output = new ResultContainer();
    // The result slot is a preview until a verified player pickup commits it.
    private static final int RESULT_SLOT = 6;

    private ItemStack expectedInputTag = ItemStack.EMPTY;
    private ItemStack expectedInputPhoto = ItemStack.EMPTY;
    private final ItemStack[] expectedMaterials = new ItemStack[] {
        ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private ItemStack expectedOutput = ItemStack.EMPTY;

    public DogTagWorkbenchMenu(int id, Inventory inv, RegistryFriendlyByteBuf data) {
        this(id, inv, ContainerLevelAccess.create(inv.player.level(), data.readBlockPos()));
    }
    public DogTagWorkbenchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(SurvivorDogTags.WORKBENCH_MENU.get(), id);
        this.playerInventory = inv;
        this.access = access;
        addDataSlots(settings);
        addSlot(new Slot(input, 0, 20, 28) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(SurvivorDogTags.DOG_TAG.get()); }
        });
        addSlot(new Slot(input, 1, 45, 28) {
            @Override public boolean mayPlace(ItemStack stack) { return ExposurePhotoValidator.isValid(stack); }
        });
        int[] ingotX = {18, 40, 62, 84};
        for (int ingotSlot = 0; ingotSlot < ingotX.length; ingotSlot++) {
            addSlot(new Slot(input, ingotSlot + 2, ingotX[ingotSlot], 65) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return DogTagMaterials.isSupportedIngot(stack);
                }
            });
        }
        addSlot(new Slot(output, 0, 134, 44) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) {
                return !output.getItem(0).isEmpty() &&
                    (player.level().isClientSide || canCommit());
            }
            @Override public ItemStack remove(int amount) {
                // The server pays the recipe BEFORE returning the item to the caller.
                if (playerInventory.player.level().isClientSide) return super.remove(amount);
                if (amount <= 0 || !canCommit()) return ItemStack.EMPTY;
                ItemStack result = output.getItem(0).copy();
                if (result.isEmpty() || !commit()) return ItemStack.EMPTY;
                return result;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 158 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 216));
        }
        initialized = true;
        if (!inv.player.level().isClientSide) updateResult();
    }

    public int getMode() { return settings.get(0); }
    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != MODE_REFINE && buttonId != MODE_PHOTO) return false;
        if (!stillValid(player)) return false;
        if (!player.level().isClientSide) {
            settings.set(0, buttonId);
            updateResult();
            broadcastChanges();
        }
        return true;
    }

    private boolean canCommit() {
        if (preview == Operation.NONE || processing || !stillValid(playerInventory.player)) return false;
        // Never grant a preview built from a different dog tag or different photo.
        if (!ItemStack.matches(input.getItem(0), expectedInputTag) ||
            !ItemStack.matches(input.getItem(1), expectedInputPhoto) ||
            !ItemStack.matches(output.getItem(0), expectedOutput)) return false;
        for (int i = 0; i < expectedMaterials.length; i++) {
            // The preview can never be claimed against a materially different
            // transaction than the one which originally produced it.
            if (!ItemStack.matches(input.getItem(i + 2), expectedMaterials[i])) return false;
        }
        ItemStack tag = input.getItem(0);
        ItemStack photo = input.getItem(1);
        boolean hasPhoto = ExposurePhotoValidator.isValid(photo);
        switch (preview) {
            case CREATE:
                return getMode() == MODE_REFINE && tag.isEmpty() && hasPhoto &&
                    DogTagMaterials.canPay(input, DogTagMaterials.costForNextLevel(-1));
            case UPGRADE:
                return getMode() == MODE_REFINE && DogTagData.isBound(tag) &&
                    DogTagData.level(tag) == previewPreviousLevel &&
                    (photo.isEmpty() || hasPhoto) &&
                    DogTagMaterials.canPay(input, DogTagMaterials.costForNextLevel(previewPreviousLevel));
            case REPLACE_PHOTO:
                return getMode() == MODE_PHOTO && DogTagData.isBound(tag) && hasPhoto;
            default:
                return false;
        }
    }
    private void updateResult() {
        if (!initialized || processing || playerInventory.player.level().isClientSide) return;
        processing = true;
        try {
            preview = Operation.NONE;
            previewPreviousLevel = -1;
            previewCosts = new DogTagMaterials.Cost[0];
            expectedInputTag = ItemStack.EMPTY;
            expectedInputPhoto = ItemStack.EMPTY;
            expectedOutput = ItemStack.EMPTY;
            java.util.Arrays.fill(expectedMaterials, ItemStack.EMPTY);
            output.setItem(0, ItemStack.EMPTY);
            ItemStack tag = input.getItem(0);
            ItemStack photo = input.getItem(1);
            boolean hasPhoto = ExposurePhotoValidator.isValid(photo);
            HolderLookup.Provider reg = playerInventory.player.registryAccess();
            ItemStack result;
            if (getMode() == MODE_REFINE && tag.isEmpty()) {
                var cost = DogTagMaterials.costForNextLevel(-1);
                if (!hasPhoto || !DogTagMaterials.canPay(input, cost)) return;
                result = DogTagData.create(playerInventory.player.getUUID(),
                    playerInventory.player.getGameProfile().getName(), photo, reg);
                preview = Operation.CREATE;
                previewCosts = cost;
            } else if (getMode() == MODE_REFINE && DogTagData.isBound(tag)) {
                int current = DogTagData.level(tag);
                if (current >= 5 || (!photo.isEmpty() && !hasPhoto)) return;
                var cost = DogTagMaterials.costForNextLevel(current);
                if (!DogTagMaterials.canPay(input, cost)) return;
                result = tag.copyWithCount(1);
                DogTagData.withLevel(result, current + 1);
                if (hasPhoto) DogTagData.withPhoto(result, photo, reg);
                preview = Operation.UPGRADE;
                previewPreviousLevel = current;
                previewCosts = cost;
            } else if (getMode() == MODE_PHOTO && DogTagData.isBound(tag)) {
                if (!hasPhoto) return;
                result = tag.copyWithCount(1);
                DogTagData.withPhoto(result, photo, reg);
                preview = Operation.REPLACE_PHOTO;
            } else return;
            expectedInputTag = tag.copy();
            expectedInputPhoto = photo.copy();
            for (int i = 0; i < expectedMaterials.length; i++) {
                expectedMaterials[i] = input.getItem(i + 2).copy();
            }
            expectedOutput = result.copy();
            output.setItem(0, result);
            broadcastChanges();
        } finally { processing = false; }
    }

    private boolean commit() {
        if (!canCommit()) return false;
        processing = true;
        try {
            if ((preview == Operation.CREATE || preview == Operation.UPGRADE) &&
                !DogTagMaterials.pay(input, previewCosts)) return false;
            if (preview != Operation.CREATE) input.removeItem(0, 1);
            // Upgrade without a photo keeps the old picture, otherwise consume the new one.
            if (!input.getItem(1).isEmpty()) input.removeItem(1, 1);
            preview = Operation.NONE;
            previewCosts = new DogTagMaterials.Cost[0];
            previewPreviousLevel = -1;
            output.setItem(0, ItemStack.EMPTY);
        } finally { processing = false; }
        updateResult();
        return true;
    }

    /**
     * Restrict the preview to ordinary left/right pickup with an empty cursor.
     * Without this, number keys, THROW, CLONE and other special click types can
     * bypass normal result-slot transaction expectations in modded menus.
     */
    @Override public void clicked(int slotId, int mouseButton, ClickType clickType, Player player) {
        if (slotId == RESULT_SLOT) {
            if (clickType != ClickType.PICKUP ||
                (mouseButton != 0 && mouseButton != 1) ||
                !getCarried().isEmpty() ||
                !stillValid(player)) return;
            if (!player.level().isClientSide && !canCommit()) return;
        }
        super.clicked(slotId, mouseButton, clickType, player);
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            processing = true;
            for (int i = 0; i < input.getContainerSize(); i++) {
                ItemStack remaining = input.removeItemNoUpdate(i);
                if (!remaining.isEmpty()) player.getInventory().placeItemBackInInventory(remaining);
            }
            output.setItem(0, ItemStack.EMPTY);
            processing = false;
        }
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        // Result previews must never be drained by a double-click PICKUP_ALL action.
        return slot != this.slots.get(RESULT_SLOT);
    }
    @Override public boolean canDragTo(Slot slot) {
        return slot != this.slots.get(RESULT_SLOT);
    }
    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, SurvivorDogTags.BENCH.get());
    }
    /**
     * Shift-click routing for ingredients and inventory only. The result slot is
     * intentionally NEVER shift-movable: the result is a server-paid transaction,
     * not a normal container stack, so it must use ordinary pickup.
     */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index == RESULT_SLOT ||
                !stillValid(player)) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack remaining = source.getItem();
        ItemStack original = remaining.copy();
        var type = remaining.is(SurvivorDogTags.DOG_TAG.get())
                ? DogTagShiftRouting.InputType.DOG_TAG
                : ExposurePhotoValidator.isValid(remaining)
                    ? DogTagShiftRouting.InputType.PHOTOGRAPH
                    : DogTagMaterials.isSupportedIngot(remaining)
                        ? DogTagShiftRouting.InputType.INGOT
                        : DogTagShiftRouting.InputType.OTHER;
        var destination = DogTagShiftRouting.destination(index, type);
        if (destination == null) return ItemStack.EMPTY;
        boolean moved = moveItemStackTo(remaining, destination.first(),
                destination.afterLast(), destination.reverse());
        if (!moved) return ItemStack.EMPTY;
        if (remaining.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return original;
    }
}
