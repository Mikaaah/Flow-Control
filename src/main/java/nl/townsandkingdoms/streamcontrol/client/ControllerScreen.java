package nl.townsandkingdoms.streamcontrol.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import nl.townsandkingdoms.streamcontrol.ControlNetwork;

/** One parchment-and-brass visual language for every control, with vanilla input/narration. */
public final class ControllerScreen extends Screen {
    private static final int PANEL_W = 316, PANEL_H = 238;
    private static final int INK = 0xFF493326, MUTED = 0xFF806246, CREAM = 0xFFF4DFAE;
    private static final int EDGE = 0xFF302018, GOLD = 0xFFC69B58, TEAL = 0xFF337F83;
    private final ControlNetwork.Open snapshot;
    private int radius;
    private double speed;
    private boolean connected;
    private Direction facing;
    private int left, top;

    public static void open(ControlNetwork.Open packet) {
        Minecraft.getInstance().setScreen(new ControllerScreen(packet));
    }

    private ControllerScreen(ControlNetwork.Open packet) {
        super(Component.translatable(packet.still() ? "block.streamcontrol.stillwater_anchor"
            : "block.streamcontrol.current_generator"));
        snapshot = packet;
        radius = Math.max(1, Math.min(packet.maxRadius(), packet.radius()));
        speed = Math.max(0.05, Math.min(packet.maxStrength(), packet.strength()));
        connected = packet.connectedOnly();
        facing = Direction.from3DDataValue(packet.facing());
        if (!facing.getAxis().isHorizontal()) facing = Direction.NORTH;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.streamcontrol." + key, args);
    }

