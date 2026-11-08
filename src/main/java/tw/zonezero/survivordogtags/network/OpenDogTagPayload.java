package tw.zonezero.survivordogtags.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import tw.zonezero.survivordogtags.SurvivorDogTags;

/** Server->client packet containing only the information needed to render the identity screen. */
public record OpenDogTagPayload(UUID owner, String ownerName, int level, int zombieKills,
                                ItemStack photo) implements CustomPacketPayload {
    public static final Type<OpenDogTagPayload> TYPE = new Type<>(SurvivorDogTags.id("open_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDogTagPayload> CODEC =
        StreamCodec.of((buf, p) -> {
            buf.writeUUID(p.owner());
            buf.writeUtf(p.ownerName(), 48);
            buf.writeVarInt(p.level());
            buf.writeVarInt(p.zombieKills());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.photo());
        }, buf -> new OpenDogTagPayload(buf.readUUID(), buf.readUtf(48),
            buf.readVarInt(), buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
