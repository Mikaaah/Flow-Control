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
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK,ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM,ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<ControllerBlock> GENERATOR = BLOCKS.register("current_generator", () -> new ControllerBlock(false, BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).strength(3)));
    public static final RegistryObject<ControllerBlock> ANCHOR = BLOCKS.register("stillwater_anchor", () -> new ControllerBlock(true, BlockBehaviour.Properties.copy(Blocks.PRISMARINE).strength(3)));
    public static final RegistryObject<UtilityBlock> SENSOR = BLOCKS.register("flow_sensor", () -> new UtilityBlock(true,BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).strength(3)));
    public static final RegistryObject<UtilityBlock> MOORING = BLOCKS.register("mooring_post", () -> new UtilityBlock(false,BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(3).noOcclusion()));
    public static final RegistryObject<BlockItem> SENSOR_ITEM = ITEMS.register("flow_sensor",()->new EngineeringBlockItem(SENSOR.get(),new Item.Properties()));
    public static final RegistryObject<BlockItem> MOORING_ITEM = ITEMS.register("mooring_post",()->new EngineeringBlockItem(MOORING.get(),new Item.Properties()));
    public static final java.util.function.Supplier<BlockEntityType<FlowSensorEntity>> SENSOR_ENTITY = ENTITIES.register("flow_sensor", () -> BlockEntityType.Builder.of(FlowSensorEntity::new,SENSOR.get()).build(null));
    public static final java.util.function.Supplier<BlockEntityType<MooringEntity>> MOORING_ENTITY = ENTITIES.register("mooring_post", () -> BlockEntityType.Builder.of(MooringEntity::new,MOORING.get()).build(null));
    public static final java.util.List<java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike>> OPTIONAL_ITEMS = new java.util.ArrayList<>();
    public static final RegistryObject<FlowWand> WAND = ITEMS.register("flow_wand", () -> new FlowWand(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockItem> GENERATOR_ITEM = ITEMS.register("current_generator",()->new EngineeringBlockItem(GENERATOR.get(),new Item.Properties()));
    public static final RegistryObject<BlockItem> ANCHOR_ITEM = ITEMS.register("stillwater_anchor",()->new EngineeringBlockItem(ANCHOR.get(),new Item.Properties()));
    public static final java.util.function.Supplier<BlockEntityType<ControllerEntity>> CONTROLLER = ENTITIES.register("controller", () -> BlockEntityType.Builder.of(ControllerEntity::new, GENERATOR.get(), ANCHOR.get()).build(null));
    static { TABS.register("flow_control", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(() -> new ItemStack(WAND.get())).displayItems((params, output) -> { output.accept(WAND.get()); output.accept(GENERATOR.get()); output.accept(ANCHOR.get()); output.accept(SENSOR.get()); output.accept(MOORING.get()); OPTIONAL_ITEMS.forEach(item -> output.accept(item.get())); }).build()); }
    public StreamControl() {
        IEventBus bus=net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        if (net.minecraftforge.fml.ModList.get().isLoaded("create")) nl.townsandkingdoms.streamcontrol.compat.create.CreateIntegration.register();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT)
            nl.townsandkingdoms.streamcontrol.client.EngineeringClient.register(bus);
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
