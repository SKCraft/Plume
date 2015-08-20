/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.skcraft.plume.common.util.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Log4jRedirect extends Handler {

    private final Logger logger;
    private final String prefix;

    public Log4jRedirect(Logger logger, String prefix) {
        checkNotNull(logger, "logger");
        checkNotNull(prefix, "prefix");
        this.logger = logger;
        this.prefix = prefix;
    }

    @Override
    public void publish(LogRecord record) {
        String message = record.getMessage();
        if (!message.startsWith(prefix + ": ") && !message.startsWith("[" + prefix + "] ")) {
            message = prefix + ": " + message;
        }
        logger.log(toLog4J(record.getLevel()), message, record.getThrown());
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    private static Level toLog4J(java.util.logging.Level level) {
        if (level.intValue() <= java.util.logging.Level.FINE.intValue()) {
            return Level.TRACE;
        } else if (level.intValue() <= java.util.logging.Level.INFO.intValue()) {
            return Level.INFO;
        } else if (level.intValue() <= java.util.logging.Level.WARNING.intValue()) {
            return Level.WARN;
        } else if (level.intValue() <= java.util.logging.Level.SEVERE.intValue()) {
            return Level.ERROR;
        } else {
            return Level.ALL;
        }
    }

}
