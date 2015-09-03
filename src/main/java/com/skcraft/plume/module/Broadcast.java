package com.skcraft.plume.module;

import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Switch;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.command.ICommandSender;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "broadcast", desc = "Server wide message broadcasting for operators")
public class Broadcast {

    @Command(aliases = {"broadcast", "bc"}, desc = "Send out a broadcast message")
    @Require("plume.broadcast.bc")
    public void broadcast(@Sender ICommandSender sender, String msg, @Switch('t') String type) {
        if (type == null || type.equalsIgnoreCase("type")) {
            Messages.broadcastInfo(msg);
        } else if (type.equalsIgnoreCase("alert")) {
            Messages.broadcastAlert(msg);
        } else {
            sender.addChatMessage(Messages.error(tr("broadcast.invalidType")));
        }
    }

}
