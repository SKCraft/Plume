package com.skcraft.plume.util.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.regions.Region;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public final class WorldEditAPI {

    private WorldEditAPI() {
    }

    public static Region getSelection(EntityPlayer player) throws IncompleteRegionException {
        LocalSession session = ForgeWorldEdit.inst.getSession((EntityPlayerMP) player);
        return session.getSelection(ForgeWorldEdit.inst.getWorld(((EntityPlayerMP) player).worldObj));
    }

}
