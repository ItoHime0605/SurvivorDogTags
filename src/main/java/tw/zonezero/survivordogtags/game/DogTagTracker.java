package tw.zonezero.survivordogtags.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.stats.Stats;
import java.util.List;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.UUID;
import tw.zonezero.survivordogtags.SurvivorDogTags;

/** Server-authoritative kill counts, persisted across server restarts and owner logouts. */
@EventBusSubscriber(modid = SurvivorDogTags.MODID)
public final class DogTagTracker {
    private static final String LEGACY_KEY = "survivordogtags:zombie_kills";
    private static final TagKey<EntityType<?>> ZOMBIES = TagKey.create(
        Registries.ENTITY_TYPE, SurvivorDogTags.id("zombie_kills"));
    private DogTagTracker() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!(event.getEntity() instanceof Zombie) && !event.getEntity().getType().is(ZOMBIES)) return;
        DogTagKillData records = DogTagKillData.get(killer.server);
        records.ensureAtLeast(killer.getUUID(), killer.getPersistentData().getInt(LEGACY_KEY));
        records.addKill(killer.getUUID());
        // Maintain migration compatibility with v0.1.0-alpha player data.
        killer.getPersistentData().putInt(LEGACY_KEY, records.read(killer.getUUID()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Import pre-install vanilla zombie kills exactly once as a lower bound.
        // SavedData remains authoritative, so re-logins never add them again.
        DogTagKillData.get(player.server).ensureAtLeast(player.getUUID(),
            DogTagKillMath.baseline(player.getPersistentData().getInt(LEGACY_KEY),
                vanillaZombieKills(player)));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        int legacy = event.getOriginal().getPersistentData().getInt(LEGACY_KEY);
        event.getEntity().getPersistentData().putInt(LEGACY_KEY, legacy);
    }

    private static final List<EntityType<?>> VANILLA_ZOMBIE_TYPES = List.of(
        EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
        EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN);

    /** Native player stats may contain years of kills from before this mod. */
    private static int vanillaZombieKills(ServerPlayer player) {
        long total = 0;
        for (EntityType<?> type : VANILLA_ZOMBIE_TYPES) {
            total += Math.max(0, player.getStats().getValue(Stats.ENTITY_KILLED.get(type)));
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int getKills(ServerPlayer viewer, UUID owner, int fallback) {
        DogTagKillData data = DogTagKillData.get(viewer.server);
        ServerPlayer actual = viewer.server.getPlayerList().getPlayer(owner);
        if (actual != null) {
            data.ensureAtLeast(owner, DogTagKillMath.baseline(
                actual.getPersistentData().getInt(LEGACY_KEY), vanillaZombieKills(actual)));
        }
        return DogTagKillMath.combine(data.read(owner), 0, 0, fallback);
    }
}
