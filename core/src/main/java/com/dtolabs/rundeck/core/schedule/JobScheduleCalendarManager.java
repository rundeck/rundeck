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

    JobCalendarBase getQuartzCalendar(String project, String jobId);

    boolean isCalendarEnable();

    List getJobCalendarDef(String jobUuid);

    List getProjectCalendarDef(String project, boolean applyAll);

    Map updateJobCalendarDef(String name, String jobUuid);

}

