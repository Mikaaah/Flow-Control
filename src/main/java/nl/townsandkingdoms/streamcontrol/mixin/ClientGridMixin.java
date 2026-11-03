package nl.townsandkingdoms.streamcontrol.mixin;
import nl.townsandkingdoms.streamcontrol.FlowOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets = "dev.streamsreflowing.flow.ProprietaryNiceJohn_001", remap = false)
public abstract class ClientGridMixin {
    @Inject(method = "seeAiNotice1(III)B", at = @At("HEAD"), cancellable = true, remap = false)
    private static void streamcontrol$angle(int x,int y,int z,CallbackInfoReturnable<Byte> cir) {
        var r = FlowOverrides.clientAt(x,y,z); if (r != null) cir.setReturnValue(FlowOverrides.visualAngle(r));
    }
    @Inject(method = "proprietary1(III)D", at = @At("HEAD"), cancellable = true, remap = false)
    private static void streamcontrol$speed(int x,int y,int z,CallbackInfoReturnable<Double> cir) {
        var r = FlowOverrides.clientAt(x,y,z); if (r != null) cir.setReturnValue(r.still() ? 0 : r.strength());
    }
}