    @Override protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        var radiusSlider = addRenderableWidget(new RadiusSlider(left + 134, top + 68, 166));
        radiusSlider.active = snapshot.maxRadius() > 1;
        radiusSlider.setTooltip(Tooltip.create(tr("radius_help")));
        if (!snapshot.still()) {
            addRenderableWidget(new SpeedSlider(left + 134, top + 110, 166));
            int i = 0;
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                final Direction direction = d;
                var button = new ParchmentButton(left + 134 + i * 43, top + 145, 37, 19,
                    tr("short_" + d.getName()), b -> facing = direction, false) {
                    @Override protected boolean selected() { return facing == direction; }
                };
                button.setTooltip(Tooltip.create(tr("direction", tr(d.getName()))));
                addRenderableWidget(button);
                i++;
            }
        }
        var waterToggle = new ParchmentButton(left + 14, top + 174, 286, 22,
            tr("connected", tr(connected ? "on" : "off")), b -> {
                connected = !connected;
                b.setMessage(tr("connected", tr(connected ? "on" : "off")));
            }, false) {
            @Override protected boolean selected() { return connected; }
            @Override protected void drawLabel(GuiGraphics g, int color) {
                g.drawString(font, tr("connected_label"), getX() + 9, getY() + 7, color, false);
                right(g, tr(connected ? "on" : "off"), getX() + getWidth() - 50, getY() + 7, color);
                int x = getX() + getWidth() - 43, y = getY() + 6;
                bevel(g, x, y, 32, 11, connected ? TEAL : 0xFF8B7456, false);
                g.fill(x + (connected ? 21 : 3), y + 3, x + (connected ? 29 : 11), y + 8, CREAM);
            }
        };
        waterToggle.setTooltip(Tooltip.create(tr("connected_help")));
        addRenderableWidget(waterToggle);
        addRenderableWidget(new ParchmentButton(left + 14, top + 202, 174, 20,
            tr("apply"), b -> apply(), true));
        addRenderableWidget(new ParchmentButton(left + 196, top + 202, 104, 20,
            tr("cancel"), b -> onClose(), false));
    }

    private void apply() {
        ControlNetwork.sendConfigure(new ControlNetwork.Configure(snapshot.pos(), radius, speed,
            connected, facing.get3DDataValue()));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g) {
        // Vanilla deferred background blur must never cover our panel or its lettering.
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0181412);
        drawPanel(g);
        g.drawString(font, title, left + 45, top + 14, CREAM, false);
        g.drawString(font, tr(snapshot.still() ? "calm_subtitle" : "current_subtitle"),
            left + 45, top + 28, 0xFFBFA47D, false);
        drawSeal(g, left + 16, top + 14);
        g.drawString(font, tr("area_preview"), left + 22, top + 54, INK, false);
        drawWaterPreview(g);
        centered(g, tr("preview_diameter", radius * 2 + 1), left + 66, top + 151, INK);
        centered(g, tr("preview_height", snapshot.vertical()), left + 66, top + 162, MUTED);
        g.drawString(font, tr("radius_label"), left + 134, top + 54, INK, false);
        right(g, tr("blocks", radius), left + 300, top + 54, TEAL);
        if (snapshot.still()) {
            bevel(g, left + 134, top + 98, 166, 65, 0xFFCAB487, false);
            g.drawString(font, tr("moored"), left + 144, top + 108, INK, false);
            g.drawString(font, tr("zero_flow"), left + 144, top + 124, TEAL, false);
            g.drawString(font, tr("no_direction"), left + 144, top + 141, MUTED, false);
        } else {
            g.drawString(font, tr("speed_label"), left + 134, top + 96, INK, false);
            right(g, Component.literal(String.format(Locale.ROOT, "%d%%", Math.round(speed * 100))),
                left + 300, top + 96, TEAL);
            g.drawString(font, tr("heading_label"), left + 134, top + 132, INK, false);
            right(g, tr(facing.getName()), left + 300, top + 132, MUTED);
        }
        // Keep status within the frame, including when redstone disables the controller.
        centered(g, tr(snapshot.powered() ? "powered_status" : "apply_hint"),
            left + PANEL_W / 2, top + 224, snapshot.powered() ? 0xFF88432F : MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics g) {
        g.fill(left + 3, top + 5, left + PANEL_W + 3, top + PANEL_H + 5, 0x70000000);
        bevel(g, left, top, PANEL_W, PANEL_H, 0xFF795137, true);
        bevel(g, left + 5, top + 5, PANEL_W - 10, PANEL_H - 10, 0xFFE0C694, true);
        g.fill(left + 8, top + 8, left + PANEL_W - 8, top + 44, 0xFF513A2C);
        g.fill(left + 8, top + 44, left + PANEL_W - 8, top + 46, GOLD);
        // Discrete, stable parchment flecks: no random flicker or non-pixel gradients.
        for (int i = 0; i < 110; i++) {
            int x = left + 9 + Math.floorMod(i * 67 + 19, PANEL_W - 18);
            int y = top + 48 + Math.floorMod(i * 31 + 7, PANEL_H - 59);
            g.fill(x, y, x + 1, y + 1, (i & 1) == 0 ? 0xFFCDB383 : 0xFFECD5A4);
        }
        for (int x : new int[]{left + 3, left + PANEL_W - 6})
            for (int y : new int[]{top + 3, top + PANEL_H - 6}) {
                g.fill(x, y, x + 3, y + 3, GOLD);
                g.fill(x, y, x + 1, y + 1, CREAM);
            }
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, boolean raised) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, raised ? 0xFFE2BD7A : 0xFF8F6D45);
        g.fill(x + 2, y + 2, x + w - 1, y + h - 1, raised ? 0xFF67472E : 0xFFF0D8A0);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
    }

    private void centered(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }
    private void right(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text), y, color, false);
    }
    private void drawSeal(GuiGraphics g, int x, int y) {
        bevel(g, x, y, 22, 23, 0xFF284C4F, true);
        for (int row = 0; row < 3; row++) {
            int offset = row == 1 ? 2 : 0;
            g.fill(x + 4 + offset, y + 6 + row * 4, x + 15 + offset, y + 7 + row * 4, 0xFF8FCBC0);
            g.fill(x + 4 + offset, y + 5 + row * 4, x + 7 + offset, y + 6 + row * 4, 0xFF8FCBC0);
        }
    }

    private void drawWaterPreview(GuiGraphics g) {
        int x = left + 22, y = top + 68;
        bevel(g, x - 3, y - 3, 88, 81, 0xFF67543B, false);
        // Schematic only: it communicates circular range, direction, and disconnected pools.
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) {
            int dx = col - 4, dz = row - 4;
            boolean pool = col >= 7 && row < 4;
            boolean wet = col < 6 || pool;
            boolean covered = dx * dx + dz * dz <= 16 && (!connected || !pool);
            int color = !wet ? 0xFF9B8157 : covered ? 0xFF5DADB0 : 0xFF396B72;
            int tx = x + col * 9, ty = y + row * 8;
            g.fill(tx, ty, tx + 8, ty + 7, color);
            if (wet) g.fill(tx + 2, ty + 2, tx + 6, ty + 3, covered ? 0xFF91CFC8 : 0xFF51858A);
        }
        int cx = x + 4 * 9 + 4, cy = y + 4 * 8 + 3;
        if (snapshot.still()) {
            g.fill(cx - 4, cy - 4, cx + 4, cy + 4, GOLD);
            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, EDGE);
        } else {
            for (int n = -5; n <= 5; n++) {
                int px = cx + facing.getStepX() * n, py = cy + facing.getStepZ() * n;
                g.fill(px, py, px + 2, py + 2, CREAM);
            }
            int ax = cx + facing.getStepX() * 5, ay = cy + facing.getStepZ() * 5;
            Direction wing = facing.getClockWise();
            for (int n = 0; n <= 3; n++) for (int sign : new int[]{-1, 1}) {
                int px = ax - facing.getStepX() * n + wing.getStepX() * n * sign;
                int py = ay - facing.getStepZ() * n + wing.getStepZ() * n * sign;
                g.fill(px, py, px + 2, py + 2, CREAM);
            }
        }
    }

    private class ParchmentButton extends Button {
        private final boolean primary;
        ParchmentButton(int x, int y, int w, int h, Component label, OnPress action, boolean primary) {
            super(x, y, w, h, label, action, DEFAULT_NARRATION);
            this.primary = primary;
        }
        protected boolean selected() { return false; }
        protected void drawLabel(GuiGraphics g, int color) {
            centered(g, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            boolean highlight = isHoveredOrFocused();
            int color = primary || selected() ? (highlight ? 0xFF498F89 : TEAL)
                : (highlight ? 0xFFEED4A0 : 0xFFD2B47F);
            bevel(g, getX(), getY(), getWidth(), getHeight(), active ? color : 0xFFAB9877, true);
            if (highlight) {
                g.fill(getX() + 3, getY() + 3, getX() + 5, getY() + getHeight() - 3, GOLD);
                g.fill(getX() + getWidth() - 5, getY() + 3, getX() + getWidth() - 3, getY() + getHeight() - 3, GOLD);
            }
            drawLabel(g, primary || selected() ? CREAM : INK);
        }
    }

    private abstract class ParchmentSlider extends AbstractSliderButton {
        ParchmentSlider(int x, int y, int w, double value) { super(x, y, w, 16, Component.empty(), value); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            int x = getX(), y = getY(), w = getWidth();
            bevel(g, x, y + 4, w, 8, 0xFF9E8157, false);
            int handle = x + 3 + (int)Math.round(value * (w - 12));
            g.fill(x + 3, y + 7, handle + 4, y + 9, TEAL);
            bevel(g, handle, y + 1, 9, 14, isHoveredOrFocused() ? 0xFFF2D18C : GOLD, true);
            g.fill(handle + 4, y + 5, handle + 5, y + 11, 0xFF815830);
        }
    }
    private final class RadiusSlider extends ParchmentSlider {
        RadiusSlider(int x, int y, int w) {
            super(x, y, w, (double)(radius - 1) / Math.max(1, snapshot.maxRadius() - 1));
            updateMessage();
        }
        @Override protected void updateMessage() { setMessage(tr("radius", radius)); }
        @Override protected void applyValue() {
            radius = 1 + (int)Math.round(value * (snapshot.maxRadius() - 1));
            updateMessage();
        }
    }
    private final class SpeedSlider extends ParchmentSlider {
        SpeedSlider(int x, int y, int w) {
            super(x, y, w, (speed - 0.05) / Math.max(0.001, snapshot.maxStrength() - 0.05));
            updateMessage();
        }
        @Override protected void updateMessage() { setMessage(tr("speed", Math.round(speed * 100) + "%")); }
        @Override protected void applyValue() {
            speed = Math.max(0.05, Math.min(snapshot.maxStrength(),
                Math.round((0.05 + value * (snapshot.maxStrength() - 0.05)) * 20) / 20.0));
            updateMessage();
        }
    }
}
