package nl.townsandkingdoms.streamcontrol.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FluidState;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
// Both registered stream-fluid implementations in the verified 2.17.3 JAR.
@Mixin(targets = {
    "streamsreflowing.proprietary.AllRightsReservedNiceJohn_006$ProprietaryNiceJohn_001",
    "streamsreflowing.proprietary.AllRightsReservedNiceJohn_006$SeeAiNoticeConstant_001"
}, remap = false, priority = 500)
public abstract class BakedFlowMixin {
    @ModifyReturnValue(method = "getFlow", at = @At("RETURN"), remap = false)
    private Vec3 streamcontrol$baked(Vec3 original, BlockGetter world, BlockPos pos, FluidState state) {
        return FlowOverrides.apply(original,world,pos,state);
    }
}
