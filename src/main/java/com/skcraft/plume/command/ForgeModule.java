package com.skcraft.plume.command;

import com.google.common.collect.ImmutableList;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.AbstractModule;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

public class ForgeModule extends AbstractModule {

    private final NamespaceProvider<ICommandSender> senderProvider = new NamespaceProvider<>(ICommandSender.class);

    @Override
    protected void configure() {
        bind(ICommandSender.class).toProvider(senderProvider);
        bind(EntityPlayer.class).toProvider(new PlayerProvider());
    }

    private class PlayerProvider implements Provider<EntityPlayer> {
        @Override
        public boolean isProvided() {
            return false;
        }

        @Nullable
        @Override
        public EntityPlayer get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
            ICommandSender sender = senderProvider.get(arguments, modifiers);
            if (sender instanceof EntityPlayer) {
                return (EntityPlayer) sender;
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
