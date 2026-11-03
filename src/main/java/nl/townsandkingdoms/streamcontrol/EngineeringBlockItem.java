package nl.townsandkingdoms.streamcontrol;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
public final class EngineeringBlockItem extends BlockItem {
    public EngineeringBlockItem(Block block,Properties p) { super(block,p); }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,display,lines,flag);
        lines.accept(Component.translatable(getDescriptionId()+".tip").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.accept(Component.translatable("engineering.streamcontrol.configure_tip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
