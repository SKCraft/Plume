package com.skcraft.plume.module.backtrack;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.Journal;
import com.skcraft.plume.common.service.journal.JournalBuffer;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.AutoRegister;
import com.skcraft.plume.module.backtrack.action.*;
import com.skcraft.plume.util.BlockSnapshot;
import com.skcraft.plume.util.NamedEntity;
import com.skcraft.plume.util.Worlds;
import com.skcraft.plume.util.inventory.Inventories;
import com.skcraft.plume.util.profile.Profiles;
import lombok.extern.java.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.util.logging.Level;

@AutoRegister
@Log
public class LoggerListener {

    private static final BlockSnapshot AIR_SNAPSHOT = new BlockSnapshot(Blocks.air, 0, null);
    @InjectConfig("backtrack") private Config<LoggerConfig> config;

    @Inject private Journal journal;
    @Inject private ActionMap actionMap;
    private JournalBuffer buffer;

    @Subscribe
    public void onFMLServerStarting(FMLServerStartingEvent event) {
        File queueFile = new File(DimensionManager.getCurrentSaveRootDirectory(), "/plume/logger/queue.dat");
        queueFile.getParentFile().mkdirs();
        buffer = new JournalBuffer(journal, queueFile);
    }

    @Subscribe
    public void onFMLServerStopping(FMLServerStoppingEvent event) {
        buffer.shutdown();
    }

    public void addRecord(@Nullable UserId userId, WorldVector3i location, Action action) {
        try {
            Record record = actionMap.createRecord(userId, location, action);
            buffer.addRecord(record);
        } catch (ActionWriteException e) {
            log.log(Level.WARNING, "Failed to create block logger record", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.world.isRemote) return;
        if (!config.get().events.blockBreak) return;

        UserId userId = Profiles.fromPlayer(event.getPlayer());
        BlockBreakAction action = new BlockBreakAction();
        action.setBefore(BlockSnapshot.toSnapshot(event.world, event.pos.getX(), event.pos.getY(), event.pos.getZ()));
        action.setAfter(AIR_SNAPSHOT);
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.world), event.pos.getX(), event.pos.getY(), event.pos.getZ()), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (event.world.isRemote) return;
        if (!config.get().events.blockPlace) return;

        UserId userId = Profiles.fromPlayer(event.player);
        BlockPlaceAction action = new BlockPlaceAction();
        action.setBefore(AIR_SNAPSHOT);
        action.setAfter(BlockSnapshot.toSnapshot(event.world, event.pos.getX(), event.pos.getY(), event.pos.getZ()));
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.world), event.pos.getX(), event.pos.getY(), event.pos.getZ()), action);
    }

    @SubscribeEvent
    public void onBlockMultiPlace(BlockEvent.MultiPlaceEvent event) {
        if (event.world.isRemote) return;
        if (!config.get().events.blockPlace) return;

        UserId userId = Profiles.fromPlayer(event.player);

        for (net.minecraftforge.common.util.BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            BlockPlaceAction action = new BlockPlaceAction();
            action.setBefore(BlockSnapshot.toSnapshot(event.world, snapshot.pos.getX(), snapshot.pos.getY(), snapshot.pos.getZ()));
            action.setAfter(BlockSnapshot.toSnapshot(snapshot));
            addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.world), snapshot.pos.getX(), snapshot.pos.getY(), snapshot.pos.getZ()), action);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockExplosion(ExplosionEvent.Detonate event) {
        if (event.world.isRemote) return;
        if (!config.get().events.explosion) return;

        for (BlockPos position : event.getAffectedBlocks()) {
            BlockExplodeAction action = new BlockExplodeAction();
            if (event.world.getBlockState(position).getBlock() != Blocks.air) {
                action.setBefore(BlockSnapshot.toSnapshot(event.world, position.getX(), position.getY(), position.getZ()));
                action.setAfter(AIR_SNAPSHOT);
                addRecord(null, new WorldVector3i(Worlds.getWorldId(event.world), position.getX(), position.getY(), position.getZ()), action);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFillBucket(FillBucketEvent event) {
        if (event.world.isRemote) return;
        if (!config.get().events.bucketFill) return;

        UserId userId = Profiles.fromPlayer(event.entityPlayer);
        BucketFillAction action = new BucketFillAction();
        action.setBefore(BlockSnapshot.toSnapshot(event.world, event.target.getBlockPos().getX(), event.target.getBlockPos().getY(), event.target.getBlockPos().getZ()));
        action.setAfter(AIR_SNAPSHOT);
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.world), event.target.getBlockPos().getX(), event.target.getBlockPos().getY(), event.target.getBlockPos().getZ()), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityItemPickupEvent event) {
        if (event.entity.worldObj.isRemote) return;
        if (!config.get().events.itemPickup) return;

        UserId userId = Profiles.fromPlayer(event.entityPlayer);
        ItemPickupAction action = new ItemPickupAction();
        action.setItemStack(event.item.getEntityItem().copy());
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.item.worldObj), (int) event.item.posX, (int) event.item.posY, (int) event.item.posZ), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemToss(ItemTossEvent event) {
        if (event.entity.worldObj.isRemote) return;
        if (!config.get().events.itemDrop) return;

        UserId userId = Profiles.fromPlayer(event.player);
        ItemDropAction action = new ItemDropAction();
        action.setItemStack(event.entityItem.getEntityItem().copy());
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.entityItem.worldObj), (int) event.entityItem.posX, (int) event.entityItem.posY, (int) event.entityItem.posZ), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerAttackEntityEvent(AttackEntityEvent event) {
        if (event.entity.worldObj.isRemote) return;
        if (!config.get().events.entityDamage) return;

        UserId userId = Profiles.fromPlayer(event.entityPlayer);
        EntityDamageAction action = new EntityDamageAction();
        action.setEntity(NamedEntity.from(event.target));
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.target.worldObj), (int) event.entity.posX, (int) event.entity.posY, (int) event.entity.posZ), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerChat(ServerChatEvent event) {
        if (event.player.worldObj.isRemote) return;
        if (!config.get().events.playerChat) return;

        UserId userId = Profiles.fromPlayer(event.player);
        PlayerChatAction action = new PlayerChatAction();
        action.setMessage(event.message);
        addRecord(userId, new WorldVector3i(Worlds.getWorldId(event.player.worldObj), (int) event.player.posX, (int) event.player.posY, (int) event.player.posZ), action);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerCommand(CommandEvent event) {
        if (!config.get().events.playerCommand) return;

        if (event.sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.sender;

            if (player.worldObj.isRemote) return;

            UserId userId = Profiles.fromPlayer(player);
            PlayerCommandAction action = new PlayerCommandAction();
            action.setMessage(event.command.getCommandName() + " " + Joiner.on(" ").join(event.parameters));
            addRecord(userId, new WorldVector3i(Worlds.getWorldId(player.worldObj), (int) player.posX, (int) player.posY, (int) player.posZ), action);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entity.worldObj.isRemote) return;
        if (!config.get().events.playerDeath) return;

        if (event.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;

            UserId userId = Profiles.fromPlayer(player);
            PlayerDeathAction action = new PlayerDeathAction();
            action.setItemStacks(Inventories.copyInventoryItems(player.inventory));
            addRecord(userId, new WorldVector3i(Worlds.getWorldId(player.worldObj), (int) player.posX, (int) player.posY, (int) player.posZ), action);
        }
    }

}
