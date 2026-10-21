package nl.townsandkingdoms.streamcontrol.mixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=FlowingFluid.class,priority=500)
public abstract class WaterFlowMixin {
 @Inject(method="getFlow",at=@At("RETURN"),cancellable=true)
 private void streamcontrol$flow(BlockGetter world,BlockPos pos,FluidState state,CallbackInfoReturnable<Vec3> cir){cir.setReturnValue(FlowOverrides.apply(cir.getReturnValue(),world,pos,state));}
}
