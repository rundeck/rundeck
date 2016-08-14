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
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import rundeck.services.execution.ValueHolder

/**
 * Counts line per-node and returns the largest so far
 */
class NodeCountingLogWriter extends FilterStreamingLogWriter implements ValueHolder<Long> {
    LargestNodeLinesCounter counter = new LargestNodeLinesCounter()

    NodeCountingLogWriter(StreamingLogWriter writer) {
        super(writer)
    }

    @Override
    void addEvent(final LogEvent event) {
        getWriter().addEvent(event)
        if (event.eventType == LogUtil.EVENT_TYPE_LOG && event.metadata?.node && event.message != null) {
            counter.nodeLogged(event.metadata.node, event.message.split('\n').length)
        }
    }

    @Override
    Long getValue() {
        return counter.value
    }
}
