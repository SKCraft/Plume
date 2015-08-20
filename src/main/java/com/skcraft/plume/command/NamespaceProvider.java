package com.skcraft.plume.command;

import com.google.common.collect.ImmutableList;
import com.sk89q.intake.argument.ArgumentException;
import com.sk89q.intake.argument.CommandArgs;
import com.sk89q.intake.parametric.Provider;
import com.sk89q.intake.parametric.ProvisionException;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

class NamespaceProvider<T> implements Provider<T> {

    private final Class<T> type;

    NamespaceProvider(Class<T> type) {
        checkNotNull(type, "type");
        this.type = type;
    }

    @Override
    public boolean isProvided() {
        return false;
    }

    @Nullable
    @Override
    public T get(CommandArgs arguments, List<? extends Annotation> modifiers) throws ArgumentException, ProvisionException {
        T value = arguments.getNamespace().get(type);
        if (value != null) {
            return value;
        } else {
            throw new ProvisionException(type.getName() + " object not found in Namespace");
        }
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        return ImmutableList.of();
    }

}
