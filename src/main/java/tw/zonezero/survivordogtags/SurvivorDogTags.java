package tw.zonezero.survivordogtags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import tw.zonezero.survivordogtags.game.DogTagItem;
import tw.zonezero.survivordogtags.network.TagNetwork;
import tw.zonezero.survivordogtags.workbench.DogTagWorkbenchBlock;
import tw.zonezero.survivordogtags.workbench.DogTagWorkbenchMenu;

@Mod(SurvivorDogTags.MODID)
public final class SurvivorDogTags {
    public static final String MODID = "survivordogtags";
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredBlock<Block> BENCH = BLOCKS.register("dog_tag_workbench",
        () -> new DogTagWorkbenchBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> BENCH_ITEM = ITEMS.registerSimpleBlockItem("dog_tag_workbench", BENCH);
    public static final DeferredItem<DogTagItem> DOG_TAG = ITEMS.register("survivor_dog_tag",
        () -> new DogTagItem(new Item.Properties().stacksTo(1)));
    public static final java.util.function.Supplier<MenuType<DogTagWorkbenchMenu>> WORKBENCH_MENU =
        MENUS.register("dog_tag_workbench", () -> IMenuTypeExtension.create(DogTagWorkbenchMenu::new));

    public SurvivorDogTags(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(TagNetwork::register);
        modBus.addListener(this::addCreativeTabs);
        // All actual gameplay events are registered on the NeoForge game bus by annotations.
    }

    private void addCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BENCH_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(DOG_TAG.get());
    }
}
