package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
public final class ControlNetwork {
 public record Update(String dimension,boolean reset,CompoundTag region,long remove){}
 private static final String VERSION="1";
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(StreamControl.ID,"region"),()->VERSION,VERSION::equals,VERSION::equals);
 public static Update decode(FriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong());}
 public static void encode(Update p,FriendlyByteBuf b){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());}
 public static void register(){CHANNEL.registerMessage(0,Update.class,ControlNetwork::encode,ControlNetwork::decode,(p,s)->{var c=s.get();c.enqueueWork(()->nl.townsandkingdoms.streamcontrol.client.ClientUpdates.apply(p));c.setPacketHandled(true);},java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
 public static void update(ServerLevel l,FlowRegion r){CHANNEL.send(PacketDistributor.DIMENSION.with(l::dimension),new Update(l.dimension().location().toString(),false,r.save(),0));}
 public static void remove(ServerLevel l,long key){CHANNEL.send(PacketDistributor.DIMENSION.with(l::dimension),new Update(l.dimension().location().toString(),false,null,key));}
 public static void sync(ServerPlayer p){var l=p.serverLevel();String dim=l.dimension().location().toString();CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(dim,true,null,0));for(var r:ControlData.get(l).all())CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Update(dim,false,r.save(),0));}
 public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
 public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
 public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
}
