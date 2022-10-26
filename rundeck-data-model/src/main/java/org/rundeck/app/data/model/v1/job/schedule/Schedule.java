package org.rundeck.app.data.model.v1.job.schedule;

public interface Schedule {
    String getMinute();
    String getHour();
    String getDayOfMonth();
    String getMonth();
    String getDayOfWeek();
    String getSeconds();
    String getYear();
    String getCrontabString();
}
