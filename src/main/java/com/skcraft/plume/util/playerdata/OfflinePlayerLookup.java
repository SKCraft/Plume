package com.skcraft.plume.util.playerdata;

import com.sk89q.intake.CommandException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.profile.ProfileService;

import java.io.File;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

public class OfflinePlayerLookup implements Callable<UserId> {

    private final ProfileService profileService;
    private final String name;

    public OfflinePlayerLookup(ProfileService profileService, String name) {
        checkNotNull(profileService, "profileService");
        checkNotNull(name, "name");
        this.profileService = profileService;
        this.name = name;
    }

    @Override
    public UserId call() throws Exception {
        UserId userId = profileService.findUserId(name);
        File saveFile = PlayerDataFiles.getPlayerDataFile(userId);
        if (!saveFile.exists()) {
            throw new CommandException(tr("args.noPlayerDataFile", name));
        }
        return userId;
    }

}
