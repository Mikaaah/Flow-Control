package nl.townsandkingdoms.streamcontrol.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.neoforge.fluid.StreamFlowingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = StreamFlowingFluid.class, remap = false, priority = 500)
public abstract class BakedFlowMixin {
    @ModifyReturnValue(method = "resolve", at = @At("RETURN"), remap = false)
    private static Vec3 streamcontrol$baked(Vec3 original, Vec3 natural, BlockGetter world, BlockPos pos) {
        return FlowOverrides.apply(original,world,pos,world.getFluidState(pos));
    }
}
