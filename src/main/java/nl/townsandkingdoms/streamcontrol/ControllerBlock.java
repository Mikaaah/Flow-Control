package nl.townsandkingdoms.streamcontrol;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
public final class ControllerBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty DIAGONAL = BooleanProperty.create("diagonal");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public final boolean still;
    public ControllerBlock(boolean still, Properties p) {
        super(p); this.still = still;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DIAGONAL, false).setValue(WATERLOGGED, false).setValue(POWERED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, WATERLOGGED, POWERED, DIAGONAL); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection()).setValue(WATERLOGGED, c.getLevel().getFluidState(c.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER)).setValue(POWERED, c.getLevel().hasNeighborSignal(c.getClickedPos())); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ControllerEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) { return level.isClientSide() ? null : createTickerHelper(type, StreamControl.CONTROLLER.get(), ControllerEntity::tick); }
    @Override protected FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s); }
    @Override protected BlockState updateShape(BlockState s, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
        if (s.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(s,level,ticks,pos,direction,neighborPos,neighbor,random);
    }
    @Override protected void neighborChanged(BlockState s, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean moving) {
        if (!level.isClientSide()) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != s.getValue(POWERED)) level.setBlock(pos, s.setValue(POWERED, powered), 3);
            if (level.getBlockEntity(pos) instanceof ControllerEntity be) be.waterChanged();
        }
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (!p.mayBuild() || !level.mayInteract(p,pos)) return InteractionResult.FAIL;
        if (!level.isClientSide() && p instanceof net.minecraft.server.level.ServerPlayer player
            && level.getBlockEntity(pos) instanceof ControllerEntity be) ControlNetwork.open(player, be);
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState old, ServerLevel level, BlockPos pos, boolean moving) {
        ControlData.get(level).remove(level, pos.asLong());
        super.affectNeighborsAfterRemoval(old,level,pos,moving);
    }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return FlowHeading.fromState(s).rotate(r).apply(s); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return FlowHeading.fromState(s).mirror(m).apply(s); }
}
