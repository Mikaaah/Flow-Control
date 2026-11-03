package nl.townsandkingdoms.streamcontrol;

import java.util.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Opt-in server scenarios. Runs in the disposable integration world only. */
public final class EngineeringChecks {
    private static void check(boolean value,String reason) { if(!value) throw new AssertionError(reason); }
    private static CompoundTag options(FlowProfile a,FlowProfile b,ControllerSettings.Redstone redstone) {
        var n=new CompoundTag();n.put("ProfileA",a.save());n.put("ProfileB",b.save());n.putInt("RedstoneMode",redstone.ordinal());return n;
    }
    private static void toolMode(ItemStack tool,FlowWand.Mode mode) {
        var n=FlowWand.settings(tool);n.putInt("CalibratorMode",mode.ordinal());tool.set(DataComponents.CUSTOM_DATA,CustomData.of(n));
    }
    public static void run(ServerLevel level,Map<String,Object> results) {
        for(String name:new String[]{"flow_wand","current_generator","stillwater_anchor","flow_sensor","mooring_post"})
            check(level.getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,net.minecraft.resources.Identifier.fromNamespaceAndPath(StreamControl.ID,name))).isPresent(),"Survival recipe missing: "+name);
        results.put("allSurvivalRecipesLoaded",true);
        BlockPos p=new BlockPos(256,120,256),q=p.east(10);var data=ControlData.get(level);
        level.setBlock(p,StreamControl.GENERATOR.get().defaultBlockState(),3);
        level.setBlock(q,StreamControl.GENERATOR.get().defaultBlockState(),3);
        var a=(ControllerEntity)level.getBlockEntity(p);var b=(ControllerEntity)level.getBlockEntity(q);
        var first=new FlowProfile(3,2,1,FlowHeading.EAST,false,FlowProfile.Action.CURRENT);
        var second=new FlowProfile(2,1,.5,FlowHeading.SOUTH,false,FlowProfile.Action.CURRENT);
        a.controls().configure(options(first,second,ControllerSettings.Redstone.ANALOG));
        check(data.byKey(p.asLong()).still(),"Analog zero must stop its controlled current");
        level.setBlock(p.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);a.refresh();
        check(data.byKey(p.asLong()).dx()>.99 && data.byKey(p.asLong()).strength()==1,"Analog full signal");
        a.controls().configure(options(first,second,ControllerSettings.Redstone.REVERSE));
        check(data.byKey(p.asLong()).dx()<-.99,"Powered reverse mode");
        level.removeBlock(p.west(),false);a.refresh();check(data.byKey(p.asLong()).dx()>.99,"Unpowered reverse mode");
        a.controls().configure(options(first,second,ControllerSettings.Redstone.SWITCH));
        level.setBlock(p.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);a.refresh();
        check(data.byKey(p.asLong()).dz()>.99 && data.byKey(p.asLong()).radius()==2 && data.byKey(p.asLong()).vertical()==1
            && data.byKey(p.asLong()).strength()==.5,"Switch must use every saved Profile B field");
        var release=new FlowProfile(2,1,.5,FlowHeading.SOUTH,false,FlowProfile.Action.RELEASE);
        a.controls().configure(options(first,release,ControllerSettings.Redstone.SWITCH));check(data.byKey(p.asLong())==null,"Profile release must restore underlying water");
        level.removeBlock(p.west(),false);a.controls().configure(options(first,second,ControllerSettings.Redstone.IGNORE));
        var saved=a.saveWithoutMetadata(level.registryAccess());var loaded=new ControllerEntity(p,a.getBlockState());loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved));
        check(loaded.controls().a().equals(first) && loaded.controls().b().equals(second) && loaded.controls().redstone()==ControllerSettings.Redstone.IGNORE,"Controller profiles save/load");
        var old=new CompoundTag();old.putInt("Radius",7);old.putDouble("Strength",.75);old.putBoolean("ConnectedOnly",true);
        loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),old));check(loaded.radius()==7 && loaded.strength()==.75 && loaded.connectedOnly(),"0.1.x controller settings migration");
        results.put("profileModesPersistenceAndMigration",true);
        check(b.controls().link(p),"Valid controller link rejected");b.refresh();
        check(data.byKey(q.asLong()).radius()==3 && data.byKey(q.asLong()).dx()>.99,"Linked controller did not follow source");
        a.configure(FlowHeading.NORTH_WEST,5,.65,false);b.refresh();
        check(data.byKey(q.asLong()).radius()==5 && data.byKey(q.asLong()).dx()<0 && data.byKey(q.asLong()).dz()<0,"Linked changes did not propagate");
        check(!a.controls().link(q) && !b.controls().link(q),"Link cycles and self-links must be rejected");
        check(!b.controls().link(q.east(300)),"Out-of-range link accepted");
        level.removeBlock(p,false);b.refresh();check(data.byKey(q.asLong())==null,"Missing linked source must pause without chunk loading");
        b.controls().unlink();check(data.byKey(q.asLong())!=null,"Unlink did not restore local profile");
        results.put("controllerLinkPropagationCycleAndMissingSource",true);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"FlowChecks"));player.getAbilities().mayBuild=true;
        var tool=new ItemStack(StreamControl.WAND.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
        toolMode(tool,FlowWand.Mode.COPY);check(FlowWand.operate(player,q,tool),"Calibrator copy failed");
        level.setBlock(p,StreamControl.GENERATOR.get().defaultBlockState(),3);a=(ControllerEntity)level.getBlockEntity(p);
        toolMode(tool,FlowWand.Mode.PASTE);check(FlowWand.operate(player,p,tool),"Calibrator paste failed");
        check(a.controls().a().equals(b.controls().a()) && a.controls().b().equals(b.controls().b()),"Calibrator did not copy both profiles");
        toolMode(tool,FlowWand.Mode.LINK);check(FlowWand.operate(player,p,tool) && FlowWand.operate(player,q,tool),"Calibrator linking failed");
        check(p.equals(b.controls().source()),"Calibrator stored wrong source");
        var legacyPos=p.north(20);level.setBlock(legacyPos,Blocks.WATER.defaultBlockState(),3);
        data.put(level,new FlowRegion(87654,legacyPos,2,1,1,0,1,false,false,player.getUUID(),data.next()));
        toolMode(tool,FlowWand.Mode.RESTORE);check(FlowWand.operate(player,legacyPos,tool) && data.byKey(87654)==null,"Calibrator cannot remove legacy wand edit");
        results.put("calibratorCopyPasteLinkAndLegacyCleanup",true);
        var sensorPos=p.east(64);level.setBlock(sensorPos,StreamControl.SENSOR.get().defaultBlockState(),3);
        var sensor=(FlowSensorEntity)level.getBlockEntity(sensorPos);var water=sensorPos.offset(2,3,0);
        level.setBlock(water,Blocks.WATER.defaultBlockState(),3);data.put(level,new FlowRegion(87655,water,1,0,1,0,1,false,true,player.getUUID(),data.next()));
        var sensorOptions=sensor.settings();sensorOptions.putInt("Radius",4);sensorOptions.putInt("Vertical",2);sensor.configure(sensorOptions);
        check(sensor.output()==0,"Sensor detected flow above its vertical range");
        sensorOptions.putInt("Vertical",3);sensor.configure(sensorOptions);check(sensor.output()==8,"Sensor did not detect elevated flow");
        check(level.getBlockState(sensorPos).hasAnalogOutputSignal() && level.getBlockState(sensorPos).getAnalogOutputSignal(level,sensorPos,Direction.NORTH)==8,"Comparator output unavailable");
        sensorOptions.putInt("Mode",1);sensor.configure(sensorOptions);check(sensor.output()==3,"East must be direction output 3");
        sensorOptions.putInt("Radius",1);sensor.configure(sensorOptions);check(sensor.output()==0,"Sensor ignored horizontal range");
        sensorOptions.putInt("Radius",4);sensorOptions.putInt("Mode",2);sensorOptions.putBoolean("EmptyOnly",true);
        var sensedBoat=net.minecraft.world.entity.EntityType.OAK_BOAT.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);sensedBoat.setPos(sensorPos.getX()+.5,sensorPos.getY()+3.5,sensorPos.getZ()+.5);level.addFreshEntity(sensedBoat);
        sensor.configure(sensorOptions);check(sensor.output()==1,"Boat count ignored vertical range");
        var rider=new net.minecraft.world.entity.animal.pig.Pig(net.minecraft.world.entity.EntityType.PIG,level);level.addFreshEntity(rider);
        sensorOptions.putInt("Vertical",2);sensor.configure(sensorOptions);check(sensor.output()==0,"Boat below/above range was counted");
        sensorOptions.putInt("Vertical",3);sensor.configure(sensorOptions);check(rider.startRiding(sensedBoat,true,true),"Sensor passenger fixture failed to board");sensor.sample();check(sensor.output()==0,"Empty-only sensor counted occupied boat");
        sensorOptions.putBoolean("EmptyOnly",false);sensor.configure(sensorOptions);check(sensor.output()==1,"All-boats sensor omitted occupied boat");
        rider.stopRiding();sensedBoat.discard();
        var reloadedSensor=new FlowSensorEntity(sensorPos,sensor.getBlockState());reloadedSensor.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),sensor.saveWithoutMetadata(level.registryAccess())));
        check(reloadedSensor.vertical()==3 && reloadedSensor.radius()==4 && reloadedSensor.mode()==FlowSensorEntity.Mode.BOATS,"Sensor range/mode persistence");
        results.put("sensorVerticalHorizontalDirectionBoatAndComparator",true);
        var mooringPos=p.south(64);
        while(Math.floorMod(level.getGameTime()+mooringPos.asLong(),10)!=0) mooringPos=mooringPos.above();
        level.setBlock(mooringPos,StreamControl.MOORING.get().defaultBlockState(),3);var mooring=(MooringEntity)level.getBlockEntity(mooringPos);
        var boat=net.minecraft.world.entity.EntityType.OAK_BOAT.create(level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);boat.setPos(mooringPos.getX()+1.5,mooringPos.getY()+1,mooringPos.getZ()+.5);level.addFreshEntity(boat);
        mooring.tick();check(boat.getUUID().equals(mooring.boatId()),"Mooring did not secure empty boat");
        double x=boat.getX(),z=boat.getZ();boat.setPos(x+.2,boat.getY(),z+.2);boat.setDeltaMovement(.2,.03,.2);mooring.tick();
        check(boat.getX()==x && boat.getZ()==z && boat.getDeltaMovement().x==0 && boat.getDeltaMovement().z==0 && boat.getDeltaMovement().y==.03,"Mooring did not hold horizontal position and preserve bobbing");
        check(rider.startRiding(boat,true,true),"Mooring passenger fixture failed to board");mooring.tick();check(mooring.boatId()==null,"Boarding did not immediately release boat");rider.stopRiding();
        for(int i=0;i<42;i++) mooring.tick();check(mooring.boatId()!=null,"Mooring did not resume after release cooldown");
        level.setBlock(mooringPos,mooring.getBlockState().setValue(UtilityBlock.POWERED,true),2);mooring.tick();check(mooring.boatId()==null,"Redstone did not release boat");
        level.removeBlock(mooringPos,false);boat.discard();rider.discard();results.put("mooringHoldBoardingRedstoneAndCooldown",true);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());
        try {
            var configure=new ControlNetwork.Configure(p.asLong(),3,1,false,FlowHeading.EAST.id(),options(first,second,ControllerSettings.Redstone.SWITCH));
            ControlNetwork.Configure.CODEC.encode(buf,configure);check(configure.equals(ControlNetwork.Configure.CODEC.decode(buf)),"Profile packet round trip");buf.clear();
            var utility=new ControlNetwork.UtilityConfigure(sensorPos.asLong(),sensorOptions);ControlNetwork.UtilityConfigure.CODEC.encode(buf,utility);
            check(utility.equals(ControlNetwork.UtilityConfigure.CODEC.decode(buf)),"Sensor packet round trip");
        } finally { buf.release(); }
        results.put("engineeringPacketRoundTrips",true);
        results.put("standaloneCoreEngineering",true);
        level.removeBlock(p,false);level.removeBlock(q,false);level.removeBlock(sensorPos,false);data.remove(level,87655);
    }
}
