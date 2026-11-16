package nl.townsandkingdoms.streamcontrol;

import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import static net.minecraft.commands.Commands.literal;

/** Capture-only scene. It is inert unless the dedicated showcase launcher opts in. */
public final class ShowcaseWorld {
    public static final BlockPos GENERATOR=new BlockPos(-6,64,0), ANCHOR=new BlockPos(-3,64,0),
        SENSOR=new BlockPos(0,64,0), THRUSTER=new BlockPos(3,64,0), MOORING=new BlockPos(6,64,0);
    private ShowcaseWorld() {}
    public static void register() {
        if(!Boolean.getBoolean("streamcontrol.showcaseServer"))return;
        NeoForge.EVENT_BUS.addListener(ShowcaseWorld::prepare);
        NeoForge.EVENT_BUS.addListener(ShowcaseWorld::login);
        NeoForge.EVENT_BUS.addListener(ShowcaseWorld::commands);
    }
    private static void prepare(ServerStartedEvent event) {
        var level=event.getServer().overworld();
        level.setDayTime(6000);level.setWeatherParameters(1000000,0,false,false);
        for(var rule:java.util.List.of(GameRules.RULE_DAYLIGHT,GameRules.RULE_WEATHER_CYCLE,
                GameRules.RULE_DOMOBSPAWNING,GameRules.RULE_ANNOUNCE_ADVANCEMENTS,GameRules.RULE_SENDCOMMANDFEEDBACK))
            level.getGameRules().getRule(rule).set(false,event.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,event.getServer());
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)
            level.setBlock(new BlockPos(x,63,z),Blocks.GREEN_TERRACOTTA.defaultBlockState(),3);
        // Real water stays below the opaque platform inside a sealed terracotta pocket.
        for(int x=-1;x<=1;x++)for(int y=61;y<=63;y++)for(int z=-1;z<=1;z++)
            level.setBlock(new BlockPos(x,y,z),Blocks.GREEN_TERRACOTTA.defaultBlockState(),3);
        level.setBlock(new BlockPos(0,62,0),Blocks.WATER.defaultBlockState(),3);
        level.setBlock(GENERATOR,StreamControl.GENERATOR.get().defaultBlockState(),3);
        level.setBlock(ANCHOR,StreamControl.ANCHOR.get().defaultBlockState(),3);
        level.setBlock(SENSOR,StreamControl.SENSOR.get().defaultBlockState(),3);
        level.setBlock(MOORING,StreamControl.MOORING.get().defaultBlockState(),3);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))
            nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.prepare(level,THRUSTER);
        else level.setBlock(THRUSTER,DormantMechanicalIntegration.BLOCK.get().defaultBlockState(),3);
        level.addFreshEntity(new Boat(level,6.5,64,-1.25));
        state(level,false);
        level.setDefaultSpawnPos(new BlockPos(0,65,0),135);
        try {Files.writeString(net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().resolve("showcase-ready"),"ready");}
        catch(java.io.IOException e){throw new IllegalStateException("Cannot signal showcase readiness",e);}
    }
    private static void state(ServerLevel level,boolean active) {
        var action=active?FlowProfile.Action.CURRENT:FlowProfile.Action.RELEASE;
        // NE is represented on the north-facing housing and gives the real sensor a visible vane turn.
        ((FlowController)level.getBlockEntity(GENERATOR)).controls().setA(new FlowProfile(8,4,1.25,
            active?FlowHeading.NORTH_EAST:FlowHeading.NORTH,false,action));
        ((FlowController)level.getBlockEntity(ANCHOR)).controls().setA(new FlowProfile(1,4,1,FlowHeading.NORTH,false,
            active?FlowProfile.Action.CALM:FlowProfile.Action.RELEASE));
        ((FlowController)level.getBlockEntity(THRUSTER)).controls().setA(new FlowProfile(1,1,1,FlowHeading.NORTH,false,action));
        if(net.neoforged.fml.ModList.get().isLoaded("create"))
            nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.speed(level,THRUSTER,active?128:0);
        var post=(MooringEntity)level.getBlockEntity(MOORING);var mooring=post.settings();
        mooring.putBoolean("Enabled",active);post.configure(mooring);
        var sensor=(FlowSensorEntity)level.getBlockEntity(SENSOR);var settings=sensor.settings();
        settings.putInt("Mode",FlowSensorEntity.Mode.SPEED.ordinal());settings.putInt("Radius",1);settings.putInt("Vertical",4);sensor.configure(settings);
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        player.setGameMode(GameType.CREATIVE);player.getInventory().clearContent();
        player.getAbilities().flying=true;player.onUpdateAbilities();
        camera(player,0,65,0,135,25);
    }
    private static int camera(ServerPlayer player,double x,double y,double z,float yaw,float pitch) {
        player.setYRot(yaw);player.setXRot(pitch);player.setYHeadRot(yaw);
        player.teleportTo(x,y,z);player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying=true;player.onUpdateAbilities();return 1;
    }
    private static int view(net.minecraft.commands.CommandSourceStack source,String shot) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player=source.getPlayerOrException();
        if(shot.equals("idle") || shot.equals("active")) {
            state(source.getLevel(),shot.equals("active"));return camera(player,5.75,69.7,-10,26.56505f,30);
        }
        return switch(shot) {
            case "generator"->camera(player,-4.248,64.497,-2.004,26.56505f,30);
            case "anchor"->camera(player,-1.248,64.497,-2.004,26.56505f,30);
            case "sensor"->camera(player,1.752,64.434,-2.004,26.56505f,30);
            case "thruster"->camera(player,4.752,64.497,-2.004,26.56505f,30);
            case "mooring"->camera(player,8.378,65.355,-4.257,26.56505f,30);
            default->throw new IllegalArgumentException(shot);
        };
    }
    private static void commands(RegisterCommandsEvent event) {
        var command=literal("fcshowcase");
        for(String shot:new String[]{"idle","active","generator","anchor","sensor","thruster","mooring"})
            command.then(literal(shot).executes(context->view(context.getSource(),shot)));
        event.getDispatcher().register(command);
    }
}
