package nl.townsandkingdoms.streamcontrol.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.townsandkingdoms.streamcontrol.ClientTestWorld;

/** Loaded only by the opt-in test fixture with Create present. */
public final class CreateClientFixture {
    public static void prepare(ServerLevel level) {
        prepare(level,ClientTestWorld.MECHANICAL);
    }
    public static void prepare(ServerLevel level,net.minecraft.core.BlockPos p) {
        level.setBlock(p.below(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,Direction.UP),3);
        level.setBlock(p,CreateIntegration.BLOCK.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)),3);
        speed(level,p,128);
    }
    public static boolean visualizationSupported(net.minecraft.world.level.Level level) {
        return dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(level);
    }
    public static void speed(ServerLevel level,int rpm) { speed(level,ClientTestWorld.MECHANICAL,rpm); }
    public static void speed(ServerLevel level,net.minecraft.core.BlockPos p,int rpm) {
        var motor=(CreativeMotorBlockEntity)level.getBlockEntity(p.below());
        motor.generatedSpeed.setValue(rpm);motor.updateGeneratedRotation();
    }
}
