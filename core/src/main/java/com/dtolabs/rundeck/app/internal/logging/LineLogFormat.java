/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.core.logging.LogEvent;

import java.io.File;

public interface LineLogFormat {
    FormatItem parseLine(String line);
    long seekBackwards(File file, int count);
    public static interface FormatItem {
        boolean getFileEnd();

        boolean getLineComplete();

        boolean getFileStart();

        boolean isInvalid();

        LogEvent getEntry();

        String getPartial();
    }
}
