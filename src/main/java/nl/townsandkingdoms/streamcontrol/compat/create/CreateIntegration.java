package nl.townsandkingdoms.streamcontrol.compat.create;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
import nl.townsandkingdoms.streamcontrol.StreamControl;

/** Loaded only after ModList confirms Create is installed. */
public final class CreateIntegration {
    public static DeferredBlock<MechanicalCurrentBlock> BLOCK;
    public static DeferredItem<BlockItem> ITEM;
    public static Supplier<BlockEntityType<MechanicalCurrentEntity>> ENTITY;
    public static void register() {
        BLOCK=StreamControl.BLOCKS.register("mechanical_current_generator",()->new MechanicalCurrentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3).noOcclusion()));
        ITEM=StreamControl.ITEMS.register("mechanical_current_generator",()->new nl.townsandkingdoms.streamcontrol.EngineeringBlockItem(BLOCK.get(),new net.minecraft.world.item.Item.Properties()));
        ENTITY=StreamControl.ENTITIES.register("mechanical_current_generator",()->BlockEntityType.Builder.of(MechanicalCurrentEntity::new,BLOCK.get()).build(null));
        StreamControl.OPTIONAL_ITEMS.add(BLOCK);
    }
}
