package nl.townsandkingdoms.streamcontrol;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
public interface UtilityAccess {
    BlockEntity blockEntity();
    CompoundTag settings();
    void configure(CompoundTag n);
}
