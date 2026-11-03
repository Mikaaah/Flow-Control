package nl.townsandkingdoms.streamcontrol.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import nl.townsandkingdoms.streamcontrol.*;
public final class StreamControlClient implements ClientModInitializer {
 @Override public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Update.TYPE,(p,context)->context.client().execute(()->ClientUpdates.apply(p)));
 ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Open.TYPE,(packet,context)->context.client().execute(()->ControllerScreen.open(packet)));
 ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.UtilityOpen.TYPE,(p,context)->context.client().execute(()->UtilityScreen.open(p)));
 EngineeringClient.register();
 net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->ControllerPreview.tick());
 ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->FlowOverrides.receive("",true,null,0));
 }
}
