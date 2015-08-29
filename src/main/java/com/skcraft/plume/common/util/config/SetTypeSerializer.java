package com.skcraft.plume.common.util.config;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class SetTypeSerializer implements TypeSerializer<Set<?>> {

    @Override
    public Set<?> deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        TypeToken<?> entryType = type.resolveType(Set.class.getTypeParameters()[0]);
        TypeSerializer entrySerial = value.getOptions().getSerializers().get(entryType);
        if (entrySerial == null) {
            throw new ObjectMappingException("No applicable type serializer for type " + entryType);
        }

        if (value.hasListChildren()) {
            List<? extends ConfigurationNode> values = value.getChildrenList();
            Set<Object> ret = Sets.newHashSetWithExpectedSize(values.size());
            for (ConfigurationNode ent : values) {
                ret.add(entrySerial.deserialize(entryType, ent));
            }
            return ret;
        } else {
            Object unwrappedVal = value.getValue();
            if (unwrappedVal != null) {
                return Sets.newHashSet(entrySerial.deserialize(entryType, value));
            }
        }
        return Sets.newHashSet();
    }

    @Override
    public void serialize(TypeToken<?> type, Set<?> obj, ConfigurationNode value) throws ObjectMappingException {
        TypeToken<?> entryType = type.resolveType(Set.class.getTypeParameters()[0]);
        TypeSerializer entrySerial = value.getOptions().getSerializers().get(entryType);
        if (entrySerial == null) {
            throw new ObjectMappingException("No applicable type serializer for type " + entryType);
        }
        value.setValue(null);
        for (Object ent : obj) {
            entrySerial.serialize(entryType, ent, value.getAppendedNode());
        }
    }

}
