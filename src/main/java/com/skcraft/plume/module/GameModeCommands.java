package com.skcraft.plume.module;


import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;


import static com.skcraft.plume.common.util.SharedLocale.tr;


@Module(name = "gamemode", desc = "Aliases for game mode switching.")
public class GameModeCommands {

    @Command(aliases = "cr", desc = "Switches player to Creative Mode")
    @Require("plume.gamemode.switchcr")
    public void switchCreative(@Sender EntityPlayerMP player) {
        player.setGameType(WorldSettings.GameType.CREATIVE);
        player.addChatMessage(new ChatComponentText(tr("gameModeCommands.switchcr")));

    }

    @Command(aliases = "su", desc = "Switches player to Survival Mode")
    @Require("plume.gamemode.switchsu")
    public void switchSurvival(@Sender EntityPlayerMP player) {
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.addChatMessage(new ChatComponentText(tr("gameModeCommands.switchsu")));

    }

    @Command(aliases = "gm", usage = "/gm [gamemode] [player]", desc = "Alias for Mojang gamemode commands.")
    @Require("plume.gamemode.gm")

    public void gameMode(@Sender EntityPlayerMP player, int mode, @Optional EntityPlayerMP target) throws CommandException {

        GameType gameType;
        try {
            gameType = GameType.values()[mode];
            player.setGameType(GameType.getByID(mode));
            player.addChatMessage(new ChatComponentText("You have switched to " + GameType.getByID(mode) + " mode."));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new CommandException("You specified an invalid gamemode.");
        }
    }
}
