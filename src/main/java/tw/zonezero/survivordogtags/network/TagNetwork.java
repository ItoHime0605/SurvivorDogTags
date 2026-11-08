package tw.zonezero.survivordogtags.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TagNetwork {
    private TagNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenDogTagPayload.TYPE, OpenDogTagPayload.CODEC,
            (payload, context) -> context.enqueueWork(() ->
                tw.zonezero.survivordogtags.client.ClientPayloadHandler.openProfile(payload)));
    }
}
