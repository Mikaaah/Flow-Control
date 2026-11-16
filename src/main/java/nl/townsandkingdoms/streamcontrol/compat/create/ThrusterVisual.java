package nl.townsandkingdoms.streamcontrol.compat.create;

import java.util.function.Consumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native Flywheel 1 visual. Both meshes use the supplied pivots, not Create's center-shaft shortcut. */
public final class ThrusterVisual extends AbstractBlockEntityVisual<MechanicalCurrentEntity> implements SimpleDynamicVisual {
    private final TransformedInstance shaft,propeller;
    private double lastTime,angle;
    public ThrusterVisual(VisualizationContext context,MechanicalCurrentEntity be,float partialTick) {
        super(context,be,partialTick);
        shaft=instancerProvider().instancer(InstanceTypes.TRANSFORMED,Models.partial(CreateClient.SHAFT)).createInstance();
        propeller=instancerProvider().instancer(InstanceTypes.TRANSFORMED,Models.partial(CreateClient.PROPELLER)).createInstance();
        lastTime=level.getGameTime()+partialTick;
        updateLight(partialTick);animate(partialTick);
    }
    @Override public void beginFrame(DynamicVisual.Context context) { animate(context.partialTick()); }
    @Override public void update(float partialTick) { animate(partialTick); }
    private void animate(float partial) {
        double now=level.getGameTime()+partial;
        double dt=net.minecraft.util.Mth.clamp(now-lastTime,0.0,5.0);lastTime=now;
        float rpm=blockEntity.getSpeed();if(!Float.isFinite(rpm))rpm=0;
        angle=(angle+rpm*.3*dt)%360;
        Direction facing=blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        float yaw=switch(facing){case EAST->-90;case SOUTH->-180;case WEST->-270;default->0;};
        PoseStack pose=new PoseStack();pose.translate(getVisualPosition().getX(),getVisualPosition().getY(),getVisualPosition().getZ());
        pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.translate(-.5,0,-.5);
        pose.pushPose();pose.translate(.5,.125,.5);pose.mulPose(Axis.YP.rotationDegrees((float)angle));pose.translate(-.5,-.125,-.5);
        shaft.setTransform(pose);shaft.setChanged();pose.popPose();
        pose.translate(.5,.5625,.1875);pose.mulPose(Axis.ZP.rotationDegrees((float)angle));pose.translate(-.5,-.5625,-.1875);
        propeller.setTransform(pose);propeller.setChanged();
    }
    @Override public void updateLight(float partialTick) { relight(shaft,propeller); }
    @Override protected void _delete() { shaft.delete();propeller.delete(); }
    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) { consumer.accept(shaft);consumer.accept(propeller); }
}
