package com.skcraft.plume.module;

import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.event.Result;
import com.skcraft.plume.event.block.BlockChange;
import com.skcraft.plume.event.block.BreakBlockEvent;
import com.skcraft.plume.event.block.PlaceBlockEvent;
import com.skcraft.plume.event.block.UseBlockEvent;
import com.skcraft.plume.event.entity.DamageEntityEvent;
import com.skcraft.plume.event.entity.DestroyEntityEvent;
import com.skcraft.plume.event.entity.SpawnEntityEvent;
import com.skcraft.plume.event.entity.UseEntityEvent;
import com.skcraft.plume.util.BlockState;
import com.skcraft.plume.util.Location3d;
import com.skcraft.plume.util.Location3i;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;

@Module(name = "abstraction-events-debug", enabled = false,
        desc = "Shows, in the server console, intermediary events used for protection and logging")
@Log
public class DebuggingListener {

    @Subscribe(priority = Priority.VERY_LATE)
    public void onPlaceBlock(PlaceBlockEvent event) {
        for (BlockChange change : event.getChanges()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PLACE");
            builder.append(" ");
            builder.append("").append(change.getReplacement());
            builder.append(" ");
            builder.append("@").append(change.getLocation());
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onBreakBlock(BreakBlockEvent event) {
        for (BlockChange change : event.getChanges()) {
            StringBuilder builder = new StringBuilder();
            builder.append("DIG");
            builder.append(" ");
            builder.append("").append(change.getCurrent());
            builder.append(" ");
            builder.append("@").append(change.getLocation());
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseBlock(UseBlockEvent event) {
        for (Location3i location : event.getLocations()) {
            StringBuilder builder = new StringBuilder();
            builder.append("INTERACT");
            builder.append(" ");
            builder.append("").append(BlockState.getBlockAndMeta(location));
            builder.append(" ");
            builder.append("@").append(location);
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onSpawnEntity(SpawnEntityEvent event) {
        for (Entity entity : event.getEntities()) {
            StringBuilder builder = new StringBuilder();
            builder.append("SPAWN");
            builder.append(" ");
            builder.append("").append(entity.getClass().getName());
            builder.append(" ");
            builder.append("@").append(Location3d.fromEntity(entity));
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onDestroyEntity(DestroyEntityEvent event) {
        for (Entity entity : event.getEntities()) {
            StringBuilder builder = new StringBuilder();
            builder.append("DESTROY");
            builder.append(" ");
            builder.append("").append(entity.getClass().getName());
            builder.append(" ");
            builder.append("@").append(Location3d.fromEntity(entity));
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void onUseEntity(UseEntityEvent event) {
        for (Entity entity : event.getEntities()) {
            StringBuilder builder = new StringBuilder();
            builder.append("INTERACT");
            builder.append(" ");
            builder.append("").append(entity.getClass().getName());
            builder.append(" ");
            builder.append("@").append(Location3d.fromEntity(entity));
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

    @Subscribe(priority = Priority.VERY_LATE)
    public void obDamageEntity(DamageEntityEvent event) {
        for (Entity entity : event.getEntities()) {
            StringBuilder builder = new StringBuilder();
            builder.append("DAMAGE");
            builder.append(" ");
            builder.append("").append(entity.getClass().getName());
            builder.append(" ");
            builder.append("@").append(Location3d.fromEntity(entity));
            builder.append(" ");
            builder.append("[").append(event.getCause()).append("]");
            if (event.getResult() != Result.DEFAULT) {
                builder.append(" [").append(event.getResult()).append("]");
            }
            log.info(builder.toString());
        }
    }

}
