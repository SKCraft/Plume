package com.skcraft.plume.common.util.concurrent;

public interface Filter<I, O> {

    O apply(I input) throws Exception;

}
