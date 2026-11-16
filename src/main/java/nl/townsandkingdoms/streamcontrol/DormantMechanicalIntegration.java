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
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(StreamControl.ID,"mechanical_current_generator");
        var block=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK,id,new DormantMechanicalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3).noOcclusion()));BLOCK=()->block;
        var item=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,id,new EngineeringBlockItem(block,new Item.Properties()));ITEM=()->item;
        var entity=net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,id,net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(DormantMechanicalEntity::new,block).build());ENTITY=()->entity;
        StreamControl.OPTIONAL_ITEMS.add(BLOCK);
    }
}
