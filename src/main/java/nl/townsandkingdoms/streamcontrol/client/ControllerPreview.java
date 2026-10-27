package nl.townsandkingdoms.streamcontrol.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import nl.townsandkingdoms.streamcontrol.FlowHeading;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.townsandkingdoms.streamcontrol.ControlNetwork;

/** Opt-in CI capture of the actual client screen. Inactive in normal games. */
@EventBusSubscriber(modid = "streamcontrol", value = Dist.CLIENT)
public final class ControllerPreview {
    private static int phase, ticks;
    private static boolean captured;
    private static final String[] NAMES = {"current-generator", "stillwater-anchor", "redstone-paused"};
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("streamcontrol.uiPreview")) return;
        var mc = Minecraft.getInstance();
        if (phase == 0) {
            if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
            open(); phase = 1; ticks = 0;
        }
        if (++ticks < 25) return;
        String name = "flow-control-" + NAMES[phase - 1] + ".png";
        if (!captured) {
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {});
            captured = true;
            return;
        }
        if (!new File(mc.gameDirectory, "screenshots/" + name).isFile()) return;
        if (phase == NAMES.length) { mc.stop(); return; }
        phase++; ticks = 0; captured = false; open();
    }
    private static void open() {
        boolean still = phase == 2;
        ControllerScreen.open(new ControlNetwork.Open(0, still ? 6 : 4, 4, 0.75, 2, 16,
            phase != 0 && phase != 1, still, phase == 3, FlowHeading.NORTH_EAST.id()));
    }
}
