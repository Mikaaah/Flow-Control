package nl.townsandkingdoms.streamcontrol;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
/** Same block-entity ID and FlowControl settings payload as the optional kinetic edition. */
public final class DormantMechanicalEntity extends ControllerEntity {
    public DormantMechanicalEntity(BlockPos p,BlockState s) { super(DormantMechanicalIntegration.ENTITY.get(),p,s); }
    @Override public float rpm() { return 0; }
    @Override protected void saveAdditional(CompoundTag n) { super.saveAdditional(n);n.put("FlowControl",controls().save()); }
    @Override public void load(CompoundTag n) { super.load(n);if(n.contains("FlowControl")) controls().load(n.getCompound("FlowControl")); }
}
