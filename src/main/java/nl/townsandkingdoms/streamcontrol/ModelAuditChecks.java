package nl.townsandkingdoms.streamcontrol;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
/** Opt-in all-five-block waterlogging and dormant kinetic fallback regression. */
public final class ModelAuditChecks {
 private static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 public static void run(ServerLevel level,Map<String,Object> results){
  Block mechanical=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(StreamControl.ID, "mechanical_current_generator"));
  check(mechanical!=null && mechanical!=Blocks.AIR,"Fifth block/dormant thruster missing");
  BlockPos pos=new BlockPos(720,150,720);
  for(Block b:new Block[]{StreamControl.GENERATOR.get(),StreamControl.ANCHOR.get(),StreamControl.SENSOR.get(),StreamControl.MOORING.get(),mechanical}){
   var dry=b.defaultBlockState();check(dry.hasProperty(BlockStateProperties.WATERLOGGED),"Not waterloggable: "+b);
   var wet=dry.setValue(BlockStateProperties.WATERLOGGED,true);
   check(wet.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.WATERLOGGED),"Rotation drained block: "+b);
   check(wet.mirror(Mirror.LEFT_RIGHT).getValue(BlockStateProperties.WATERLOGGED),"Mirror drained block: "+b);
   check(wet.getFluidState().is(FluidTags.WATER),"Wet state has no water: "+b);
   check(!dry.getFluidState().is(FluidTags.WATER),"Dry state unexpectedly contains water: "+b);
   check(!wet.canOcclude(),"Hollow model still occludes surrounding water: "+b);
   level.setBlock(pos,wet,2);check(level.getFluidState(pos).is(FluidTags.WATER),"Placement lost waterlogging: "+b);
   if(b==mechanical && level.getBlockEntity(pos) instanceof FlowController c){c.refresh();check(c.rpm()==0,"Unpowered/dormant thruster has RPM");check(!c.visualActive(),"Unpowered/dormant thruster active");check(ControlData.get(level).byKey(pos.asLong())==null,"Unpowered/dormant thruster generated free current");}
   level.removeBlock(pos,false);pos=pos.east(4);
  }
  results.put("allFiveBlocksWaterloggingRotationAndNonOcclusion",true);
  results.put("dormantOrStoppedThrusterCannotGenerateFreeFlow",true);
 }
}
