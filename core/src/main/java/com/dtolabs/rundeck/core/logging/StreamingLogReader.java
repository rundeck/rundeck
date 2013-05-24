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

/*
 * StreamingLogReader.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/22/13 6:54 PM
 * 
 */

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

/** Readers log entries */
public interface StreamingLogReader extends LogEntryIterator, Closeable {
    /**
     * Read log entries starting at the specified offset
     *
     * @param offset
     *
     * @return
     */
    void openStream(Long offset) throws IOException;

    /**
     * Return the total size
     *
     * @return
     */
    long getTotalSize();

    /**
     * Return the last modification time of the log (e.g. last log entry time, or null if not modified)
     *
     * @return
     */
    Date getLastModified();
}
