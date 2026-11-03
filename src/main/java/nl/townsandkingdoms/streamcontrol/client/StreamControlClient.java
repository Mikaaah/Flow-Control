package nl.townsandkingdoms.streamcontrol.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import nl.townsandkingdoms.streamcontrol.*;
public final class StreamControlClient implements ClientModInitializer {
 public void onInitializeClient(){
  ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Open.TYPE,(client,handler,buf,sender)->{var p=ControlNetwork.Open.CODEC.decode(buf);client.execute(()->ControllerScreen.open(p));});
  ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.UtilityOpen.TYPE,(client,handler,buf,sender)->{var p=ControlNetwork.UtilityOpen.CODEC.decode(buf);client.execute(()->UtilityScreen.open(p));});
  ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Update.TYPE,(client,handler,buf,sender)->{var p=ControlNetwork.Update.CODEC.decode(buf);client.execute(()->ClientUpdates.apply(p));});
  EngineeringClient.register();net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->ControllerPreview.tick());
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->FlowOverrides.receive("",true,null,0));
 }
}
