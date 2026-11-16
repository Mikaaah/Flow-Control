package nl.townsandkingdoms.streamcontrol.client;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import nl.townsandkingdoms.streamcontrol.StreamControl;
import nl.townsandkingdoms.streamcontrol.DormantMechanicalIntegration;

/** Fabric-side hooks only; common initialization never resolves rendering classes. */
public final class ConduitClient {
    private ConduitClient() {}
    public static void register() {
        BlockEntityRenderers.register(StreamControl.CONTROLLER.get(),ConduitBlockRenderer::new);
        BlockEntityRenderers.register(StreamControl.SENSOR_ENTITY.get(),ConduitBlockRenderer::new);
        BlockEntityRenderers.register(StreamControl.MOORING_ENTITY.get(),ConduitBlockRenderer::new);
        if(DormantMechanicalIntegration.ENTITY!=null) BlockEntityRenderers.register(
                DormantMechanicalIntegration.ENTITY.get(),ConduitBlockRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(),StreamControl.GENERATOR.get(),
                StreamControl.ANCHOR.get(),StreamControl.SENSOR.get(),StreamControl.MOORING.get());
        BuiltInRegistries.BLOCK.getOptional(ID("mechanical_current_generator")).ifPresent(block ->
                BlockRenderLayerMap.INSTANCE.putBlock(block,RenderType.cutout()));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public ResourceLocation getFabricId() { return ID("conduit_meshes"); }
            @Override public void onResourceManagerReload(ResourceManager manager) { ConduitBlockRenderer.clearCaches(); }
        });
    }
    private static ResourceLocation ID(String path) { return ResourceLocation.fromNamespaceAndPath(StreamControl.ID,path); }
}
