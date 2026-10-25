package nl.townsandkingdoms.streamcontrol;
import java.util.UUID;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import dev.streamsreflowing.flow.StreamFlowGrid;
/** Regression checks run only by the explicit disposable-world integration harness. */
public final class ControllerRegressionChecks {
 private static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 public static void run(ServerLevel level){
  var center=new BlockPos(176,140,176);var data=ControlData.get(level);var owner=new UUID(0,0);
  level.setBlock(center,Blocks.STONE.defaultBlockState(),3);
  level.setBlock(center.east(),Blocks.WATER.defaultBlockState(),3);
  level.setBlock(center.east(2),Blocks.STONE.defaultBlockState(),3);
  level.setBlock(center.east(3),Blocks.WATER.defaultBlockState(),3);
  var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
  try {
   var edit=new ControlNetwork.Configure(center.asLong(),4,.5,true,Direction.EAST.get3DDataValue());
   ControlNetwork.encodeConfigure(edit,buf);
   check(ControlNetwork.decodeConfigure(buf).equals(edit),"GUI configure packet round trip");
   buf.clear();
   var snapshot=new ControlNetwork.Open(center.asLong(),4,0,.5,2,16,true,false,false,Direction.EAST.get3DDataValue());
   ControlNetwork.encodeOpen(snapshot,buf);
   check(ControlNetwork.decodeOpen(buf).equals(snapshot),"GUI open packet round trip");
  } finally {buf.release();}
  var mask=ConnectedWater.scan(level,center,Direction.EAST,4,0);
  var region=new FlowRegion(901,center,4,0,1,0,.5,true,true,owner,data.next(),true,mask);
  check(region.contains(177,140,176),"Connected pool missing");
  check(!region.contains(179,140,176),"Disconnected pool affected");
  check(!region.contains(177,141,176),"Connected mask leaked vertically");
  var reload=FlowRegion.load(region.save());
  check(reload.equals(region),"Connected mask persistence/equality");
  mask[0]=0;
  check(region.equals(reload),"Region retained mutable input mask");
  var exposed=region.connectedMask();Arrays.fill(exposed,0);
  check(region.equals(reload),"Region exposed mutable internal mask");
  check(FlowOverrides.visualAngle(region)==StreamFlowGrid.encode(0,0),"Stillwater visual angle");
  data.put(level,region);
  check(level.getFluidState(center.east()).getFlow(level,center.east()).lengthSqr()<1e-10,"Stillwater final fluid flow");
  data.remove(level,901);
  level.setBlock(center,StreamControl.GENERATOR.get().defaultBlockState(),3);
  var be=(ControllerEntity)level.getBlockEntity(center);
  be.configure(Direction.EAST,4,.5,true);
  check(be.connectedOnly() && be.radius()==4 && be.strength()==.5,"Controller GUI settings application");
  check(data.byKey(center.asLong()).connectedOnly(),"Controller connected option missing");
  be.configure(Direction.WEST,2,.75,false);
  var next=data.byKey(center.asLong());
  check(!next.connectedOnly() && next.radius()==2 && next.dx()==-1 && next.strength()==.75,"Controller setting replacement");
  level.removeBlock(center,false);
  check(data.byKey(center.asLong())==null,"Removed controller left stale region");
 }
}
