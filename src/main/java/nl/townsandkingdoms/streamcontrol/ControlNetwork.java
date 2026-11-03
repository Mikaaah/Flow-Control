package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ControlNetwork {
    /** Server-authoritative snapshot used to open the controller's client GUI. */
    public record Open(long pos, int radius, int vertical, double strength, double maxStrength,
                       int maxRadius, boolean connectedOnly, boolean still, boolean powered, int facing, CompoundTag settings)
        implements CustomPacketPayload {
        public Open(long pos,int radius,int vertical,double strength,double maxStrength,int maxRadius,boolean connectedOnly,boolean still,boolean powered,int facing) {
            this(pos,radius,vertical,strength,maxStrength,maxRadius,connectedOnly,still,powered,facing,new CompoundTag());
        }
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID, "open_controller_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = new StreamCodec<>() {
            public Open decode(RegistryFriendlyByteBuf b) {
                return new Open(b.readLong(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(),
                    b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readNbt());
            }
            public void encode(RegistryFriendlyByteBuf b, Open p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeVarInt(p.vertical); b.writeDouble(p.strength);
                b.writeDouble(p.maxStrength); b.writeVarInt(p.maxRadius); b.writeBoolean(p.connectedOnly);
                b.writeBoolean(p.still); b.writeBoolean(p.powered); b.writeVarInt(p.facing); b.writeNbt(p.settings);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Never trust client-supplied dimensions, speed or access rights. */
    public record Configure(long pos, int radius, double strength, boolean connectedOnly, int facing, CompoundTag settings)
        implements CustomPacketPayload {
        public Configure(long pos,int radius,double strength,boolean connectedOnly,int facing) {
            this(pos,radius,strength,connectedOnly,facing,new CompoundTag());
        }
        public static final Type<Configure> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID, "configure_controller_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Configure> CODEC = new StreamCodec<>() {
            public Configure decode(RegistryFriendlyByteBuf b) {
                return new Configure(b.readLong(), b.readVarInt(), b.readDouble(), b.readBoolean(), b.readVarInt(), b.readNbt());
            }
            public void encode(RegistryFriendlyByteBuf b, Configure p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeDouble(p.strength);
                b.writeBoolean(p.connectedOnly); b.writeVarInt(p.facing); b.writeNbt(p.settings);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UtilityOpen(long pos,CompoundTag settings) implements CustomPacketPayload {
        public static final Type<UtilityOpen> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID,"open_utility"));
        public static final StreamCodec<RegistryFriendlyByteBuf,UtilityOpen> CODEC=new StreamCodec<>() {
            public UtilityOpen decode(RegistryFriendlyByteBuf b) { return new UtilityOpen(b.readLong(),b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b,UtilityOpen p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record UtilityConfigure(long pos,CompoundTag settings) implements CustomPacketPayload {
        public static final Type<UtilityConfigure> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID,"configure_utility"));
        public static final StreamCodec<RegistryFriendlyByteBuf,UtilityConfigure> CODEC=new StreamCodec<>() {
            public UtilityConfigure decode(RegistryFriendlyByteBuf b) { return new UtilityConfigure(b.readLong(),b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b,UtilityConfigure p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Update(String dimension, boolean reset, CompoundTag region, long remove) implements CustomPacketPayload {
        public static final Type<Update> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID, "region"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = new StreamCodec<>() {
            public Update decode(RegistryFriendlyByteBuf b) { return new Update(b.readUtf(256), b.readBoolean(), b.readNbt(), b.readLong()); }
            public void encode(RegistryFriendlyByteBuf b, Update p) { b.writeUtf(p.dimension, 256); b.writeBoolean(p.reset); b.writeNbt(p.region); b.writeLong(p.remove); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToClient(UtilityOpen.TYPE,UtilityOpen.CODEC,(p,context)->context.enqueueWork(()->
            nl.townsandkingdoms.streamcontrol.client.UtilityScreen.open(p)));
        registrar.playToServer(UtilityConfigure.TYPE,UtilityConfigure.CODEC,(p,context)->context.enqueueWork(()->{
            Player player=context.player();BlockPos pos=BlockPos.of(p.pos());
            if(!(player.level() instanceof ServerLevel level) || !level.hasChunkAt(pos) || player.blockPosition().distSqr(pos)>64
                || !player.mayBuild() || !level.mayInteract(player,pos) || p.settings()==null) return;
            if(level.getBlockEntity(pos) instanceof UtilityAccess utility) utility.configure(p.settings());
        }));
        registrar.playToClient(Update.TYPE, Update.CODEC, (p, context) -> context.enqueueWork(() -> {
            nl.townsandkingdoms.streamcontrol.client.ClientUpdates.apply(p);
        }));
        registrar.playToClient(Open.TYPE, Open.CODEC, (p, context) -> context.enqueueWork(() ->
            nl.townsandkingdoms.streamcontrol.client.ControllerScreen.open(p)));
        registrar.playToServer(Configure.TYPE, Configure.CODEC, (p, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            BlockPos pos = BlockPos.of(p.pos());
            if (!(player.level() instanceof ServerLevel) || player.blockPosition().distSqr(pos) > 64
                || !player.mayBuild() || !player.level().mayInteract(player, pos)) return;
            if (!(player.level().getBlockEntity(pos) instanceof FlowController be)) return;
            FlowHeading d = FlowHeading.fromId(p.facing());
            if (d == null) return;
            if (p.settings()!=null && p.settings().contains("ProfileA") && p.settings().contains("ProfileB")) be.controls().configure(p.settings());
            else be.configure(d,ControlConfig.radius(p.radius()),ControlConfig.strength(p.strength()),p.connectedOnly());
        }));
    }
    public static void open(ServerPlayer player, FlowController be) {
        var state = be.blockEntity().getBlockState();
        PacketDistributor.sendToPlayer(player, new Open(be.blockEntity().getBlockPos().asLong(), be.radius(),
            be.controls().a().vertical(), be.strength(), ControlConfig.MAX_SPEED.get(),
            ControlConfig.MAX_RADIUS.get(), be.connectedOnly(), be.isStill(),
            state.getValue(ControllerBlock.POWERED), be.heading().id(), snapshot(be)));
    }
    private static CompoundTag snapshot(FlowController controller) {
        var n=controller.controls().save();n.putBoolean("Kinetic",!Float.isNaN(controller.rpm()));
        n.putFloat("RPM",Float.isFinite(controller.rpm())?controller.rpm():0);n.putInt("MaxVertical",ControlConfig.MAX_VERTICAL.get());return n;
    }
    public static void openUtility(ServerPlayer player,UtilityAccess be) {
        PacketDistributor.sendToPlayer(player,new UtilityOpen(be.blockEntity().getBlockPos().asLong(),be.settings()));
    }
    public static void update(ServerLevel level, FlowRegion r) { PacketDistributor.sendToPlayersInDimension(level, new Update(level.dimension().location().toString(), false, r.save(), 0)); }
    public static void remove(ServerLevel level, long key) { PacketDistributor.sendToPlayersInDimension(level, new Update(level.dimension().location().toString(), false, null, key)); }
    public static void sync(ServerPlayer player) {
        var level = player.serverLevel(); var dim = level.dimension().location().toString();
        PacketDistributor.sendToPlayer(player, new Update(dim, true, null, 0));
        for (var r : ControlData.get(level).all()) PacketDistributor.sendToPlayer(player, new Update(dim, false, r.save(), 0));
    }
    public static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
}
