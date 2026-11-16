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
        BLOCK=StreamControl.BLOCKS.register("mechanical_current_generator",()->new DormantMechanicalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3).noOcclusion()));
        ITEM=StreamControl.ITEMS.register("mechanical_current_generator",()->new EngineeringBlockItem(BLOCK.get(),new Item.Properties()));
        ENTITY=StreamControl.ENTITIES.register("mechanical_current_generator",()->BlockEntityType.Builder.of(DormantMechanicalEntity::new,BLOCK.get()).build(null));
        StreamControl.OPTIONAL_ITEMS.add(BLOCK);
    }
}
