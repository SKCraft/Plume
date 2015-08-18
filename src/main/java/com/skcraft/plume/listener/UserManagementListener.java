package com.skcraft.plume.listener;

import com.skcraft.plume.Plume;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.ban.Ban;
import com.skcraft.plume.event.network.PlayerAuthenticateEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class UserManagementListener {

    @SubscribeEvent
    public void onAuthenticate(PlayerAuthenticateEvent event) {
        List<Ban> bans = Plume.INSTANCE.getBanManager().findActiveBans(new UserId(event.getProfile().getId()));
        if(bans != null && !bans.isEmpty()) {
            bans.sort((Ban ban1, Ban ban2) -> ban1.getExpireTime() == null ? 1 : ban2.getExpireTime() == null ? -1 : ban1.getExpireTime().compareTo(ban2.getExpireTime()));

            Ban latest = bans.get(bans.size() - 1);
            if (latest != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("Your access has been suspended. To appeal, mention #");
                builder.append(latest.getId());
                if (latest.getExpireTime() != null) {
                    builder.append("\nExpires: ");
                    builder.append(latest.getExpireTime());
                }
                event.getNetHandler().func_147322_a(builder.toString());
            }
        }
    }
}
