package nl.townsandkingdoms.streamcontrol;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
/** Same block-entity ID and FlowControl settings payload as the optional kinetic edition. */
public final class DormantMechanicalEntity extends ControllerEntity {
    public DormantMechanicalEntity(BlockPos p,BlockState s) { super(DormantMechanicalIntegration.ENTITY.get(),p,s); }
    @Override public float rpm() { return 0; }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);out.store("FlowControl",CompoundTag.CODEC,controls().save());
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);in.read("FlowControl",CompoundTag.CODEC).ifPresent(controls()::load);
    }
}
