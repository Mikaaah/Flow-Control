package nl.townsandkingdoms.streamcontrol;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
public final class EngineeringBlockItem extends BlockItem {
    public EngineeringBlockItem(Block block,Properties p) { super(block,p); }
    /** Description lines consumed by the loader's client tooltip event. */
    public void appendTooltip(java.util.function.Consumer<Component> lines) {
        lines.accept(Component.translatable(getDescriptionId()+".tip").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.accept(Component.translatable("engineering.streamcontrol.configure_tip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
