package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.fabricmc.fabric.api.networking.v1.*;
public final class ControlNetwork {
    /** Server-authoritative snapshot used to open the controller's client GUI. */
    public record Open(long pos, int radius, int vertical, double strength, double maxStrength,
                       int maxRadius, boolean connectedOnly, boolean still, boolean powered, int facing)
        implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "open_controller_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = new StreamCodec<>() {
            public Open decode(RegistryFriendlyByteBuf b) {
                return new Open(b.readLong(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(),
                    b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readVarInt());
            }
            public void encode(RegistryFriendlyByteBuf b, Open p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeVarInt(p.vertical); b.writeDouble(p.strength);
                b.writeDouble(p.maxStrength); b.writeVarInt(p.maxRadius); b.writeBoolean(p.connectedOnly);
                b.writeBoolean(p.still); b.writeBoolean(p.powered); b.writeVarInt(p.facing);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Never trust client-supplied dimensions, speed or access rights. */
    public record Configure(long pos, int radius, double strength, boolean connectedOnly, int facing)
        implements CustomPacketPayload {
        public static final Type<Configure> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "configure_controller_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Configure> CODEC = new StreamCodec<>() {
            public Configure decode(RegistryFriendlyByteBuf b) {
                return new Configure(b.readLong(), b.readVarInt(), b.readDouble(), b.readBoolean(), b.readVarInt());
            }
            public void encode(RegistryFriendlyByteBuf b, Configure p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeDouble(p.strength);
                b.writeBoolean(p.connectedOnly); b.writeVarInt(p.facing);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void configure(ServerPlayer player, Configure p) {
        var pos = net.minecraft.core.BlockPos.of(p.pos());
        if (player.blockPosition().distSqr(pos) > 64 || !player.mayBuild()
            || !player.level().mayInteract(player,pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof ControllerEntity be)) return;
        var direction = FlowHeading.fromId(p.facing());
        if (direction == null) return;
        be.configure(direction, ControlConfig.radius(p.radius()), ControlConfig.strength(p.strength()),p.connectedOnly());
    }

 public record Update(String dimension,boolean reset,CompoundTag region,long remove,boolean enabled) implements CustomPacketPayload {
  public static final Type<Update> TYPE=new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID,"region"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Update> CODEC=new StreamCodec<>(){
   public Update decode(RegistryFriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,Update p){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());b.writeBoolean(p.enabled());}
  };
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(){PayloadTypeRegistry.clientboundPlay().register(Update.TYPE,Update.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Configure.TYPE,Configure.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Configure.TYPE,(packet,context) -> context.server().execute(() -> configure(context.player(),packet)));}
 public static void open(ServerPlayer player, ControllerEntity be) { ServerPlayNetworking.send(player,new Open(be.getBlockPos().asLong(),be.radius(),ControlConfig.VERTICAL.get(),
            be.strength(),ControlConfig.MAX_SPEED.get(),ControlConfig.MAX_RADIUS.get(),be.connectedOnly(),
            ((ControllerBlock)be.getBlockState().getBlock()).still,be.getBlockState().getValue(ControllerBlock.POWERED),
            be.heading().id())); }
 public static void sendConfigure(Configure packet) { net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet); }
 public static void update(ServerLevel l,FlowRegion r){for(var p:l.players())ServerPlayNetworking.send(p,new Update(l.dimension().identifier().toString(),false,r.save(),0,ControlConfig.ENABLED.get()));}
 public static void remove(ServerLevel l,long key){for(var p:l.players())ServerPlayNetworking.send(p,new Update(l.dimension().identifier().toString(),false,null,key,ControlConfig.ENABLED.get()));}
 public static void sync(ServerPlayer p){var l=p.level();String dim=l.dimension().identifier().toString();ServerPlayNetworking.send(p,new Update(dim,true,null,0,ControlConfig.ENABLED.get()));for(var r:ControlData.get(l).all())ServerPlayNetworking.send(p,new Update(dim,false,r.save(),0,ControlConfig.ENABLED.get()));}
}
