/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.plume.common.util.concurrent;

public interface Callback<T> {

    void handle(T value) throws Exception;

}
