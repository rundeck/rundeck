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

package com.dtolabs.rundeck.util.quartz

import com.codahale.metrics.Counter
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.listeners.SchedulerListenerSupport

/**
 * Use a counter to track how many jobs are scheduled by quartz
 */
class MetricsSchedulerListener extends SchedulerListenerSupport{
    Counter counter;

    MetricsSchedulerListener(Counter counter) {
        this.counter = counter
    }

    @Override
    void triggerFinalized(Trigger trigger) {
        counter.dec()
    }

    @Override
    void jobScheduled(Trigger trigger) {
        counter.inc()
    }

    @Override
    void jobUnscheduled(TriggerKey triggerKey) {
        counter.dec()
    }

}
