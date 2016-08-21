package com.skcraft.plume.module.inventory;

import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.NBTConstants;
import com.skcraft.plume.util.playerdata.PlayerDataFiles;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.inventory.PlayerInventoryChestAdapter;
import com.skcraft.plume.util.profile.Profiles;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.io.File;
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
    public void closeInventory(EntityPlayer player) {
        super.closeInventory(player);

        if (!online) {
            File saveFile = PlayerDataFiles.getPlayerDataFile(userId);
            NBTTagCompound data;

            try {
                data = PlayerDataFiles.readPlayer(userId);
                FakePlayer fakePlayer = new FakePlayer((WorldServer) MinecraftServer.getServer().getEntityWorld(), Profiles.toProfile(userId));
                InventoryPlayer fakeInventory = new InventoryPlayer(fakePlayer);
                fakeInventory.readFromNBT(data.getTagList("Inventory", NBTConstants.COMPOUND_TAG));
                Inventories.copyDirectly(getDelegate(), fakeInventory);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to read " + saveFile.getAbsolutePath() + " for update for /viewinv", e);
                return;
            }

            try {
                PlayerDataFiles.writePlayer(userId, data);
                log.info("Saved " + saveFile.getAbsolutePath() + " for /viewinv of " + userId);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to write " + saveFile.getAbsolutePath() + " for update for /viewinv", e);
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
        FakePlayer fakePlayer = new FakePlayer((WorldServer) MinecraftServer.getServer().getEntityWorld(), Profiles.toProfile(userId));
        InventoryPlayer fakeInventory = new InventoryPlayer(fakePlayer);
        NBTTagCompound data = PlayerDataFiles.readPlayer(userId);
        fakeInventory.readFromNBT(data.getTagList("Inventory", NBTConstants.COMPOUND_TAG));
        return fakeInventory;
    }

}
