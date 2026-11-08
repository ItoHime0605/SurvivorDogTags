package tw.zonezero.survivordogtags.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tw.zonezero.survivordogtags.workbench.DogTagWorkbenchMenu;
import tw.zonezero.survivordogtags.game.DogTagData;
import tw.zonezero.survivordogtags.game.DogTagUpgradePlan;
import tw.zonezero.survivordogtags.workbench.DogTagMaterials;
import net.minecraft.world.item.ItemStack;

/** Two explicit operations avoid accidentally upgrading when simply changing the photo. */
public final class DogTagWorkbenchScreen extends AbstractContainerScreen<DogTagWorkbenchMenu> {
    public DogTagWorkbenchScreen(DogTagWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 241;
        inventoryLabelY = 147;
        titleLabelX = 8;
        titleLabelY = 7;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.survivordogtags.mode_upgrade"),
            button -> setMode(DogTagWorkbenchMenu.MODE_REFINE))
            .bounds(leftPos + 7, topPos + 90, 79, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.survivordogtags.mode_photo"),
            button -> setMode(DogTagWorkbenchMenu.MODE_PHOTO))
            .bounds(leftPos + 90, topPos + 90, 79, 20).build());
    }
    private void setMode(int mode) {
        if (Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, mode);
        }
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff101712);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xff3a453f);
        g.fill(x + 4, y + 4, x + 172, y + 22, 0xff222d25);
        g.fill(x + 8, y + 23, x + 108, y + 88, 0xff26332b);
        g.fill(x + 115, y + 25, x + 161, y + 78, 0xff26332b);
        g.drawString(font, ">", x + 112, y + 49, 0xffcfccae, false);
        for (int[] p : new int[][]{{20,28},{45,28},{18,65},{40,65},{62,65},{84,65},{134,44}}) {
            g.fill(x + p[0] - 2, y + p[1] - 2, x + p[0] + 18, y + p[1] + 18, 0xff141d17);
            g.fill(x + p[0] - 1, y + p[1] - 1, x + p[0] + 17, y + p[1] + 17, 0xff596559);
        }
        for (int i = 0; i < 36; i++) {
            int pX = x + 8 + (i % 9) * 18;
            int pY = y + (i < 27 ? 158 + (i / 9) * 18 : 216);
            g.fill(pX - 1, pY - 1, pX + 17, pY + 17, 0xff1a231b);
        }
        int activeX = menu.getMode() == DogTagWorkbenchMenu.MODE_REFINE ? x + 7 : x + 90;
        g.fill(activeX, y + 88, activeX + 79, y + 90, 0xffa0d48b);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0xffe1e9dc, false);
        g.drawString(font, Component.translatable("screen.survivordogtags.bench_tag"),
            10, 48, 0xffacbcad, false);
        g.drawString(font, Component.translatable("screen.survivordogtags.bench_photo"),
            45, 48, 0xffacbcad, false);
        g.drawString(font, Component.translatable("screen.survivordogtags.bench_materials"),
            9, 56, 0xffc1cfc1, false);
        g.drawString(font, Component.translatable("screen.survivordogtags.bench_result"),
            116, 25, 0xffc1cfc1, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xffe1e9dc, false);
        // Show each ingredient on its own row, including quantities already placed
        // in the four input slots. No ellipsis hiding the second 80-ingot cost.
        String hint = getMaterialHint();
        g.drawString(font, hint, 8, 113, 0xffc6d8bd, false);
        if (menu.getMode() == DogTagWorkbenchMenu.MODE_REFINE) {
            ItemStack oldTag = menu.getSlot(0).getItem();
            int current = oldTag.isEmpty() ? -1 : DogTagData.isBound(oldTag) ? DogTagData.level(oldTag) : 5;
            if (current < 5) {
                var costs = DogTagUpgradePlan.forCurrentLevel(current);
                for (int i = 0; i < costs.length && i < 2; i++) {
                    var need = costs[i];
                    String key = "material.survivordogtags." + need.material().name().toLowerCase(java.util.Locale.ROOT);
                    int have = countMaterial(need.material());
                    String amount = Component.translatable(key).getString() + " " + have + "/" + need.count();
                    g.drawString(font, amount, 10, 124 + i * 10,
                        have >= need.count() ? 0xffa9e5a0 : 0xffefc796, false);
                }
            }
        }
    }
    private int countMaterial(DogTagUpgradePlan.Material material) {
        int total = 0;
        var kind = DogTagMaterials.Kind.valueOf(material.name());
        for (int slot = 2; slot <= 5; slot++) {
            var stack = menu.getSlot(slot).getItem();
            if (kind.matches(stack)) total += stack.getCount();
        }
        return total;
    }
    /** Client-side hint only; the server checks all costs before committing. */
    private String getMaterialHint() {
        ItemStack oldTag = menu.getSlot(0).getItem();
        if (menu.getMode() == DogTagWorkbenchMenu.MODE_PHOTO) {
            return Component.translatable("screen.survivordogtags.hint_photo").getString();
        }
        if (!oldTag.isEmpty() && !DogTagData.isBound(oldTag)) {
            return Component.translatable("screen.survivordogtags.hint_unbound").getString();
        }
        int current = oldTag.isEmpty() ? -1 : DogTagData.level(oldTag);
        if (current >= 5) return Component.translatable("screen.survivordogtags.hint_max").getString();
        return current == -1
            ? Component.translatable("screen.survivordogtags.hint_create").getString()
            : Component.translatable("screen.survivordogtags.hint_upgrade", current + 1).getString();
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
    }
}
