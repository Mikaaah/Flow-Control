package nl.townsandkingdoms.streamcontrol;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
/** Opt-in release data-pack check; invoked only by the disposable server harness. */
public final class ReleaseAssetChecks {
 public static void run(ServerLevel level,Map<String,Object> results){
  BlockPos pos=new BlockPos(512,120,512);
  for(Block block:new Block[]{StreamControl.GENERATOR.get(),StreamControl.ANCHOR.get(),StreamControl.SENSOR.get(),StreamControl.MOORING.get()}){
   var state=block.defaultBlockState();level.setBlock(pos,state,3);
   var drops=Block.getDrops(state,level,pos,level.getBlockEntity(pos));
   if(drops.stream().noneMatch(stack->stack.is(block.asItem())))throw new AssertionError("Survival block drop missing: "+block);
   level.removeBlock(pos,false);pos=pos.east(4);
  }
  results.put("allSurvivalBlockDrops",true);
 }
}
