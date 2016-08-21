package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.skcraft.plume.common.util.config.Config;
import com.skcraft.plume.common.util.config.InjectConfig;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Messages;
import com.skcraft.plume.util.concurrent.TickExecutorService;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ninja.leaping.configurate.objectmapping.Setting;

import static com.skcraft.plume.common.util.SharedLocale.tr;

@Module(name = "anytime-sleep", desc = "Lets players right click a bed to set his or her home during the day", enabled = false)
public class AnytimeSleep {

    @InjectConfig("anytime_sleep")
    private Config<AnytimeSleepConfig> config;
    @Inject
    private TickExecutorService tickExecutor;

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world.isRemote) return;

        switch (event.action) {
            case RIGHT_CLICK_BLOCK:
                if (event.useBlock != Result.DENY) {
                    Block block = event.world.getBlockState(new BlockPos(event.pos.getX(), event.pos.getY(), event.pos.getZ())).getBlock();

                    if (block == Blocks.bed) {
                        boolean forceSpawn = config.get().forceSpawn;

                        // Run it later so we bypass the bed set
                        tickExecutor.execute(() -> {
                            BlockPos coords = new BlockPos(event.pos.getX(), event.pos.getY(), event.pos.getZ());
                            event.entityPlayer.setSpawnChunk(coords, forceSpawn, event.world.provider.getDimensionId());

                            if (forceSpawn) {
                                event.entityPlayer.addChatComponentMessage(Messages.info(tr("anytimeSleep.respawnHereRegardless")));
                            } else {
                                event.entityPlayer.addChatComponentMessage(Messages.info(tr("anytimeSleep.respawnHere")));
                            }
                        });
                    }
                }
                break;
        }
    }

    private static class AnytimeSleepConfig {
        @Setting(comment = "Force respawn at the player's bed even if the bed has removed (this setting is applied when the bed is set)")
        public boolean forceSpawn = false;
    }

}
