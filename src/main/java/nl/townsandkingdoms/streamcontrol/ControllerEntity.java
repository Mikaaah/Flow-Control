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
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {super.saveAdditional(out);out.store("Engineering",CompoundTag.CODEC,controls.save());}
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);var n=in.read("Engineering",CompoundTag.CODEC).orElseGet(()->{
            var legacy=new CompoundTag();legacy.putInt("Radius",in.getIntOr("Radius",-1));legacy.putDouble("Strength",in.getDoubleOr("Strength",-1));legacy.putBoolean("ConnectedOnly",in.getBooleanOr("ConnectedOnly",false));return legacy;
        });controls.load(n);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    public static void tick(Level l,BlockPos p,BlockState s,ControllerEntity be) { be.controls.tick(); }
}
