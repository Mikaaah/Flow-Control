package nl.townsandkingdoms.streamcontrol.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.townsandkingdoms.streamcontrol.StreamControl;

/** Opt-in verification gallery of Minecraft's real baked item models. */
final class ModelPreviewScreen extends Screen {
    private final List<ItemStack> items=new ArrayList<>();
    ModelPreviewScreen() {
        super(Component.literal("Flow Control - Block Models"));
        items.add(new ItemStack(StreamControl.GENERATOR.get()));items.add(new ItemStack(StreamControl.ANCHOR.get()));
        items.add(new ItemStack(StreamControl.SENSOR.get()));items.add(new ItemStack(StreamControl.MOORING.get()));
        StreamControl.OPTIONAL_ITEMS.forEach(item->items.add(new ItemStack(item.get())));
    }
    @Override public void renderBackground(GuiGraphics g) {}
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        int left=(width-316)/2,top=(height-238)/2;
        g.fill(0,0,width,height,0xFF151C2E);g.blit(EngineeringWidgets.texture("automation_controller"),left,top,0f,0f,316,238,316,238);
        g.fill(left+16,top+49,left+300,top+219,0xFF446077);
        EngineeringWidgets.text(g,title,left+158,top+12,EngineeringWidgets.TEXT);
        EngineeringWidgets.text(g,Component.literal("Actual Minecraft item renders"),left+158,top+32,EngineeringWidgets.MUTED);
        for(int i=0;i<items.size();i++) {
            int remaining=items.size()-3;int center=left+(i<3?62+i*96:158+(i-3)*96-(remaining-1)*48);
            int y=top+(i<3?62:143);ItemStack stack=items.get(i);
            g.pose().pushPose();g.pose().translate(center-24,y,0);g.pose().scale(3,3,3);g.renderItem(stack,0,0);g.pose().popPose();
            int ty=y+51;
            for(var line:font.split(stack.getHoverName(),92)) { g.drawString(font,line,center-font.width(line)/2,ty,EngineeringWidgets.TEXT,false);ty+=10; }
        }
        super.render(g,mx,my,tick);
    }
}
