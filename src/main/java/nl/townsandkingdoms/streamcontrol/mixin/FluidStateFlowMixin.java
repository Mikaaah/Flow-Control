package nl.townsandkingdoms.streamcontrol.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Finalize the flow vector seen by vanilla/NeoForge fluid rendering.
 * Streams or another fluid implementation may modify getFlow after our
 * lower-level hooks; the FluidState result is the common read boundary.
 */
@Mixin(FluidState.class)
public abstract class FluidStateFlowMixin {
    @Inject(method = "getFlow", at = @At("RETURN"), cancellable = true)
    private void streamcontrol$finalFlow(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(FlowOverrides.apply(cir.getReturnValue(), world, pos, (FluidState) (Object) this));
    }
}
