package tw.zonezero.survivordogtags.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import tw.zonezero.survivordogtags.SurvivorDogTags;

/** Flat melee and direct TaCZ gun-hit bonus, paid at most once per shot per target. */
@EventBusSubscriber(modid = SurvivorDogTags.MODID)
public final class DogTagCombat {
    private DogTagCombat() {}
    private static final String SHOT_KEY = "survivordogtags:shot_id";

    public static int damageBonus(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> {
            var necklaces = handler.getStacksHandler("necklace");
            if (necklaces.isEmpty()) return 0;
            var stacks = necklaces.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (!handler.isSlotActive("necklace", i)) continue;
                ItemStack stack = stacks.getStackInSlot(i);
                if (DogTagData.isBound(stack)) {
                    // First active dog tag also owns the health/speed bonuses.
                    return DogTagProgression.forLevel(DogTagData.level(stack)).physicalDamage();
                }
            }
            return 0;
        }).orElse(0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMelee(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        int bonus = damageBonus(player);
        if (bonus > 0 && Float.isFinite(event.getAmount()) && event.getAmount() > 0) {
            float raised = event.getAmount() + bonus;
            if (Float.isFinite(raised)) event.setAmount(raised);
        }
    }

    /** TaCZ fires exactly one GunFireEvent for each burst-cycle / trigger shot. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGunFire(GunFireEvent event) {
        if (!event.isCanceled() && event.getShooter() instanceof ServerPlayer shooter) {
            DogTagShotLedger.beginShot(shooter.getUUID(), shooter.server.getTickCount());
        }
    }

    /** Each pellet/projectile of that shot gets the same server-only identifier. */
    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityKineticBullet bullet)) return;
        if (!(bullet.getOwner() instanceof ServerPlayer owner)) return;
        long token = DogTagShotLedger.currentShot(owner.getUUID(), owner.server.getTickCount());
        if (token != 0) bullet.getPersistentData().putLong(SHOT_KEY, token);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DogTagShotLedger.forgetShooter(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTaCZBullet(EntityHurtByGunEvent.Pre event) {
        if (event.isCanceled() ||
            !(event.getAttacker() instanceof ServerPlayer shooter) ||
            !(event.getHurtEntity() instanceof LivingEntity victim)) return;
        int bonus = damageBonus(shooter);
        if (bonus <= 0 || !Float.isFinite(event.getBaseAmount()) ||
            event.getBaseAmount() <= 0 || event.getBullet() == null ||
            event.getGunId() == null) return;
        float headshot = event.isHeadShot() ? event.getHeadshotMultiplier() : 1.0f;
        if (headshot <= 0 || !Float.isFinite(headshot)) return;

        long now = shooter.server.getTickCount();
        long shotId = event.getBullet().getPersistentData().getLong(SHOT_KEY);
        if (shotId == 0L) {
            // Unstamped projectiles cannot be safely identified as belonging to
            // one shotgun shot. Fail closed instead of granting +2 per pellet.
            // Stock TaCZ 1.1.8 emits GunFireEvent before spawning each projectile.
            return;
        }
        if (!DogTagShotLedger.markHit(shooter.getUUID(), victim.getUUID(),
                event.getGunId().toString(), shotId, now)) return;

        // TaCZ multiplies its base damage by the headshot factor AFTER this event.
        // Dividing the bonus here preserves a fixed +1/+2, not bonus * headshot.
        float raisedBase = DogTagDamageMath.boostedBase(event.getBaseAmount(), bonus, headshot);
        if (Float.isFinite(raisedBase)) event.setBaseAmount(raisedBase);
    }
}
