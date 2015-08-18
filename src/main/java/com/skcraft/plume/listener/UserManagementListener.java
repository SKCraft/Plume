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
            Ban latest = null;
            for (Ban ban : bans) {
                if (latest == null) {
                    latest = ban;
                } else if (ban.getExpireTime() == null) {
                    latest = ban;
                    break;
                } else if (ban.getExpireTime().compareTo(latest.getExpireTime()) > 0) {
                    latest = ban;
                }
            }

            if (latest != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("Your access has been suspended. To appeal, mention #");
                builder.append(latest.getId());
                if (latest.getExpireTime() != null) {
                    builder.append("\n");
                    builder.append("Expires: ");
                    builder.append(latest.getExpireTime());
                }
                event.getNetHandler().func_147322_a(builder.toString());
            }
        }
    }
}
