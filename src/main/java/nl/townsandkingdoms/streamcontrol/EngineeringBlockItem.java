package nl.townsandkingdoms.streamcontrol;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
public final class EngineeringBlockItem extends BlockItem {
    public EngineeringBlockItem(Block block,Properties p) { super(block,p); }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable(getDescriptionId()+".tip").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("engineering.streamcontrol.configure_tip").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
