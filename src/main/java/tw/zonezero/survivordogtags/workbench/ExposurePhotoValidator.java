package tw.zonezero.survivordogtags.workbench;

import java.lang.reflect.Method;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Only accepts photos with real Exposure frame data (never blank paper).
 * Reflection keeps the photo dependency out of the compile classpath, and
 * avoids touching Exposure client classes on a dedicated server.
 */
public final class ExposurePhotoValidator {
    private ExposurePhotoValidator() {}

    public static boolean isValid(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!itemId.equals("exposure:photograph") &&
            !itemId.equals("exposure:aged_photograph")) return false;
        try {
            Method getFrame = stack.getItem().getClass().getMethod("getFrame", ItemStack.class);
            Object frame = getFrame.invoke(stack.getItem(), stack);
            if (frame == null) return false;
            Object identifier = frame.getClass().getMethod("identifier").invoke(frame);
            return identifier != null &&
                !Boolean.TRUE.equals(identifier.getClass().getMethod("isEmpty").invoke(identifier));
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            // Fail closed. Never consume a photo whose frame cannot be verified.
            return false;
        }
    }
}
