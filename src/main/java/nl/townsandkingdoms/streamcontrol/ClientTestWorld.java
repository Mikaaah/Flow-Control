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
        l.setBlock(GENERATOR,StreamControl.GENERATOR.get().defaultBlockState(),3);
        l.setBlock(TARGET,StreamControl.GENERATOR.get().defaultBlockState(),3);
        l.setBlock(SENSOR,StreamControl.SENSOR.get().defaultBlockState(),3);
        l.setBlock(MOORING,StreamControl.MOORING.get().defaultBlockState(),3);
        l.setBlock(ANCHOR,StreamControl.ANCHOR.get().defaultBlockState(),3);
        ((ControllerEntity)l.getBlockEntity(ANCHOR)).configure(FlowHeading.NORTH,3,1,false);
        // A separate high pool makes the sensor's independent vertical range observable.
        l.setBlock(SENSOR.above(6),Blocks.WATER.defaultBlockState(),3);
        for(var d:net.minecraft.core.Direction.Plane.HORIZONTAL) l.setBlock(SENSOR.above(6).relative(d),Blocks.GLASS.defaultBlockState(),3);
        l.setBlock(SENSOR.above(5),Blocks.GLASS.defaultBlockState(),3);
        ControlData.get(l).put(l,new FlowRegion(9001,SENSOR.above(6),1,0,1,0,2,false,false,new java.util.UUID(0,0),ControlData.get(l).next()));
        var boat=new Boat(l,.5,100.8,4.5);l.addFreshEntity(boat);
        if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.prepare(l);
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
    private static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(literal("flowclient").then(literal("power").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(GENERATOR.below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);return 1;
        })).then(literal("unpower").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(GENERATOR.below(),Blocks.STONE.defaultBlockState(),3);return 1;
        })).then(literal("dock").executes(c->{var p=c.getSource().getPlayerOrException();p.teleportTo(.5,101,3.5);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return 1;
        })).then(literal("view").executes(c->{
            var l=c.getSource().getLevel();l.setBlock(new BlockPos(4,102,-4),Blocks.STONE.defaultBlockState(),3);
            var p=c.getSource().getPlayerOrException();p.teleportTo(4.5,103,-3.5);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return 1;
        })).then(literal("reverse").executes(c->{
            if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.speed(c.getSource().getLevel(),-128);return 1;
        })).then(literal("stop").executes(c->{
            if(net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.speed(c.getSource().getLevel(),0);return 1;
        })));
    }
}
