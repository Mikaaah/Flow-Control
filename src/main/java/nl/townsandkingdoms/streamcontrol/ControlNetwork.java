package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.*;
public final class ControlNetwork {
    /** Server-authoritative snapshot used to open the controller's client GUI. */
    public record Open(long pos, int radius, int vertical, double strength, double maxStrength,
                       int maxRadius, boolean connectedOnly, boolean still, boolean powered, int facing, CompoundTag settings)
        implements CustomPacketPayload {
        public Open(long pos,int radius,int vertical,double strength,double maxStrength,int maxRadius,boolean connectedOnly,boolean still,boolean powered,int facing) {
            this(pos,radius,vertical,strength,maxStrength,maxRadius,connectedOnly,still,powered,facing,new CompoundTag());
        }
        public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "open_controller_v3"));
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
        public static final Type<Configure> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "configure_controller_v3"));
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
        public static final Type<UtilityOpen> TYPE=new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID,"open_utility"));
        public static final StreamCodec<RegistryFriendlyByteBuf,UtilityOpen> CODEC=new StreamCodec<>() {
            public UtilityOpen decode(RegistryFriendlyByteBuf b) { return new UtilityOpen(b.readLong(),b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b,UtilityOpen p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record UtilityConfigure(long pos,CompoundTag settings) implements CustomPacketPayload {
        public static final Type<UtilityConfigure> TYPE=new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID,"configure_utility"));
        public static final StreamCodec<RegistryFriendlyByteBuf,UtilityConfigure> CODEC=new StreamCodec<>() {
            public UtilityConfigure decode(RegistryFriendlyByteBuf b) { return new UtilityConfigure(b.readLong(),b.readNbt()); }
            public void encode(RegistryFriendlyByteBuf b,UtilityConfigure p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Update(String dimension, boolean reset, CompoundTag region, long remove,boolean enabled) implements CustomPacketPayload {
        public Update(String dimension,boolean reset,CompoundTag region,long remove) {this(dimension,reset,region,remove,ControlConfig.ENABLED.get());}
        public static final Type<Update> TYPE = new Type<>(Identifier.fromNamespaceAndPath(StreamControl.ID, "region"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC = new StreamCodec<>() {
            public Update decode(RegistryFriendlyByteBuf b) { return new Update(b.readUtf(256), b.readBoolean(), b.readNbt(), b.readLong(),b.readBoolean()); }
            public void encode(RegistryFriendlyByteBuf b, Update p) { b.writeUtf(p.dimension, 256); b.writeBoolean(p.reset); b.writeNbt(p.region); b.writeLong(p.remove);b.writeBoolean(p.enabled); }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void configure(ServerPlayer player,Configure p) {
        var pos=BlockPos.of(p.pos());var level=player.level();
        if(!level.hasChunkAt(pos) || player.blockPosition().distSqr(pos)>64 || !player.mayBuild() || !level.mayInteract(player,pos))return;
        if(!(level.getBlockEntity(pos) instanceof FlowController be))return;
        var heading=FlowHeading.fromId(p.facing());if(heading==null)return;
        if(p.settings()!=null && p.settings().contains("ProfileA") && p.settings().contains("ProfileB"))be.controls().configure(p.settings());
        else be.configure(heading,ControlConfig.radius(p.radius()),ControlConfig.strength(p.strength()),p.connectedOnly());
    }
    public static void configureUtility(ServerPlayer player,UtilityConfigure p) {
        var pos=BlockPos.of(p.pos());var level=player.level();
        if(!level.hasChunkAt(pos) || player.blockPosition().distSqr(pos)>64 || !player.mayBuild() || !level.mayInteract(player,pos) || p.settings()==null)return;
        if(level.getBlockEntity(pos) instanceof UtilityAccess be)be.configure(p.settings());
    }
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(Open.TYPE,Open.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(UtilityOpen.TYPE,UtilityOpen.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(Update.TYPE,Update.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(Configure.TYPE,Configure.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(UtilityConfigure.TYPE,UtilityConfigure.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Configure.TYPE,(p,c)->c.server().execute(()->configure(c.player(),p)));
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(UtilityConfigure.TYPE,(p,c)->c.server().execute(()->configureUtility(c.player(),p)));
    }
    public static void sendConfigure(Configure p) {net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(p);}
    public static void sendConfigure(UtilityConfigure p) {net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(p);}
    public static void open(ServerPlayer player,FlowController be) {
        var n=be.controls().save();n.putInt("MaxVertical",ControlConfig.MAX_VERTICAL.get());
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,new Open(be.blockEntity().getBlockPos().asLong(),be.radius(),be.controls().a().vertical(),be.strength(),ControlConfig.MAX_SPEED.get(),ControlConfig.MAX_RADIUS.get(),be.connectedOnly(),be.isStill(),be.blockEntity().getBlockState().getValue(ControllerBlock.POWERED),be.heading().id(),n));
    }
    public static void openUtility(ServerPlayer p,UtilityAccess be) {net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,new UtilityOpen(be.blockEntity().getBlockPos().asLong(),be.settings()));}
    private static void send(ServerPlayer p,Update u) {net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,u);}
    public static void update(ServerLevel l,FlowRegion r) {for(var p:l.players())send(p,new Update(l.dimension().identifier().toString(),false,r.save(),0));}
    public static void remove(ServerLevel l,long key) {for(var p:l.players())send(p,new Update(l.dimension().identifier().toString(),false,null,key));}
    public static void sync(ServerPlayer p) {var l=p.level();String dim=l.dimension().identifier().toString();send(p,new Update(dim,true,null,0));for(var r:ControlData.get(l).all())send(p,new Update(dim,false,r.save(),0));}
}
