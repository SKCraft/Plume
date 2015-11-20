package com.skcraft.plume.module.commune;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
class CommuneClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JedisPool pool;
    private final BlockingQueue<Message> receiveQueue;
    private final BlockingQueue<Message> sendQueue;
    private final String source;
    private boolean running = true;

    CommuneClient(JedisPool pool, BlockingQueue<Message> receiveQueue, BlockingQueue<Message> sendQueue, String source) {
        this.pool = pool;
        this.receiveQueue = receiveQueue;
        this.sendQueue = sendQueue;
        this.source = source;

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Thread thread = new Thread(new ReceiveRunnable(), "Commune Message Receiver");
        thread.setDaemon(true);
        thread.start();

        thread = new Thread(new SendRunnable(), "Commune Message Sender");
        thread.setDaemon(true);
        thread.start();
    }

    public void close() {
        running = false;
        pool.close();
    }

    private class SendRunnable implements Runnable {
        @Override
        public void run() {
            while (running) {
                try {
                    Message message = sendQueue.poll(1, TimeUnit.SECONDS);
                    if (message != null) {
                        try (Jedis jedis = pool.getResource()) {
                            jedis.publish("commune", mapper.writeValueAsString(message));
                        } catch (JsonProcessingException e) {
                            log.log(Level.WARNING, "Failed to serialize Commune message", e);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private class ReceiveRunnable implements Runnable {
        @Override
        public void run() {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new ReceiveListener(), "commune", "commune.minecraft", "commune." + source);
            }
        }
    }

    private class ReceiveListener extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            if (running) {
                try {
                    Message m = mapper.readValue(message, Message.class);
                    if (!m.getSource().equals(source)) {
                        receiveQueue.add(m);
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to deserialize Commune message: " + message, e);
                }
            } else {
                // Jedis is not thread safe so this is the only time we can unsubscribe
                unsubscribe();
            }
        }
    }

}
