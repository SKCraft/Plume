package com.skcraft.plume.command;

import com.sk89q.intake.CommandMapping;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;

abstract class CommandAdapter extends CommandBase {
    private CommandMapping command;

    protected CommandAdapter(CommandMapping command) {
        this.command = command;
    }

    @Override
    public String getCommandName() {
        return command.getPrimaryAlias();
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList(command.getAllAliases());
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/" + command.getPrimaryAlias() + " " + command.getDescription().getUsage();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
