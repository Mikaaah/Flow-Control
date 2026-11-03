package nl.townsandkingdoms.streamcontrol;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
public final class ControlCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("streamcontrol")
            .then(Commands.literal("status").executes(c->{
                var p=net.minecraft.core.BlockPos.containing(c.getSource().getPosition());var data=ControlData.get(c.getSource().getLevel());var r=data.at(p.getX(),p.getY(),p.getZ());
                c.getSource().sendSuccess(()->Component.literal("Flow Control: "+data.all().size()+" regions; here="+(r==null?"natural":r.still()?"still":"directed")),false);return 1;
            }))
            .then(Commands.literal("clear").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)).executes(c->{
                var level=c.getSource().getLevel();var data=ControlData.get(level);int count=0;
                for(var r:data.all()) if(!r.controller()) { data.remove(level,r.key());count++; }
                final int removed=count;c.getSource().sendSuccess(()->Component.literal("Removed "+removed+" legacy wand regions in this dimension."),true);return count;
            })));
    }
}
