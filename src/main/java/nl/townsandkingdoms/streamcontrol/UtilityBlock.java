package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class UtilityBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED, POWERED = BlockStateProperties.POWERED;
    public final boolean sensor;
    public UtilityBlock(boolean sensor,Properties p) {
        super(p);this.sensor=sensor;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(WATERLOGGED,false).setValue(POWERED,false));
    }
    @Override public void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,WATERLOGGED,POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(FluidTags.WATER)).setValue(POWERED,c.getLevel().hasNeighborSignal(c.getClickedPos())); }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        if(sensor) return Block.box(0,0,0,16,14,16);
        return switch(s.getValue(FACING)) {
            case EAST -> Block.box(4,0,4,13,18,12);
            case SOUTH -> Block.box(4,0,4,12,18,13);
            case WEST -> Block.box(3,0,4,12,18,12);
            default -> Block.box(4,0,3,12,18,12);
        };
    }
    @Override public BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return rotate(s,m.getRotation(s.getValue(FACING))); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s) { return sensor ? new FlowSensorEntity(p,s) : new MooringEntity(p,s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t) {
        if (l.isClientSide) return null;
        return sensor ? createTickerHelper(t,StreamControl.SENSOR_ENTITY.get(),FlowSensorEntity::tick)
            : createTickerHelper(t,StreamControl.MOORING_ENTITY.get(),MooringEntity::tick);
    }
    @Override public FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s); }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q) {
        if(s.getValue(WATERLOGGED)) l.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,d,other,l,p,q);
    }
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos q,boolean moving) {
        if(!l.isClientSide) {
            boolean on=l.hasNeighborSignal(p);
            if(on!=s.getValue(POWERED)) l.setBlock(p,s.setValue(POWERED,on),3);
            if(l.getBlockEntity(p) instanceof MooringEntity mooring && on) mooring.release();
        }
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if(player.getItemInHand(hand).is(StreamControl.WAND.get())) return InteractionResult.PASS;
        if(!player.mayBuild() || !l.mayInteract(player,p)) return InteractionResult.FAIL;
        if(player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof UtilityAccess utility) ControlNetwork.openUtility(server,utility);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState s) { return sensor; }
    @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p) { return l.getBlockEntity(p) instanceof FlowSensorEntity be ? be.output() : 0; }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving) {
        if(!old.is(next.getBlock())) {
            if(l.getBlockEntity(p) instanceof MooringEntity m) m.release();
            if(sensor) l.updateNeighbourForOutputSignal(p,this);
        }
        super.onRemove(old,l,p,next,moving);
    }
}
