package com.skcraft.plume.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sk89q.intake.*;
import com.sk89q.intake.argument.Namespace;
import com.sk89q.intake.dispatcher.SimpleDispatcher;
import com.sk89q.intake.parametric.Injector;
import com.sk89q.intake.parametric.ParametricBuilder;
import com.sk89q.intake.parametric.provider.PrimitivesModule;
import com.sk89q.intake.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.event.lifecycle.PostInitializationEvent;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context.Builder;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Contexts;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import lombok.Getter;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@Module(name = "command-manager", hidden = true)
@Log
public class CommandManager {

    private static final Joiner argJoiner = Joiner.on(" ");

    private final EventBus eventBus;
    private final Environment environment;

    @Getter private final Injector injector;
    @InjectService private Service<Authorizer> authorizer;
    private final SimpleDispatcher dispatcher;
    private final ParametricBuilder builder;

    private boolean registered = false;
    private List<Object> commandObjects = Lists.newArrayList();

    @Inject
    public CommandManager(EventBus eventBus, Environment environment) {
        this.eventBus = eventBus;
        this.environment = environment;

        injector = Intake.createInjector();
        injector.install(new PrimitivesModule());
        injector.install(new ForgeModule());

        builder = new ParametricBuilder(injector);
        builder.setAuthorizer(new AuthorizerAdapter());

        dispatcher = new SimpleDispatcher();
    }

    public void registerCommands(Object object) {
        checkNotNull(object, "object");
        synchronized (this) {
            if (!registered) {
                commandObjects.add(object);
            } else {
                doCommandRegistration(object);
            }
        }
    }

    @Subscribe
    public void onPostInitialization(PostInitializationEvent event) {
        eventBus.post(new CommandRegistrationEvent(dispatcher, builder));
    }

    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        for (CommandMapping command : dispatcher.getCommands()) {
            CommandAdapter adapter = new CommandAdapter(command) {
                @Override
                public void processCommand(ICommandSender sender, String[] args) {
                    Namespace namespace = new Namespace();
                    namespace.put(ICommandSender.class, sender);

                    try {
                        command.getCallable().call(argJoiner.join(args), namespace, Lists.newArrayList());
                    } catch (CommandException e) {
                        ChatComponentText msg = new ChatComponentText(e.getMessage());
                        msg.getChatStyle().setColor(EnumChatFormatting.RED);
                        sender.addChatMessage(msg);
                    } catch (AuthorizationException e) {
                        ChatComponentText msg = new ChatComponentText("You don't have permission to use that command.");
                        msg.getChatStyle().setColor(EnumChatFormatting.RED);
                        sender.addChatMessage(msg);
                    } catch (InvocationCommandException e) {
                        ChatComponentText msg = new ChatComponentText("An error occurred while running your command. The server log will have the error that can be reported.");
                        log.log(Level.WARNING, "Command execution failed", e);
                        msg.getChatStyle().setColor(EnumChatFormatting.RED);
                        sender.addChatMessage(msg);
                    }
                }
            };
            event.registerServerCommand(adapter);
        }
    }

    @Subscribe
    public void onCommandRegistration(CommandRegistrationEvent event) {
        synchronized (this) {
            if (!registered) {
                for (Object object : commandObjects) {
                    doCommandRegistration(object);
                }
                registered = true;
            }
        }
    }

    private void doCommandRegistration(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            Command definition = method.getAnnotation(Command.class);
            Group group = method.getAnnotation(Group.class);
            if (definition != null) {
                CommandCallable callable = builder.build(object, method);

                if (group != null) {
                    for (At at : group.value()) {
                        SimpleDispatcher node = dispatcher;
                        for (String entry : at.value()) {
                            CommandMapping mapping = node.get(entry);
                            if (mapping == null) {
                                SimpleDispatcher child = new SimpleDispatcher();
                                node.registerCommand(child, entry);
                                node = child;
                            } else if (mapping.getCallable() instanceof SimpleDispatcher) {
                                node = (SimpleDispatcher) mapping.getCallable();
                            } else {
                                throw new IllegalStateException("Can't put command at " + Arrays.toString(at.value()) + " because there is an existing command there");
                            }
                        }
                        node.registerCommand(callable, definition.aliases());
                    }
                } else {
                    dispatcher.registerCommand(callable, definition.aliases());
                }
            }
        }

    }

    private class AuthorizerAdapter implements com.sk89q.intake.util.auth.Authorizer {
        @Override
        public boolean testPermission(Namespace namespace, String permission) {
            Authorizer authorizer = CommandManager.this.authorizer.provide();
            ICommandSender sender = checkNotNull(namespace.get(ICommandSender.class), "ICommandSender");
            if (sender instanceof MinecraftServer || sender instanceof RConConsoleSource) {
                return true;
            } else if (sender instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) sender;
                Builder context = new Builder();
                environment.update(context);
                Contexts.update(context, player);
                return authorizer.getSubject(Profiles.fromPlayer(player)).hasPermission(permission, context.build());
            } else {
                return false;
            }
        }
    }

}
