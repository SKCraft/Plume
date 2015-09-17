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
import com.skcraft.plume.util.OptionalPlayer;
import com.skcraft.plume.util.Server;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public class ForgeModule extends AbstractModule {

    private final NamespaceProvider<ICommandSender> senderProvider = new NamespaceProvider<>(ICommandSender.class);

    @Override
    protected void configure() {
        bind(ICommandSender.class).annotatedWith(Sender.class).toProvider(senderProvider);
        bind(EntityPlayer.class).annotatedWith(Sender.class).toProvider(new PlayerSenderProvider<>());
        bind(EntityPlayerMP.class).annotatedWith(Sender.class).toProvider(new PlayerSenderProvider<>());
        bind(OptionalPlayer.class).toProvider(new OptionalPlayerProvider());
        bind(EntityPlayer.class).toProvider(new PlayerProvider<>());
        bind(EntityPlayerMP.class).toProvider(new PlayerProvider<>());
        bind(Key.<Set<EntityPlayer>>get(new TypeToken<Set<EntityPlayer>>(){}.getType())).toProvider(new PlayerSetProvider<>());
        bind(Key.<Set<EntityPlayerMP>>get(new TypeToken<Set<EntityPlayerMP>>(){}.getType())).toProvider(new PlayerSetProvider<>());
        bind(Key.<Set<OptionalPlayer>>get(new TypeToken<Set<OptionalPlayer>>(){}.getType())).toProvider(new OptionalPlayerSetProvider());
    }

    public static EntityPlayerMP findBestPlayer(String test) throws ArgumentParseException {
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

    private static OptionalPlayer findOptionalPlayer(String test) throws ArgumentParseException {
        if (test.startsWith("$")) {
            test = test.substring(1);
            EntityPlayerMP player = Server.findPlayer(test);
            if (player != null) {
                return new OptionalPlayer(test, player);
            } else {
                return new OptionalPlayer(test, null);
            }
        } else {
            return new OptionalPlayer(test, findBestPlayer(test));
        }
    }

    private class OptionalPlayerProvider implements Provider<OptionalPlayer> {
        @Override
        public boolean isProvided() {
            return false;
        }

        @Nullable
        @Override
        public OptionalPlayer get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            return findOptionalPlayer(arguments.next());
        }

        @Override
        public List<String> getSuggestions(String prefix) {
            return ImmutableList.of();
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
            return (T) findBestPlayer(arguments.next());
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

    private abstract class SetAdapter<T> implements Provider<Set<T>> {
        protected abstract Collection<? extends T> getAll() throws ArgumentParseException;

        protected abstract T find(String token) throws ArgumentParseException;

        @Nullable
        @Override
        public Set<T> get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            Set<T> entries = Sets.newHashSet();
            String input;

            do {
                input = arguments.next();
                String[] tokens = input.split(",");
                for (String token : tokens) {
                    token = token.trim();
                    if (token.equals("*")) {
                        entries.addAll(getAll());
                    } else {
                        entries.add(find(token));
                    }
                }
            } while (!input.isEmpty() && input.charAt(input.length() - 1) == ',');

            return entries;
        }

        @Override
        public List<String> getSuggestions(String prefix) {
            return ImmutableList.of();
        }
    }

    private class OptionalPlayerSetProvider extends SetAdapter<OptionalPlayer> {
        @Override
        protected Collection<? extends OptionalPlayer> getAll() throws ArgumentParseException {
            return Server.getOnlinePlayers().stream().map(player -> new OptionalPlayer(player.getGameProfile().getName(), player)).collect(Collectors.toList());
        }

        @Override
        protected OptionalPlayer find(String token) throws ArgumentParseException {
            return findOptionalPlayer(token);
        }

        @Override
        public boolean isProvided() {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private class PlayerSetProvider<T extends EntityPlayer> extends SetAdapter<T> {
        @Override
        protected Collection<? extends T> getAll() {
            return (Collection<? extends T>) Server.getOnlinePlayers();
        }

        @Override
        protected T find(String token) throws ArgumentParseException {
            return (T) findBestPlayer(token);
        }

        @Override
        public boolean isProvided() {
            return false;
        }
    }

}
