package com.skcraft.plume.module.commune;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.skcraft.plume.common.event.lifecycle.LoadConfigEvent;
import com.skcraft.plume.common.event.lifecycle.ShutdownEvent;
import com.skcraft.plume.common.util.Environment;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.module.chat.FancyName;
import com.skcraft.plume.module.commune.ServerStatus.Status;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import lombok.Getter;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Log
@Module(name = "commune", desc = "Cross-server chat")
public class Commune {

    @Inject private Environment environment;
    @Getter @Inject(optional = true) private FancyName fancyName;
    @Getter @InjectConfig("commune") private Config<CommuneConfig> config;
    private final BlockingQueue<Message> receiveQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> sendQueue = new LinkedBlockingQueue<>();
    private CommuneClient client;

    public String getSource() {
        return "minecraft." + environment.getServerId();
    }

    public void publish(Message message) {
        sendQueue.add(message);
    }

    private void open() {
        if (config.get().enabled) {
            log.info("Initalizing Commune connection...");

            RedisServer server = config.get().server;

            GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
            poolConfig.setJmxEnabled(false);
            poolConfig.setMinIdle(0);

            JedisPool pool = new JedisPool(
                    poolConfig,
                    server.host,
                    server.port,
                    server.connectionTimeout,
                    server.socketTimeout,
                    Strings.emptyToNull(server.password),
                    Protocol.DEFAULT_DATABASE,
                    "plume_commune");

            client = new CommuneClient(pool, receiveQueue, sendQueue, getSource());
        }
    }

    private void close() {
        if (client != null) {
            log.info("Closing Commune connection...");
            client.close();
            client = null;
        }
    }

    @Subscribe
    public void onLoadConfig(LoadConfigEvent event) {
        close();
        open();
    }

    @Subscribe
    public void onShutdown(ShutdownEvent event) {
        close();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        //noinspection MismatchedQueryAndUpdateOfCollection
        List<Message> pending = new ArrayList<>();
        receiveQueue.drainTo(pending);
        for (Message message : pending) {
            message.execute(this);
        }
    }

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        ServerStatus status = new ServerStatus();
        status.setSource(getSource());
        status.setStatus(Status.ONLINE);
        publish(status);
    }

    @Subscribe
    public void onServerStopping(FMLServerStoppingEvent event) {
        ServerStatus status = new ServerStatus();
        status.setSource(getSource());
        status.setStatus(Status.OFFLINE);
        publish(status);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Presence presence = new Presence();
        presence.setSource(getSource());
        presence.setName(event.player.getGameProfile().getName());
        presence.setStatus(Presence.Status.ONLINE);
        publish(presence);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Presence presence = new Presence();
        presence.setSource(getSource());
        presence.setName(event.player.getGameProfile().getName());
        presence.setStatus(Presence.Status.OFFLINE);
        publish(presence);
    }

    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        Chat chat = new Chat();
        chat.setSource(getSource());
        chat.setSender(event.player.getGameProfile().getName());
        chat.setMessage(event.message);
        publish(chat);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entity instanceof EntityPlayer) {
            GameAction action = new GameAction();
            action.setSource(getSource());
            action.setSender(((EntityPlayer) event.entity).getGameProfile().getName());
            action.setMessage("died.");
            publish(action);
        }
    }

    public static class CommuneConfig {
        @Setting(comment = "Whether to connect to the given Redis server")
        public boolean enabled = false;

        @Setting(comment = "Whether to allow execution of commands received")
        public boolean executeCommands = false;

        @Setting(comment = "Server connection information")
        public RedisServer server = new RedisServer();
    }

    @ConfigSerializable
    public static class RedisServer {
        @Setting(comment = "The address of the server")
        public String host = "localhost";

        @Setting(comment = "The port number of the server")
        public int port = Protocol.DEFAULT_PORT;

        @Setting(comment = "The connection timeout in milliseconds")
        public int connectionTimeout = Protocol.DEFAULT_TIMEOUT;

        @Setting(comment = "The socket timeout in milliseconds")
        public int socketTimeout = Protocol.DEFAULT_TIMEOUT;

        @Setting(comment = "The optional password set on the Redis server")
        public String password = "";
    }
}
