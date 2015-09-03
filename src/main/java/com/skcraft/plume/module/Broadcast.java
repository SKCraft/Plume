package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;


@Module(name = "broadcast", desc = "Allows server wide message broadcasting for operators.")

public class Broadcast {

    @Command(aliases = "bc", usage = "/bc [type] [message]", desc = "Information or Alert broadcasts.")
    @Require("plume.broadcast.bc")

    public void broadCast(@Sender ICommandSender sender, String bctype, String msg) throws CommandException {
        if (bctype.equals("alert")) {
            Messages.broadcastAlert(msg);

        } else if (bctype.equals("info")) {
            Messages.broadcastInfo(msg);

        } else throw new CommandException("You specified an invalid broadcast type.");
    }
}