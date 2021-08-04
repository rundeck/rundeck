package org.rundeck.core.executions.provenance;

import lombok.*;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ScheduledTrigger
        implements Provenance<ScheduledTrigger.TriggerData>
{
    private TriggerData data;


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class TriggerData {
        private Date scheduleTime;
        private Date fireTime;
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
