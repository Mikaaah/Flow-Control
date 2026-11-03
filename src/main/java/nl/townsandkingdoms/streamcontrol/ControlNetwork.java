package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
public final class ControlNetwork {
 public interface Codec<T> { T decode(FriendlyByteBuf b); void encode(FriendlyByteBuf b,T p); }
    /** Server-authoritative snapshot used to open the controller's client GUI. */
    public record Open(long pos, int radius, int vertical, double strength, double maxStrength,
                       int maxRadius, boolean connectedOnly, boolean still, boolean powered, int facing, CompoundTag settings)
         {
        public Open(long pos,int radius,int vertical,double strength,double maxStrength,int maxRadius,boolean connectedOnly,boolean still,boolean powered,int facing) {
            this(pos,radius,vertical,strength,maxStrength,maxRadius,connectedOnly,still,powered,facing,new CompoundTag());
        }
        public static final ResourceLocation TYPE = new ResourceLocation(StreamControl.ID, "open_controller_v3");
        public static final Codec< Open> CODEC = new Codec<>() {
            public Open decode(FriendlyByteBuf b) {
                return new Open(b.readLong(), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(),
                    b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readNbt());
            }
            public void encode(FriendlyByteBuf b, Open p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeVarInt(p.vertical); b.writeDouble(p.strength);
                b.writeDouble(p.maxStrength); b.writeVarInt(p.maxRadius); b.writeBoolean(p.connectedOnly);
                b.writeBoolean(p.still); b.writeBoolean(p.powered); b.writeVarInt(p.facing); b.writeNbt(p.settings);
            }
        };
    }

    /** Never trust client-supplied dimensions, speed or access rights. */
    public record Configure(long pos, int radius, double strength, boolean connectedOnly, int facing, CompoundTag settings)
         {
        public Configure(long pos,int radius,double strength,boolean connectedOnly,int facing) {
            this(pos,radius,strength,connectedOnly,facing,new CompoundTag());
        }
        public static final ResourceLocation TYPE = new ResourceLocation(StreamControl.ID, "configure_controller_v3");
        public static final Codec< Configure> CODEC = new Codec<>() {
            public Configure decode(FriendlyByteBuf b) {
                return new Configure(b.readLong(), b.readVarInt(), b.readDouble(), b.readBoolean(), b.readVarInt(), b.readNbt());
            }
            public void encode(FriendlyByteBuf b, Configure p) {
                b.writeLong(p.pos); b.writeVarInt(p.radius); b.writeDouble(p.strength);
                b.writeBoolean(p.connectedOnly); b.writeVarInt(p.facing); b.writeNbt(p.settings);
            }
        };
    }

