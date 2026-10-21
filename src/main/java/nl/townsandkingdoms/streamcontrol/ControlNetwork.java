package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.fabricmc.fabric.api.networking.v1.*;
public final class ControlNetwork {
 public static final ResourceLocation ID=new ResourceLocation(StreamControl.ID,"region");
 public record Update(String dimension,boolean reset,CompoundTag region,long remove,boolean enabled){}
 public static void register(){}
 public static Update decode(FriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong(),b.readBoolean());}
 public static void encode(Update p,FriendlyByteBuf b){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());b.writeBoolean(p.enabled());}
 private static void send(ServerPlayer p,Update u){var b=PacketByteBufs.create();encode(u,b);ServerPlayNetworking.send(p,ID,b);}
 public static void update(ServerLevel l,FlowRegion r){for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,r.save(),0,ControlConfig.ENABLED.get()));}
 public static void remove(ServerLevel l,long key){for(var p:l.players())send(p,new Update(l.dimension().location().toString(),false,null,key,ControlConfig.ENABLED.get()));}
 public static void sync(ServerPlayer p){var l=p.serverLevel();String dim=l.dimension().location().toString();send(p,new Update(dim,true,null,0,ControlConfig.ENABLED.get()));for(var r:ControlData.get(l).all())send(p,new Update(dim,false,r.save(),0,ControlConfig.ENABLED.get()));}
}
