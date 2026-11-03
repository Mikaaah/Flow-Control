package nl.townsandkingdoms.streamcontrol.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;


public final class CreateClient {
    public static void renderers() { net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(CreateIntegration.ENTITY.get(),ShaftRenderer::new); }
    private static final class ShaftRenderer extends KineticBlockEntityRenderer<MechanicalCurrentEntity> {
        ShaftRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected void renderSafe(MechanicalCurrentEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            // Always render this un-instanced consumer's shaft, including when Flywheel is active.
            renderRotatingKineticBlock(be,shaft(Direction.Axis.Y),pose,buffers.getBuffer(RenderType.solid()),light);
        }
    }
}
