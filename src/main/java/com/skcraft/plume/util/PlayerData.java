package com.skcraft.plume.util;

import lombok.Getter;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipException;

public class PlayerData {

    @Getter @Nullable private NBTTagCompound data;
    @Getter private File file;

    public PlayerData(String path) {
        this(new File(path));
    }

    public PlayerData(File file) {
        this.file = file;

        if(!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    public void putInventory(EntityPlayerMP player) {
        if(data != null) {
            player.inventory.readFromNBT(data.getTagList("Inventory", 10));
        } else {
            for(int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
                player.inventory.mainInventory[slot] = null;
            }
            for(int slot = 0; slot < player.inventory.armorInventory.length; slot++) {
                player.inventory.armorInventory[slot] = null;
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public void putAll(EntityPlayerMP player) {
        if(data != null) {
            NBTTagList pos = data.getTagList("Pos", 6);
            TeleportHelper.teleport(player, pos.getDoubleAt(0), pos.getDoubleAt(1), pos.getDoubleAt(2), data.getInteger("Dimension"));
            player.readEntityFromNBT(data);
        } else {
            for(int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
                player.inventory.mainInventory[slot] = null;
            }
            for(int slot = 0; slot < player.inventory.armorInventory.length; slot++) {
                player.inventory.armorInventory[slot] = null;
            }
        }
        player.inventoryContainer.detectAndSendChanges();
    }

    public NBTTagCompound load(EntityPlayerMP player) {
        data = new NBTTagCompound();
        player.writeEntityToNBT(data);
        player.writeToNBT(data);
        return data;
    }

    public NBTTagCompound load() throws IOException {
        if(!file.exists()) return null;
        FileInputStream in = new FileInputStream(file);
        try {
            data = CompressedStreamTools.readCompressed(in);
        } catch(ZipException e) {
            if(e.getMessage().equals("Not in GZIP format")) {
                data = CompressedStreamTools.read(file);
            } else {
                throw e;
            }
        }
        in.close();
        return data;
    }

    public void save() throws IOException {
        if(data != null) {
            FileOutputStream out = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(data, out);
            out.close();
        }
    }
}