    public record UtilityOpen(long pos,CompoundTag settings)  {
        public static final ResourceLocation TYPE=new ResourceLocation(StreamControl.ID,"open_utility");
        public static final Codec<UtilityOpen> CODEC=new Codec<>() {
            public UtilityOpen decode(FriendlyByteBuf b) { return new UtilityOpen(b.readLong(),b.readNbt()); }
            public void encode(FriendlyByteBuf b,UtilityOpen p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
    }
    public record UtilityConfigure(long pos,CompoundTag settings)  {
        public static final ResourceLocation TYPE=new ResourceLocation(StreamControl.ID,"configure_utility");
        public static final Codec<UtilityConfigure> CODEC=new Codec<>() {
            public UtilityConfigure decode(FriendlyByteBuf b) { return new UtilityConfigure(b.readLong(),b.readNbt()); }
            public void encode(FriendlyByteBuf b,UtilityConfigure p) { b.writeLong(p.pos);b.writeNbt(p.settings); }
        };
    }
    public record Update(String dimension, boolean reset, CompoundTag region, long remove,boolean enabled)  {
        public Update(String dimension,boolean reset,CompoundTag region,long remove) {this(dimension,reset,region,remove,ControlConfig.ENABLED.get());}
        public static final ResourceLocation TYPE = new ResourceLocation(StreamControl.ID, "region");
        public static final Codec< Update> CODEC = new Codec<>() {
            public Update decode(FriendlyByteBuf b) { return new Update(b.readUtf(256), b.readBoolean(), b.readNbt(), b.readLong(),b.readBoolean()); }
            public void encode(FriendlyByteBuf b, Update p) { b.writeUtf(p.dimension, 256); b.writeBoolean(p.reset); b.writeNbt(p.region); b.writeLong(p.remove);b.writeBoolean(p.enabled); }
        };
    }
    public static void configure(ServerPlayer player,Configure p) {
        var pos=BlockPos.of(p.pos());var level=player.serverLevel();
        if(!level.hasChunkAt(pos) || player.blockPosition().distSqr(pos)>64 || !player.mayBuild() || !level.mayInteract(player,pos))return;
        if(!(level.getBlockEntity(pos) instanceof FlowController be))return;
        var heading=FlowHeading.fromId(p.facing());if(heading==null)return;
        if(p.settings()!=null && p.settings().contains("ProfileA") && p.settings().contains("ProfileB"))be.controls().configure(p.settings());
        else be.configure(heading,ControlConfig.radius(p.radius()),ControlConfig.strength(p.strength()),p.connectedOnly());
    }
    public static void configureUtility(ServerPlayer player,UtilityConfigure p) {
        var pos=BlockPos.of(p.pos());var level=player.serverLevel();
        if(!level.hasChunkAt(pos) || player.blockPosition().distSqr(pos)>64 || !player.mayBuild() || !level.mayInteract(player,pos) || p.settings()==null)return;
        if(level.getBlockEntity(pos) instanceof UtilityAccess be)be.configure(p.settings());
    }
    public static final ResourceLocation ID=Update.TYPE,OPEN_ID=Open.TYPE,CONFIGURE_ID=Configure.TYPE;
    public static Open decodeOpen(FriendlyByteBuf b) {return Open.CODEC.decode(b);}
    public static void encodeOpen(Open p,FriendlyByteBuf b) {Open.CODEC.encode(b,p);}
    public static Configure decodeConfigure(FriendlyByteBuf b) {return Configure.CODEC.decode(b);}
    public static void encodeConfigure(Configure p,FriendlyByteBuf b) {Configure.CODEC.encode(b,p);}
    public static Update decode(FriendlyByteBuf b) {return Update.CODEC.decode(b);}
    public static void encode(Update p,FriendlyByteBuf b) {Update.CODEC.encode(b,p);}
    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(Configure.TYPE,(server,player,handler,buf,sender)->{var packet=Configure.CODEC.decode(buf);server.execute(()->configure(player,packet));});
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(UtilityConfigure.TYPE,(server,player,handler,buf,sender)->{var packet=UtilityConfigure.CODEC.decode(buf);server.execute(()->configureUtility(player,packet));});
    }
    private static void send(ServerPlayer player,Object packet) {
        if(packet instanceof Open p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();Open.CODEC.encode(b,p);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,Open.TYPE,b);return;}
        if(packet instanceof UtilityOpen p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();UtilityOpen.CODEC.encode(b,p);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,UtilityOpen.TYPE,b);return;}
        if(packet instanceof Update p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();Update.CODEC.encode(b,p);net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,Update.TYPE,b);return;}
    }
    public static void sendConfigure(Configure p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();Configure.CODEC.encode(b,p);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(Configure.TYPE,b);}
    public static void sendConfigure(UtilityConfigure p){var b=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();UtilityConfigure.CODEC.encode(b,p);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(UtilityConfigure.TYPE,b);}
    public static void open(ServerPlayer player,FlowController be) {
        var n=be.controls().save();n.putInt("MaxVertical",ControlConfig.MAX_VERTICAL.get());
        send(player,new Open(be.blockEntity().getBlockPos().asLong(),be.radius(),be.controls().a().vertical(),be.strength(),ControlConfig.MAX_SPEED.get(),ControlConfig.MAX_RADIUS.get(),be.connectedOnly(),be.isStill(),be.blockEntity().getBlockState().getValue(ControllerBlock.POWERED),be.heading().id(),n));
    }
    public static void openUtility(ServerPlayer p,UtilityAccess be) {send(p,new UtilityOpen(be.blockEntity().getBlockPos().asLong(),be.settings()));}
    public static void update(ServerLevel l,FlowRegion r) {for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,r.save(),0));}
    public static void remove(ServerLevel l,long key) {for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,null,key));}
    public static void sync(ServerPlayer p) {var l=p.serverLevel();String dim=l.dimension().location().toString();send(p,new Update(dim,true,null,0));for(var r:ControlData.get(l).all())send(p,new Update(dim,false,r.save(),0));}
}
