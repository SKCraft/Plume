package com.skcraft.plume.module.player;

import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.intake.parametric.annotation.Optional;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "gamemode", desc = "Aliases for game mode switching")
public class GameModes {

    @Command(aliases = "cr", desc = "Switches player to creative mode")
    @Require("plume.gamemode.switchcr")
    public void switchCreative(@Sender EntityPlayerMP player) {
        player.setGameType(WorldSettings.GameType.CREATIVE);
        player.addChatMessage(Messages.info(tr("gameMode.switchCreative")));
    }

    @Command(aliases = "su", desc = "Switches player to survival mode")
    @Require("plume.gamemode.switchsu")
    public void switchSurvival(@Sender EntityPlayerMP player) {
        player.setGameType(WorldSettings.GameType.SURVIVAL);
        player.addChatMessage(Messages.info(tr("gameMode.switchSurvival")));
    }

    @Command(aliases = "gm", desc = "Change gamemode")
    @Require("plume.gamemode.gm")
    public void gameMode(@Sender EntityPlayerMP player, int mode, @Optional EntityPlayerMP target) {
        try {
            GameType gameType = GameType.getByID(mode);
            player.setGameType(gameType);
            player.addChatMessage(Messages.info(tr("gameMode.switchedToMode",gameType)));
        } catch (ArrayIndexOutOfBoundsException e) {
            player.addChatMessage(Messages.error(tr("gameMode.invalidGameMode")));
        }
    }

}
