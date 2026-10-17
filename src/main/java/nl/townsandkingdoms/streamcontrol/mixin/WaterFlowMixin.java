package nl.townsandkingdoms.streamcontrol.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = FlowingFluid.class, priority = 500)
public abstract class WaterFlowMixin {
    @ModifyReturnValue(method = "getFlow", at = @At("RETURN"))
    private Vec3 streamcontrol$flow(Vec3 original, BlockGetter world, BlockPos pos, FluidState state) {
        return FlowOverrides.apply(original,world,pos,state);
    }
}
