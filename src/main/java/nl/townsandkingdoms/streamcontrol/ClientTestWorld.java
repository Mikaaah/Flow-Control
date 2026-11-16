package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import static net.minecraft.commands.Commands.literal;

/** Test fixture only; disabled unless explicitly requested by the CI launcher. */
public final class ClientTestWorld {
    public static final BlockPos GENERATOR=new BlockPos(0,100,0), TARGET=new BlockPos(4,100,0),
        SENSOR=new BlockPos(-4,100,0), MOORING=new BlockPos(0,100,3), ANCHOR=new BlockPos(0,100,6),
        MECHANICAL=new BlockPos(4,100,3);
    public static final BlockPos MODEL_GENERATOR=new BlockPos(0,104,12), MODEL_ANCHOR=new BlockPos(3,104,12),
        MODEL_SENSOR=new BlockPos(6,104,12), MODEL_MECHANICAL=new BlockPos(9,104,12), MODEL_MOORING=new BlockPos(12,105,12);
    public static void register() {
        if(!Boolean.getBoolean("streamcontrol.clientTestServer")) return;
        NeoForge.EVENT_BUS.addListener(ClientTestWorld::prepare);
        NeoForge.EVENT_BUS.addListener(ClientTestWorld::login);
        NeoForge.EVENT_BUS.addListener(ClientTestWorld::commands);
        NeoForge.EVENT_BUS.addListener(ClientTestWorld::interaction);
    }
    private static void prepare(ServerStartedEvent e) {
        var l=e.getServer().overworld();l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);
        for(int x=-8;x<=8;x++) for(int z=-4;z<=9;z++) {
            l.setBlock(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState(),3);
            l.setBlock(new BlockPos(x,100,z),(z<-1 || Math.abs(x)==8 || z==9?Blocks.STONE:Blocks.WATER).defaultBlockState(),3);
        }
        l.setBlock(GENERATOR,StreamControl.GENERATOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        l.setBlock(TARGET,StreamControl.GENERATOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        l.setBlock(SENSOR,StreamControl.SENSOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        l.setBlock(MOORING,StreamControl.MOORING.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        l.setBlock(ANCHOR,StreamControl.ANCHOR.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        ((ControllerEntity)l.getBlockEntity(ANCHOR)).configure(FlowHeading.NORTH,3,1,false);
        // A separate high pool makes the sensor's independent vertical range observable.
        l.setBlock(SENSOR.above(6),Blocks.WATER.defaultBlockState(),3);
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL) l.setBlock(SENSOR.above(6).relative(d),Blocks.GLASS.defaultBlockState(),3);
        l.setBlock(SENSOR.above(5),Blocks.GLASS.defaultBlockState(),3);
        ControlData.get(l).put(l,new FlowRegion(9001,SENSOR.above(6),1,0,1,0,2,false,false,new java.util.UUID(0,0),ControlData.get(l).next()));
        var boat=new Boat(l,.5,100.8,4.5);l.addFreshEntity(boat);
        if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.prepare(l);
        else l.setBlock(MECHANICAL,DormantMechanicalIntegration.BLOCK.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),3);
        try { java.nio.file.Files.writeString(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("client-test-ready"),"ready"); }
        catch(java.io.IOException ex) { throw new RuntimeException(ex); }
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        p.setGameMode(GameType.CREATIVE);p.teleportTo(.5,101,-2.5);p.setYRot(0);p.setXRot(20);
        p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(8);
        p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(8);
        p.getInventory().setItem(0,new ItemStack(StreamControl.WAND.get()));
        p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();
    }
    private static void interaction(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if(e.getTarget() instanceof Boat b) System.out.println("FLOW_BOARD_INPUT boat="+b.position()+" underwater="+b.isUnderWater()+" eyeFluid="+b.getEyeInFluidType()+" passengers="+b.getPassengers().size()+" hand="+e.getItemStack()+" crouch="+e.getEntity().isShiftKeyDown()+" player="+e.getEntity().position());
    }
    /** Dry stage keeps Streams water surfaces out of the geometry/rope proof photographs. */
    private static void prepareModels(net.minecraft.server.level.ServerLevel level) {
        for(int x=-3;x<=16;x++)for(int z=7;z<=18;z++) {
            level.setBlock(new BlockPos(x,103,z),Blocks.STONE.defaultBlockState(),3);
            for(int y=104;y<=108;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        // A sealed rear trough supplies real water vectors without flooding the display row.
        for(int x=-2;x<=14;x++)for(int z=14;z<=17;z++) {
            boolean edge=x==-2 || x==14 || z==14 || z==17;
            level.setBlock(new BlockPos(x,104,z),(edge?Blocks.GLASS:Blocks.WATER).defaultBlockState(),3);
        }
        level.setBlock(MODEL_GENERATOR,StreamControl.GENERATOR.get().defaultBlockState(),3);
        level.setBlock(MODEL_ANCHOR,StreamControl.ANCHOR.get().defaultBlockState(),3);
        level.setBlock(MODEL_SENSOR,StreamControl.SENSOR.get().defaultBlockState(),3);
        level.setBlock(MODEL_MOORING.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(MODEL_MOORING,StreamControl.MOORING.get().defaultBlockState(),3);
        ((FlowController)level.getBlockEntity(MODEL_GENERATOR)).controls().setA(new FlowProfile(4,1,1,FlowHeading.NORTH,false,FlowProfile.Action.CURRENT));
        ((FlowController)level.getBlockEntity(MODEL_ANCHOR)).controls().setA(new FlowProfile(3,1,1,FlowHeading.NORTH,false,FlowProfile.Action.CALM));
        var sensorFlow=new BlockPos(6,104,15);
        var data=ControlData.get(level);
        data.put(level,new FlowRegion(9002,sensorFlow,1,0,1,0,1,false,false,new java.util.UUID(0,0),data.next()));
        var sensor=(FlowSensorEntity)level.getBlockEntity(MODEL_SENSOR);
        var settings=sensor.settings();settings.putInt("Mode",FlowSensorEntity.Mode.SPEED.ordinal());settings.putInt("Radius",4);settings.putInt("Vertical",1);sensor.configure(settings);
        if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.prepare(level,MODEL_MECHANICAL);
        else level.setBlock(MODEL_MECHANICAL,DormantMechanicalIntegration.BLOCK.get().defaultBlockState(),3);
        // Grounded empty boat is stationary and unobscured; the normal mooring ticker must claim it.
        level.addFreshEntity(new Boat(level,12.5,104,9.5));
    }
    private static int modelView(net.minecraft.commands.CommandSourceStack source, double x, double y, double z) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(x,y,z);player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return 1;
    }
    private static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(literal("flowclient").then(literal("power").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(GENERATOR.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);return 1;
        })).then(literal("unpower").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(GENERATOR.below(),Blocks.STONE.defaultBlockState(),3);return 1;
        })).then(literal("dock").executes(c->{var p=c.getSource().getPlayerOrException();p.teleportTo(.5,101,3.5);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return 1;
        })).then(literal("view").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(new BlockPos(4,102,-4),Blocks.STONE.defaultBlockState(),3);
            var p=c.getSource().getPlayerOrException();p.teleportTo(4.5,103,-3.5);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return 1;
        })).then(literal("models").executes(c->{
            prepareModels(c.getSource().getLevel());return modelView(c.getSource(),6.5,108,1.5);
        })).then(literal("model_overview").executes(c->modelView(c.getSource(),6.5,108,1.5)))
        .then(literal("model_generator").executes(c->modelView(c.getSource(),1.9,104.5,9.2)))
        .then(literal("model_anchor").executes(c->modelView(c.getSource(),4.9,104.5,9.2)))
        .then(literal("model_sensor").executes(c->modelView(c.getSource(),7.9,104.5,9.2)))
        .then(literal("model_thruster").executes(c->modelView(c.getSource(),10.9,104.1,9.2)))
        .then(literal("model_rope").executes(c->modelView(c.getSource(),16.5,105,7.5)))
        .then(literal("reverse").executes(c->{
            if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.speed(c.getSource().getLevel(),-128);return 1;
        })).then(literal("stop").executes(c->{
            if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.speed(c.getSource().getLevel(),0);return 1;
        })));
    }
}
