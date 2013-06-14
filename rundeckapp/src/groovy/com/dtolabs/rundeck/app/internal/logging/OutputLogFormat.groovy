package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/21/13
 * Time: 5:40 PM
 */
public interface OutputLogFormat {
    String outputBegin()
    String outputEvent(LogEvent entry)
    String outputFinish()
}
