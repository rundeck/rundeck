package org.rundeck.core.executions.provenance;

import lombok.*;

@Getter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SchedulerProvenance
        implements Provenance<SchedulerProvenance.ScheduleData>
{
    private ScheduleData data;

    @Override
    public String toString() {
        return "Schedule "
               + (data.type != null ? "[" + data.type + "]" : "")
               + (data.name != null ? data.name : "")
               + (data.id != null ? (" ID: " + data.id) : "")
               + " Cron: "
               + data.crontabExpression;
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Getter
    static class ScheduleData {
        private String type;
        private String name;
        private String id;
        private String crontabExpression;
    }

    public static SchedulerProvenance from(String type, String name, String id, String crontab) {
        return new SchedulerProvenance(new ScheduleData(type, name, id, crontab));
    }

}
