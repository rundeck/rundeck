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

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/29/13
 * Time: 4:36 PM
 */
class EventStreamingLogWriter extends FilterStreamingLogWriter {
    Closure onAddEvent
    Closure onClose
    Closure onOpenStream

    EventStreamingLogWriter(StreamingLogWriter writer) {
        super(writer)
    }

    @Override
    void openStream() throws IOException {
        super.openStream()
        if (null != onOpenStream) {
            onOpenStream.call()
        }
    }

    @Override
    void close() {
        super.close()
        if (null != onClose) {
            onClose.call()
        }
    }

    void onClose(Closure closure) {
        this.onClose = closure
    }

    void onAddEvent(Closure closure) {
        this.onAddEvent = closure
    }

    void onOpenStream(Closure closure) {
        this.onOpenStream = closure
    }

    @Override
    void addEvent(LogEvent event) {
        super.addEvent(event)
        if (null != onAddEvent) {
            onAddEvent.call(event)
        }
    }
}
