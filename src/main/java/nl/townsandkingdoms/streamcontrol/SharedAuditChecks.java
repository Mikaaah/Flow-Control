package nl.townsandkingdoms.streamcontrol;

import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Disposable-world regression coverage; invoked only by the opt-in integration harness. */
public final class SharedAuditChecks {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void run(ServerLevel level,Map<String,Object> results) {
        var data=ControlData.get(level);var owner=UUID.randomUUID();
        var pos=new BlockPos(336,140,336);
        var moving=new BlockPos.MutableBlockPos(336,140,336);
        var immutable=new FlowRegion(99001,moving,2,1,1,0,1,false,false,owner,data.next());
        moving.set(0,0,0);check(immutable.center().equals(pos),"Flow region retained mutable center");
        var legacyOwner=immutable.save();long most=owner.getMostSignificantBits(),least=owner.getLeastSignificantBits();
        legacyOwner.putIntArray("Owner",new int[]{(int)(most>>>32),(int)most,(int)(least>>>32),(int)least});
        check(FlowRegion.load(legacyOwner).owner().equals(owner),"Legacy UUID ownership was lost during migration");
        var anchor=new FlowRegion(99002,pos,2,1,1,0,1,true,true,owner,data.next());
        data.put(level,anchor);
        for(int i=0;i<16;i++) {
            data.put(level,new FlowRegion(99003,pos,2,1,1,0,.5+i*.01,false,true,owner,data.next()));
            check(data.at(336,140,336).key()==anchor.key(),"Generator updates displaced stillwater priority");
        }
        check(data.removeMatching(level,r->r.owner().equals(owner))==2,"Bulk clear removed wrong region count");
        check(data.at(336,140,336)==null,"Bulk clear left indexed flow");
        results.put("auditImmutableCentersStableStillwaterAndBulkClear",true);

        level.setBlock(pos,StreamControl.GENERATOR.get().defaultBlockState(),3);
        var controller=(ControllerEntity)level.getBlockEntity(pos);
        controller.configure(FlowHeading.EAST,2,.75,false);
        check(controller.visualActive() && controller.visualHeading()==FlowHeading.EAST
                && controller.visualStrength()==.75,"Resolved controller visuals not published");
        controller.setRemoved();check(data.byKey(pos.asLong())==null,"Unloaded controller left flow override");
        controller.clearRemoved();controller.refresh();
        check(data.byKey(pos.asLong())!=null,"Reloaded controller did not restore its own settings");
        level.removeBlock(pos,false);
        results.put("auditControllerUnloadReregistrationAndVisualState",true);

        pos=new BlockPos(384,140,384);var water=pos.above(2);
        level.setBlock(pos,StreamControl.SENSOR.get().defaultBlockState(),3);
        level.setBlock(water.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(water,Blocks.WATER.defaultBlockState(),3);
        var sensor=(FlowSensorEntity)level.getBlockEntity(pos);
        var settings=new CompoundTag();settings.putInt("Radius",1);settings.putInt("Vertical",3);settings.putInt("Mode",0);
        try {
            data.put(level,new FlowRegion(99004,water,1,0,1,0,.75,false,false,owner,data.next()));
            sensor.configure(settings);int output=sensor.output();
            check(sensor.measuredHeading()==FlowHeading.EAST,"Sensor did not measure east");
            data.put(level,new FlowRegion(99004,water,1,0,0,-1,.75,false,false,owner,data.next()));
            sensor.sample();check(sensor.output()==output && sensor.measuredHeading()==FlowHeading.NORTH,
                    "Sensor lost same-strength direction change");
            CompoundTag snapshot=sensor.getUpdateTag(level.registryAccess());
            check(snapshot.getInt("MeasuredHeading")==FlowHeading.NORTH.id() && Math.abs(snapshot.getDouble("MeasuredSpeed")-.75)<1e-9,
                    "Sensor measurements missing from client update tag");
        } finally { data.remove(level,99004);level.removeBlock(pos,false); }
        results.put("auditSensorSameSignalHeadingAndUpdateTag",true);
    }
}
