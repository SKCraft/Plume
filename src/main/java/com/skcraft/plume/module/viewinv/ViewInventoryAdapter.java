package com.skcraft.plume.module.viewinv;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.NBTConstants;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.inventory.PlayerInventoryChestAdapter;
import com.skcraft.plume.util.profile.Profiles;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import static com.google.common.base.Preconditions.checkNotNull;

@Log
class ViewInventoryAdapter extends PlayerInventoryChestAdapter {

    private final UserId userId;
    private boolean online;

    public ViewInventoryAdapter(UserId userId, @Nullable EntityPlayer entity) throws IOException {
        super(chooseInventory(userId, entity), userId.getName());
        checkNotNull(userId, "userId");
        this.userId = userId;
        this.online = entity != null;
    }

    public void setPlayerEntity(@Nullable EntityPlayer entity) throws IOException {
        InventoryPlayer newInventory = chooseInventory(userId, entity);
        if (!online) { // Was offline
            Inventories.copyDirectly(getDelegate(), newInventory);
        }
        setDelegate(newInventory);
        this.online = entity != null;
    }

    @Override
    public void closeInventory() {
        super.closeInventory();

        if (!online) {
            File saveFile = Server.getPlayerDataFile(userId);
            File tempFile = new File(saveFile.getAbsoluteFile().getParent(), saveFile.getName() + ".tmp");
            NBTTagCompound data;

            try (FileInputStream fis = new FileInputStream(saveFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                FakePlayer fakePlayer = new FakePlayer((WorldServer) MinecraftServer.getServer().getEntityWorld(), Profiles.toProfile(userId));
                InventoryPlayer fakeInventory = new InventoryPlayer(fakePlayer);
                data = CompressedStreamTools.readCompressed(bis);
                fakeInventory.readFromNBT(data.getTagList("Inventory", NBTConstants.COMPOUND_TAG));
                Inventories.copyDirectly(getDelegate(), fakeInventory);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to read " + saveFile.getAbsolutePath() + " for update for /viewinv", e);
                return;
            }

            try (FileOutputStream fos = new FileOutputStream(tempFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                CompressedStreamTools.writeCompressed(data, bos);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to write " + saveFile.getAbsolutePath() + " for update for /viewinv", e);
                return;
            }

            if (tempFile.exists()) {
                saveFile.delete();
                tempFile.renameTo(saveFile);
                log.info("Saved " + saveFile.getAbsolutePath() + " for /viewinv of " + userId);
            }
        }
    }

    private static InventoryPlayer chooseInventory(UserId userId, EntityPlayer entityPlayer) throws IOException {
        if (entityPlayer != null) {
            return entityPlayer.inventory;
        } else {
            return readOfflineInventory(userId);
        }
    }

    private static InventoryPlayer readOfflineInventory(UserId userId) throws IOException {
        File saveFile = Server.getPlayerDataFile(userId);
        try (FileInputStream fis = new FileInputStream(saveFile);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            FakePlayer fakePlayer = new FakePlayer((WorldServer) MinecraftServer.getServer().getEntityWorld(), Profiles.toProfile(userId));
            InventoryPlayer fakeInventory = new InventoryPlayer(fakePlayer);
            NBTTagCompound data = CompressedStreamTools.readCompressed(bis);
            fakeInventory.readFromNBT(data.getTagList("Inventory", NBTConstants.COMPOUND_TAG));
            return fakeInventory;
        }
    }

}
