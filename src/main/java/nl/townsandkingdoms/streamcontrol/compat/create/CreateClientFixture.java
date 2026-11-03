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
        var p=ClientTestWorld.MECHANICAL;
        level.setBlock(p.below(),AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,Direction.UP),3);
        level.setBlock(p,CreateIntegration.BLOCK.get().defaultBlockState(),3);
        speed(level,128);
    }
    public static void speed(ServerLevel level,int rpm) {
        var motor=(CreativeMotorBlockEntity)level.getBlockEntity(ClientTestWorld.MECHANICAL.below());
        motor.generatedSpeed.setValue(rpm);motor.updateGeneratedRotation();
    }
}
