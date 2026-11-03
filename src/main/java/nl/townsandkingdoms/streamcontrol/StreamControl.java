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
    public static final DeferredBlock<UtilityBlock> SENSOR = BLOCKS.register("flow_sensor", () -> new UtilityBlock(true,BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3)));
    public static final DeferredBlock<UtilityBlock> MOORING = BLOCKS.register("mooring_post", () -> new UtilityBlock(false,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(3).noOcclusion()));
    public static final DeferredItem<BlockItem> SENSOR_ITEM = ITEMS.register("flow_sensor",()->new EngineeringBlockItem(SENSOR.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> MOORING_ITEM = ITEMS.register("mooring_post",()->new EngineeringBlockItem(MOORING.get(),new Item.Properties()));
    public static final java.util.function.Supplier<BlockEntityType<FlowSensorEntity>> SENSOR_ENTITY = ENTITIES.register("flow_sensor", () -> BlockEntityType.Builder.of(FlowSensorEntity::new,SENSOR.get()).build(null));
    public static final java.util.function.Supplier<BlockEntityType<MooringEntity>> MOORING_ENTITY = ENTITIES.register("mooring_post", () -> BlockEntityType.Builder.of(MooringEntity::new,MOORING.get()).build(null));
    public static final java.util.List<java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike>> OPTIONAL_ITEMS = new java.util.ArrayList<>();
    public static final DeferredItem<FlowWand> WAND = ITEMS.register("flow_wand", () -> new FlowWand(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> GENERATOR_ITEM = ITEMS.register("current_generator",()->new EngineeringBlockItem(GENERATOR.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> ANCHOR_ITEM = ITEMS.register("stillwater_anchor",()->new EngineeringBlockItem(ANCHOR.get(),new Item.Properties()));
    public static final java.util.function.Supplier<BlockEntityType<ControllerEntity>> CONTROLLER = ENTITIES.register("controller", () -> BlockEntityType.Builder.of(ControllerEntity::new, GENERATOR.get(), ANCHOR.get()).build(null));
    static { TABS.register("flow_control", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(() -> new ItemStack(WAND.get())).displayItems((params, output) -> { output.accept(WAND.get()); output.accept(GENERATOR.get()); output.accept(ANCHOR.get()); output.accept(SENSOR.get()); output.accept(MOORING.get()); OPTIONAL_ITEMS.forEach(item -> output.accept(item.get())); }).build()); }
    public StreamControl(IEventBus bus, ModContainer container) {
        if (net.neoforged.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateIntegration.register();
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT)
            nl.townsandkingdoms.streamcontrol.client.EngineeringClient.register(bus);
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        container.registerConfig(ModConfig.Type.SERVER, ControlConfig.SPEC);
        bus.addListener(ControlNetwork::register);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::login);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::dimension);
        NeoForge.EVENT_BUS.addListener(ControlNetwork::respawn);
        NeoForge.EVENT_BUS.addListener(ControlCommands::register);
        IntegrationChecks.register();
        ClientTestWorld.register();
    }
}
