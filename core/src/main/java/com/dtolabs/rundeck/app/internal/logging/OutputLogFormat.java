/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.core.logging.LogEvent;

public interface OutputLogFormat {
    String outputBegin();
    String outputEvent(LogEvent entry);
    String outputFinish();
}
