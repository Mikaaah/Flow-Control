package nl.townsandkingdoms.streamcontrol;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
/** Optional-mod-free registration with stable mechanical_current_generator save IDs. */
public final class DormantMechanicalIntegration {
    public static Supplier<DormantMechanicalBlock> BLOCK;
    public static Supplier<BlockItem> ITEM;
    public static Supplier<BlockEntityType<DormantMechanicalEntity>> ENTITY;
    public static void register() {
        BLOCK=StreamControl.BLOCKS.registerBlock("mechanical_current_generator",DormantMechanicalBlock::new,()->BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3).noOcclusion());
        ITEM=StreamControl.ITEMS.registerItem("mechanical_current_generator",p->new EngineeringBlockItem(BLOCK.get(),p));
        ENTITY=StreamControl.ENTITIES.register("mechanical_current_generator",()->new BlockEntityType<>(DormantMechanicalEntity::new,BLOCK.get()));
        StreamControl.OPTIONAL_ITEMS.add(BLOCK);
    }
}
