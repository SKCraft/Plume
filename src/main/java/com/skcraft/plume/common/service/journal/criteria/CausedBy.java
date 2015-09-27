package com.skcraft.plume.common.service.journal.criteria;

import com.google.inject.Inject;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.ArgumentParseException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.argument.Namespace;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.util.profile.ProfileLookupException;
import com.skcraft.plume.util.profile.ProfileNotFoundException;
import com.skcraft.plume.util.profile.ProfileService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.jooq.Condition;

import java.util.logging.Level;

import static com.skcraft.plume.common.service.sql.model.data.tables.UserId.USER_ID;
import static com.skcraft.plume.common.util.SharedLocale.tr;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log
public class CausedBy implements Criteria {

    @Inject
    private ProfileService profileService;
    private UserId userId;

    public CausedBy(UserId userId) {
        this.userId = userId;
    }

    @Override
    public Condition toCondition() {
        return USER_ID.UUID.eq(userId.getUuid().toString());
    }

    @Override
    public void parse(CommandArgs args, Namespace namespace) throws ArgumentException {
        String name = args.next();
        try {
            userId = profileService.findUserId(name);
        } catch (ProfileLookupException e) {
            log.log(Level.WARNING, "Failed to lookup a player's profile", e);
            throw new ArgumentParseException(tr("args.minecraftUserLookupFailed", name));
        } catch (ProfileNotFoundException e) {
            throw new ArgumentParseException(tr("args.minecraftUserNotFound", name));
        }
    }

    @Override
    public boolean hasSpecificCriteria() {
        return true;
    }

}
