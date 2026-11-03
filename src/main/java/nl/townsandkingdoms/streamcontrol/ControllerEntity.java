package nl.townsandkingdoms.streamcontrol;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class ControllerEntity extends BlockEntity implements FlowController {
    private final ControllerSettings controls = new ControllerSettings(this);
    public ControllerEntity(BlockPos p, BlockState s) { super(StreamControl.CONTROLLER.get(),p,s); }
    @Override public BlockEntity blockEntity() { return this; }
    @Override public ControllerSettings controls() { return controls; }
    @Override protected void saveAdditional(CompoundTag n) { super.saveAdditional(n); n.merge(controls.save()); }
    @Override public void load(CompoundTag n) { super.load(n); controls.load(n); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    public static void tick(Level l,BlockPos p,BlockState s,ControllerEntity be) { be.controls.tick(); }
}
