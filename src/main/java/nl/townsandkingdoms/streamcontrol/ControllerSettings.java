package nl.townsandkingdoms.streamcontrol;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Persistent profiles and shared execution for normal and optional kinetic controllers. */
public final class ControllerSettings {
    public enum Redstone { PAUSE, IGNORE, ANALOG, REVERSE, SWITCH;
        public static Redstone from(int id) { return values()[Math.clamp(id,0,values().length-1)]; }
    }
    private final FlowController host;
    private FlowProfile a, b;
    private Redstone redstone = Redstone.PAUSE;
    private BlockPos source;
    private boolean dirty = true;
    private boolean initialized;
    private long lastMaskScanTick = Long.MIN_VALUE, nextMaskScanTick;
    private boolean visualActive;
    private double visualStrength;
    private FlowHeading visualHeading = FlowHeading.NORTH;
    public boolean visualActive() { return visualActive; }
    public double visualStrength() { return visualStrength; }
    public FlowHeading visualHeading() { return visualHeading; }
    private void visual(boolean active, FlowHeading heading, double strength) {
        if (visualActive == active && visualHeading == heading && Math.abs(visualStrength-strength) < 1e-6) return;
        visualActive=active; visualHeading=heading; visualStrength=strength;
        var be=host.blockEntity();
        if(be.getLevel() instanceof ServerLevel level && !be.isRemoved())
            level.sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),2);
    }
    /** Controller profiles live in the block entity; unloaded blocks must not leave active flow claims. */
    public void removed() {
        var be=host.blockEntity();
        if(be.getLevel() instanceof ServerLevel level) ControlData.get(level).remove(level,be.getBlockPos().asLong());
        visualActive=false; visualStrength=0; initialized=false; dirty=true;
    }
    private void pause(ServerLevel level, long key) {
        ControlData.get(level).remove(level,key);
        visual(false,host.heading(),0);
    }
    public ControllerSettings(FlowController host) { this.host = host; }
    public FlowProfile a() { if (a == null) a = new FlowProfile(ControlConfig.DEFAULT_RADIUS.get(),ControlConfig.VERTICAL.get(),ControlConfig.SPEED.get(),host.heading(),false,host.isStill()?FlowProfile.Action.CALM:FlowProfile.Action.CURRENT); return a; }
    public FlowProfile b() {
        if (b == null) b = new FlowProfile(a().radius(),a().vertical(),a().strength(),a().heading(),a().connectedOnly(),
            host.isStill() ? FlowProfile.Action.RELEASE : FlowProfile.Action.REVERSE);
        return b;
    }
    public Redstone redstone() { return redstone; }
    public BlockPos source() { return source; }
    private static FlowProfile bounded(FlowProfile p) {
        return new FlowProfile(ControlConfig.radius(p.radius()),Math.clamp(p.vertical(),0,ControlConfig.MAX_VERTICAL.get()),
            ControlConfig.strength(p.strength()),p.heading(),p.connectedOnly(),p.action());
    }
    private void changed() {
        var be=host.blockEntity();be.setChanged();
        if(be.getLevel() instanceof ServerLevel l) l.sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),2);
    }
    public void invalidate() { dirty = true; }
    public void setA(FlowProfile profile) {
        a = bounded(profile.forAnchor(host.isStill()));
        var be = host.blockEntity();
        if (be.getLevel() != null) be.getLevel().setBlock(be.getBlockPos(),a.heading().apply(be.getBlockState()),3);
        dirty = true; changed(); refresh();
    }
    public CompoundTag save() {
        a=a().withHeading(host.heading());
        var n = new CompoundTag(); n.put("ProfileA",a().save()); n.put("ProfileB",b().save());
        n.putInt("RedstoneMode",redstone.ordinal()); if (source != null) n.putLong("LinkedSource",source.asLong());
        // Older world data keys remain readable for migration and diagnostics.
        n.putInt("Radius",a().radius()); n.putDouble("Strength",a().strength()); n.putBoolean("ConnectedOnly",a().connectedOnly());
        n.putBoolean("VisualActive",visualActive);n.putDouble("VisualStrength",visualStrength);n.putInt("VisualHeading",visualHeading.id());
        return n;
    }
    public void load(CompoundTag n) {
        a = n.contains("ProfileA") ? FlowProfile.load(n.getCompoundOrEmpty("ProfileA"),host.isStill()) :
            new FlowProfile(n.contains("Radius") && n.getIntOr("Radius",0) > 0 ? n.getIntOr("Radius",0) : ControlConfig.DEFAULT_RADIUS.get(),
                ControlConfig.VERTICAL.get(), n.contains("Strength") && n.getDoubleOr("Strength",0.0) >= 0 ? n.getDoubleOr("Strength",0.0) : ControlConfig.SPEED.get(),
                host.heading(), n.getBooleanOr("ConnectedOnly",false),host.isStill() ? FlowProfile.Action.CALM : FlowProfile.Action.CURRENT);
        b = n.contains("ProfileB") ? FlowProfile.load(n.getCompoundOrEmpty("ProfileB"),host.isStill()) : null;
        redstone = Redstone.from(n.getIntOr("RedstoneMode",0));
        source = n.contains("LinkedSource") ? BlockPos.of(n.getLongOr("LinkedSource",0L)) : null; dirty = true;
        visualActive=n.getBooleanOr("VisualActive",false);visualStrength=n.getDoubleOr("VisualStrength",0.0);
        visualHeading=FlowHeading.fromId(n.getIntOr("VisualHeading",0));
        if(visualHeading==null) visualHeading=host.heading();
        if(!Double.isFinite(visualStrength) || visualStrength<0) visualStrength=0;
    }
    /** Client edits may change profiles and clear a link, never forge a link to an arbitrary position. */
    public void configure(CompoundTag n) {
        a = FlowProfile.load(n.getCompoundOrEmpty("ProfileA"),host.isStill());
        b = bounded(FlowProfile.load(n.getCompoundOrEmpty("ProfileB"),host.isStill()));
        redstone = Redstone.from(n.getIntOr("RedstoneMode",0));
        if (!n.contains("LinkedSource")) source = null;
        setA(a);
    }
    public CompoundTag clipboard() {
        ControllerSettings effective=host.blockEntity().getLevel() instanceof ServerLevel level ? resolved(level) : this;
        var n=(effective==null?this:effective).save();n.remove("LinkedSource");return n;
    }
    public void paste(CompoundTag n) { source = null; configure(n); }
    public void unlink() { source = null; dirty = true; changed(); refresh(); }
    public boolean link(BlockPos pos) {
        var be = host.blockEntity();
        if (!(be.getLevel() instanceof ServerLevel level) || pos.equals(be.getBlockPos())
            || pos.distSqr(be.getBlockPos()) > (long)ControlConfig.LINK_RANGE.get()*ControlConfig.LINK_RANGE.get()) return false;
        Set<BlockPos> seen = new HashSet<>(); seen.add(be.getBlockPos()); BlockPos current = pos;
        for (int i=0;i<16;i++) {
            if (!seen.add(current) || !level.hasChunkAt(current)
                || !(level.getBlockEntity(current) instanceof FlowController controller)) return false;
            if (controller.controls().source == null) {
                source = pos.immutable(); dirty = true; changed(); refresh(); return true;
            }
            current = controller.controls().source;
        }
        return false;
    }
    private ControllerSettings resolved(ServerLevel level) {
        ControllerSettings settings = this; Set<BlockPos> seen = new HashSet<>(); seen.add(host.blockEntity().getBlockPos());
        for (int i=0;settings.source!=null && i<16;i++) {
            var pos = settings.source;
            if (!seen.add(pos) || !level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof FlowController next)) return null;
            settings = next.controls();
        }
        return settings.source == null ? settings : null;
    }
    public void tick() {
        var level = host.blockEntity().getLevel();
        if (level != null && (!initialized || Math.floorMod(level.getGameTime()+host.blockEntity().getBlockPos().asLong(),10) == 0)) { initialized = true; refresh(); }
    }
    public void refresh() {
        BlockEntity be = host.blockEntity(); if (!(be.getLevel() instanceof ServerLevel level) || be.isRemoved()) return;
        var data = ControlData.get(level); var pos = be.getBlockPos(); long key = pos.asLong();
        ControllerSettings settings = resolved(level);
        if (!ControlConfig.ENABLED.get() || settings == null) { pause(level,key); return; }
        int signal = level.getBestNeighborSignal(pos);
        // Read the block property too: preserves explicit powered states and structure/test placements.
        boolean powered = signal > 0 || be.getBlockState().getValue(ControllerBlock.POWERED);
        Redstone mode = settings.redstone;
        if (mode == Redstone.PAUSE && powered) { pause(level,key); return; }
        FlowProfile p = mode == Redstone.SWITCH && powered ? settings.b() : settings.a();
        p = bounded(p.forAnchor(host.isStill()));
        if (p.action() == FlowProfile.Action.RELEASE) { pause(level,key); return; }
        FlowHeading heading = mode != Redstone.SWITCH || !powered ? settings.host.heading() : p.heading();
        boolean reverse = p.action() == FlowProfile.Action.REVERSE;
        boolean still = host.isStill() || p.action() == FlowProfile.Action.CALM;
        double speed = p.strength();
        if (mode == Redstone.REVERSE && powered) reverse = !reverse;
        if (mode == Redstone.ANALOG) {
            if (host.isStill() && signal == 0) { pause(level,key); return; }
            speed *= signal/15.0;
            if (signal == 0) still = true;
        }
        float rpm = host.rpm();
        if (!Float.isNaN(rpm)) {
            if (!Float.isFinite(rpm) || Math.abs(rpm) < 0.001f) { pause(level,key); return; }
            speed *= Math.abs(rpm)/ControlConfig.RPM_FULL_STRENGTH.get();
            if (rpm < 0) reverse = !reverse;
        }
        speed = Double.isFinite(speed) ? Math.clamp(speed,0,ControlConfig.MAX_SPEED.get()) : 0;
        if (reverse) heading = heading.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_180);
        var old = data.byKey(key); long[] mask = new long[0];
        if (p.connectedOnly()) {
            long now=level.getGameTime();
            boolean geometryChanged = old == null || !old.connectedOnly() || old.radius()!=p.radius() || old.vertical()!=p.vertical()
                || old.dx()!=heading.dx() || old.dz()!=heading.dz();
            boolean rebuild = geometryChanged || lastMaskScanTick!=now && (dirty || now>=nextMaskScanTick);
            if(rebuild) {
                mask=ConnectedWater.scan(level,pos,heading.cardinal(),p.radius(),p.vertical());
                lastMaskScanTick=now;nextMaskScanTick=now+100;
            } else mask=old.connectedMask();
            // Keep a second same-tick water change pending instead of rescanning the volume repeatedly.
            dirty=dirty && !rebuild;
        }
        if(!p.connectedOnly()) dirty=false;
        visual(true,heading,still?0:speed);
        if (old != null && old.radius()==p.radius() && old.vertical()==p.vertical() && old.dx()==heading.dx()
            && old.dz()==heading.dz() && old.strength()==speed && old.still()==still
            && old.connectedOnly()==p.connectedOnly() && Arrays.equals(old.connectedMask(),mask)) return;
        data.put(level,new FlowRegion(key,pos.immutable(),p.radius(),p.vertical(),heading.dx(),heading.dz(),speed,still,true,
            new UUID(0,0),data.next(),p.connectedOnly(),mask));
    }
}
