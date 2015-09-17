package com.skcraft.plume.util.playerdata;

import com.skcraft.plume.common.UserId;
import lombok.extern.java.Log;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.SaveHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Log
public final class PlayerDataFiles {

    private PlayerDataFiles() {
    }

    public static File getPlayerDataFile(UserId userId) {
        SaveHandler saveHandler = (SaveHandler) MinecraftServer.getServer().getConfigurationManager().playerNBTManagerObj;
        return new File(saveHandler.playersDirectory, userId.getUuid().toString() + ".dat");
    }

    public static NBTTagCompound readPlayer(UserId userId) throws IOException {
        File saveFile = getPlayerDataFile(userId);
        try (FileInputStream fis = new FileInputStream(saveFile);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            return CompressedStreamTools.readCompressed(bis);
        }
    }

    public static void writePlayer(UserId userId, NBTTagCompound tag) throws IOException {
        File saveFile = getPlayerDataFile(userId);
        File tempFile = new File(saveFile.getAbsoluteFile().getParent(), saveFile.getName() + ".tmp");

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            CompressedStreamTools.writeCompressed(tag, bos);
        }

        if (tempFile.exists()) {
            saveFile.delete();
            tempFile.renameTo(saveFile);
        }
    }

}
