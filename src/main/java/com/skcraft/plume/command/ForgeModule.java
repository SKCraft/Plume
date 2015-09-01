package com.skcraft.plume.command;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.AbstractModule;
import com.sk89q.intake.parametric.Key;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import com.skcraft.plume.util.Server;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
        bind(Key.<Set<EntityPlayer>>get(new TypeToken<Set<EntityPlayer>>(){}.getType())).toProvider(new PlayerSetProvider<>());
        bind(Key.<Set<EntityPlayerMP>>get(new TypeToken<Set<EntityPlayerMP>>(){}.getType())).toProvider(new PlayerSetProvider<>());
    }

    private static EntityPlayerMP findPlayer(String test) throws ArgumentParseException {
        test = test.toLowerCase();

        List<EntityPlayerMP> candidates = Lists.newArrayList();

        for (EntityPlayerMP player : Server.getOnlinePlayers()) {
            String name = player.getGameProfile().getName().toLowerCase();
            if (name.equalsIgnoreCase(test)) {
                return player;
            } else if (name.startsWith(test)) {
                candidates.add(player);
            }
        }

        if (candidates.isEmpty()) {
            throw new ArgumentParseException(tr("args.noPlayersMatched", test));
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            Joiner joiner = Joiner.on(tr("listSeparator"));
            throw new ArgumentParseException(tr("args.didYouMean", joiner.join(candidates)));
        }
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
            return (T) findPlayer(arguments.next());
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

    private class PlayerSetProvider<T extends EntityPlayer> implements Provider<Set<T>> {
        @Override
        public boolean isProvided() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public Set<T> get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            Set<T> players = Sets.newHashSet();
            String input;

            do {
                input = arguments.next();
                String[] tokens = input.split(",");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.equals("*")) {
                        players.addAll((Collection<? extends T>) Server.getOnlinePlayers());
                    } else {
                        players.add((T) findPlayer(token));
                    }
                }
            } while (!input.isEmpty() && input.charAt(input.length() - 1) == ',');

            return players;
        }

        @Override
        public List<String> getSuggestions(String prefix) {
            return ImmutableList.of();
        }
    }

}
