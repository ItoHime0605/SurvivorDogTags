package tw.zonezero.survivordogtags.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side, bounded per-trigger-shot hit tracking. Pure Java so the two cases
 * which can otherwise break shotgun balance can be tested without Minecraft:
 * many pellets in one shot and two separate shots in the same game tick.
 */
public final class DogTagShotLedger {
    private DogTagShotLedger() {}

    private record Fired(long id, long tick) {}
    private record Hit(UUID shooter, UUID victim, String gunId, long shotId) {}
    private static final Map<UUID, Fired> FIRED = new HashMap<>();
    private static final LinkedHashMap<Hit, Long> HITS = new LinkedHashMap<>();
    private static long sequence;
    private static long lastSweep = Long.MIN_VALUE;

    /** Called once per actual TaCZ GunFireEvent, never once per pellet. */
    public static long beginShot(UUID shooter, long now) {
        sweep(now);
        if (sequence == Long.MAX_VALUE) sequence = 0;
        long shot = ++sequence; // zero is reserved for an unidentified projectile
        FIRED.put(shooter, new Fired(shot, now));
        return shot;
    }

    /** Called as each TaCZ projectile is added to its server-side world. */
    public static long currentShot(UUID shooter, long now) {
        sweep(now);
        Fired fired = FIRED.get(shooter);
        // Stock TaCZ calls GunFireEvent then spawns all pellets in the SAME
        // server tick. Never lend its token to delayed unrelated projectiles.
        return fired != null && now == fired.tick ? fired.id : 0;
    }

    /** Only the first direct hit of one shot on one target receives the flat bonus. */
    public static boolean markHit(UUID shooter, UUID victim, String gunId, long shotId, long now) {
        sweep(now);
        return HITS.putIfAbsent(new Hit(shooter, victim, gunId, shotId), now) == null;
    }

    public static void forgetShooter(UUID shooter) {
        FIRED.remove(shooter);
        HITS.keySet().removeIf(hit -> hit.shooter.equals(shooter));
    }

    private static void sweep(long now) {
        if (lastSweep != Long.MIN_VALUE && now >= lastSweep && now - lastSweep < 20
                && HITS.size() <= 4096 && FIRED.size() <= 1024) return;
        boolean clockReset = lastSweep != Long.MIN_VALUE && now < lastSweep;
        lastSweep = now;
        if (clockReset) { FIRED.clear(); HITS.clear(); return; }
        FIRED.entrySet().removeIf(e -> e.getValue().tick > now || now - e.getValue().tick > 120);
        HITS.entrySet().removeIf(e -> e.getValue() > now || now - e.getValue() > 120);
        trim(HITS, 4096);
        trim(FIRED, 1024);
    }

    private static <K,V> void trim(Map<K,V> map, int max) {
        Iterator<K> it = map.keySet().iterator();
        while (map.size() > max && it.hasNext()) { it.next(); it.remove(); }
    }
}
