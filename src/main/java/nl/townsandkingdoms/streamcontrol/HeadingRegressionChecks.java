package nl.townsandkingdoms.streamcontrol;

import net.minecraft.nbt.NbtOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** Actual fluid flow, block-state persistence and packet checks for every octant. */
public final class HeadingRegressionChecks {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void run(ServerLevel level){
        BlockPos pos=new BlockPos(240,140,240);
        level.setBlock(pos,StreamControl.GENERATOR.get().defaultBlockState(),3);
        level.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),3);
        var be=(ControllerEntity)level.getBlockEntity(pos);
        var data=ControlData.get(level);
        try {
            check(!be.getBlockState().getValue(ControllerBlock.DIAGONAL),"Legacy/default controller became diagonal");
            check(FlowHeading.fromId(-1)==null && FlowHeading.fromId(8)==null
                && FlowHeading.fromId(Integer.MAX_VALUE)==null,"Invalid network heading accepted");
            for(Direction d:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
                be.configure(d,4,.8,false);
                check(be.heading()==FlowHeading.fromDirection(d),"Legacy cardinal configuration changed");
            }
            for(var heading:FlowHeading.values()){
                be.configure(heading,4,.8,false);
                check(be.heading()==heading,"Controller heading was not applied: "+heading);
                var r=data.byKey(pos.asLong());
                check(Math.abs(r.vector().length()-.8)<1e-9,"Diagonal current was faster: "+heading);
                check(Math.abs(r.dx()-heading.dx())<1e-9 && Math.abs(r.dz()-heading.dz())<1e-9,"Wrong flow vector: "+heading);
                var flow=level.getFluidState(pos.east()).getFlow(level,pos.east());
                check(Math.abs(flow.x-heading.dx()*.8)<1e-9 && Math.abs(flow.z-heading.dz()*.8)<1e-9,"Actual water flow mismatch: "+heading+" "+flow);
                var restored=FlowRegion.load(r.save());
                check(Math.abs(restored.vector().length()-.8)<1e-9,"Saved diagonal speed changed");
                var state=be.getBlockState();
                var nbt=BlockState.CODEC.encodeStart(NbtOps.INSTANCE,state).result().orElseThrow();
                var loaded=BlockState.CODEC.parse(NbtOps.INSTANCE,nbt).result().orElseThrow();
                check(FlowHeading.fromState(loaded)==heading,"Block state lost diagonal on save/load");
                var cw=FlowHeading.fromState(state.rotate(Rotation.CLOCKWISE_90));
                check(cw.stepX()==-heading.stepZ() && cw.stepZ()==heading.stepX(),"Quarter turn lost heading");
                var lr=FlowHeading.fromState(state.mirror(Mirror.LEFT_RIGHT));
                check(lr.stepX()==heading.stepX() && lr.stepZ()==-heading.stepZ(),"North/south mirror lost heading");
                var fb=FlowHeading.fromState(state.mirror(Mirror.FRONT_BACK));
                check(fb.stepX()==-heading.stepX() && fb.stepZ()==heading.stepZ(),"East/west mirror lost heading");
                var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
                try {
                    var edit=new ControlNetwork.Configure(pos.asLong(),4,.8,false,heading.id());
                    ControlNetwork.Configure.CODEC.encode(buf,edit);
                    check(ControlNetwork.Configure.CODEC.decode(buf).equals(edit),"Eight-way edit packet round trip");
                    buf.clear();
                    var open=new ControlNetwork.Open(pos.asLong(),4,4,.8,2,16,false,false,false,heading.id());
                    ControlNetwork.Open.CODEC.encode(buf,open);
                    check(ControlNetwork.Open.CODEC.decode(buf).equals(open),"Eight-way GUI packet round trip");
                } finally {buf.release();}
            }
        } finally {level.removeBlock(pos,false);}
    }
}
