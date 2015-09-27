package com.skcraft.plume.network;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.IOException;

public interface PlumePacket {

    void readFromServer(ByteBufInputStream in) throws IOException;

    void readFromClient(ByteBufInputStream in) throws IOException;

    void processOnClient(EntityPlayer player);

    void processOnServer(EntityPlayerMP player);

    void write(ByteBufOutputStream out) throws IOException;

}
