package nl.townsandkingdoms.streamcontrol.client;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.townsandkingdoms.streamcontrol.*;
import static nl.townsandkingdoms.streamcontrol.ShowcaseWorld.*;

/** Seven raw native-framebuffer screenshots; no release build or regression harness is invoked. */
@EventBusSubscriber(modid="streamcontrol",value=Dist.CLIENT)
public final class ShowcaseCapture {
    private record Shot(String name,String view,boolean active,double x,double y,double z,int settle) {}
    private static final List<Shot> SHOTS=List.of(
        new Shot("00-idle-overview","idle",false,5.75,69.7,-10,120),
        new Shot("01-active-overview","active",true,5.75,69.7,-10,120),
        new Shot("02-current-generator","generator",true,-4.248,64.497,-2.004,70),
        new Shot("03-stillwater-anchor","anchor",true,-1.248,64.497,-2.004,70),
        new Shot("04-flow-sensor","sensor",true,1.752,64.434,-2.004,70),
        new Shot("05-hydro-current-thruster","thruster",true,4.752,64.497,-2.004,70),
        new Shot("06-mooring-post","mooring",true,8.378,65.355,-4.257,70)
    );
    private static final List<Map<String,Object>> captures=new ArrayList<>();
    private static int total,phase,wait,stable;
    private static boolean connecting,started,saving,done;
    private ShowcaseCapture() {}
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static boolean create(){return net.neoforged.fml.ModList.get().isLoaded("create");}
    private static void cleanView() {
        var minecraft=mc();minecraft.options.hideGui=true;
        minecraft.options.fov().set(38);minecraft.options.fovEffectScale().set(0.0);
        minecraft.options.bobView().set(false);minecraft.options.particles().set(ParticleStatus.MINIMAL);
        minecraft.options.cloudStatus().set(CloudStatus.OFF);
        minecraft.getToasts().clear();minecraft.getTutorial().setStep(TutorialSteps.NONE);
        minecraft.gui.getChat().clearMessages(false);
        minecraft.player.setYRot(26.56505f);minecraft.player.setXRot(30);
        minecraft.player.setDeltaMovement(Vec3.ZERO);minecraft.player.getAbilities().flying=true;
    }
    private static boolean ready(Shot shot) {
        if(mc().player.position().distanceToSqr(new Vec3(shot.x,shot.y,shot.z))>.01)return false;
        for(var pos:List.of(GENERATOR,ANCHOR,SENSOR,THRUSTER,MOORING)) {
            var be=mc().level.getBlockEntity(pos);if(be==null)return false;
            var frame=ConduitAnimations.extract(be,0);
            if(frame.parts().isEmpty() || frame.parts().stream().anyMatch(part->ConduitMeshes.mesh(part.model()).isEmpty()))return false;
        }
        var generator=(FlowController)mc().level.getBlockEntity(GENERATOR);
        var anchor=(FlowController)mc().level.getBlockEntity(ANCHOR);
        var thruster=(FlowController)mc().level.getBlockEntity(THRUSTER);
        var sensor=(FlowSensorEntity)mc().level.getBlockEntity(SENSOR);
        var post=(MooringEntity)mc().level.getBlockEntity(MOORING);
        if(generator.visualActive()!=shot.active || anchor.visualActive()!=shot.active)return false;
        if(create() && (thruster.visualActive()!=shot.active || (shot.active?thruster.rpm()==0:thruster.rpm()!=0)))return false;
        if(shot.active) {
            if(sensor.measuredSpeed()<=0 || sensor.measuredHeading()==null || !post.secured())return false;
            if(ConduitAnimations.extract(post,0).rope()==null)return false;
        } else if(sensor.measuredSpeed()>1e-6 || post.enabled() || post.secured())return false;
        return true;
    }
    private static Map<String,Object> evidence(Shot shot,String file) {
        var generator=(FlowController)mc().level.getBlockEntity(GENERATOR);
        var anchor=(FlowController)mc().level.getBlockEntity(ANCHOR);
        var thruster=(FlowController)mc().level.getBlockEntity(THRUSTER);
        var sensor=(FlowSensorEntity)mc().level.getBlockEntity(SENSOR);
        var post=(MooringEntity)mc().level.getBlockEntity(MOORING);
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("file",file);row.put("state",shot.active?"active":"idle");
        row.put("width",mc().getMainRenderTarget().width);row.put("height",mc().getMainRenderTarget().height);
        row.put("fov",38);row.put("playerPosition",List.of(mc().player.getX(),mc().player.getY(),mc().player.getZ()));
        row.put("yaw",mc().player.getYRot());row.put("pitch",mc().player.getXRot());
        row.put("creative",mc().gameMode.hasInfiniteItems());row.put("generatorActive",generator.visualActive());
        row.put("anchorActive",anchor.visualActive());row.put("sensorSpeed",sensor.measuredSpeed());
        row.put("sensorHeading",sensor.measuredHeading()==null?"none":sensor.measuredHeading().name());
        row.put("thrusterRPM",thruster.rpm());row.put("mooringSecured",post.secured());
        row.put("liveRope",ConduitAnimations.extract(post,0).rope()!=null);
        row.put("nativeFlywheel",create() && nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.visualizationSupported(mc().level));
        return row;
    }
    private static void capture(Shot shot) {
        if(mc().getMainRenderTarget().width!=2560 || mc().getMainRenderTarget().height!=1440)
            throw new IllegalStateException("Showcase requires the native2560x1440 framebuffer; got "+mc().getMainRenderTarget().width+"x"+mc().getMainRenderTarget().height);
        String file="flow-control-showcase-"+shot.name+".png";var row=evidence(shot,file);saving=true;
        Screenshot.grab(mc().gameDirectory,file,mc().getMainRenderTarget(),message->mc().execute(()->{
            try {
                if(!Files.isRegularFile(mc().gameDirectory.toPath().resolve("screenshots").resolve(file)))
                    throw new IllegalStateException("Screenshot was not saved: "+message.getString());
                captures.add(row);System.out.println("FLOW_SHOWCASE_CAPTURE "+row);phase++;wait=0;stable=0;started=false;saving=false;
                if(phase==SHOTS.size())finish(null);
            } catch(Throwable error){finish(error);}
        }));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("streamcontrol.showcaseClient") || done)return;
        try {
            if(++total>12000)throw new IllegalStateException("Showcase client timeout");
            if(!connecting) {
                if(!(mc().screen instanceof TitleScreen) || mc().getOverlay()!=null)return;
                connecting=true;
                ConnectScreen.startConnecting(mc().screen,mc(),ServerAddress.parseString("127.0.0.1:25565"),
                    new ServerData("Flow Control showcase","127.0.0.1:25565",ServerData.Type.OTHER),false,null);return;
            }
            if(mc().level==null || mc().player==null)return;
            cleanView();if(saving)return;
            Shot shot=SHOTS.get(phase);
            if(!started) {
                mc().setScreen(null);mc().player.connection.sendCommand("fcshowcase "+shot.view);
                started=true;wait=0;stable=0;return;
            }
            if(++wait>600)throw new IllegalStateException("Showcase scene not ready for "+shot.name);
            if(!ready(shot)){stable=0;return;}
            if(++stable>=shot.settle)capture(shot);
        } catch(Throwable error){finish(error);}
    }
    private static void finish(Throwable error) {
        if(done)return;done=true;
        Map<String,Object> report=new LinkedHashMap<>();report.put("complete",error==null);
        report.put("captureMethod","Minecraft Screenshot.grab, unmodified native framebuffer PNG");
        report.put("minecraft","1.21.1");report.put("loader","NeoForge");report.put("captures",captures);
        if(error!=null){error.printStackTrace();report.put("error",error.toString());}
        try {Files.writeString(mc().gameDirectory.toPath().resolve("showcase-capture.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        catch(Exception failure){failure.printStackTrace();}
        mc().stop();
    }
}
