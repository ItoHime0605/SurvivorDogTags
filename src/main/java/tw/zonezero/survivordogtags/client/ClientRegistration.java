package tw.zonezero.survivordogtags.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import tw.zonezero.survivordogtags.SurvivorDogTags;

@EventBusSubscriber(modid = SurvivorDogTags.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientRegistration {
    private ClientRegistration() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SurvivorDogTags.WORKBENCH_MENU.get(), DogTagWorkbenchScreen::new);
    }
}
