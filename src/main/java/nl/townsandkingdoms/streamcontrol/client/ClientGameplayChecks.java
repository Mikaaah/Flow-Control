package nl.townsandkingdoms.streamcontrol.client;

import java.nio.file.Files;
import java.util.*;
import java.util.function.BooleanSupplier;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.townsandkingdoms.streamcontrol.*;
import static nl.townsandkingdoms.streamcontrol.ClientTestWorld.*;

/** Real client, dedicated server, real input handlers and packets. Explicit CI opt-in only. */
@EventBusSubscriber(modid="streamcontrol",value=Dist.CLIENT)
public final class ClientGameplayChecks {
    private record Step(String name,Runnable action,BooleanSupplier ready) {}
    private static final List<Step> steps=new ArrayList<>();
    private static final Map<String,Object> results=new LinkedHashMap<>();
    private static int step=-1,ticks,total;private static boolean connecting,done;
    private static java.util.concurrent.CompletableFuture<Void> modelReload;
    private static Minecraft mc() { return Minecraft.getInstance(); }
    private static FlowRegion region(BlockPos p) { return FlowOverrides.clientByKey(p.asLong()); }
    private static FlowController controller(BlockPos p) { return (FlowController)mc().level.getBlockEntity(p); }
    private static UtilityAccess utility(BlockPos p) { return (UtilityAccess)mc().level.getBlockEntity(p); }
    private static void add(String n,Runnable a,BooleanSupplier r) { steps.add(new Step(n,a,r)); }
    private static void use(BlockPos p) {
        mc().gameMode.useItemOn(mc().player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false));
    }
    private static void click(String key) {
        var label=Component.translatable(key).getString();
        var b=mc().screen.children().stream().filter(c->c instanceof Button button && button.getMessage().getString().equals(label)).map(c->(Button)c).findFirst().orElseThrow(()->new AssertionError("Missing button "+key));
        mc().screen.mouseClicked(b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0,0);
    }
    private static void at(int x,int y) { mc().screen.mouseClicked((mc().screen.width-316)/2.0+x,(mc().screen.height-238)/2.0+y,0); }
    private static void slider(int index,double fraction) {
        var sliders=mc().screen.children().stream().filter(c->c instanceof AbstractSliderButton).map(c->(AbstractSliderButton)c).toList();
        var s=sliders.get(index);double x=s.getX()+4+fraction*(s.getWidth()-8),y=s.getY()+8;
        mc().screen.mouseClicked(x,y,0);mc().screen.mouseReleased(x,y,0);
    }
    private static void screenshot(String name) { Screenshot.grab(mc().gameDirectory,"gameplay-"+name+".png",mc().getMainRenderTarget(),m->{}); }
    private static void command(String name) { mc().player.connection.sendCommand("flowclient "+name); }
    private static void cycle(FlowWand.Mode mode) {
        add("calibrator "+mode,()->{mc().player.setXRot(-90);mc().options.keyShift.setDown(true);},()->{
            if(ticks==4) mc().gameMode.useItem(mc().player,InteractionHand.MAIN_HAND);
            if(ticks==7) mc().options.keyShift.setDown(false);
            return ticks>8 && FlowWand.mode(mc().player.getMainHandItem())==mode;
        });
    }
    private static ConduitAnimations.Frame modelFrame(BlockPos p) {
        var be=mc().level.getBlockEntity(p);if(be==null)return null;
        var frame=ConduitAnimations.extract(be,0);
        if(frame.parts().isEmpty() || frame.parts().stream().anyMatch(part->ConduitMeshes.mesh(part.model()).isEmpty()))return null;
        if(be instanceof FlowSensorEntity sensor && (sensor.measuredSpeed()<=0 || sensor.measuredHeading()==null))return null;
        if(be instanceof MooringEntity post && (!post.secured() || frame.rope()==null))return null;
        if(be instanceof FlowController controller) {
            boolean kinetic=p.equals(MODEL_MECHANICAL);
            boolean create=net.neoforged.fml.ModList.get().isLoaded("create");
            if((!kinetic || create) && !controller.visualActive())return null;
            if(kinetic && (create?controller.rpm()==0:controller.rpm()!=0))return null;
        }
        return frame;
    }
    private static float modelAngle(ConduitAnimations.Frame frame) {
        var first=frame.parts().getFirst();return first.angleY()+first.angleZ();
    }
    private static void modelCapture(String name,String view,BlockPos p,float yaw,float pitch,boolean animate) {
        int[] captures={0};float[] firstAngle={Float.NaN};
        add("placed model render "+name,()->{mc().setScreen(null);mc().options.hideGui=true;mc().options.particles().set(net.minecraft.client.ParticleStatus.MINIMAL);command(view);},()->{
            mc().player.setYRot(yaw);mc().player.setXRot(pitch);
            if(!mc().player.isSpectator())return false;
            var frame=modelFrame(p);if(frame==null)return false;
            if(name.equals("all-five"))for(var position:List.of(MODEL_GENERATOR,MODEL_ANCHOR,MODEL_SENSOR,MODEL_MECHANICAL,MODEL_MOORING))if(modelFrame(position)==null)return false;
            if(ticks==60 || ticks==100) {
                screenshot("model-"+name+"-"+ticks);captures[0]++;
                results.put("placed model parts "+name,frame.parts().size());
                if(p.equals(MODEL_MECHANICAL) && net.neoforged.fml.ModList.get().isLoaded("create")) {
                    results.put("native Flywheel visualization supported",nl.townsandkingdoms.streamcontrol.compat.create.CreateClientFixture.visualizationSupported(mc().level));
                }
                if(p.equals(MODEL_MOORING))results.put("placed mooring has live rope",frame.rope()!=null);
                if(ticks==60)firstAngle[0]=modelAngle(frame);
                else if(animate) {
                    float change=Math.abs(net.minecraft.util.Mth.wrapDegrees(modelAngle(frame)-firstAngle[0]));
                    if(!Float.isFinite(change) || change<1)throw new AssertionError("No rendered animation advance for "+name);
                    results.put("placed model rotation advance "+name,change);
                }
            }
            return ticks>105 && captures[0]==2;
        });
    }
    private static void setup() {
        add("client world and region login sync",()->{},()->mc().level.getBlockEntity(GENERATOR)!=null && region(GENERATOR)!=null && mc().player.getMainHandItem().is(StreamControl.WAND.get()));
        add("open controller through block interaction",()->use(GENERATOR),()->mc().screen instanceof ControllerScreen);
        add("GUI direction and radius apply round trip",()->{
            screenshot("controller");click("gui.streamcontrol.short_e");slider(0,4.0/15);click("gui.streamcontrol.apply");
        },()->region(GENERATOR)!=null && region(GENERATOR).dx()==1 && region(GENERATOR).radius()==5);
        cycle(FlowWand.Mode.COPY);
        add("copy settings with real Calibrator item use",()->use(GENERATOR),()->FlowWand.settings(mc().player.getMainHandItem()).contains("Calibration"));
        cycle(FlowWand.Mode.PASTE);
        add("paste settings through server and sync",()->use(TARGET),()->region(TARGET)!=null && region(TARGET).radius()==5 && region(TARGET).dx()==1);
        cycle(FlowWand.Mode.LINK);
        add("select linked source",()->use(GENERATOR),()->FlowWand.settings(mc().player.getMainHandItem()).contains("LinkPos"));
        add("link target through server and block entity sync",()->use(TARGET),()->GENERATOR.equals(controller(TARGET).controls().source()));
        cycle(FlowWand.Mode.RESTORE);cycle(FlowWand.Mode.INSPECT);
        add("open linked target",()->use(TARGET),()->mc().screen instanceof ControllerScreen);
        add("unlink using GUI",()->{click("engineering.streamcontrol.automation_tab");click("engineering.streamcontrol.unlink");click("gui.streamcontrol.apply");},()->controller(TARGET).controls().source()==null);
        add("open automation",()->use(GENERATOR),()->mc().screen instanceof ControllerScreen);
        add("edit redstone and Profile B through GUI",()->{
            click("engineering.streamcontrol.automation_tab");for(int i=0;i<4;i++) at(150,66);
            click("engineering.streamcontrol.profile_b");slider(0,3.0/16);screenshot("automation");click("gui.streamcontrol.apply");
        },()->controller(GENERATOR).controls().redstone()==ControllerSettings.Redstone.SWITCH && controller(GENERATOR).controls().b().vertical()==3);
        add("redstone switches to reversed Profile B",()->command("power"),()->region(GENERATOR)!=null && region(GENERATOR).dz()==1 && region(GENERATOR).vertical()==3);
        add("unpowered restores Profile A",()->command("unpower"),()->region(GENERATOR)!=null && region(GENERATOR).dx()==1 && region(GENERATOR).vertical()!=3);
        add("open sensor through block interaction",()->use(SENSOR),()->mc().screen instanceof UtilityScreen);
        add("sensor GUI independent vertical range and live speed output",()->{
            slider(0,0);slider(1,6.0/16);screenshot("sensor");click("gui.streamcontrol.apply");
        },()->((FlowSensorEntity)utility(SENSOR)).radius()==1 && ((FlowSensorEntity)utility(SENSOR)).vertical()==6 && ((FlowSensorEntity)utility(SENSOR)).output()==15);
        add("reopen sensor",()->use(SENSOR),()->mc().screen instanceof UtilityScreen);
        add("sensor direction comparator and settings sync",()->{at(150,149);click("gui.streamcontrol.apply");},()->((FlowSensorEntity)utility(SENSOR)).mode()==FlowSensorEntity.Mode.DIRECTION && ((FlowSensorEntity)utility(SENSOR)).output()==3);
        add("reopen sensor boat mode",()->use(SENSOR),()->mc().screen instanceof UtilityScreen);
        add("sensor boat mode and zero vertical range",()->{at(150,149);slider(0,7.0/15);slider(1,0);click("gui.streamcontrol.apply");},()->((FlowSensorEntity)utility(SENSOR)).mode()==FlowSensorEntity.Mode.BOATS && ((FlowSensorEntity)utility(SENSOR)).output()==1);
        add("mooring live boat capture sync",()->command("dock"),()->utility(MOORING).settings().getBoolean("Secured") && mc().player.blockPosition().distSqr(MOORING)<=2);
        add("open mooring",()->use(MOORING),()->mc().screen instanceof UtilityScreen);
        add("disable mooring from GUI releases boat",()->{screenshot("mooring");at(150,186);click("gui.streamcontrol.apply");},()->!utility(MOORING).settings().getBoolean("Enabled") && !utility(MOORING).settings().getBoolean("Secured"));
        add("reopen mooring",()->use(MOORING),()->mc().screen instanceof UtilityScreen);
        add("enable mooring recaptures after cooldown",()->{at(150,186);click("gui.streamcontrol.apply");},()->utility(MOORING).settings().getBoolean("Secured"));
        add("real player boarding releases mooring",()->{
            mc().player.getInventory().selected=1;
            var boat=mc().level.getEntitiesOfClass(Boat.class,new AABB(MOORING).inflate(5)).getFirst();
            System.out.println("FLOW_BOARD_CLIENT boat="+boat.position()+" underwater="+boat.isUnderWater()+" result="+mc().gameMode.interact(mc().player,boat,InteractionHand.MAIN_HAND));
        },()->mc().player.isPassenger() && !utility(MOORING).settings().getBoolean("Secured"));
        add("dismount",()->mc().options.keyShift.setDown(true),()->{if(ticks>10)mc().options.keyShift.setDown(false);return ticks>12 && !mc().player.isPassenger();});
        add("stillwater anchor live GUI",()->{mc().player.getInventory().selected=0;use(ANCHOR);},()->mc().screen instanceof ControllerScreen);
        add("stillwater GUI release action removes client override",()->{
            screenshot("stillwater");click("engineering.streamcontrol.automation_tab");at(150,122);click("gui.streamcontrol.apply");
        },()->region(ANCHOR)==null);
        if(net.neoforged.fml.ModList.get().isLoaded("create")) {
            add("Create live kinetic region",()->use(MECHANICAL),()->mc().screen instanceof ControllerScreen && region(MECHANICAL)!=null);
            add("Create GUI direction apply",()->{screenshot("mechanical");click("gui.streamcontrol.short_e");click("gui.streamcontrol.apply");},()->region(MECHANICAL)!=null && region(MECHANICAL).dx()==1);
            add("Create actual motor reversal reaches client",()->command("reverse"),()->region(MECHANICAL)!=null && region(MECHANICAL).dx()==-1);
            add("Create stopped motor clears client override",()->command("stop"),()->region(MECHANICAL)==null);
        }
        add("render Calibrator HUD and world bounds",()->{
            mc().setScreen(null);command("view");mc().player.setXRot(36);mc().player.setYRot(45);
        },()->{if(ticks==200)screenshot("world");return ticks>210;});
        modelCapture("all-five","models",MODEL_GENERATOR,0,25,true);
        modelCapture("current-generator","model_generator",MODEL_GENERATOR,25,28,true);
        modelCapture("stillwater-anchor","model_anchor",MODEL_ANCHOR,25,28,true);
        modelCapture("flow-sensor","model_sensor",MODEL_SENSOR,25,28,false);
        modelCapture("hydro-current-thruster","model_thruster",MODEL_MECHANICAL,25,22,net.neoforged.fml.ModList.get().isLoaded("create"));
        modelCapture("mooring-post-rope","model_rope",MODEL_MOORING,48,20,false);
        add("resource reload preserves all placed model renderers",()->{modelReload=mc().reloadResourcePacks();command("model_generator");},()->{
            mc().player.setYRot(25);mc().player.setXRot(28);
            if(!modelReload.isDone())return false;
            modelReload.join();
            for(var position:List.of(MODEL_GENERATOR,MODEL_ANCHOR,MODEL_SENSOR,MODEL_MECHANICAL,MODEL_MOORING))if(modelFrame(position)==null)return false;
            if(ticks>=100) {screenshot("model-current-generator-after-reload");return true;}
            return false;
        });
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("streamcontrol.clientIntegrationTest") || done) return;
        try {
            if(++total>12000) throw new AssertionError("Client overall timeout; screen="+mc().screen);
            if(!connecting) {
                if(!(mc().screen instanceof TitleScreen) || mc().getOverlay()!=null) return;
                connecting=true;
                ConnectScreen.startConnecting(mc().screen,mc(),ServerAddress.parseString("127.0.0.1:25565"),new ServerData("Flow test","127.0.0.1:25565",ServerData.Type.OTHER),false,null);
                return;
            }
            if(mc().level==null || mc().player==null) return;
            if(steps.isEmpty()) setup();
            if(step<0) {step=0;ticks=0;}
            var s=steps.get(step);
            if(ticks==0) {System.out.println("FLOW_CLIENT_BEGIN "+s.name);s.action.run();}
            ticks++;
            if(ticks>1 && s.ready.getAsBoolean()) {
                results.put(s.name,true);System.out.println("FLOW_CLIENT_PASS "+s.name);step++;ticks=0;
                if(step==steps.size()) finish(null);
            } else if(ticks>240) throw new AssertionError("Timeout: "+s.name+"; screen="+mc().screen+"; player="+mc().player.position()+"; passenger="+mc().player.isPassenger()+"; crouching="+mc().player.isShiftKeyDown());
        } catch(Throwable t) {t.printStackTrace();finish(t);}
    }
    private static void finish(Throwable error) {
        done=true;mc().options.keyShift.setDown(false);mc().options.hideGui=false;results.put("passed",error==null);
        if(error!=null) {results.put("error",error.toString());screenshot("failure");}
        try {Files.writeString(mc().gameDirectory.toPath().resolve("streamcontrol-client-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}
        catch(Exception e) {e.printStackTrace();}
        System.out.println("STREAMCONTROL_CLIENT_TEST "+results);mc().stop();
    }
}
