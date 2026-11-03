package nl.townsandkingdoms.streamcontrol.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import nl.townsandkingdoms.streamcontrol.*;

/** Local inspection of already synchronized data. No per-frame packets or chunk scans. */
public final class EngineeringClient {
    private record Readout(BlockPos center,int radius,int vertical,List<Component> lines,boolean bounds) {}
    private static Readout readout;
    private static long lastTick=Long.MIN_VALUE;
    public static void register(IEventBus bus) {
        bus.addListener(EngineeringClient::layers);
        if(ModList.get().isLoaded("create")) bus.addListener(nl.townsandkingdoms.streamcontrol.compat.create.CreateClient::renderers);
        MinecraftForge.EVENT_BUS.addListener(EngineeringClient::world);
    }
    private static Component tr(String key,Object...args) { return Component.translatable("engineering.streamcontrol."+key,args); }
    private static ItemStack tool(Minecraft mc) {
        if(mc.player==null) return ItemStack.EMPTY;
        if(mc.player.getMainHandItem().is(StreamControl.WAND.get())) return mc.player.getMainHandItem();
        if(mc.player.getOffhandItem().is(StreamControl.WAND.get())) return mc.player.getOffhandItem();return ItemStack.EMPTY;
    }
    private static void update() {
        var mc=Minecraft.getInstance();var stack=tool(mc);
        if(mc.level==null || stack.isEmpty() || mc.screen!=null || mc.options.hideGui) { readout=null;lastTick=Long.MIN_VALUE;return; }
        long tick=mc.level.getGameTime();if(tick==lastTick) return;lastTick=tick;
        var start=mc.player.getEyePosition();var end=start.add(mc.player.getLookAngle().scale(mc.gameMode.getPickRange()));
        var hit=mc.level.clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,mc.player));
        List<Component> lines=new ArrayList<>();lines.add(Component.translatable("calibrator.streamcontrol.mode",Component.translatable(FlowWand.mode(stack).key())));
        if(hit.getType()!=HitResult.Type.BLOCK) { readout=new Readout(mc.player.blockPosition(),0,0,lines,false);return; }
        var pos=hit.getBlockPos();var be=mc.level.getBlockEntity(pos);int radius=0,vertical=0;BlockPos center=pos;
        FlowRegion region=FlowOverrides.clientAt(pos.getX(),pos.getY(),pos.getZ());
        if(be instanceof FlowController controller) {
            var override=FlowOverrides.clientByKey(pos.asLong());
            radius=override==null?controller.radius():override.radius();vertical=override==null?controller.controls().a().vertical():override.vertical();
            lines.add(be.getBlockState().getBlock().getName());
            if(controller.controls().source()!=null) lines.add(tr("linked_source",controller.controls().source().toShortString()));
            if(!Float.isNaN(controller.rpm())) lines.add(tr("drive_rpm",Math.round(controller.rpm())));
        } else if(be instanceof FlowSensorEntity sensor) {
            radius=sensor.radius();vertical=sensor.vertical();lines.add(tr("sensor_output",sensor.output()));
            lines.add(tr("sensor_mode",tr("sensor_mode_"+sensor.mode().ordinal())));
        } else if(be instanceof MooringEntity mooring) {
            radius=mooring.radius();vertical=mooring.vertical();lines.add(tr(mooring.boatId()==null?"mooring_waiting":"mooring_secured"));
        } else if(mc.level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
            var flow=mc.level.getFluidState(pos).getFlow(mc.level,pos);var heading=FlowHeading.fromVector(flow.x,flow.z);
            lines.add(tr("readout_speed",Math.round(Math.hypot(flow.x,flow.z)*100)));
            lines.add(tr("readout_direction",heading==null?tr("still"):Component.translatable("gui.streamcontrol."+heading.key())));
            lines.add(tr(region==null?"source_natural":region.controller()?"source_controller":"source_legacy"));
            if(region!=null) { radius=region.radius();vertical=region.vertical();center=region.center(); }
        }
        if(radius>0) lines.add(tr("readout_range",radius,vertical));
        readout=new Readout(center,radius,vertical,lines,radius>0 && ControlConfig.PARTICLES.get() && !FlowWand.settings(stack).getBoolean("HideBounds"));
    }
    private static void layers(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("calibrator",(gui,g,delta,width,height)->hud(g));
    }
    private static void hud(GuiGraphics g) {
        update();if(readout==null) return;var mc=Minecraft.getInstance();int widest=0;
        for(var line:readout.lines) widest=Math.max(widest,mc.font.width(line));
        int w=widest+16,x=mc.getWindow().getGuiScaledWidth()-w-8,y=8;
        g.fill(x,y,x+w,y+readout.lines.size()*11+12,0xE620293F);
        g.fill(x,y,x+2,y+readout.lines.size()*11+12,0xFFC09473);
        for(int i=0;i<readout.lines.size();i++) g.drawString(mc.font,readout.lines.get(i),x+8,y+6+i*11,i==0?0xFFF4EBE5:0xFFA9D2CD,false);
    }
    private static void world(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;update();
        if(readout==null || !readout.bounds) return;
        var mc=Minecraft.getInstance();var pose=e.getPoseStack();var camera=e.getCamera().getPosition();
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var vertices=buffers.getBuffer(RenderType.lines());
        double x=readout.center.getX()+.5,z=readout.center.getZ()+.5,r=readout.radius+.5;
        double low=readout.center.getY()-readout.vertical,high=readout.center.getY()+readout.vertical+1;
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
            double ax=x+Math.cos(a)*r,az=z+Math.sin(a)*r,bx=x+Math.cos(b)*r,bz=z+Math.sin(b)*r;
            line(pose,vertices,ax,low,az,bx,low,bz);line(pose,vertices,ax,high,az,bx,high,bz);
            if(i%8==0) line(pose,vertices,ax,low,az,ax,high,az);
        }
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
    private static void line(PoseStack pose,VertexConsumer out,double x,double y,double z,double bx,double by,double bz) {
        double length=Math.sqrt((bx-x)*(bx-x)+(by-y)*(by-y)+(bz-z)*(bz-z));
        float nx=(float)((bx-x)/length),ny=(float)((by-y)/length),nz=(float)((bz-z)/length);
        out.vertex(pose.last().pose(),(float)x,(float)y,(float)z).color(0.66f,0.83f,0.80f,0.85f).normal(pose.last().normal(),nx,ny,nz).endVertex();
        out.vertex(pose.last().pose(),(float)bx,(float)by,(float)bz).color(0.66f,0.83f,0.80f,0.85f).normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
}
