package nl.townsandkingdoms.streamcontrol;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/** Placeable dormant assembly: no Create references and no source of free kinetic power. */
public final class DormantMechanicalBlock extends ControllerBlock {
    public DormantMechanicalBlock(Properties p) { super(false,p); }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return new DormantMechanicalEntity(p,s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,DormantMechanicalIntegration.ENTITY.get(),(l,p,s,be)->be.controls().tick());
    }
}
