package nl.townsandkingdoms.streamcontrol.compat.create;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import nl.townsandkingdoms.streamcontrol.*;

public final class MechanicalCurrentEntity extends KineticBlockEntity implements FlowController {
    private final ControllerSettings controls=new ControllerSettings(this);
    public MechanicalCurrentEntity(BlockPos p,BlockState s) { super(CreateIntegration.ENTITY.get(),p,s); }
    @Override public BlockEntity blockEntity() { return this; }
    @Override public ControllerSettings controls() { return controls; }
    @Override public float rpm() { return getSpeed(); }
    @Override public void tick() { super.tick();if(level!=null && !level.isClientSide) controls.tick(); }
    @Override public void onSpeedChanged(float previous) { super.onSpeedChanged(previous);if(controls!=null) controls.refresh(); }
    @Override public float calculateStressApplied() { return lastStressApplied=ControlConfig.KINETIC_STRESS.get().floatValue(); }
    @Override protected void write(CompoundTag n,boolean packet) { super.write(n,packet);n.put("FlowControl",controls.save()); }
    @Override protected void read(CompoundTag n,boolean packet) { super.read(n,packet);if(n.contains("FlowControl")) controls.load(n.getCompound("FlowControl")); }
}
