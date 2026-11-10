package nl.townsandkingdoms.streamcontrol;
import java.util.function.Supplier;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
public final class StreamControl implements ModInitializer {
 public static final String ID="streamcontrol";
 public static final java.util.List<java.util.function.Supplier<? extends net.minecraft.world.level.ItemLike>> OPTIONAL_ITEMS=new java.util.ArrayList<>();
 private static Identifier id(String n){return Identifier.fromNamespaceAndPath(ID,n);}
 private static ControllerBlock block(String n,boolean still,BlockBehaviour.Properties p){return Registry.register(BuiltInRegistries.BLOCK,id(n),new ControllerBlock(still,p.setId(ResourceKey.create(Registries.BLOCK,id(n)))));}
 private static <T extends Item>T item(String n,java.util.function.Function<Item.Properties,T> factory,Item.Properties p){return Registry.register(BuiltInRegistries.ITEM,id(n),factory.apply(p.setId(ResourceKey.create(Registries.ITEM,id(n)))));}
 private static final ControllerBlock GENERATOR_BLOCK=block("current_generator",false,BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).strength(3));
 private static final ControllerBlock ANCHOR_BLOCK=block("stillwater_anchor",true,BlockBehaviour.Properties.ofFullCopy(Blocks.PRISMARINE).strength(3));
 public static final Supplier<ControllerBlock> GENERATOR=()->GENERATOR_BLOCK,ANCHOR=()->ANCHOR_BLOCK;
 private static final UtilityBlock SENSOR_BLOCK=Registry.register(BuiltInRegistries.BLOCK,id("flow_sensor"),new UtilityBlock(true,BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK.weathering().unaffected()).strength(3).setId(ResourceKey.create(Registries.BLOCK,id("flow_sensor")))));
 private static final UtilityBlock MOORING_BLOCK=Registry.register(BuiltInRegistries.BLOCK,id("mooring_post"),new UtilityBlock(false,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(3).noOcclusion().setId(ResourceKey.create(Registries.BLOCK,id("mooring_post")))));
 public static final Supplier<UtilityBlock> SENSOR=()->SENSOR_BLOCK,MOORING=()->MOORING_BLOCK;
 public static final Supplier<BlockItem> SENSOR_ITEM=()->(BlockItem)SENSOR_BLOCK.asItem(),MOORING_ITEM=()->(BlockItem)MOORING_BLOCK.asItem();
 private static final BlockEntityType<FlowSensorEntity> SENSOR_TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("flow_sensor"),FabricBlockEntityTypeBuilder.create(FlowSensorEntity::new,SENSOR_BLOCK).build());
 private static final BlockEntityType<MooringEntity> MOORING_TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("mooring_post"),FabricBlockEntityTypeBuilder.create(MooringEntity::new,MOORING_BLOCK).build());
 public static final Supplier<BlockEntityType<FlowSensorEntity>> SENSOR_ENTITY=()->SENSOR_TYPE;
 public static final Supplier<BlockEntityType<MooringEntity>> MOORING_ENTITY=()->MOORING_TYPE;
 private static final FlowWand WAND_ITEM=item("flow_wand",FlowWand::new,new Item.Properties().stacksTo(1));
 public static final Supplier<FlowWand> WAND=()->WAND_ITEM;
 public static final Supplier<BlockItem> GENERATOR_ITEM=()->(BlockItem)GENERATOR_BLOCK.asItem(),ANCHOR_ITEM=()->(BlockItem)ANCHOR_BLOCK.asItem();
 private static final BlockEntityType<ControllerEntity> CONTROLLER_TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("controller"),FabricBlockEntityTypeBuilder.create(ControllerEntity::new,GENERATOR_BLOCK,ANCHOR_BLOCK).build());
 public static final Supplier<BlockEntityType<ControllerEntity>> CONTROLLER=()->CONTROLLER_TYPE;
 @Override public void onInitialize(){
  item("current_generator",p->new EngineeringBlockItem(GENERATOR_BLOCK,p),new Item.Properties());item("stillwater_anchor",p->new EngineeringBlockItem(ANCHOR_BLOCK,p),new Item.Properties());
  item("flow_sensor",p->new EngineeringBlockItem(SENSOR_BLOCK,p),new Item.Properties());item("mooring_post",p->new EngineeringBlockItem(MOORING_BLOCK,p),new Item.Properties());
  Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("flow_control"),FabricCreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(()->new ItemStack(WAND_ITEM)).displayItems((p,o)->{o.accept(WAND_ITEM);o.accept(GENERATOR_BLOCK);o.accept(ANCHOR_BLOCK);o.accept(SENSOR_BLOCK);o.accept(MOORING_BLOCK);}).build());
  ControlNetwork.register();ServerLifecycleEvents.SERVER_STARTING.register(ControlConfig::load);
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->ControlNetwork.sync(handler.player));
  ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player,origin,destination)->ControlNetwork.sync(player));
  ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->ControlNetwork.sync(newPlayer));
  CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->ControlCommands.register(dispatcher));
  IntegrationChecks.register();
 }
}
