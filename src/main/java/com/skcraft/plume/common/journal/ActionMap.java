package com.skcraft.plume.common.journal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;

/**
 * Keeps a mapping of actions to their numeric code.
 */
public class ActionMap {

    private final BiMap<Class<? extends Action>, Short> actions = Maps.synchronizedBiMap(HashBiMap.create());

    /**
     * Map the given action type to the given ID.
     *
     * @param type The class of the action
     * @param code The ID
     */
    public void registerAction(Class<? extends Action> type, short code) {
        if (actions.putIfAbsent(type, code) != null) {
            throw new IllegalArgumentException("Can't register " + type + " as action #" + code +
                    " because it's already registered to " + actions.inverse().get(code));
        }
    }

    /**
     * Parse the given data and return an action.
     *
     * @param code The action ID
     * @param data The data
     * @return An action, or null if parsing failed
     */
    @Nullable
    public Action parse(short code, String data) {
        Class<? extends Action> type = actions.inverse().get(code);
        if (type != null) {
            try {
                Action action = type.newInstance();
                action.readData(data);
                return action;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ActionPersistenceException("Could not create Action object for rollback", e);
            }
        } else {
            return null;
        }
    }

}
