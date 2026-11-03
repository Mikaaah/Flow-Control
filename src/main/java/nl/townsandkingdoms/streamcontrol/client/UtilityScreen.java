package nl.townsandkingdoms.streamcontrol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import nl.townsandkingdoms.streamcontrol.*;
import static nl.townsandkingdoms.streamcontrol.client.EngineeringWidgets.*;

public final class UtilityScreen extends Screen {
    private final ControlNetwork.UtilityOpen snapshot;
    private final CompoundTag options;
    private final boolean sensor;
    private int radius,vertical,mode,left,top;
    private boolean toggle;
    public static void open(ControlNetwork.UtilityOpen packet) { Minecraft.getInstance().setScreen(new UtilityScreen(packet)); }
    private UtilityScreen(ControlNetwork.UtilityOpen packet) {
        super(Component.translatable("block.streamcontrol."+("sensor".equals(packet.settings().getString("Kind"))?"flow_sensor":"mooring_post")));
        snapshot=packet;options=packet.settings().copy();sensor="sensor".equals(options.getString("Kind"));
        radius=options.getInt("Radius");vertical=options.getInt("Vertical");mode=nl.townsandkingdoms.streamcontrol.Numbers.clamp(options.getInt("Mode"),0,2);
        toggle=options.getBoolean(sensor?"EmptyOnly":"Enabled");
    }
    private Component tr(String key,Object...args) { return Component.translatable("engineering.streamcontrol."+key,args); }
    @Override protected void init() {
        left=(width-316)/2;top=(height-238)/2;
        addRenderableWidget(new RangeSlider(left+26,top+72,264,1,Math.max(1,options.getInt("MaxRadius")),radius,"radius",n->radius=n));
        addRenderableWidget(new RangeSlider(left+26,top+112,264,0,Math.max(0,options.getInt("MaxVertical")),vertical,"vertical",n->vertical=n));
        var select=new ActionButton(left+26,top+138,264,22,sensor?modeLabel():tr("mooring_automatic"),b->{ mode=(mode+1)%3;b.setMessage(modeLabel()); },()->false);
        select.active=sensor;addRenderableWidget(select);
        addRenderableWidget(new ActionButton(left+14,top+176,286,22,toggleLabel(),b->{toggle=!toggle;b.setMessage(toggleLabel());},()->toggle));
        addRenderableWidget(new ActionButton(left+14,top+200,174,20,Component.translatable("gui.streamcontrol.apply"),b->apply(),()->true));
        addRenderableWidget(new ActionButton(left+196,top+200,104,20,Component.translatable("gui.streamcontrol.cancel"),b->onClose(),()->false));
    }
    private Component modeLabel() { return tr("sensor_mode",tr("sensor_mode_"+mode)); }
    private Component toggleLabel() { return tr(sensor?"empty_boats":"auto_moor",Component.translatable("gui.streamcontrol."+(toggle?"on":"off"))); }
    private void apply() {
        options.putInt("Radius",radius);options.putInt("Vertical",vertical);options.putInt("Mode",mode);options.putBoolean(sensor?"EmptyOnly":"Enabled",toggle);
        PacketDistributor.sendToServer(new ControlNetwork.UtilityConfigure(snapshot.pos(),options));onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g) {}
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        g.fill(0,0,width,height,0xA0141C2E);
        g.blit(texture("automation_controller"),left,top,0f,0f,316,238,316,238);
        var menu=net.minecraft.resources.new ResourceLocation("streamcontrol","menu");
        text(g,title.copy().withStyle(s->s.withFont(menu)),left+158,top+12,TEXT);
        text(g,tr(sensor?"sensor_subtitle":"mooring_subtitle"),left+158,top+32,MUTED);
        g.drawString(font,tr("horizontal_range"),left+26,top+57,TEXT,false);
        right(g,tr("blocks",radius),left+290,top+57);
        g.drawString(font,tr("vertical_range"),left+26,top+97,TEXT,false);
        right(g,Component.literal("+/- "+vertical),left+290,top+97);
        Component detail=sensor ? tr("sensor_hint_"+mode) : tr("mooring_release_hint");
        text(g,detail,left+158,top+164,MUTED);
        Component status=sensor?tr("sensor_output",options.getInt("Output")):
            tr(options.getBoolean("Powered")?"mooring_powered":options.getBoolean("Secured")?"mooring_secured":"mooring_waiting");
        if(minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(net.minecraft.core.BlockPos.of(snapshot.pos())) instanceof FlowSensorEntity be)
            status=tr("sensor_output",be.output());
        if(minecraft!=null && minecraft.level!=null && minecraft.level.getBlockEntity(net.minecraft.core.BlockPos.of(snapshot.pos())) instanceof MooringEntity be) {
            var live=be.settings();status=tr(live.getBoolean("Powered")?"mooring_powered":live.getBoolean("Secured")?"mooring_secured":"mooring_waiting");
        }
        text(g,status,left+158,top+222,TEAL);super.render(g,mx,my,tick);
    }
    private void right(GuiGraphics g,Component s,int x,int y) { g.drawString(font,s,x-font.width(s),y,TEAL,false); }
}
