package nl.townsandkingdoms.streamcontrol.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import nl.townsandkingdoms.streamcontrol.*;
public final class StreamControlClient implements ClientModInitializer {
 @Override public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.ID,(client,handler,buf,sender)->{var p=ControlNetwork.decode(buf);client.execute(()->ClientUpdates.apply(p));});
 ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->FlowOverrides.receive("",true,null,0));
 }
}
