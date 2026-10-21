package nl.townsandkingdoms.streamcontrol.client;

import net.minecraft.client.Minecraft;
import nl.townsandkingdoms.streamcontrol.*;

/** Loaded only by the clientbound packet handler. */
public final class ClientUpdates {
    public static void apply(ControlNetwork.Update update) {
        ControlConfig.ENABLED.set(update.enabled());
        FlowRegion region = update.region() == null ? null : FlowRegion.load(update.region());
        FlowRegion previous = FlowOverrides.clientByKey(region == null ? update.remove() : region.key());
        FlowOverrides.receive(update.dimension(), update.reset(), region, update.remove());
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().toString().equals(update.dimension())) return;
        if (update.reset()) { mc.levelRenderer.allChanged(); return; }
        dirty(previous); dirty(region);
    }
    private static void dirty(FlowRegion region) {
        if (region == null) return;
        var c = region.center(); int r = region.radius()+1, y = region.vertical()+1;
        Minecraft.getInstance().levelRenderer.setBlocksDirty(c.getX()-r,c.getY()-y,c.getZ()-r,c.getX()+r,c.getY()+y,c.getZ()+r);
    }
}
