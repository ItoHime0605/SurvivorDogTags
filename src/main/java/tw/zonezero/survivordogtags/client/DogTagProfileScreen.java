package tw.zonezero.survivordogtags.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import tw.zonezero.survivordogtags.network.OpenDogTagPayload;

/** Static military-terminal layout; real player model plus renderable Exposure photo. */
public final class DogTagProfileScreen extends Screen {
    private final OpenDogTagPayload record;
    private boolean modelRenderingUnavailable;
    // GUI-only model for offline or distant owners; never spawned into the world.
    private RemotePlayer offlinePreview;
    private ClientLevel offlinePreviewLevel;
    private boolean showingFallbackModel;
    private static final int WIDTH = 320;
    private static final int HEIGHT = 196;
    public DogTagProfileScreen(OpenDogTagPayload record) {
        super(Component.translatable("screen.survivordogtags.title"));
        this.record = record;
    }
    private void txt(GuiGraphics g, String key, int x, int y, int color) {
        g.drawString(font, Component.translatable(key), x, y, color, false);
    }
    /** Prefer the real loaded player; otherwise render an inert skin preview. */
    private Player getOwnerModel() {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return null;
        for (Player player : world.players()) {
            if (player.getUUID().equals(record.owner())) {
                showingFallbackModel = false;
                return player;
            }
        }
        showingFallbackModel = true;
        if (offlinePreview == null || offlinePreviewLevel != world) {
            // Minecraft may show a default skin until the owner's texture is
            // in the client skin cache. No extra network calls are made here.
            offlinePreview = new RemotePlayer(world,
                new GameProfile(record.owner(), record.ownerName()));
            offlinePreviewLevel = world;
        }
        return offlinePreview;
    }
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        this.renderBackground(g, mx, my, delta);
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xff151b18);
        g.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xff2a352f);
        g.fill(x + 7, y + 7, x + WIDTH - 7, y + 25, 0xff19241e);
        txt(g, "screen.survivordogtags.title", x + 14, y + 13, 0xffe2ecdd);
        g.drawString(font, "ZONE ZERO / ID RECORD", x + 139, y + 13, 0xffa0ac9b, false);

        g.fill(x + 10, y + 33, x + 122, y + 157, 0xff18241e);
        g.fill(x + 11, y + 34, x + 121, y + 156, 0xff344139);
        txt(g, "screen.survivordogtags.model", x + 17, y + 38, 0xffcadbc8);
        Player ownerModel = null;
        if (!modelRenderingUnavailable) {
            try {
                ownerModel = getOwnerModel();
            } catch (RuntimeException | LinkageError previewError) {
                modelRenderingUnavailable = true;
            }
        }
        if (ownerModel != null && !modelRenderingUnavailable) {
            // Some modded player renderers throw on inventory-preview rendering.
            // An unavailable preview must not crash the entire identity screen.
            try {
                InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 17, y + 54, x + 115,
                    y + 148, 43, 0.0f, 0.0f, 0.0f, ownerModel);
            } catch (RuntimeException | LinkageError renderError) {
                modelRenderingUnavailable = true;
            }
        }
        if (ownerModel == null || modelRenderingUnavailable) {
            g.drawString(font, Component.translatable("screen.survivordogtags.model_unavailable"),
                x + 18, y + 95, 0xffb3bfae, false);
        } else if (showingFallbackModel) {
            // Preview is still usable if the owner is offline; appearance may
            // temporarily fall back to Minecraft's default skin.
            g.drawString(font, Component.translatable("screen.survivordogtags.offline_preview"),
                x + 15, y + 144, 0xffd1dbc5, false);
        }
        g.fill(x + 131, y + 33, x + 309, y + 89, 0xff1a261f);
        txt(g, "screen.survivordogtags.name", x + 139, y + 39, 0xffa7b8a5);
        g.drawString(font, font.plainSubstrByWidth(record.ownerName(), 163), x + 139, y + 54, 0xffe7f4e3, false);
        txt(g, "screen.survivordogtags.kills", x + 139, y + 66, 0xffa7b8a5);
        g.drawString(font, String.format(java.util.Locale.ROOT, "%,d", record.zombieKills()),
            x + 245, y + 66, 0xfff6c697, false);
        txt(g, "screen.survivordogtags.level", x + 139, y + 78, 0xffa7b8a5);
        g.drawString(font, "Lv." + record.level(), x + 252, y + 78, 0xffd2e5c9, false);

        g.fill(x + 131, y + 94, x + 309, y + 169, 0xff101a14);
        txt(g, "screen.survivordogtags.photo", x + 139, y + 97, 0xffcadbc8);
        txt(g, "screen.survivordogtags.zoom_hint", x + 220, y + 97, 0xff9dbaa0);
        ItemStack photo = record.photo();
        if (!photo.isEmpty()) {
            if (!ExposurePhotoRenderer.draw(g, photo, x + 154, y + 111, 130, 51)) {
                g.renderItem(photo, x + 209, y + 129);
                txt(g, "screen.survivordogtags.photo_loading", x + 142, y + 153, 0xffacbaaa);
            }
        } else {
            txt(g, "screen.survivordogtags.no_photo", x + 159, y + 126, 0xffd0d8cc);
        }
        g.fill(x + 10, y + 173, x + 309, y + 187, 0xff17231c);
        txt(g, "screen.survivordogtags.footer", x + 16, y + 176, 0xff98b19b);
        super.render(g, mx, my, delta);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - WIDTH) / 2;
        int y = (height - HEIGHT) / 2;
        if (button == 0 && !record.photo().isEmpty()
            && mouseX >= x + 131 && mouseX < x + 309
            && mouseY >= y + 94 && mouseY < y + 169) {
            Minecraft.getInstance().setScreen(new DogTagPhotoScreen(this, record.photo()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean isPauseScreen() { return false; }
}
