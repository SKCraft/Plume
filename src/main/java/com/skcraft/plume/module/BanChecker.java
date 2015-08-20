package com.skcraft.plume.module;

import com.google.common.base.Optional;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.ban.Ban;
import com.skcraft.plume.common.ban.BanManager;
import com.skcraft.plume.common.extension.InjectService;
import com.skcraft.plume.common.extension.Service;
import com.skcraft.plume.common.extension.module.Module;
import com.skcraft.plume.event.network.PlayerAuthenticateEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Module(name = "ban-checker")
public class BanChecker {

    @InjectService
    private Service<BanManager> bans;

    @SubscribeEvent
    public void onAuthenticate(PlayerAuthenticateEvent event) {
        Optional<BanManager> optional = bans.get();
        if (optional.isPresent()) {
            List<Ban> bans = optional.get().findActiveBans(new UserId(event.getProfile().getId()));
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
}
