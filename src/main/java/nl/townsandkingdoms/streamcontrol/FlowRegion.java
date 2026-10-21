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
        n.putUUID("Owner", owner); n.putLong("Sequence", sequence); return n;
    }
    public static FlowRegion load(CompoundTag n) {
        double dx = n.getDouble("DX"), dz = n.getDouble("DZ"), length = Math.hypot(dx, dz);
        if (!Double.isFinite(length) || length < 1e-6) { dx = 1; dz = 0; length = 1; }
        double speed = n.getDouble("Strength");
        return new FlowRegion(n.getLong("Key"), BlockPos.of(n.getLong("Pos")), Math.max(1, Math.min(32, n.getInt("Radius"))),
            Math.max(0, Math.min(16, n.getInt("Vertical"))), dx / length, dz / length,
            Double.isFinite(speed) ? Math.max(0.05, Math.min(2, speed)) : 1, n.getBoolean("Still"), n.getBoolean("Controller"),
            n.hasUUID("Owner") ? n.getUUID("Owner") : new UUID(0, 0), n.getLong("Sequence"));
    }
}
