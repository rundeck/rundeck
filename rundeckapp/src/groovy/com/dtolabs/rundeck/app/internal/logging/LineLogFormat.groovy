package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 6:05 PM
 */
public interface LineLogFormat {
    FormatItem parseLine(String line)

    static interface FormatItem {
        boolean getFileEnd()
        boolean getLineComplete()
        boolean getFileStart()
        boolean isInvalid()
        LogEvent getEntry()
        String getPartial()
    }
}
