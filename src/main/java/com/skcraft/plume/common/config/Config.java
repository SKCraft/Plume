package com.skcraft.plume.common.config;

public interface Config<T> {

    boolean load();

    boolean save();

    T get();

}
