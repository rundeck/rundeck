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

package rundeck.services.scm

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Can be overridden to filter events to pass to the sub listener, default action is to pass all events
 */
class FilterJobChangeListener implements JobChangeListener {
    JobChangeListener listener

    FilterJobChangeListener(final JobChangeListener listener) {
        this.listener = listener
    }

    @Override
    void jobChangeEvent(final JobChangeEvent event, final JobSerializer serializer) {
        listener?.jobChangeEvent(event, serializer)
    }
}
