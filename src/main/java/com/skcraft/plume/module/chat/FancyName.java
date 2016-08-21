package com.skcraft.plume.module.chat;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import ninja.leaping.configurate.objectmapping.Setting;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Module(name = "fancy-name", desc = "Assigns users a random name color on join")
public class FancyName {

    private final Random random = new Random();
    @InjectConfig("fancy_name") private Config<FancyNameConfig> config;
    private final LoadingCache<String, Optional<EnumChatFormatting>> nameColorCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Optional<EnumChatFormatting>>() {
                @Override
                public Optional<EnumChatFormatting> load(String key) throws Exception {
                    return Optional.fromNullable(nextColor());
                }
            });

    @Nullable
    public EnumChatFormatting nextColor() {
        List<EnumChatFormatting> colors = config.get().colors;
        if (!colors.isEmpty()) {
            return colors.get(random.nextInt(colors.size()));
        } else {
            return null;
        }
    }

    @Nullable
    public EnumChatFormatting nextColor(String name) {
        return nameColorCache.getUnchecked(name.toLowerCase().trim()).orNull();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNameFormat(NameFormat event) {
        EnumChatFormatting color = nextColor();
        if (color != null) {
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
