package com.skcraft.plume.common.util.config;

public interface Config<T> {

    boolean load();

    boolean save();

    T get();

}
