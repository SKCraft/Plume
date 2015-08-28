package com.skcraft.plume.module;

import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import ninja.leaping.configurate.objectmapping.Setting;

import java.util.List;
import java.util.Random;

@Module(name = "fancy-name", desc = "Assigns users a random name color on join")
public class FancyName {

    private final Random random = new Random();
    @InjectConfig("fancy_name") private Config<FancyNameConfig> config;

    @SubscribeEvent
    public void onNameFormat(NameFormat event) {
        List<EnumChatFormatting> colors = config.get().colors;
        if (colors.size() > 0) {
            EnumChatFormatting color = colors.get(random.nextInt(colors.size()));
            event.displayname = color + event.username;
        }
    }

    private static class FancyNameConfig {
        @Setting(value = "colors", comment = "List of colors that names can use")
        private List<EnumChatFormatting> colors = Lists.newArrayList(
                EnumChatFormatting.DARK_GREEN,
                EnumChatFormatting.DARK_AQUA,
                EnumChatFormatting.DARK_PURPLE,
                EnumChatFormatting.GOLD,
                EnumChatFormatting.GRAY,
                EnumChatFormatting.BLUE,
                EnumChatFormatting.GREEN,
                EnumChatFormatting.AQUA,
                EnumChatFormatting.RED,
                EnumChatFormatting.LIGHT_PURPLE,
                EnumChatFormatting.YELLOW);
    }

}
