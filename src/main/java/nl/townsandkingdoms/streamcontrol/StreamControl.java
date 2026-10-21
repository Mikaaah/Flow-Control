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
 private static Identifier id(String n){return Identifier.fromNamespaceAndPath(ID,n);}
 private static ControllerBlock block(String n,boolean still,BlockBehaviour.Properties p){return Registry.register(BuiltInRegistries.BLOCK,id(n),new ControllerBlock(still,p.setId(ResourceKey.create(Registries.BLOCK,id(n)))));}
 private static <T extends Item>T item(String n,java.util.function.Function<Item.Properties,T> factory,Item.Properties p){return Registry.register(BuiltInRegistries.ITEM,id(n),factory.apply(p.setId(ResourceKey.create(Registries.ITEM,id(n)))));}
 private static final ControllerBlock GENERATOR_BLOCK=block("current_generator",false,BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).strength(3));
 private static final ControllerBlock ANCHOR_BLOCK=block("stillwater_anchor",true,BlockBehaviour.Properties.ofFullCopy(Blocks.PRISMARINE).strength(3));
 public static final Supplier<ControllerBlock> GENERATOR=()->GENERATOR_BLOCK,ANCHOR=()->ANCHOR_BLOCK;
 private static final FlowWand WAND_ITEM=item("flow_wand",FlowWand::new,new Item.Properties().stacksTo(1));
 public static final Supplier<FlowWand> WAND=()->WAND_ITEM;
 public static final Supplier<BlockItem> GENERATOR_ITEM=()->(BlockItem)GENERATOR_BLOCK.asItem(),ANCHOR_ITEM=()->(BlockItem)ANCHOR_BLOCK.asItem();
 private static final BlockEntityType<ControllerEntity> CONTROLLER_TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("controller"),FabricBlockEntityTypeBuilder.create(ControllerEntity::new,GENERATOR_BLOCK,ANCHOR_BLOCK).build());
 public static final Supplier<BlockEntityType<ControllerEntity>> CONTROLLER=()->CONTROLLER_TYPE;
 @Override public void onInitialize(){
  item("current_generator",p->new BlockItem(GENERATOR_BLOCK,p),new Item.Properties());item("stillwater_anchor",p->new BlockItem(ANCHOR_BLOCK,p),new Item.Properties());
  Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("flow_control"),FabricCreativeModeTab.builder().title(Component.translatable("itemGroup.streamcontrol")).icon(()->new ItemStack(WAND_ITEM)).displayItems((p,o)->{o.accept(WAND_ITEM);o.accept(GENERATOR_BLOCK);o.accept(ANCHOR_BLOCK);}).build());
  ControlNetwork.register();ServerLifecycleEvents.SERVER_STARTING.register(ControlConfig::load);
  ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->ControlNetwork.sync(handler.player));
  ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player,origin,destination)->ControlNetwork.sync(player));
  ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->ControlNetwork.sync(newPlayer));
  CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->ControlCommands.register(dispatcher));
  IntegrationChecks.register();
 }
}
