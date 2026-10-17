package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;

@Mod(StreamControl.ID)
public final class StreamControl {
    public static final String ID = "streamcontrol";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredBlock<ControllerBlock> GENERATOR = BLOCKS.register("current_generator", () -> new ControllerBlock(false, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3)));
    public static final DeferredBlock<ControllerBlock> ANCHOR = BLOCKS.register("stillwater_anchor", () -> new ControllerBlock(true, BlockBehaviour.Properties.ofFullCopy(Blocks.PRISMARINE).strength(3)));
    public static final DeferredItem<FlowWand> WAND = ITEMS.register("flow_wand", () -> new FlowWand(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(GENERATOR);
    public static final DeferredItem<BlockItem> ANCHOR_ITEM = ITEMS.registerSimpleBlockItem(ANCHOR);
    public static final java.util.function.Supplier<BlockEntityType<ControllerEntity>> CONTROLLER = ENTITIES.register("controller", () -> BlockEntityType.Builder.of(ControllerEntity::new, GENERATOR.get(), ANCHOR.get()).build(null));
    static { TABS.register("flow_control", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(() -> new ItemStack(WAND.get())).displayItems((params, output) -> { output.accept(WAND.get()); output.accept(GENERATOR.get()); output.accept(ANCHOR.get()); }).build()); }
    public StreamControl(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, ControlConfig.SPEC);
        bus.addListener(ControlNetwork::register);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::login);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::dimension);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::respawn);
        NeoForge.EVENT_BUS.addListener(ControlCommands::register);
        IntegrationChecks.register();
    }
}
