package nl.townsandkingdoms.streamcontrol.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import nl.townsandkingdoms.streamcontrol.*;
import static nl.townsandkingdoms.streamcontrol.ControllerBlock.*;

/** A real kinetic consumer with a bottom shaft, not an adjacent-block RPM probe. */
public final class MechanicalCurrentBlock extends KineticBlock implements IBE<MechanicalCurrentEntity>, SimpleWaterloggedBlock {
    public MechanicalCurrentBlock(Properties p) { super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(DIAGONAL,false).setValue(POWERED,false).setValue(WATERLOGGED,false)); }
    @Override public void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,DIAGONAL,POWERED,WATERLOGGED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING,c.getHorizontalDirection()).setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER)).setValue(POWERED,c.getLevel().hasNeighborSignal(c.getClickedPos())); }
    @Override public Class<MechanicalCurrentEntity> getBlockEntityClass() { return MechanicalCurrentEntity.class; }
    @Override public BlockEntityType<? extends MechanicalCurrentEntity> getBlockEntityType() { return CreateIntegration.ENTITY.get(); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction face) { return face==Direction.DOWN; }
    @Override public FluidState getFluidState(BlockState s) { return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s); }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q) {
        if(s.getValue(WATERLOGGED)) l.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return super.updateShape(s,d,n,l,p,q);
    }
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos q,boolean moving) {
        if(!l.isClientSide) {
            boolean on=l.hasNeighborSignal(p);if(on!=s.getValue(POWERED)) l.setBlock(p,s.setValue(POWERED,on),3);
            if(l.getBlockEntity(p) instanceof FlowController controller) controller.waterChanged();
        }
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if(player.getItemInHand(hand).is(StreamControl.WAND.get())) return InteractionResult.PASS;
        if(!player.mayBuild() || !l.mayInteract(player,p)) return InteractionResult.FAIL;
        if(player instanceof ServerPlayer server && l.getBlockEntity(p) instanceof FlowController controller) ControlNetwork.open(server,controller);
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving) {
        if(!old.is(next.getBlock()) && l instanceof ServerLevel server) ControlData.get(server).remove(server,p.asLong());
        super.onRemove(old,l,p,next,moving);
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder context) { return java.util.List.of(new net.minecraft.world.item.ItemStack(this)); }
    @Override public BlockState rotate(BlockState s,Rotation r) { return FlowHeading.fromState(s).rotate(r).apply(s); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return FlowHeading.fromState(s).mirror(m).apply(s); }
}
