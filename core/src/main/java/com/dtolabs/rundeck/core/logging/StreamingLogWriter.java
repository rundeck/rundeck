package com.dtolabs.rundeck.core.logging;

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.io.IOException;
import java.util.Map;

/**
 * writes log entries in a streaming manner
 */
public interface StreamingLogWriter {
    /**
     * Open a stream, called before addEvent is called
     * @throws java.io.IOException if an io error occurs
     */
    void openStream() throws IOException;

    /**
     * Add a new event
     * @param event log event
     */
    void addEvent(LogEvent event);

    /**
     * Close the stream.
     */
    void close();
}
