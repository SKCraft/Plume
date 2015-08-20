package com.skcraft.plume.util.concurrent;

import com.google.common.util.concurrent.*;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.concurrent.*;
import java.util.logging.Level;

@Singleton
@Log
public class BackgroundExecutor {

    private static final long MESSAGE_DELAY = 500;

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("Plume Background Executor #%d").setDaemon(true).build();
    @Getter
    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory));
    private final ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);

    public void addCallbacks(ListenableFuture<?> future, ICommandSender sender) {
        ChatComponentText message = new ChatComponentText("Please wait... processing your command.");
        message.getChatStyle().setColor(EnumChatFormatting.RED);
        Future<?> messageFuture = timer.schedule(new MessageTask(sender, message), MESSAGE_DELAY, TimeUnit.MILLISECONDS);
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                messageFuture.cancel(false);
            }

            @Override
            public void onFailure(Throwable t) {
                messageFuture.cancel(false);
                log.log(Level.SEVERE, "Error occurred during background processing", t);
                ChatComponentText message = new ChatComponentText("An error occurred while processing your command.");
                message.getChatStyle().setColor(EnumChatFormatting.RED);
                sender.addChatMessage(message);
            }
        });
    }

    @RequiredArgsConstructor
    private static class MessageTask implements Runnable {
        private final ICommandSender sender;
        private final ChatComponentText message;

        @Override
        public void run() {
            sender.addChatMessage(message);
        }
    }

}
