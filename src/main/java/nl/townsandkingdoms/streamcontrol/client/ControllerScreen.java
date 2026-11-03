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
import nl.townsandkingdoms.streamcontrol.FlowProfile;
import nl.townsandkingdoms.streamcontrol.ControllerSettings;
import net.minecraft.nbt.CompoundTag;

/** Slate/copper TNK menu artwork, with native input, focus, tooltips and narration. */
public final class ControllerScreen extends Screen {
    private static final int PANEL_W = 316, PANEL_H = 238;
    private static final int INK = 0xFFF4EBE5, MUTED = 0xFFA9C3C8, CREAM = 0xFFF4EBE5;
    private static final int EDGE = 0xFF20293F, GOLD = 0xFFC09473, TEAL = 0xFFA9D2CD;
    private static final ResourceLocation CURRENT_BACKGROUND = texture("current_controller");
    private static final ResourceLocation STILL_BACKGROUND = texture("stillwater_controller");
    private static final ResourceLocation MENU_FONT = ResourceLocation.fromNamespaceAndPath("streamcontrol", "menu");
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("streamcontrol", "textures/gui/" + name + ".png");
    }
    private final ControlNetwork.Open snapshot;
    private int radius;
    private double speed;
    private boolean connected;
    private FlowHeading facing;
    private int left, top;
    private final CompoundTag settings;
    private FlowProfile profileA, profileB;
    private int editProfile, vertical;
    private FlowProfile.Action action;
    private boolean automation;

    public static void open(ControlNetwork.Open packet) {
        Minecraft.getInstance().setScreen(new ControllerScreen(packet));
    }

    private ControllerScreen(ControlNetwork.Open packet) {
        super(Component.translatable(packet.still() ? "block.streamcontrol.stillwater_anchor"
            : packet.settings()!=null && packet.settings().getBoolean("Kinetic") ? "block.streamcontrol.mechanical_current_generator" : "block.streamcontrol.current_generator"));
        snapshot = packet;
        radius = Math.max(1, Math.min(packet.maxRadius(), packet.radius()));
        speed = Math.max(0.05, Math.min(packet.maxStrength(), packet.strength()));
        connected = packet.connectedOnly();
        facing = FlowHeading.fromId(packet.facing());
        if (facing == null) facing = FlowHeading.NORTH;
        settings = packet.settings()==null ? new CompoundTag() : packet.settings().copy();
        profileA = settings.contains("ProfileA") ? FlowProfile.load(settings.getCompound("ProfileA"),packet.still()) :
            new FlowProfile(radius,packet.vertical(),speed,facing,connected,packet.still()?FlowProfile.Action.CALM:FlowProfile.Action.CURRENT);
        profileB = settings.contains("ProfileB") ? FlowProfile.load(settings.getCompound("ProfileB"),packet.still()) :
            new FlowProfile(radius,packet.vertical(),speed,facing,connected,packet.still()?FlowProfile.Action.RELEASE:FlowProfile.Action.REVERSE);
        readProfile();
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.streamcontrol." + key, args);
    }

    private Component engineering(String key,Object...args) { return Component.translatable("engineering.streamcontrol."+key,args); }
    private void readProfile() {
        var p=editProfile==0?profileA:profileB;radius=p.radius();vertical=p.vertical();speed=p.strength();facing=p.heading();connected=p.connectedOnly();action=p.action();
    }
    private void storeProfile() {
        var p=new FlowProfile(radius,vertical,speed,facing,connected,action);
        if(editProfile==0) profileA=p;else profileB=p;
    }
    private void rebuild() { clearWidgets();init(); }
    private void changeProfile(int selected) { storeProfile();editProfile=selected;readProfile();rebuild(); }
    private EngineeringWidgets.ActionButton actionButton(int x,int y,int w,int h,Component label,Button.OnPress press,java.util.function.BooleanSupplier selected) {
        return addRenderableWidget(new EngineeringWidgets.ActionButton(left+x,top+y,w,h,label,press,selected));
    }
    @Override protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        actionButton(16,29,138,14,engineering("settings_tab",editProfile==0?"A":"B"),b->{automation=false;rebuild();},()->!automation);
        actionButton(162,29,138,14,engineering("automation_tab"),b->{automation=true;rebuild();},()->automation);
        if(automation) {
            actionButton(26,56,264,22,engineering("redstone_"+settings.getInt("RedstoneMode")),b->{
                int next=(settings.getInt("RedstoneMode")+1)%ControllerSettings.Redstone.values().length;
                settings.putInt("RedstoneMode",next);b.setMessage(engineering("redstone_"+next));
            },()->false);
            actionButton(26,84,100,20,engineering("profile_a"),b->changeProfile(0),()->editProfile==0);
            actionButton(134,84,100,20,engineering("profile_b"),b->changeProfile(1),()->editProfile==1);
            var unlink=actionButton(242,84,48,20,engineering("unlink"),b->{settings.remove("LinkedSource");rebuild();},()->false);
            unlink.active=settings.contains("LinkedSource");
            actionButton(26,111,264,22,engineering("action_"+action.ordinal()),b->{
                action=snapshot.still()?(action==FlowProfile.Action.CALM?FlowProfile.Action.RELEASE:FlowProfile.Action.CALM):FlowProfile.Action.values()[(action.ordinal()+1)%4];
                b.setMessage(engineering("action_"+action.ordinal()));
            },()->false);
            addRenderableWidget(new EngineeringWidgets.RangeSlider(left+26,top+154,264,0,settings.contains("MaxVertical")?settings.getInt("MaxVertical"):16,vertical,"vertical",n->vertical=n));
        } else {
        var radiusSlider = addRenderableWidget(new RadiusSlider(left + (snapshot.still() ? 26 : 182),
            top + (snapshot.still() ? 156 : 83), snapshot.still() ? 264 : 108));
        radiusSlider.active = snapshot.maxRadius() > 1;
        radiusSlider.setTooltip(Tooltip.create(tr("radius_help")));
        if (!snapshot.still()) {
            addRenderableWidget(new SpeedSlider(left + 182, top + 133, 108));
            for (FlowHeading d : FlowHeading.values()) {
                final FlowHeading direction = d;
                // Eight compass points surround the chart. Every hitbox stays inside its card.
                int cx = 89 + d.stepX() * (d.diagonal() ? 48 : 56);
                int cy = 112 + d.stepZ() * (d.diagonal() ? 28 : 38);
                var button = new SlateButton(left + cx - 13, top + cy - 7, 26, 14,
                    tr("short_" + d.abbreviation()), b -> facing = direction, false) {
                    @Override protected boolean selected() { return facing == direction; }
                };
                button.setTooltip(Tooltip.create(tr("direction", tr(d.key()))));
                addRenderableWidget(button);
            }
        }
        }
        var waterToggle = new SlateButton(left + 14, top + 176, 286, 22,
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
        storeProfile();settings.put("ProfileA",profileA.save());settings.put("ProfileB",profileB.save());
        ControlNetwork.sendConfigure(new ControlNetwork.Configure(snapshot.pos(),profileA.radius(),profileA.strength(),
            profileA.connectedOnly(),profileA.heading().id(),settings));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Vanilla deferred background blur must never cover our panel or its lettering.
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xA0141C2E);
        drawPanel(g);
        Component heading = title.copy().withStyle(style -> style.withFont(MENU_FONT));
        centered(g, heading, left + PANEL_W / 2, top + 12, CREAM);
        if(automation) {
            g.drawString(font,engineering("vertical_range"),left+26,top+141,INK,false);
            right(g,Component.literal("+/- "+vertical),left+290,top+141,TEAL);
        } else {
        drawWaterPreview(g);
        if (snapshot.still()) {
            centered(g, tr(action==FlowProfile.Action.RELEASE?"released_area":"calm_area"), left + 158, top + 54, INK);
            centered(g, tr("diameter_label"), left + 66, top + 82, MUTED);
            centered(g, tr("blocks", radius * 2 + 1), left + 66, top + 98, INK);
            centered(g, tr("height_label"), left + 250, top + 82, MUTED);
            centered(g, Component.literal("+/- " + vertical), left + 250, top + 98, INK);
            centered(g, tr(action==FlowProfile.Action.RELEASE?"release_flow":"zero_flow"), left + 158, top + 129, TEAL);
            g.drawString(font, tr("radius_label"), left + 26, top + 144, INK, false);
            right(g, tr("blocks", radius), left + 290, top + 144, TEAL);
        } else {
            centered(g, tr("heading_label"), left + 89, top + 54, INK);
            centered(g, tr(facing.key()), left + 89, top + 162, MUTED);
            centered(g, tr("radius_label"), left + 236, top + 56, INK);
            centered(g, tr("blocks", radius), left + 236, top + 70, TEAL);
            centered(g, tr("speed_label"), left + 236, top + 106, INK);
            centered(g, Component.literal(String.format(Locale.ROOT, "%d%%", Math.round(speed * 100))),
                left + 236, top + 120, TEAL);
            centered(g, tr("preview_diameter", radius * 2 + 1), left + 236, top + 151, INK);
            centered(g, tr("preview_height", vertical), left + 236, top + 162, MUTED);
        }
        }
        // Keep status within the frame, including when redstone disables the controller.
        boolean paused=snapshot.powered() && settings.getInt("RedstoneMode")==0;
        Component status=settings.contains("LinkedSource")?engineering("linked_source",net.minecraft.core.BlockPos.of(settings.getLong("LinkedSource")).toShortString()):
            settings.getBoolean("Kinetic")?engineering("drive_rpm",Math.round(settings.getFloat("RPM"))):tr(paused?"powered_status":"apply_hint");
        centered(g,status,left+PANEL_W/2,top+222,paused?0xFFFFB093:MUTED);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics g) {
        g.blit(automation ? texture("automation_controller") : snapshot.still() ? STILL_BACKGROUND : CURRENT_BACKGROUND, left, top,
            0f, 0f, PANEL_W, PANEL_H, PANEL_W, PANEL_H);
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
    private void drawWaterPreview(GuiGraphics g) {
        // Odd-sized chart has one exact center shared by the compass and anchor metrics.
        int cx = left + (snapshot.still() ? 158 : 89), cy = top + (snapshot.still() ? 96 : 112);
        int x = cx - 26, y = cy - 26;
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) {
            int dx = col - 4, dz = row - 4;
            if (dx * dx + dz * dz > 20) continue;
            boolean isolated = col >= 7 && row <= 2;
            boolean covered = !connected || !isolated;
            int tx = x + col * 6, ty = y + row * 6;
            g.fill(tx, ty, tx + 5, ty + 5, covered ? 0xFF54768B : 0xFF344759);
            g.fill(tx + 1, ty + 2, tx + 4, ty + 3, covered ? TEAL : 0xFF54768B);
        }
        if (snapshot.still()) {
            // Anchor glyph; the generator uses a heading arrow instead.
            g.fill(cx - 3, cy - 8, cx + 4, cy - 3, CREAM);
            g.fill(cx - 1, cy - 7, cx + 2, cy - 5, EDGE);
            g.fill(cx, cy - 3, cx + 2, cy + 8, CREAM);
            g.fill(cx - 4, cy - 1, cx + 6, cy + 1, CREAM);
            g.fill(cx - 6, cy + 4, cx - 4, cy + 8, CREAM);
            g.fill(cx + 6, cy + 4, cx + 8, cy + 8, CREAM);
            g.fill(cx - 4, cy + 7, cx + 6, cy + 9, CREAM);
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
            if(getWidth()==26) control(g,"compass",getX(),getY(),getWidth(),getHeight(),state);
            else EngineeringWidgets.button(g,getX(),getY(),getWidth(),getHeight(),state);
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
            g.blit(texture(w == 264 ? "track_wide" : "track_small"), x, y + 4, 0f, 0f, w, 8, w, 8);
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
