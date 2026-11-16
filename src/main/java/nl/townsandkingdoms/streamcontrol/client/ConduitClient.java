package nl.townsandkingdoms.streamcontrol.client;

import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import nl.townsandkingdoms.streamcontrol.StreamControl;
import nl.townsandkingdoms.streamcontrol.DormantMechanicalIntegration;

/** Rendering registration remains behind the Forge physical-client entry point. */
public final class ConduitClient {
    private ConduitClient() {}
    public static void register(IEventBus bus) {
        bus.addListener(ConduitClient::renderers);
        bus.addListener(ConduitClient::reload);
    }
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StreamControl.CONTROLLER.get(),ConduitBlockRenderer::new);
        event.registerBlockEntityRenderer(StreamControl.SENSOR_ENTITY.get(),ConduitBlockRenderer::new);
        event.registerBlockEntityRenderer(StreamControl.MOORING_ENTITY.get(),ConduitBlockRenderer::new);
        if(DormantMechanicalIntegration.ENTITY!=null) event.registerBlockEntityRenderer(
                DormantMechanicalIntegration.ENTITY.get(),ConduitBlockRenderer::new);
    }
    private static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> ConduitMeshes.clear());
    }
}
