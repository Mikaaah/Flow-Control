package nl.townsandkingdoms.streamcontrol.mixin;
import net.minecraft.world.entity.Entity;
import nl.townsandkingdoms.streamcontrol.MooringEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Release only after the vehicle accepted its passenger; mounting cancellation keeps the tether. */
@Mixin(Entity.class)
public abstract class MooringMountMixin {
    @Inject(method="addPassenger",at=@At("TAIL"))
    private void streamcontrol$releaseMooredBoat(Entity passenger,CallbackInfo ci) {
        MooringEntity.releaseMounted((Entity)(Object)this);
    }
}
