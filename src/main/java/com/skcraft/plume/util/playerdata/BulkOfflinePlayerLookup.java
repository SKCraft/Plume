package com.skcraft.plume.util.playerdata;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.sk89q.intake.CommandException;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.skcraft.plume.common.util.SharedLocale.tr;

public class BulkOfflinePlayerLookup implements Callable<List<UserId>> {

    private final ProfileService profileService;
    private final List<String> names;

    public BulkOfflinePlayerLookup(ProfileService profileService, List<String> names) {
        checkNotNull(profileService, "profileService");
        checkNotNull(names, "names");
        this.profileService = profileService;
        this.names = names;
    }

    @Override
    public List<UserId> call() throws ProfileNotFoundException, CommandException, ProfileLookupException {
        Map<String, Optional<UserId>> results = profileService.findUserIds(names);
        List<UserId> accepted = Lists.newArrayList();

        for (Map.Entry<String, Optional<UserId>> entry : results.entrySet()) {
            if (entry.getValue().isPresent()) {
                File saveFile = PlayerDataFiles.getPlayerDataFile(entry.getValue().get());
                if (!saveFile.exists()) {
                    throw new CommandException(tr("args.noPlayerDataFile", entry.getKey()));
                }
                accepted.add(entry.getValue().get());
            } else {
                throw new ProfileNotFoundException(entry.getKey());
            }
        }

        return accepted;
    }
}
