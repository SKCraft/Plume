package com.skcraft.plume.module.perf.watchdog;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.skcraft.plume.asm.util.MethodMatcher;
import com.skcraft.plume.module.perf.CurrentTickingObject;
import com.skcraft.plume.util.GameRegistryUtils;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import java.util.List;

import static com.skcraft.plume.common.util.SharedLocale.tr;

public class StallCauseDetector {

    private final List<CausePattern> patterns = Lists.newArrayList();
    private final CurrentTickingObject currentTicking;

    {
        patterns.add(new CausePattern(
                new MethodMatcher("net.minecraft.item.crafting.CraftingManager", "func_82787_a", "findMatchingRecipe", ""),
                tr("watchdog.cause.crafting")
        ));
        patterns.add(new CausePattern(
                new MethodMatcher("cpw.mods.fml.common.FMLCommonHandler", "onPostServerTick", "onPostServerTick", ""),
                tr("watchdog.cause.postServerTick")
        ));
        patterns.add(new CausePattern(
                new MethodMatcher("cpw.mods.fml.common.FMLCommonHandler", "onPostWorldTick", "onPostWorldTick", ""),
                tr("watchdog.cause.postWorldTick")
        ));
        patterns.add(new CausePattern(
                new MethodMatcher("cpw.mods.fml.common.FMLCommonHandler", "onPreServerTick", "onPreServerTick", ""),
                tr("watchdog.cause.preServerTick")
        ));
        patterns.add(new CausePattern(
                new MethodMatcher("cpw.mods.fml.common.FMLCommonHandler", "onPreWorldTick", "onPreWorldTick", ""),
                tr("watchdog.cause.preWorldTick")
        ));
    }

    @Inject
    public StallCauseDetector(CurrentTickingObject currentTicking) {
        this.currentTicking = currentTicking;
    }

    public String analyze(Thread thread) {
        if (!thread.isAlive()) {
            return null;
        }
        return analyze(thread.getStackTrace());
    }

    public String analyze(StackTraceElement[] stackTrace) {
        for (int i = stackTrace.length - 1; i >= 0; i--) {
            StackTraceElement element = stackTrace[i];
            for (CausePattern pattern : patterns) {
                if (pattern.method.matchesRemappedMethod(element.getClassName(), element.getMethodName())) {
                    return pattern.message;
                }
            }
        }

        Object currentObject = currentTicking.getCurrentObject();
        if (currentObject instanceof TileEntity) {
            return tr("watchdog.cause.tickingTileEntity", GameRegistryUtils.getBlockId(((TileEntity) currentObject).getBlockType()));
        } else if (currentObject instanceof Entity) {
            return tr("watchdog.cause.tickingEntity", currentObject.getClass().getName());
        }

        return null;
    }

    private static class CausePattern {
        private final MethodMatcher method;
        private final String message;

        private CausePattern(MethodMatcher method, String message) {
            this.method = method;
            this.message = message;
        }
    }

}
