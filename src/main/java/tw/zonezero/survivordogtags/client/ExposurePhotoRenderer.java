package tw.zonezero.survivordogtags.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;

/** Optional reflective bridge: avoids copying image pixels / embedding Exposure code. */
public final class ExposurePhotoRenderer {
    private ExposurePhotoRenderer() {}
    private static boolean found = false;
    private static Method getRenderer;
    private static Method render;
    private static boolean attempted;
    // Never disable rendering of every photo because one picture is corrupt.
    private static ItemStack rejectedPhoto = ItemStack.EMPTY;

    public static boolean draw(GuiGraphics g, ItemStack photo, int x, int y, int w, int h) {
        if (photo.isEmpty() || !ModList.get().isLoaded("exposure")) return false;
        if (!rejectedPhoto.isEmpty() && ItemStack.isSameItemSameComponents(rejectedPhoto, photo)) return false;
        if (!attempted) init();
        if (!found) return false;
        boolean pushed = false;
        try {
            Object renderer = getRenderer.invoke(null);
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            g.pose().pushPose();
            pushed = true;
            try {
                // Exposure image coordinates are normalized and square. Preserve its
                // aspect ratio: do not stretch a photo into a non-square ID panel.
                int side = Math.max(1, Math.min(w, h));
                g.pose().translate(x + (w - side) / 2.0, y + (h - side) / 2.0, 200);
                g.pose().scale(side, side, side);
                return (boolean) render.invoke(renderer, photo, false, false, g.pose(),
                    buffers, LightTexture.FULL_BRIGHT);
            } finally {
                buffers.endBatch();
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            // A corrupt frame must not crash the GUI or break other photos.
            // Suppress retries for only this exact item, which otherwise would
            // repeat a renderer exception every frame.
            rejectedPhoto = photo.copyWithCount(1);
            return false;
        } finally {
            if (pushed) g.pose().popPose();
        }
    }
    private static void init() {
        attempted = true;
        try {
            Class<?> client = Class.forName("io.github.mortuusars.exposure.ExposureClient");
            Class<?> renderer = Class.forName("io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer");
            getRenderer = client.getMethod("photographRenderer");
            render = renderer.getMethod("render", ItemStack.class, boolean.class, boolean.class,
                PoseStack.class, MultiBufferSource.class, int.class);
            found = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            found = false;
        }
    }
}
