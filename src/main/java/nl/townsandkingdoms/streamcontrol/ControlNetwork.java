package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ControlNetwork {
    public record Update(String dimension, boolean reset, CompoundTag region, long remove) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "region"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = new StreamCodec<>() {
            public Update decode(RegistryFriendlyByteBuf b) { return new Update(b.readUtf(256), b.readBoolean(), b.readNbt(), b.readLong()); }
            public void encode(RegistryFriendlyByteBuf b, Update p) { b.writeUtf(p.dimension, 256); b.writeBoolean(p.reset); b.writeNbt(p.region); b.writeLong(p.remove); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Update.TYPE, Update.CODEC, (p, context) -> context.enqueueWork(() -> {
            nl.townsandkingdoms.streamcontrol.client.ClientUpdates.apply(p);
        }));
    }
    public static void update(ServerLevel level, FlowRegion r) { PacketDistributor.sendToPlayersInDimension(level, new Update(level.dimension().identifier().toString(), false, r.save(), 0)); }
    public static void remove(ServerLevel level, long key) { PacketDistributor.sendToPlayersInDimension(level, new Update(level.dimension().identifier().toString(), false, null, key)); }
    public static void sync(ServerPlayer player) {
        var level = player.level(); var dim = level.dimension().identifier().toString();
        PacketDistributor.sendToPlayer(player, new Update(dim, true, null, 0));
        for (var r : ControlData.get(level).all()) PacketDistributor.sendToPlayer(player, new Update(dim, false, r.save(), 0));
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
}
