package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.fabricmc.fabric.api.networking.v1.*;
public final class ControlNetwork {
 public record Open(long pos,int radius,int vertical,double strength,double maxStrength,int maxRadius,boolean connectedOnly,boolean still,boolean powered,int facing) {}
 public record Configure(long pos,int radius,double strength,boolean connectedOnly,int facing) {}
 public static Open decodeOpen(FriendlyByteBuf b) { return new Open(b.readLong(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt()); }
 public static void encodeOpen(Open p,FriendlyByteBuf b) { b.writeLong(p.pos());b.writeVarInt(p.radius());b.writeVarInt(p.vertical());b.writeDouble(p.strength());b.writeDouble(p.maxStrength());b.writeVarInt(p.maxRadius());b.writeBoolean(p.connectedOnly());b.writeBoolean(p.still());b.writeBoolean(p.powered());b.writeVarInt(p.facing()); }
 public static Configure decodeConfigure(FriendlyByteBuf b) {return new Configure(b.readLong(),b.readVarInt(),b.readDouble(),b.readBoolean(),b.readVarInt());}
 public static void encodeConfigure(Configure p,FriendlyByteBuf b) {b.writeLong(p.pos());b.writeVarInt(p.radius());b.writeDouble(p.strength());b.writeBoolean(p.connectedOnly());b.writeVarInt(p.facing());}
    public static void configure(ServerPlayer player, Configure p) {
        var pos = net.minecraft.core.BlockPos.of(p.pos());
        if (player.blockPosition().distSqr(pos) > 64 || !player.mayBuild()
            || !player.level().mayInteract(player,pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof ControllerEntity be)) return;
        if (p.facing() < 0 || p.facing() > 5) return;
        var direction = net.minecraft.core.Direction.from3DDataValue(p.facing());
        if (!direction.getAxis().isHorizontal()) return;
        be.configure(direction, ControlConfig.radius(p.radius()), ControlConfig.strength(p.strength()),p.connectedOnly());
    }

 public static final ResourceLocation ID=new ResourceLocation(StreamControl.ID,"region");
 public record Update(String dimension,boolean reset,CompoundTag region,long remove,boolean enabled){}
 public static final ResourceLocation OPEN_ID=new ResourceLocation(StreamControl.ID,"open_controller");
 public static final ResourceLocation CONFIGURE_ID=new ResourceLocation(StreamControl.ID,"configure_controller");
 public static void register(){ServerPlayNetworking.registerGlobalReceiver(CONFIGURE_ID,(server,player,handler,buf,sender)->{var packet=decodeConfigure(buf);server.execute(()->configure(player,packet));});}
 public static Update decode(FriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong(),b.readBoolean());}
 public static void encode(Update p,FriendlyByteBuf b){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());b.writeBoolean(p.enabled());}
 private static void send(ServerPlayer p,Update u){var b=PacketByteBufs.create();encode(u,b);ServerPlayNetworking.send(p,ID,b);}
 public static void open(ServerPlayer player,ControllerEntity be) {var b=PacketByteBufs.create();encodeOpen(new Open(be.getBlockPos().asLong(),be.radius(),ControlConfig.VERTICAL.get(),
            be.strength(),ControlConfig.MAX_SPEED.get(),ControlConfig.MAX_RADIUS.get(),be.connectedOnly(),
            ((ControllerBlock)be.getBlockState().getBlock()).still,be.getBlockState().getValue(ControllerBlock.POWERED),
            be.getBlockState().getValue(ControllerBlock.FACING).get3DDataValue()),b);ServerPlayNetworking.send(player,OPEN_ID,b);}
 public static void sendConfigure(Configure packet) {var b=PacketByteBufs.create();encodeConfigure(packet,b);net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(CONFIGURE_ID,b);}
 public static void update(ServerLevel l,FlowRegion r){for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,r.save(),0,ControlConfig.ENABLED.get()));}
 public static void remove(ServerLevel l,long key){for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,null,key,ControlConfig.ENABLED.get()));}
 public static void sync(ServerPlayer p){var l=p.serverLevel();String dim=l.dimension().location().toString();send(p,new Update(dim,true,null,0,ControlConfig.ENABLED.get()));for(var r:ControlData.get(l).all())send(p,new Update(dim,false,r.save(),0,ControlConfig.ENABLED.get()));}
}
