package com.skcraft.plume.module.inventory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.sk89q.intake.Command;
import com.sk89q.intake.CommandException;
import com.sk89q.intake.Require;
import com.skcraft.plume.command.Sender;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.util.concurrent.Deferred;
import com.skcraft.plume.common.util.concurrent.Deferreds;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.OptionalPlayer;
import com.skcraft.plume.util.Server;
import com.skcraft.plume.util.concurrent.BackgroundExecutor;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.playerdata.OfflinePlayerLookup;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.LoadFromFile;
import net.minecraftforge.event.entity.player.PlayerEvent.SaveToFile;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Log
@Module(name = "view-inventory", desc = "View the inventories of other players")
public class ViewInventory {

    @Inject private ProfileService profileService;
    @Inject private BackgroundExecutor backgroundExecutor;
    @Inject private TickExecutorService tickExecutor;
    private final Map<UserId, ViewInventoryAdapter> openInventories = new WeakHashMap<>();
    private final Set<UserId> online = Sets.newHashSet();

    private ViewInventoryAdapter getInventoryAdapter(UserId userId, @Nullable EntityPlayer player) throws IOException {
        ViewInventoryAdapter adapter = openInventories.get(userId);
        if (adapter != null) {
            return adapter;
        } else {
            adapter = new ViewInventoryAdapter(userId, player);
            openInventories.put(userId, adapter);
            return adapter;
        }
    }

    @SubscribeEvent
    public void onPlayedLoggedIn(PlayerLoggedInEvent event) {
        online.add(Profiles.fromPlayer(event.player));
    }

    @SubscribeEvent
    public void onPlayedLoggedOut(PlayerLoggedOutEvent event) {
        online.remove(Profiles.fromPlayer(event.player));
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.player.worldObj.isRemote) return;

        ViewInventoryAdapter adapter = openInventories.get(Profiles.fromProfile(event.player.getGameProfile()));
        if (adapter != null) {
            try {
                adapter.setPlayerEntity(event.player);
                adapter.markDirty();
            } catch (IOException e) {
                log.log(Level.SEVERE, "This should not have happened", e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoad(LoadFromFile event) {
        ViewInventoryAdapter adapter = openInventories.get(Profiles.fromProfile(event.entityPlayer.getGameProfile()));
        if (adapter != null) {
            Inventories.copyDirectly(adapter.getDelegate(), event.entityPlayer.inventory);
            try {
                adapter.setPlayerEntity(event.entityPlayer);
                adapter.markDirty();
            } catch (IOException e) {
                log.log(Level.SEVERE, "This should not have happened", e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerSave(SaveToFile event) {
        UserId userId = Profiles.fromPlayer(event.entityPlayer);
        ViewInventoryAdapter adapter = openInventories.get(userId);
        if (adapter != null && userId != null && !online.contains(userId)) { // Player has quit
            Inventories.copyDirectly(adapter.getDelegate(), event.entityPlayer.inventory);
            try {
                adapter.setPlayerEntity(null);
                adapter.markDirty();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to read from the player data file for " + userId + " even " +
                        "though it should exist, which means that existing /viewinv views for this player may be broken", e);
            }
        }
    }

    @Command(aliases = {"inventory", "inven", "viewinv"}, desc = "See the inventory of another player")
    @Require("plume.viewinven.online")
    public void viewInventory(@Sender EntityPlayer sender, OptionalPlayer target) throws IOException {
        if (target.getPlayerEntity() != null) {
            sender.displayGUIChest(getInventoryAdapter(Profiles.fromPlayer(target.getPlayerEntity()), target.getPlayerEntity()));
        } else {
            Deferred<?> deferred = Deferreds
                    .when(new OfflinePlayerLookup(profileService, target.getName()), backgroundExecutor.getExecutor())
                    .filter(userId -> {
                        EntityPlayer player = Server.findPlayer(userId.getUuid());
                        sender.displayGUIChest(getInventoryAdapter(userId, player));
                        return null;
                    }, tickExecutor)
                    .fail(e -> {
                        if (e instanceof ProfileNotFoundException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserNotFound", ((ProfileNotFoundException) e).getName())));
                        } else if (e instanceof ProfileLookupException) {
                            sender.addChatMessage(Messages.error(tr("args.minecraftUserLookupFailed", ((ProfileLookupException) e).getName())));
                        } else if (e instanceof CommandException) {
                            sender.addChatMessage(Messages.error(e.getMessage()));
                        } else {
                            sender.addChatMessage(Messages.exception(e));
                        }
                    }, tickExecutor);

            backgroundExecutor.notifyOnDelay(deferred, sender);
        }
    }

}
