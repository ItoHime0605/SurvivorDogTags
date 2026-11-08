package tw.zonezero.survivordogtags.client;

import net.minecraft.client.Minecraft;
import tw.zonezero.survivordogtags.network.OpenDogTagPayload;

/** Referenced by packet callbacks, never directly loaded by the dedicated-server entrypoint. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}
    public static void openProfile(OpenDogTagPayload payload) {
        Minecraft.getInstance().setScreen(new DogTagProfileScreen(payload));
    }
}
