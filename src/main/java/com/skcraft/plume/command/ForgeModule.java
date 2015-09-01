package com.skcraft.plume.command;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.AbstractModule;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public class ForgeModule extends AbstractModule {

    private final NamespaceProvider<ICommandSender> senderProvider = new NamespaceProvider<>(ICommandSender.class);

    @Override
    protected void configure() {
        bind(ICommandSender.class).annotatedWith(Sender.class).toProvider(senderProvider);
        bind(EntityPlayer.class).annotatedWith(Sender.class).toProvider(new PlayerSenderProvider<>());
        bind(EntityPlayerMP.class).annotatedWith(Sender.class).toProvider(new PlayerSenderProvider<>());
        bind(EntityPlayer.class).toProvider(new PlayerProvider<>());
        bind(EntityPlayerMP.class).toProvider(new PlayerProvider<>());
    }

    private class PlayerProvider<T extends EntityPlayer> implements Provider<T> {
        @Override
        public boolean isProvided() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public T get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            String test = arguments.next();
            List<EntityPlayer> candidates = Lists.newArrayList();

            for (Object object : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
                EntityPlayer player = (EntityPlayer) object;
                String name = player.getGameProfile().getName();
                if (name.equalsIgnoreCase(test)) {
                    return (T) player;
                } else {
                    candidates.add(player);
                }
            }

            if (candidates.isEmpty()) {
                throw new ArgumentParseException(tr("args.noPlayersMatched", test));
            } else if (candidates.size() == 1) {
                return (T) candidates.get(0);
            } else {
                Joiner joiner = Joiner.on(tr("listSeparator"));
                throw new ArgumentParseException(tr("args.didYouMean", joiner.join(candidates)));
            }
        }

        @Override
        public List<String> getSuggestions(String prefix) {
            return ImmutableList.of();
        }
    }


    private class PlayerSenderProvider<T extends EntityPlayer> implements Provider<T> {
        @Override
        public boolean isProvided() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public T get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            ICommandSender sender = senderProvider.get(arguments, modifiers);
            if (sender instanceof EntityPlayer) {
                return (T) sender;
            } else {
                throw new ArgumentParseException("Only players can use this command.");
            }
        }

        @Override
        public List<String> getSuggestions(String prefix) {
            return ImmutableList.of();
        }
    }

}
