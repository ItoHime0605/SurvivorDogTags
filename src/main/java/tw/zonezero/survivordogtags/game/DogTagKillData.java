package tw.zonezero.survivordogtags.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;

/** Persistent per-world records: offline owners' kill counts do not disappear on logout. */
public final class DogTagKillData extends SavedData {
    private static final String SAVE_NAME = "survivordogtags_kill_records";
    private static final SavedData.Factory<DogTagKillData> FACTORY =
            new SavedData.Factory<>(DogTagKillData::new, DogTagKillData::load);
    private final Map<UUID, Integer> kills = new HashMap<>();

    public static DogTagKillData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, SAVE_NAME);
    }
    public int read(UUID owner) { return kills.getOrDefault(owner, 0); }
    public void ensureAtLeast(UUID owner, int count) {
        int newValue = Math.max(read(owner), Math.max(0, count));
        if (newValue != read(owner)) {
            kills.put(owner, newValue);
            setDirty();
        }
    }
    public void addKill(UUID owner) {
        int old = read(owner);
        if (old < Integer.MAX_VALUE) {
            kills.put(owner, old + 1);
            setDirty();
        }
    }
    public static DogTagKillData load(CompoundTag tag, HolderLookup.Provider registries) {
        DogTagKillData result = new DogTagKillData();
        CompoundTag records = tag.getCompound("Records");
        for (String key : records.getAllKeys()) {
            try {
                UUID owner = UUID.fromString(key);
                result.kills.put(owner, Math.max(0, records.getInt(key)));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted UUID keys without crashing the world.
            }
        }
        return result;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag records = new CompoundTag();
        kills.forEach((owner, count) -> records.putInt(owner.toString(), count));
        tag.put("Records", records);
        return tag;
    }
}
