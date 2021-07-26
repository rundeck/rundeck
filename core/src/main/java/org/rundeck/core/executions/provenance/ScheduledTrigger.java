package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Getter
public class ScheduledTrigger
        implements Provenance<ScheduledTrigger.TriggerData>
{
    private final TriggerData data;

    public ScheduledTrigger(final TriggerData data) {
        this.data = data;
    }

    @Getter
    @RequiredArgsConstructor
    public static class TriggerData {
        private final Date scheduleTime;
        private final Date fireTime;
    }

    @Override
    public String toString() {
        return "Schedule Fired " +
               "actual: " + (data.getFireTime()) + ", "
               + "scheduled: " + data.getScheduleTime();
    }

    public static ScheduledTrigger from(Date scheduleTime, Date triggerTime) {
        return new ScheduledTrigger(new ScheduledTrigger.TriggerData(scheduleTime, triggerTime));
    }

}
