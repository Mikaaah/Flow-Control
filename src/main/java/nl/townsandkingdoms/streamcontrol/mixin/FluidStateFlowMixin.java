package nl.townsandkingdoms.streamcontrol.mixin;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FluidState.class)
public abstract class FluidStateFlowMixin {
 @Inject(method="getFlow",at=@At("RETURN"),cancellable=true)
 private void flow(BlockGetter level,BlockPos pos,CallbackInfoReturnable<Vec3> c){c.setReturnValue(FlowOverrides.apply(c.getReturnValue(),level,pos,(FluidState)(Object)this));}
}
