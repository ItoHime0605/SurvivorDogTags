package tw.zonezero.survivordogtags.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Expand a stored Exposure photograph without duplicating its pixels or server records. */
public final class DogTagPhotoScreen extends Screen {
    private final Screen parent;
    private final ItemStack photo;

    public DogTagPhotoScreen(Screen parent, ItemStack photo) {
        super(Component.translatable("screen.survivordogtags.photo_enlarged"));
        this.parent = parent;
        this.photo = photo.copyWithCount(1);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics, mouseX, mouseY, tickDelta);
        int side = Math.max(16, Math.min(width - 48, height - 58));
        int x = (width - side) / 2;
        int y = (height - side) / 2;
        graphics.fill(x - 5, y - 5, x + side + 5, y + side + 5, 0xff303e34);
        graphics.fill(x - 2, y - 2, x + side + 2, y + side + 2, 0xffd3ddc7);
        graphics.fill(x, y, x + side, y + side, 0xff16241b);
        if (!ExposurePhotoRenderer.draw(graphics, photo, x, y, side, side)) {
            graphics.renderItem(photo, x + (side - 16) / 2, y + (side - 16) / 2);
            graphics.drawCenteredString(font, Component.translatable("screen.survivordogtags.photo_loading"),
                width / 2, y + side - 20, 0xffb5c5b3);
        }
        graphics.drawCenteredString(font, Component.translatable("screen.survivordogtags.photo_enlarged"),
            width / 2, Math.max(5, y - 19), 0xffe3f0d9);
        graphics.drawCenteredString(font, Component.translatable("screen.survivordogtags.photo_close"),
            width / 2, Math.min(height - 10, y + side + 14), 0xffa8c0a9);
        super.render(graphics, mouseX, mouseY, tickDelta);
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
