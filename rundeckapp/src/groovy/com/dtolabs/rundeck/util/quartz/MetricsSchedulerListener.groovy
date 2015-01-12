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
