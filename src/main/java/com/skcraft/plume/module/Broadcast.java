package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Text;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

@Module(name = "broadcast", desc = "Server wide message broadcasting for operators")
public class Broadcast {

    @Command(aliases = {"broadcast", "bc"}, desc = "Send out a broadcast message")
    @Require("plume.broadcast.broadcast")
    public void broadcast(@Sender ICommandSender sender, @Text String msg) {
        Messages.broadcast(Messages.info("SERVER: " + msg));
    }

    @Command(aliases = {"alert"}, desc = "Send out a broadcast message")
    @Require("plume.broadcast.broadcast")
    public void alert(@Sender ICommandSender sender, @Text String msg) {
        Messages.broadcast(Messages.error("SERVER: " + msg));
    }

    @Command(aliases = {"print"}, desc = "Print an unformatted message to chat")
    @Require("plume.broadcast.print")
    public void print(@Sender ICommandSender sender, @Text String msg) {
        Messages.broadcast(new ChatComponentText(msg));
    }

}
