package nl.townsandkingdoms.streamcontrol.client;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import nl.townsandkingdoms.streamcontrol.FlowHeading;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import nl.townsandkingdoms.streamcontrol.ControlNetwork;

/** Slate/copper TNK menu artwork, with native input, focus, tooltips and narration. */
public final class ControllerScreen extends Screen {
    private static final int PANEL_W = 316, PANEL_H = 238;
    private static final int INK = 0xFFF4EBE5, MUTED = 0xFFA9C3C8, CREAM = 0xFFF4EBE5;
    private static final int EDGE = 0xFF20293F, GOLD = 0xFFC09473, TEAL = 0xFFA9D2CD;
    private static final ResourceLocation BACKGROUND = texture("controller");
    private static final ResourceLocation MENU_FONT = new ResourceLocation("streamcontrol", "menu");
    private static ResourceLocation texture(String name) {
        return new ResourceLocation("streamcontrol", "textures/gui/" + name + ".png");
    }
    private final ControlNetwork.Open snapshot;
    private int radius;
    private double speed;
    private boolean connected;
    private FlowHeading facing;
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
        facing = FlowHeading.fromId(packet.facing());
        if (facing == null) facing = FlowHeading.NORTH;
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
            for (FlowHeading d : FlowHeading.values()) {
                final FlowHeading direction = d;
                var button = new SlateButton(left + 218 + (d.stepX() + 1) * 28,
                    top + 125 + (d.stepZ() + 1) * 15, 26, 14,
                    tr("short_" + d.abbreviation()), b -> facing = direction, false) {
                    @Override protected boolean selected() { return facing == direction; }
                };
                button.setTooltip(Tooltip.create(tr("direction", tr(d.key()))));
                addRenderableWidget(button);
            }
        }
        var waterToggle = new SlateButton(left + 14, top + 174, 286, 22,
            tr("connected", tr(connected ? "on" : "off")), b -> {
                connected = !connected;
                b.setMessage(tr("connected", tr(connected ? "on" : "off")));
            }, false) {
            @Override protected boolean selected() { return connected; }
            @Override protected void drawLabel(GuiGraphics g, int color) {
                g.drawString(font, tr("connected_label"), getX() + 9, getY() + 7, color, false);
                right(g, tr(connected ? "on" : "off"), getX() + getWidth() - 50, getY() + 7, color);
                int x = getX() + getWidth() - 43, y = getY() + 6;
                bevel(g, x, y, 32, 11, connected ? TEAL : 0xFF344759, false);
                g.fill(x + (connected ? 21 : 3), y + 3, x + (connected ? 29 : 11), y + 8, CREAM);
            }
        };
        waterToggle.setTooltip(Tooltip.create(tr("connected_help")));
        addRenderableWidget(waterToggle);
        addRenderableWidget(new SlateButton(left + 14, top + 200, 174, 20,
            tr("apply"), b -> apply(), true));
        addRenderableWidget(new SlateButton(left + 196, top + 200, 104, 20,
            tr("cancel"), b -> onClose(), false));
    }

    private void apply() {
        ControlNetwork.sendConfigure(new ControlNetwork.Configure(snapshot.pos(), radius, speed,
            connected, facing.id()));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g) {
        // Vanilla deferred background blur must never cover our panel or its lettering.
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0141C2E);
        drawPanel(g);
        Component heading = title.copy().withStyle(style -> style.withFont(MENU_FONT));
        centered(g, heading, left + PANEL_W / 2, top + 12, CREAM);
        g.drawString(font, tr(snapshot.still() ? "calm_subtitle" : "current_subtitle"),
            left + 45, top + 31, MUTED, false);
        drawSeal(g, left + 16, top + 23);
        g.drawString(font, tr("area_preview"), left + 22, top + 54, INK, false);
        drawWaterPreview(g);
        centered(g, tr("preview_diameter", radius * 2 + 1), left + 66, top + 148, INK);
        centered(g, tr("preview_height", snapshot.vertical()), left + 66, top + 159, MUTED);
        g.drawString(font, tr("radius_label"), left + 134, top + 54, INK, false);
        right(g, tr("blocks", radius), left + 300, top + 54, TEAL);
        if (snapshot.still()) {
            bevel(g, left + 134, top + 98, 166, 65, 0xFF446077, false);
            g.drawString(font, tr("moored"), left + 144, top + 108, INK, false);
            g.drawString(font, tr("zero_flow"), left + 144, top + 124, TEAL, false);
            g.drawString(font, tr("no_direction"), left + 144, top + 141, MUTED, false);
        } else {
            g.drawString(font, tr("speed_label"), left + 134, top + 96, INK, false);
            right(g, Component.literal(String.format(Locale.ROOT, "%d%%", Math.round(speed * 100))),
                left + 300, top + 96, TEAL);
            g.drawString(font, tr("heading_compact"), left + 134, top + 132, INK, false);
            g.drawString(font, tr(facing.key()), left + 134, top + 148, MUTED, false);
            bevel(g, left + 246, top + 140, 26, 14, 0xFF304358, false);
            g.fill(left + 257, top + 144, left + 261, top + 150, GOLD);
        }
        // Keep status within the frame, including when redstone disables the controller.
        centered(g, tr(snapshot.powered() ? "powered_status" : "apply_hint"),
            left + PANEL_W / 2, top + 222, snapshot.powered() ? 0xFFFFB093 : MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics g) {
        g.blit(BACKGROUND, left, top, 0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
    }

    private static void control(GuiGraphics g, String name, int x, int y, int w, int h, int state) {
        g.blit(texture(name), x, y, 0f, state * h, w, h, w, h * 5);
    }

    private static void bevel(GuiGraphics g, int x, int y, int w, int h, int fill, boolean raised) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, raised ? 0xFFA9C3C8 : 0xFF304358);
        g.fill(x + 2, y + 2, x + w - 1, y + h - 1, raised ? 0xFF304358 : 0xFF729AA1);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill);
    }

    private void centered(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }
    private void right(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x - font.width(text), y, color, false);
    }
    private void drawSeal(GuiGraphics g, int x, int y) {
        bevel(g, x, y, 22, 23, 0xFF446077, true);
        for (int row = 0; row < 3; row++) {
            int offset = row == 1 ? 2 : 0;
            g.fill(x + 4 + offset, y + 6 + row * 4, x + 15 + offset, y + 7 + row * 4, 0xFFA9D2CD);
            g.fill(x + 4 + offset, y + 5 + row * 4, x + 7 + offset, y + 6 + row * 4, 0xFFA9D2CD);
        }
    }

    private void drawWaterPreview(GuiGraphics g) {
        int x = left + 22, y = top + 68;
        // Schematic only: it communicates circular range, direction, and disconnected pools.
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) {
            int dx = col - 4, dz = row - 4;
            boolean pool = col >= 7 && row < 4;
            boolean wet = col < 6 || pool;
            boolean covered = dx * dx + dz * dz <= 16 && (!connected || !pool);
            int color = !wet ? 0xFFB88B68 : covered ? 0xFF729AA1 : 0xFF344759;
            int tx = x + col * 9, ty = y + row * 8;
            g.fill(tx, ty, tx + 8, ty + 7, color);
            if (wet) g.fill(tx + 2, ty + 2, tx + 6, ty + 3, covered ? 0xFFA9D2CD : 0xFF54768B);
        }
        int cx = x + 4 * 9 + 4, cy = y + 4 * 8 + 3;
        if (snapshot.still()) {
            g.fill(cx - 4, cy - 4, cx + 4, cy + 4, GOLD);
            g.fill(cx - 2, cy - 2, cx + 2, cy + 2, EDGE);
        } else {
            for (int n = -5; n <= 5; n++) {
                int px = cx + facing.stepX() * n, py = cy + facing.stepZ() * n;
                g.fill(px, py, px + 2, py + 2, CREAM);
            }
            int ax = cx + facing.stepX() * 5, ay = cy + facing.stepZ() * 5;
            FlowHeading wing = facing.clockwise();
            for (int n = 0; n <= 3; n++) for (int sign : new int[]{-1, 1}) {
                int px = ax - facing.stepX() * n + wing.stepX() * n * sign;
                int py = ay - facing.stepZ() * n + wing.stepZ() * n * sign;
                g.fill(px, py, px + 2, py + 2, CREAM);
            }
        }
    }

    private class SlateButton extends Button {
        private final boolean primary;
        SlateButton(int x, int y, int w, int h, Component label, OnPress action, boolean primary) {
            super(x, y, w, h, label, action, DEFAULT_NARRATION);
            this.primary = primary;
        }
        protected boolean selected() { return false; }
        protected void drawLabel(GuiGraphics g, int color) {
            centered(g, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            boolean highlight = isHoveredOrFocused();
            int state = !active ? 4 : primary || selected() ? (highlight ? 3 : 2) : (highlight ? 1 : 0);
            String name = getWidth() == 26 ? "compass" : getWidth() == 286 ? "toggle"
                : getWidth() == 174 ? "apply" : "cancel";
            control(g, name, getX(), getY(), getWidth(), getHeight(), state);
            if (highlight) {
                int x = getX(), y = getY(), w = getWidth(), h = getHeight();
                g.fill(x, y, x + w, y + 1, CREAM);
                g.fill(x, y + h - 1, x + w, y + h, CREAM);
                g.fill(x, y, x + 1, y + h, CREAM);
                g.fill(x + w - 1, y, x + w, y + h, CREAM);
            }
            drawLabel(g, active ? CREAM : 0xFF91ABB6);
        }
    }

    private abstract class SlateSlider extends AbstractSliderButton {
        SlateSlider(int x, int y, int w, double value) { super(x, y, w, 16, Component.empty(), value); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
            int x = getX(), y = getY(), w = getWidth();
            g.blit(texture("track"), x, y + 4, 0f, 0f, w, 8, w, 8);
            int handle = x + 3 + (int)Math.round(value * (w - 12));
            g.fill(x + 3, y + 7, handle + 4, y + 9, TEAL);
            control(g, "handle", handle, y + 1, 9, 14, !active ? 4 : isHoveredOrFocused() ? 3 : 2);
            g.fill(handle + 4, y + 5, handle + 5, y + 11, 0xFF4D2B32);
        }
    }
    private final class RadiusSlider extends SlateSlider {
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
    private final class SpeedSlider extends SlateSlider {
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
