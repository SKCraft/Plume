package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.Require;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.skcraft.plume.command.At;
import com.skcraft.plume.command.Group;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.event.lifecycle.ReloadEvent;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import net.minecraft.command.ICommandSender;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "plume-commands", desc = "Commands to manage Plume, such as reloading config or data")
public class PlumeCommands {

    @Inject
    private BackgroundExecutor executor;
    @Inject
    private TickExecutorService tickExecutorService;
    @Inject
    private EventBus eventBus;

    @Command(aliases = "reload", desc = "Reload modules' data")
    @Group(@At("plume"))
    @Require("plume.reload")
    public void reload(@Sender ICommandSender sender) {
        Deferred<?> deferred = Deferreds
                .when(() -> {
                    ReloadEvent event = new ReloadEvent();
                    eventBus.post(event);
                    return event;
                }, executor.getExecutor())
                .done(event -> {
                    sender.addChatMessage(Messages.info(tr("plume.reloaded")));
                }, tickExecutorService)
                .fail(e -> {
                    sender.addChatMessage(Messages.exception(e));
                }, tickExecutorService);

        executor.notifyOnDelay(deferred, sender);
    }

}

