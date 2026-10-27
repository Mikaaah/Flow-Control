package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
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
        var direction = FlowHeading.fromId(p.facing());
        if (direction == null) return;
        be.configure(direction, ControlConfig.radius(p.radius()), ControlConfig.strength(p.strength()),p.connectedOnly());
    }

 public record Update(String dimension,boolean reset,CompoundTag region,long remove){}
 private static final String VERSION="3";
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(StreamControl.ID,"region"),()->VERSION,VERSION::equals,VERSION::equals);
 public static Update decode(FriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong());}
 public static void encode(Update p,FriendlyByteBuf b){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());}
 public static void register(){
 CHANNEL.registerMessage(0,Update.class,ControlNetwork::encode,ControlNetwork::decode,(p,s)->{var c=s.get();c.enqueueWork(()->nl.townsandkingdoms.streamcontrol.client.ClientUpdates.apply(p));c.setPacketHandled(true);},java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
 CHANNEL.registerMessage(1,Open.class,ControlNetwork::encodeOpen,ControlNetwork::decodeOpen,(p,s)->{var c=s.get();c.enqueueWork(()->nl.townsandkingdoms.streamcontrol.client.ControllerScreen.open(p));c.setPacketHandled(true);},java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
 CHANNEL.registerMessage(2,Configure.class,ControlNetwork::encodeConfigure,ControlNetwork::decodeConfigure,(p,s)->{var c=s.get();c.enqueueWork(()->{if(c.getSender()!=null)configure(c.getSender(),p);});c.setPacketHandled(true);},java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
 }
 public static void sendConfigure(Configure packet){CHANNEL.sendToServer(packet);}
 public static void open(ServerPlayer player,ControllerEntity be){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Open(be.getBlockPos().asLong(),be.radius(),ControlConfig.VERTICAL.get(),
            be.strength(),ControlConfig.MAX_SPEED.get(),ControlConfig.MAX_RADIUS.get(),be.connectedOnly(),
            ((ControllerBlock)be.getBlockState().getBlock()).still,be.getBlockState().getValue(ControllerBlock.POWERED),
            be.heading().id()));}
 public static void update(ServerLevel l,FlowRegion r){CHANNEL.send(PacketDistributor.DIMENSION.with(l::dimension),new Update(l.dimension().location().toString(),false,r.save(),0));}
 public static void remove(ServerLevel l,long key){CHANNEL.send(PacketDistributor.DIMENSION.with(l::dimension),new Update(l.dimension().location().toString(),false,null,key));}
 public static void sync(ServerPlayer p){var l=p.serverLevel();String dim=l.dimension().location().toString();CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(dim,true,null,0));for(var r:ControlData.get(l).all())CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(dim,false,r.save(),0));}
 public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
 public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
 public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
}
