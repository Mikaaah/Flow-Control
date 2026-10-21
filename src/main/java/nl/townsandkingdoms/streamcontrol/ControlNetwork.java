package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.fabricmc.fabric.api.networking.v1.*;
public final class ControlNetwork {
 public record Update(String dimension,boolean reset,CompoundTag region,long remove,boolean enabled) implements CustomPacketPayload {
  public static final Type<Update> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(StreamControl.ID,"region"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Update> CODEC=new StreamCodec<>(){
   public Update decode(RegistryFriendlyByteBuf b){return new Update(b.readUtf(256),b.readBoolean(),b.readNbt(),b.readLong(),b.readBoolean());}
   public void encode(RegistryFriendlyByteBuf b,Update p){b.writeUtf(p.dimension(),256);b.writeBoolean(p.reset());b.writeNbt(p.region());b.writeLong(p.remove());b.writeBoolean(p.enabled());}
  };
  @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 public static void register(){PayloadTypeRegistry.playS2C().register(Update.TYPE,Update.CODEC);}
 public static void update(ServerLevel l,FlowRegion r){for(var p:l.players())ServerPlayNetworking.send(p,new Update(l.dimension().location().toString(),false,r.save(),0,ControlConfig.ENABLED.get()));}
 public static void remove(ServerLevel l,long key){for(var p:l.players())ServerPlayNetworking.send(p,new Update(l.dimension().location().toString(),false,null,key,ControlConfig.ENABLED.get()));}
 public static void sync(ServerPlayer p){var l=p.serverLevel();String dim=l.dimension().location().toString();ServerPlayNetworking.send(p,new Update(dim,true,null,0,ControlConfig.ENABLED.get()));for(var r:ControlData.get(l).all())ServerPlayNetworking.send(p,new Update(dim,false,r.save(),0,ControlConfig.ENABLED.get()));}
}
