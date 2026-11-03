package nl.townsandkingdoms.streamcontrol;

import net.minecraft.nbt.CompoundTag;

/** A bounded, portable controller preset. No world coordinates or permissions are copied. */
public record FlowProfile(int radius, int vertical, double strength, FlowHeading heading,
                          boolean connectedOnly, Action action) {
    public enum Action { CURRENT, REVERSE, CALM, RELEASE;
        public static Action from(int id) { return values()[Math.clamp(id, 0, values().length-1)]; }
    }
    public FlowProfile {
        radius = Math.clamp(radius,1,32);
        vertical = Math.clamp(vertical,0,16);
        strength = Double.isFinite(strength) ? Math.clamp(strength,0.05,2.0) : 1.0;
        if (heading == null) heading = FlowHeading.NORTH;
        if (action == null) action = Action.CURRENT;
    }
    public static FlowProfile defaults(boolean still) {
        return new FlowProfile(4,4,1.0, FlowHeading.NORTH, false, still ? Action.CALM : Action.CURRENT);
    }
    public FlowProfile forAnchor(boolean anchor) {
        return anchor && action != Action.RELEASE ? new FlowProfile(radius,vertical,strength,heading,connectedOnly,Action.CALM) : this;
    }
    public FlowProfile withHeading(FlowHeading h) { return new FlowProfile(radius,vertical,strength,h,connectedOnly,action); }
    public CompoundTag save() {
        var n = new CompoundTag(); n.putInt("Radius",radius); n.putInt("Vertical",vertical);
        n.putDouble("Strength",strength); n.putInt("Heading",heading.id());
        n.putBoolean("ConnectedOnly",connectedOnly); n.putInt("Action",action.ordinal()); return n;
    }
    public static FlowProfile load(CompoundTag n, boolean still) {
        var d = defaults(still);
        return new FlowProfile(n.contains("Radius") ? n.getInt("Radius") : d.radius,
            n.contains("Vertical") ? n.getInt("Vertical") : d.vertical,
            n.contains("Strength") ? n.getDouble("Strength") : d.strength,
            n.contains("Heading") ? FlowHeading.fromId(n.getInt("Heading")) : d.heading,
            n.getBoolean("ConnectedOnly"), n.contains("Action") ? Action.from(n.getInt("Action")) : d.action).forAnchor(still);
    }
}
