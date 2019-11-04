package com.dtolabs.rundeck.core.schedule;

import java.util.List;
import java.util.Map;

/**
 * Manage scheduling calendars
 *
 * @author roberto
 * @since 10/26/19
 */
public interface JobScheduleCalendarManager {

    Map<String, Object> getCalendar(String project, String jobId);

    boolean isCalendarEnable();

    List getProjectCalendars(String project);

    List getSystemCalendars();
}
