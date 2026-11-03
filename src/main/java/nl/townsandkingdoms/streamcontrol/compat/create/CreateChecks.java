package nl.townsandkingdoms.streamcontrol.compat.create;

import java.util.Map;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import nl.townsandkingdoms.streamcontrol.*;

/** Opt-in checks against the actual Create 6.0.10 kinetic network. */
public final class CreateChecks {
    private static void check(boolean b,String why) { if(!b) throw new AssertionError(why); }
    public static void run(ServerLevel level,Map<String,Object> results) {
        var pos=new BlockPos(400,120,400);var motorPos=pos.below();
        level.setBlock(motorPos,AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING,Direction.UP),3);
        var motor=(CreativeMotorBlockEntity)level.getBlockEntity(motorPos);motor.initialize();
        level.setBlock(pos,CreateIntegration.BLOCK.get().defaultBlockState(),3);var be=(MechanicalCurrentEntity)level.getBlockEntity(pos);be.initialize();
        be.configure(FlowHeading.EAST,4,1,false);
        for(var direction:Direction.values()) check(CreateIntegration.BLOCK.get().hasShaftTowards(level,pos,be.getBlockState(),direction)==(direction==Direction.DOWN),"Unexpected kinetic connection: "+direction);
        motor.generatedSpeed.setValue(128);motor.updateGeneratedRotation();for(int i=0;i<10;i++) { motor.tick();be.tick(); }
        check(Math.abs(be.getSpeed())==128 && be.hasNetwork(),"Mechanical controller did not join the motor network: "+be.getSpeed());be.refresh();
        var region=ControlData.get(level).byKey(pos.asLong());check(region!=null && region.strength()==1,"128 RPM did not produce configured strength");
        double originalX=region.dx();
        motor.generatedSpeed.setValue(1);motor.updateGeneratedRotation();for(int i=0;i<10;i++) { motor.tick();be.tick(); }be.refresh();
        var slow=ControlData.get(level).byKey(pos.asLong());check(slow!=null && Math.abs(slow.strength()-1.0/128)<1e-8,"Low RPM was clamped to manual minimum strength");
        var loaded=FlowRegion.load(slow.save());check(loaded.strength()==slow.strength(),"Sub-5-percent flow did not survive sync/persistence");
        motor.generatedSpeed.setValue(-128);motor.updateGeneratedRotation();for(int i=0;i<10;i++) { motor.tick();be.tick(); }be.refresh();
        region=ControlData.get(level).byKey(pos.asLong());check(region!=null && region.dx()==-originalX,"Motor reversal did not reverse flow");
        check(be.calculateStressApplied()==ControlConfig.KINETIC_STRESS.get().floatValue(),"Kinetic stress consumer value");
        be.updateFromNetwork(0,512,2);be.refresh();check(ControlData.get(level).byKey(pos.asLong())==null,"Overstressed drive left an active flow override");
        motor.generatedSpeed.setValue(0);motor.updateGeneratedRotation();for(int i=0;i<10;i++) { motor.tick();be.tick(); }be.refresh();
        check(ControlData.get(level).byKey(pos.asLong())==null,"Stopped motor left an active override");
        level.removeBlock(pos,false);level.removeBlock(motorPos,false);results.put("createRealNetworkRpmReverseStressAndStop",true);
    }
}
