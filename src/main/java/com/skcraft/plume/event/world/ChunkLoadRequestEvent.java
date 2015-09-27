package com.skcraft.plume.event.world;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;
import lombok.Getter;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.ListIterator;

@Getter
@Cancelable
public class ChunkLoadRequestEvent extends Event {

    private final WorldServer world;
    private final int x;
    private final int z;

    public ChunkLoadRequestEvent(WorldServer world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public static void stripStackTrace(List<StackTraceElement> stackTrace) {
        ListIterator<StackTraceElement> it = stackTrace.listIterator(stackTrace.size());
        boolean found = false;
        while (it.hasPrevious()) {
            StackTraceElement element = it.previous();
            if (!found && element.getClassName().equals("com.skcraft.plume.asm.transformer.ChunkLoadHelper") && element.getMethodName().equals("mayLoadChunk")) {
                found = true;
            }
            if (found) {
                it.remove();
            }
        }
    }

}
