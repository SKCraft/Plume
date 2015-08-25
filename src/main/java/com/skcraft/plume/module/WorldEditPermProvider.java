package com.skcraft.plume.module;

import com.sk89q.worldedit.forge.ForgePermissionsProvider;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.auth.Authorizer;
import com.skcraft.plume.common.service.auth.Context;
import com.skcraft.plume.common.service.auth.Context.Builder;
import com.skcraft.plume.common.util.module.Module;
import com.skcraft.plume.common.util.service.InjectService;
import com.skcraft.plume.common.util.service.Service;
import com.skcraft.plume.util.Contexts;
import com.skcraft.plume.util.profile.Profiles;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;

@Module(name = "worldedit-perm-provider", hidden = true)
public class WorldEditPermProvider implements ForgePermissionsProvider {

    @InjectService
    private Service<Authorizer> authorizer;

    @Subscribe
    public void onFMLServerStartingEvent(FMLServerStartingEvent event) {
        ForgeWorldEdit.inst.setPermissionsProvider(this);
    }

    @Override
    public boolean hasPermission(EntityPlayerMP player, String permission) {
        UserId userId = Profiles.fromPlayer(player);
        Context.Builder builder = new Builder();
        Contexts.update(builder, player);
        return authorizer.provide().getSubject(userId).hasPermission(permission, builder.build());
    }

    @Override
    public void registerPermission(ICommand command, String permission) {
    }

}
