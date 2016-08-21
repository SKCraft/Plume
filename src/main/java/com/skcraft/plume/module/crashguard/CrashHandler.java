package com.skcraft.plume.module.crashguard;

import com.skcraft.plume.util.Messages;
import lombok.extern.java.Log;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;

import java.util.logging.Level;

@Log
enum CrashHandler {
    DEFAULT {
        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, Entity entity) {
            return false;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, TileEntity tileEntity) {
            return false;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable) {
            return false;
        }
    },
    SUPPRESS {
        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, Entity entity) {
            crashGuard.logCrashPeriodically(entity, throwable, () -> Messages.toString(entity));
            return true;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, TileEntity tileEntity) {
            crashGuard.logCrashPeriodically(tileEntity, throwable, () -> Messages.toString(tileEntity));
            return true;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable) {
            crashGuard.logCrashPeriodically(null, throwable, () -> "unknown");
            return true;
        }
    },
    REMOVE {
        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, Entity entity) {
            log.log(Level.WARNING, "Crash intercepted for " + Messages.toString(entity) + " -- NOW REMOVING THE ENTITY", throwable);
            entity.setDead();
            return true;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable, TileEntity tileEntity) {
            log.log(Level.WARNING, "Crash intercepted for " + Messages.toString(tileEntity) + " -- NOW REMOVING THE TILE ENTITY", throwable);
            BlockPos pos = new BlockPos(tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
            tileEntity.getWorld().setBlockToAir(pos);
            return true;
        }

        @Override
        public boolean apply(CrashGuard crashGuard, Throwable throwable) {
            crashGuard.logCrashPeriodically(null, throwable, () -> "unknown");
            return true;
        }
    };

    public abstract boolean apply(CrashGuard crashGuard, Throwable throwable, Entity entity);

    public abstract boolean apply(CrashGuard crashGuard, Throwable throwable, TileEntity tileEntity);

    public abstract boolean apply(CrashGuard crashGuard, Throwable throwable);

}
