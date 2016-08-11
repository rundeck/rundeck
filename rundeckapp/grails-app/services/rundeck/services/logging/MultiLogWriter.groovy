/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * A StreamingLogWriter which writes to a list of multiple writers.
 * Each writer's {@link StreamingLogWriter#openStream()} will be called, but
 * if an error occurs it will not be enabled.
 * Each enabled writer will then be passed events via addEvent and
 * closed via close().
 */
class MultiLogWriter implements StreamingLogWriter {
    public static final Logger log = Logger.getLogger(MultiLogWriter.class)
    List<StreamingLogWriter> writers

    MultiLogWriter (List<StreamingLogWriter> writers) {
        this.writers = new ArrayList<StreamingLogWriter>(writers)
    }

    @Override
    void openStream() {
        writers*.openStream()
    }

    @Override
    void addEvent(LogEvent event) {
        writers*.addEvent(event)
    }

    @Override
    void close() {
        writers*.close()
    }
}
