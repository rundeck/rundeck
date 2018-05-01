/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.app.internal.logging.DefaultLogEvent
import com.dtolabs.rundeck.core.execution.workflow.state.StateUtils
import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Uses a Map of step number (string) to step label, and inserts `step.label` value if
 * an event has metadata value `stepctx` and the first context number has an entry in the labels map
 */
class StepLabellingStreamingLogWriter extends FilterStreamingLogWriter {
    Map<String, String> labels

    StepLabellingStreamingLogWriter(final StreamingLogWriter writer, final Map<String, String> labels) {
        super(writer)
        this.labels = labels
    }

    @Override
    void addEvent(LogEvent event) {
        if (labels && event.metadata?.stepctx) {
            try {
                def stepid = event.metadata.stepctx.split('/', 2)[0]
                def stepnum = StateUtils.stepContextIdFromString(stepid).step.toString()
                if (stepnum && labels.get(stepnum)) {
                    event = DefaultLogEvent.with(event, ['step.label': labels.get(stepnum)])
                }
            } catch (IllegalArgumentException ignored) {
            }

        }
        super.addEvent(event)
    }
}
