package nl.townsandkingdoms.streamcontrol.client;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FontDescription;

import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Reuses the controller's copper/slate control atlas with fixed-width pixel borders. */
final class EngineeringWidgets {
    static final int TEXT=0xFFF4EBE5, MUTED=0xFFA9C3C8, TEAL=0xFFA9D2CD;
    static Identifier texture(String name) { return Identifier.fromNamespaceAndPath("streamcontrol","textures/gui/"+name+".png"); }
    static void text(GuiGraphicsExtractor g,Component text,int center,int y,int color) { var f=Minecraft.getInstance().font;g.text(f,text,center-f.width(text)/2,y,color,false); }
    static void button(GuiGraphicsExtractor g,int x,int y,int w,int h,int state) {
        // Nine texture regions: retain 3px bevels even on short navigation controls.
        int[] sourceX={0,3,171},sourceY={state*20,state*20+3,state*20+17};
        int[] sw={3,168,3},sh={3,14,3},dw={3,w-6,3},dh={3,h-6,3};
        int yy=y;
        for(int row=0;row<3;row++) { int xx=x;
            for(int col=0;col<3;col++) {
                g.blit(RenderPipelines.GUI_TEXTURED,texture("apply"),xx,yy,(float)sourceX[col],(float)sourceY[row],dw[col],dh[row],sw[col],sh[row],174,100);
                xx+=dw[col];
            }
            yy+=dh[row];
        }
    }
    static class ActionButton extends Button {
        private final BooleanSupplier selected;
        ActionButton(int x,int y,int w,int h,Component label,OnPress action,BooleanSupplier selected) { super(x,y,w,h,label,action,DEFAULT_NARRATION);this.selected=selected; }
        @Override public void extractContents(GuiGraphicsExtractor g,int mx,int my,float tick) {
            boolean hot=isHoveredOrFocused();int state=!active?4:selected.getAsBoolean()?(hot?3:2):(hot?1:0);
            button(g,getX(),getY(),getWidth(),getHeight(),state);
            text(g,getMessage(),getX()+getWidth()/2,getY()+(getHeight()-8)/2,active?TEXT:MUTED);
        }
    }
    static final class RangeSlider extends AbstractSliderButton {
        private final int min,max;private final IntConsumer setter;private final String key;
        RangeSlider(int x,int y,int w,int min,int max,int value,String key,IntConsumer setter) {
            super(x,y,w,16,Component.empty(),(double)(value-min)/Math.max(1,max-min));this.min=min;this.max=max;this.setter=setter;this.key=key;updateMessage();active=max>min;
        }
        private int current() { return min+(int)Math.round(value*(max-min)); }
        @Override protected void updateMessage() { setMessage(Component.translatable("engineering.streamcontrol."+key,current())); }
        @Override protected void applyValue() { setter.accept(current());updateMessage(); }
        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
            int x=getX(),y=getY(),w=getWidth();
            // Reuse the generated native wide track at its exact width.
            g.blit(RenderPipelines.GUI_TEXTURED,texture("track_wide"),x,y+4,0f,0f,w,8,w,8);
            int handle=x+3+(int)Math.round(value*(w-12));g.fill(x+3,y+7,handle+4,y+9,TEAL);
            int state=!active?4:isHoveredOrFocused()?3:2;
            g.blit(RenderPipelines.GUI_TEXTURED,texture("handle"),handle,y+1,0f,state*14,9,14,9,70);
        }
    }
}
