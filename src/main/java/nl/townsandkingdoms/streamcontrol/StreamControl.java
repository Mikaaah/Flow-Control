package nl.townsandkingdoms.streamcontrol;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.*;

@Mod(StreamControl.ID)
public final class StreamControl {
    public static final String ID = "streamcontrol";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<ControllerBlock> GENERATOR = BLOCKS.register("current_generator", () -> new ControllerBlock(false, BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).strength(3)));
    public static final RegistryObject<ControllerBlock> ANCHOR = BLOCKS.register("stillwater_anchor", () -> new ControllerBlock(true, BlockBehaviour.Properties.copy(Blocks.PRISMARINE).strength(3)));
    public static final RegistryObject<FlowWand> WAND = ITEMS.register("flow_wand", () -> new FlowWand(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> GENERATOR_ITEM = ITEMS.register("current_generator", () -> new BlockItem(GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> ANCHOR_ITEM = ITEMS.register("stillwater_anchor", () -> new BlockItem(ANCHOR.get(), new Item.Properties()));
    public static final java.util.function.Supplier<BlockEntityType<ControllerEntity>> CONTROLLER = ENTITIES.register("controller", () -> BlockEntityType.Builder.of(ControllerEntity::new, GENERATOR.get(), ANCHOR.get()).build(null));
    static { TABS.register("flow_control", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(() -> new ItemStack(WAND.get())).displayItems((params, output) -> { output.accept(WAND.get()); output.accept(GENERATOR.get()); output.accept(ANCHOR.get()); }).build()); }
    public StreamControl() {
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ControlConfig.SPEC);
        ControlNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(ControlNetwork::login);
        MinecraftForge.EVENT_BUS.addListener(ControlNetwork::dimension);
        MinecraftForge.EVENT_BUS.addListener(ControlNetwork::respawn);
        MinecraftForge.EVENT_BUS.addListener(ControlCommands::register);
        IntegrationChecks.register();
    }
}
