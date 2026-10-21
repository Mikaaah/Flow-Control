package nl.townsandkingdoms.streamcontrol;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
public final class ControlCommands {
    public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("streamcontrol")
            .then(Commands.literal("radius").then(Commands.argument("blocks",IntegerArgumentType.integer(1,32)).executes(c -> {
                var p = c.getSource().getPlayerOrException(); var s = p.getMainHandItem();
                if (!s.is(StreamControl.WAND.get())) return 0;
                var n = FlowWand.settings(s); n.putInt("Radius",ControlConfig.radius(IntegerArgumentType.getInteger(c,"blocks"))); FlowWand.saveSettings(s,n); return 1;
            })))
            .then(Commands.literal("strength").then(Commands.argument("value",DoubleArgumentType.doubleArg(0.05,2)).executes(c -> {
                var p = c.getSource().getPlayerOrException(); var s = p.getMainHandItem(); if (!s.is(StreamControl.WAND.get())) return 0;
                var n = FlowWand.settings(s); n.putDouble("Strength",ControlConfig.strength(DoubleArgumentType.getDouble(c,"value"))); FlowWand.saveSettings(s,n); return 1;
            })))
            .then(Commands.literal("status").executes(c -> {
                var p = net.minecraft.core.BlockPos.containing(c.getSource().getPosition()); var data = ControlData.get(c.getSource().getLevel()); var r = data.at(p.getX(),p.getY(),p.getZ());
                c.getSource().sendSuccess(() -> Component.literal("Flow Control: " + data.all().size() + " regions; here=" + (r == null ? "natural" : r.still() ? "still" : "directed")),false); return 1;
            }))
            .then(Commands.literal("clear").requires(s -> s.hasPermission(2)).executes(c -> {
                var level = c.getSource().getLevel(); var data = ControlData.get(level); int count = 0;
                for (var r : data.all()) if (!r.controller()) { data.remove(level,r.key()); count++; }
                final int removed = count; c.getSource().sendSuccess(() -> Component.literal("Removed " + removed + " wand regions in this dimension."),true); return count;
            })));
    }
}
