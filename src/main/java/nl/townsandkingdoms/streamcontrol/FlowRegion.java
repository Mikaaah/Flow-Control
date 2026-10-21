package nl.townsandkingdoms.streamcontrol;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Immutable cylinder. Block regions have priority, then stillness, then newest sequence. */
public record FlowRegion(long key, BlockPos center, int radius, int vertical, double dx, double dz,
                         double strength, boolean still, boolean controller, UUID owner, long sequence) {
    public boolean contains(int x, int y, int z) {
        long ax = (long)x - center.getX(), az = (long)z - center.getZ();
        return ax * ax + az * az <= (long)radius * radius && Math.abs((long)y - center.getY()) <= vertical;
    }
    public Vec3 vector() { return still ? Vec3.ZERO : new Vec3(dx * strength, 0, dz * strength); }
    public int priority() { return (controller ? 2 : 0) + (still ? 1 : 0); }
    public CompoundTag save() {
        var n = new CompoundTag(); n.putLong("Key", key); n.putLong("Pos", center.asLong());
        n.putInt("Radius", radius); n.putInt("Vertical", vertical); n.putDouble("DX", dx); n.putDouble("DZ", dz);
        n.putDouble("Strength", strength); n.putBoolean("Still", still); n.putBoolean("Controller", controller);
        n.putString("Owner", owner.toString()); n.putLong("Sequence", sequence); return n;
    }
    private static UUID owner(CompoundTag n) { try { return UUID.fromString(n.getStringOr("Owner", "")); } catch (IllegalArgumentException e) { return new UUID(0,0); } }
    public static FlowRegion load(CompoundTag n) {
        double dx = n.getDoubleOr("DX", 0), dz = n.getDoubleOr("DZ", 0), length = Math.hypot(dx, dz);
        if (!Double.isFinite(length) || length < 1e-6) { dx = 1; dz = 0; length = 1; }
        double speed = n.getDoubleOr("Strength", 0);
        return new FlowRegion(n.getLongOr("Key", 0L), BlockPos.of(n.getLongOr("Pos", 0L)), Math.clamp(n.getIntOr("Radius", 0), 1, 32),
            Math.clamp(n.getIntOr("Vertical", 0), 0, 16), dx / length, dz / length,
            Double.isFinite(speed) ? Math.clamp(speed, 0.05, 2) : 1, n.getBooleanOr("Still", false), n.getBooleanOr("Controller", false),
            owner(n), n.getLongOr("Sequence", 0L));
    }
}
