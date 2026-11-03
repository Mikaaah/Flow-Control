package nl.townsandkingdoms.streamcontrol.compat.create;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import nl.townsandkingdoms.streamcontrol.*;
public final class CreateIntegration {
 public static Supplier<MechanicalCurrentBlock> BLOCK;public static Supplier<BlockItem> ITEM;public static Supplier<BlockEntityType<MechanicalCurrentEntity>> ENTITY;
 public static void register(){
  var id=new ResourceLocation(StreamControl.ID,"mechanical_current_generator");
  var block=Registry.register(BuiltInRegistries.BLOCK,id,new MechanicalCurrentBlock(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).strength(3).noOcclusion()));BLOCK=()->block;
  var item=Registry.register(BuiltInRegistries.ITEM,id,new EngineeringBlockItem(block,new Item.Properties()));ITEM=()->item;
  var entity=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id,FabricBlockEntityTypeBuilder.create(MechanicalCurrentEntity::new,block).build());ENTITY=()->entity;
  StreamControl.OPTIONAL_ITEMS.add(BLOCK);
 }
}
