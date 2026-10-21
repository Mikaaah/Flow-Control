package nl.townsandkingdoms.streamcontrol.mixin;
import dev.streamsreflowing.forge.fluid.StreamFlowingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=StreamFlowingFluid.class,remap=false,priority=500)
public abstract class BakedFlowMixin {
 @Inject(method="resolve",at=@At("RETURN"),cancellable=true,remap=false)
 private static void streamcontrol$baked(Vec3 natural,BlockGetter world,BlockPos pos,CallbackInfoReturnable<Vec3> cir){cir.setReturnValue(FlowOverrides.apply(cir.getReturnValue(),world,pos,world.getFluidState(pos)));}
}
