package nl.townsandkingdoms.streamcontrol;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class ControllerEntity extends BlockEntity {
    private int radius = -1;
    private double strength = -1;
    private boolean initialized;
    public ControllerEntity(BlockPos p, BlockState s) { super(StreamControl.CONTROLLER.get(), p, s); }
    public int radius() { return ControlConfig.radius(radius < 1 ? ControlConfig.DEFAULT_RADIUS.get() : radius); }
    public void setRadius(int r) { radius = ControlConfig.radius(r); setChanged(); refresh(); }
    public void configure(Direction d, int r, double speed) {
        radius = r; strength = speed;
        if (level != null) level.setBlock(worldPosition, getBlockState().setValue(ControllerBlock.FACING,d),3);
        setChanged(); refresh();
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) { super.saveAdditional(tag); tag.putInt("Radius",radius); tag.putDouble("Strength",strength); }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) { super.loadAdditional(tag); radius = tag.getIntOr("Radius", -1); strength = tag.getDoubleOr("Strength", -1); }
    public static void tick(Level l, BlockPos p, BlockState s, ControllerEntity be) {
        if (!be.initialized || l.getGameTime() % 20 == 0) { be.initialized = true; be.refresh(); }
    }
    public void refresh() {
        if (!(level instanceof ServerLevel server)) return;
        var data = ControlData.get(server); long key = worldPosition.asLong();
        if (getBlockState().getValue(ControllerBlock.POWERED) || !ControlConfig.ENABLED.get()) { data.remove(server,key); return; }
        Direction d = getBlockState().getValue(ControllerBlock.FACING);
        boolean still = ((ControllerBlock)getBlockState().getBlock()).still;
        double sp = ControlConfig.strength(strength < 0 ? ControlConfig.SPEED.get() : strength);
        var old = data.byKey(key);
        if (old != null && old.radius() == radius() && old.vertical() == ControlConfig.VERTICAL.get()
            && old.dx() == d.getStepX() && old.dz() == d.getStepZ() && old.strength() == sp && old.still() == still) return;
        data.put(server, new FlowRegion(key, worldPosition.immutable(), radius(), ControlConfig.VERTICAL.get(), d.getStepX(), d.getStepZ(), sp, still, true, new UUID(0,0),data.next()));
    }
}
