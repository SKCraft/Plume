package com.skcraft.plume.common.util;

import java.io.Closeable;

public interface Cursor<V> extends Closeable, AutoCloseable {

    boolean hasNext();

    V next();

}
