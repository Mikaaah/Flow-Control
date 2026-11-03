package nl.townsandkingdoms.streamcontrol;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** The old flow_wand registry ID now represents a Calibrator. Legacy regions remain removable. */
public final class FlowWand extends Item {
    public enum Mode { INSPECT, COPY, PASTE, LINK, RESTORE;
        public String key() { return "calibrator.streamcontrol."+name().toLowerCase(Locale.ROOT); }
    }
    public FlowWand(Properties p) { super(p.stacksTo(1)); }
    public static CompoundTag settings(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); }
    public static Mode mode(ItemStack s) { return Mode.values()[Math.floorMod(settings(s).getIntOr("CalibratorMode",0),Mode.values().length)]; }
    private static void store(ItemStack s,CompoundTag n) { s.set(DataComponents.CUSTOM_DATA,CustomData.of(n)); }
    private static void message(Player p,String key,Object... args) { if(p instanceof ServerPlayer sp)sp.sendSystemMessage(Component.translatable("calibrator.streamcontrol."+key,args),true); }
    public static boolean allowed(Player p,BlockPos pos,ItemStack stack) {
        return ControlConfig.ENABLED.get() && ControlConfig.WAND_ENABLED.get() && (!ControlConfig.OP_ONLY.get() || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            && p.mayBuild() && p.level().mayInteract(p,pos) && p.mayUseItemAt(pos,Direction.UP,stack);
    }
    @Override public InteractionResult useOn(UseOnContext c) {
        var player=c.getPlayer();if(player==null) return InteractionResult.PASS;
        var be=c.getLevel().getBlockEntity(c.getClickedPos());
        if(!(be instanceof FlowController) && !(be instanceof UtilityAccess)) return InteractionResult.PASS;
        if(c.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer server) || !allowed(player,c.getClickedPos(),c.getItemInHand())) return InteractionResult.FAIL;
        operate(server,c.getClickedPos(),c.getItemInHand());return InteractionResult.CONSUME;
    }
    public static boolean operate(ServerPlayer player,BlockPos pos,ItemStack stack) {
        var level=player.level();
        if(!level.hasChunkAt(pos) || !allowed(player,pos,stack)) return false;
        var be=level.getBlockEntity(pos);var n=settings(stack);Mode mode=mode(stack);
        if(mode==Mode.INSPECT) {
            if(be instanceof FlowController controller) ControlNetwork.open(player,controller);
            else if(be instanceof UtilityAccess utility) ControlNetwork.openUtility(player,utility);
            else inspect(player,pos);
            return true;
        }
        if(mode==Mode.COPY) {
            CompoundTag clipboard;
            if(be instanceof FlowController controller) { clipboard=controller.controls().clipboard();clipboard.putString("Kind","controller"); }
            else if(be instanceof UtilityAccess utility) clipboard=utility.settings();
            else { message(player,"controller_required");return false; }
            n.put("Calibration",clipboard);store(stack,n);message(player,"copied");return true;
        }
        if(mode==Mode.PASTE) {
            var clipboard=n.getCompoundOrEmpty("Calibration");String kind=clipboard.getStringOr("Kind","");
            if("controller".equals(kind) && be instanceof FlowController controller) controller.controls().paste(clipboard);
            else if(be instanceof UtilityAccess utility && utility.settings().getStringOr("Kind","").equals(kind)) utility.configure(clipboard);
            else { message(player,"incompatible");return false; }
            message(player,"pasted");return true;
        }
        if(mode==Mode.LINK) {
            if(!(be instanceof FlowController controller)) { message(player,"controller_required");return false; }
            if(player.isShiftKeyDown()) { controller.controls().unlink();n.remove("LinkPos");n.remove("LinkDimension");store(stack,n);message(player,"unlinked");return true; }
            if(!n.contains("LinkPos")) {
                n.putLong("LinkPos",pos.asLong());n.putString("LinkDimension",level.dimension().identifier().toString());store(stack,n);message(player,"source_selected");return true;
            }
            BlockPos source=BlockPos.of(n.getLongOr("LinkPos",0L));
            if(!level.dimension().identifier().toString().equals(n.getStringOr("LinkDimension","")) || !level.hasChunkAt(source)
                || !allowed(player,source,stack) || !controller.controls().link(source)) { message(player,"invalid_link");return false; }
            message(player,"linked");return true;
        }
        var data=ControlData.get(level);var old=data.at(pos.getX(),pos.getY(),pos.getZ());
        if(old==null || old.controller()) { message(player,"no_legacy_edit");return false; }
        if(!old.owner().equals(player.getUUID()) && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) { message(player,"denied");return false; }
        data.remove(level,old.key());message(player,"restored");return true;
    }
    private static void inspect(ServerPlayer player,BlockPos pos) {
        var level=player.level();var flow=level.getFluidState(pos).getFlow(level,pos);
        FlowHeading heading=FlowHeading.fromVector(flow.x,flow.z);var region=ControlData.get(level).at(pos.getX(),pos.getY(),pos.getZ());
        String direction=heading==null?"Still":heading.key();String source=region==null?"Natural":region.controller()?"Controller":"Legacy edit";
        player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,"%s | %s | %.0f%%",source,direction,Math.hypot(flow.x,flow.z)*100)),true);
    }
    @Override public InteractionResult use(Level world,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);if(world.isClientSide()) return InteractionResult.SUCCESS;
        var hit=getPlayerPOVHitResult(world,player,ClipContext.Fluid.ANY);
        if(hit.getType()==HitResult.Type.MISS) {
            var n=settings(stack);
            if(player.isShiftKeyDown()) {
                int next=(mode(stack).ordinal()+1)%Mode.values().length;n.putInt("CalibratorMode",next);
                n.remove("LinkPos");n.remove("LinkDimension");message(player,"mode",Component.translatable(Mode.values()[next].key()));
            } else { n.putBoolean("HideBounds",!n.getBooleanOr("HideBounds",false));message(player,n.getBooleanOr("HideBounds",false)?"bounds_off":"bounds_on"); }
            store(stack,n);return InteractionResult.SUCCESS;
        }
        if(!(player instanceof ServerPlayer server) || !allowed(player,hit.getBlockPos(),stack)) return InteractionResult.FAIL;
        if(!world.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) return InteractionResult.PASS;
        return operate(server,hit.getBlockPos(),stack)?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag) {
        lines.accept(Component.translatable("calibrator.streamcontrol.mode",Component.translatable(mode(stack).key())));
        lines.accept(Component.translatable("calibrator.streamcontrol.tooltip"));
        lines.accept(Component.translatable("calibrator.streamcontrol.tooltip_cycle"));
        lines.accept(Component.translatable("calibrator.streamcontrol.tooltip_bounds"));
        if(settings(stack).contains("Calibration")) lines.accept(Component.translatable("calibrator.streamcontrol.clipboard_ready"));
        if(settings(stack).contains("LinkPos")) lines.accept(Component.translatable("calibrator.streamcontrol.source_ready"));
    }
}
