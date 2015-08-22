package com.skcraft.plume.util.concurrent;

import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.skcraft.plume.util.Messages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.concurrent.*;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Singleton
@Log
public class BackgroundExecutor {

    private static final long MESSAGE_DELAY = 500;

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Plume Background Executor #%d").setDaemon(true).build();
    @Getter
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory));
    @Inject
    private TickExecutorService tickExecutorService;
    private final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);

    public void notifyOnDelay(ListenableFuture<?> future, ICommandSender sender) {
        ChatComponentText message = new ChatComponentText(tr("task.pleaseWaitProcessing"));
        message.getChatStyle().setColor(EnumChatFormatting.GRAY);
        Future<?> messageFuture = timer.schedule(new MessageTask(sender, message), MESSAGE_DELAY, TimeUnit.MILLISECONDS);
        future.addListener(() -> messageFuture.cancel(false), MoreExecutors.sameThreadExecutor());
    }

    public void addCallbacks(ListenableFuture<?> future, ICommandSender sender) {
        notifyOnDelay(future, sender);

        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
            }

            @Override
            public void onFailure(Throwable t) {
                sender.addChatMessage(Messages.exception(t));
            }
        });
    }

    @RequiredArgsConstructor
    private class MessageTask implements Runnable {
        private final ICommandSender sender;
        private final ChatComponentText message;

        @Override
        public void run() {
            tickExecutorService.execute(() -> sender.addChatMessage(message));
        }
    }

}
