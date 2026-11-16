package nl.townsandkingdoms.streamcontrol.compat.create;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import nl.townsandkingdoms.streamcontrol.client.ConduitBlockRenderer;

/** Loaded only after ModList confirms Create; no Create/Flywheel types escape this package. */
public final class CreateClient {
    static final PartialModel SHAFT=PartialModel.of(new ResourceLocation("streamcontrol","block/parts/mechanical_current_generator/shaft"));
    static final PartialModel PROPELLER=PartialModel.of(new ResourceLocation("streamcontrol","block/parts/mechanical_current_generator/propeller"));
    /** Initialize PartialModel registrations before the first model-baking reload. */
    public static void bootstrap() {}
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CreateIntegration.ENTITY.get(),ThrusterRenderer::new);
        SimpleBlockEntityVisualizer.builder(CreateIntegration.ENTITY.get()).factory(ThrusterVisual::new)
            .skipVanillaRender(be->true).apply();
    }
    private static final class ThrusterRenderer implements BlockEntityRenderer<MechanicalCurrentEntity> {
        ThrusterRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(MechanicalCurrentEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            // Flywheel owns exactly the two supplied moving partials when it is enabled.
            if(!VisualizationManager.supportsVisualization(be.getLevel()))
                ConduitBlockRenderer.renderParts(be,partial,pose,buffers,light,overlay);
        }
    }
}
