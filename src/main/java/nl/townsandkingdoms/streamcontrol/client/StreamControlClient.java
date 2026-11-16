package nl.townsandkingdoms.streamcontrol.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import nl.townsandkingdoms.streamcontrol.*;
public final class StreamControlClient implements ClientModInitializer {
 @Override public void onInitializeClient(){ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Update.TYPE,(p,context)->context.client().execute(()->ClientUpdates.apply(p)));
 ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.Open.TYPE,(packet,context)->context.client().execute(()->ControllerScreen.open(packet)));
 ClientPlayNetworking.registerGlobalReceiver(ControlNetwork.UtilityOpen.TYPE,(p,context)->context.client().execute(()->UtilityScreen.open(p)));
 EngineeringClient.register();
 net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.CONTROLLER.get(),ConduitBlockRenderer::new);
 net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.SENSOR_ENTITY.get(),ConduitBlockRenderer::new);
 net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(StreamControl.MOORING_ENTITY.get(),ConduitBlockRenderer::new);
 net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(DormantMechanicalIntegration.ENTITY.get(),ConduitBlockRenderer::new);
 net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES).registerReloadListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(StreamControl.ID,"conduit_models"),ConduitBlockRenderer.reloadListener());
 net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->ControllerPreview.tick());
 ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{FlowOverrides.clearClient();ConduitAnimations.clear();EngineeringClient.clear();});
 }
}
