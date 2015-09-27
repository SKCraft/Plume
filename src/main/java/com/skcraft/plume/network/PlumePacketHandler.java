package com.skcraft.plume.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.skcraft.plume.Plume;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import lombok.extern.java.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

@Log
public class PlumePacketHandler {

    private final FMLEventChannel eventChannel;
    private final BiMap<Character, Class<? extends PlumePacket>> handlerMap = HashBiMap.create();

    public PlumePacketHandler(FMLEventChannel eventChannel) {
        this.eventChannel = eventChannel;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onReceiveFromServer(FMLNetworkEvent.ClientCustomPacketEvent evt) {
        try (ByteBufInputStream in = new ByteBufInputStream(evt.packet.payload())) {
            char type = in.readChar();
            Class<? extends PlumePacket> packetClass = handlerMap.get(type);
            if (packetClass != null) {
                PlumePacket packet = packetClass.newInstance();
                packet.readFromServer(in);
                packet.processOnClient(Minecraft.getMinecraft().thePlayer);
            } else {
                log.log(Level.WARNING, "Got a Plume packet from the server with an unknown packet type of " + type);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to read packet data from the server", e);
        } catch (InstantiationException | IllegalAccessException e) {
            log.log(Level.WARNING, "Failed to parse packet data from the server", e);
        }
    }

    @SubscribeEvent
    public void onReceiveFromClient(FMLNetworkEvent.ServerCustomPacketEvent evt) {
        EntityPlayerMP player = ((NetHandlerPlayServer) evt.handler).playerEntity;

        if (player != null) {
            try (ByteBufInputStream in = new ByteBufInputStream(evt.packet.payload())) {
                char type = in.readChar();
                Class<? extends PlumePacket> packetClass = handlerMap.get(type);
                if (packetClass != null) {
                    PlumePacket packet = packetClass.newInstance();
                    packet.readFromClient(in);
                    packet.processOnServer(player);
                } else {
                    log.log(Level.WARNING, "Got a Plume packet from " + player.getGameProfile() + " with an unknown packet type of " + type);
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to read packet data from " + player.getGameProfile(), e);
            } catch (InstantiationException | IllegalAccessException e) {
                log.log(Level.WARNING, "Failed to parse packet data from " + player.getGameProfile(), e);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to handle packet data from " + player.getGameProfile(), e);
            }
        } else {
            log.log(Level.WARNING, "Received a Plume packet packet from an null player!");
        }
    }

    public void sendToServer(PlumePacket packet) {
        Character id = handlerMap.inverse().get(packet.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Packet " + packet.getClass() + " is not registered!");
        }

        try {
            ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());
            out.writeChar(id);
            packet.write(out);

            FMLProxyPacket proxyPacket = new FMLProxyPacket(out.buffer(), Plume.CHANNEL_ID);
            eventChannel.sendToServer(proxyPacket);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to build packet to send", e);
        }
    }


    public void sendToAllClients(PlumePacket packet) {
        sendToClient(packet, null);
    }

    public void sendToClient(PlumePacket packet, @Nullable List<EntityPlayer> players) {
        Character id = handlerMap.inverse().get(packet.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Packet " + packet.getClass() + " is not registered!");
        }

        try {
            ByteBufOutputStream out = new ByteBufOutputStream(Unpooled.buffer());
            out.writeChar(id);
            packet.write(out);

            FMLProxyPacket proxyPacket = new FMLProxyPacket(out.buffer(), Plume.CHANNEL_ID);
            if (players == null) {
                eventChannel.sendToAll(proxyPacket);
            } else {
                for (EntityPlayer player : players) {
                    if (player instanceof EntityPlayerMP) {
                        eventChannel.sendTo(proxyPacket, (EntityPlayerMP) player);
                    }
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to build packet to send", e);
        }
    }
}
