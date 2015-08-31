package com.skcraft.plume.module.backtrack;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.skcraft.plume.common.UserId;
import com.skcraft.plume.common.service.journal.Record;
import com.skcraft.plume.common.util.WorldVector3i;
import com.skcraft.plume.module.backtrack.action.Action;
import com.skcraft.plume.util.NBTUtils;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * Keeps a mapping of actions to their numeric code.
 */
@Singleton
public class ActionMap {

    private final BiMap<Class<? extends Action>, Short> actions = Maps.synchronizedBiMap(HashBiMap.create());
    private final Map<String, Short> actionNames = Maps.newHashMap();

    @Nullable
    public Short getActionIdByName(String name) {
        return actionNames.get(name.toLowerCase());
    }

    public void registerAction(int id, Class<? extends Action> type, String... names) {
        if (actions.putIfAbsent(type, (short) id) != null) {
            throw new IllegalArgumentException("Can't register " + type.getName() + " as action #" + id +
                    " because it's already registered to " + actions.inverse().get((short) id));
        }

        for (String name : names) {
            if (actionNames.putIfAbsent(name.toLowerCase(), (short) id) != null) {
                throw new IllegalArgumentException("There's already an existing action with the name '" + name + "'");
            }
        }
    }

    public Record createRecord(UserId userId, WorldVector3i location, Action action) throws ActionWriteException {
        Class<? extends Action> type = action.getClass();
        if (actions.containsKey(type)) {
            short code = actions.get(type);
            Record record = new Record();
            record.setUserId(userId);
            record.setLocation(location);
            record.setAction(code);
            NBTTagCompound tag = new NBTTagCompound();
            action.writeToTag(tag);
            if (!tag.hasNoTags()) {
                try {
                    record.setData(NBTUtils.compoundToBytes(tag));
                } catch (IOException e) {
                    throw new ActionWriteException("Couldn't convert compound tag to bytes", e);
                }
            }
            return record;
        } else {
            throw new IllegalArgumentException("Don't have an action registered for " + type.getName());
        }
    }

    public Action readRecord(Record record) throws ActionReadException {
        Class<? extends Action> type = actions.inverse().get(record.getAction());
        if (type != null) {
            try {
                Action action = type.newInstance();
                if (record.getData() != null) {
                    action.readFromTag(NBTUtils.compoundFromBytes(record.getData()));
                }
                return action;
            } catch (IllegalAccessException | InstantiationException e) {
                throw new ActionReadException("Could not instantiate " + type.getName() + " for " + record, e);
            } catch (IOException e) {
                throw new ActionReadException("Could not parse " + record, e);
            }
        } else {
            throw new ActionReadException("There's no known type registered to " + record.getAction());
        }
    }

}
