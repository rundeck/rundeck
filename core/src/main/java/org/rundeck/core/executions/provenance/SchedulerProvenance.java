package org.rundeck.core.executions.provenance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter

public class SchedulerProvenance
        implements Provenance<SchedulerProvenance.ScheduleData>
{
    private final ScheduleData data;

    public SchedulerProvenance(final ScheduleData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Schedule "
               + (data.name != null ? data.name : "")
               + (data.id != null ? (" ID: " + data.id) : "")
               + " Cron: "
               + data.crontabExpression;
    }

    @Getter
    @RequiredArgsConstructor
    static class ScheduleData {
        private final String name;
        private final String id;
        private final String crontabExpression;
    }

    public static SchedulerProvenance from(String name, String id, String crontab) {
        return new SchedulerProvenance(new ScheduleData(name, id, crontab));
    }

}
