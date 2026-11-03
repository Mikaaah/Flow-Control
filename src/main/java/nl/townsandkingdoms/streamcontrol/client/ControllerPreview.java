package nl.townsandkingdoms.streamcontrol.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.townsandkingdoms.streamcontrol.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in CI capture and interaction check. Inactive in normal games. */
@EventBusSubscriber(modid="streamcontrol",value=Dist.CLIENT)
public final class ControllerPreview {
    private static int phase,ticks;
    private static boolean captured;
    private static final String[] NAMES={"current-generator","stillwater-anchor","redstone-paused","automation","flow-sensor","mooring-post","block-models","mechanical-controller"};
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("streamcontrol.uiPreview")) return;
        var mc=Minecraft.getInstance();
        if(phase==0) {
            if(!(mc.screen instanceof TitleScreen) || mc.getOverlay()!=null) return;
            open();phase=1;ticks=0;
        }
        if(++ticks<25) return;
        String name="flow-control-"+NAMES[phase-1]+".png";
        if(!captured) { Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),1,message->{});captured=true;return; }
        if(!new File(mc.gameDirectory,"screenshots/"+name).isFile()) return;
        if(phase==NAMES.length) { mc.stop();return; }
        phase++;ticks=0;captured=false;open();
    }
    private static void open() {
        var mc=Minecraft.getInstance();String name=NAMES[phase==0?0:phase-1];
        if(name.equals("block-models")) { mc.setScreen(new ModelPreviewScreen());return; }
        if(name.equals("flow-sensor") || name.equals("mooring-post")) {
            var n=new CompoundTag();n.putString("Kind",name.equals("flow-sensor")?"sensor":"mooring");n.putInt("Radius",4);n.putInt("Vertical",4);
            n.putInt("MaxRadius",16);n.putInt("MaxVertical",16);n.putBoolean("Enabled",true);n.putInt("Output",8);
            UtilityScreen.open(new ControlNetwork.UtilityOpen(0,n));return;
        }
        boolean still=name.equals("stillwater-anchor");var n=new CompoundTag();n.putInt("MaxVertical",16);
        n.putBoolean("Kinetic",name.equals("mechanical-controller"));n.putFloat("RPM",128);
        n.put("ProfileA",new FlowProfile(4,4,.75,FlowHeading.NORTH_EAST,false,still?FlowProfile.Action.CALM:FlowProfile.Action.CURRENT).save());
        n.put("ProfileB",new FlowProfile(6,3,.5,FlowHeading.SOUTH_WEST,true,still?FlowProfile.Action.RELEASE:FlowProfile.Action.REVERSE).save());
        n.putInt("RedstoneMode",name.equals("automation")?4:0);
        ControllerScreen.open(new ControlNetwork.Open(0,4,4,.75,2,16,false,still,name.equals("redstone-paused"),FlowHeading.NORTH_EAST.id(),n));
        if(name.equals("automation")) {
            var button=mc.screen.children().stream().filter(c->c instanceof Button b && b.getMessage().equals(Component.translatable("engineering.streamcontrol.automation_tab")))
                .map(c->(Button)c).findFirst().orElseThrow();button.onPress(null);
            var profile=mc.screen.children().stream().filter(c->c instanceof Button b && b.getMessage().equals(Component.translatable("engineering.streamcontrol.profile_b")))
                .map(c->(Button)c).findFirst().orElseThrow();profile.onPress(null);
        }
    }
}
