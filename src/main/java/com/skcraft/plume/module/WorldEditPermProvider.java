package com.skcraft.plume.module;

import com.google.inject.Inject;
import com.sk89q.worldedit.forge.ForgePermissionsProvider;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.auth.Context.Builder;
import com.skcraft.plume.common.util.event.Subscribe;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.util.Contexts;
import com.skcraft.plume.util.profile.Profiles;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;

@Module(name = "worldedit-perm-provider", hidden = true)
public class WorldEditPermProvider implements ForgePermissionsProvider {

    @Inject
    private Authorizer authorizer;

    @Subscribe
    public void onFMLServerStartingEvent(FMLServerStartingEvent event) {
        ForgeWorldEdit.inst.setPermissionsProvider(this);
    }

    @Subscribe
    public void onFMLServerStoppingEvent(FMLServerStoppingEvent event) {
        ForgeWorldEdit.inst.setPermissionsProvider(null);
    }

    @Override
    public boolean hasPermission(EntityPlayerMP player, String permission) {
        UserId userId = Profiles.fromPlayer(player);
        Context.Builder builder = new Builder();
        Contexts.update(builder, player);
        return authorizer.getSubject(userId).hasPermission(permission, builder.build());
    }

    @Override
    public void registerPermission(ICommand command, String permission) {
    }

}
