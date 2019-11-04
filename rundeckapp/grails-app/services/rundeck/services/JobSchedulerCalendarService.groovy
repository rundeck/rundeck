package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleCalendarManager

class JobSchedulerCalendarService implements JobScheduleCalendarManager{
    static transactional = false

    def JobScheduleCalendarManager rundeckJobScheduleCalendarManager

    @Override
    Map<String, Object>  getCalendar(String project, String jobId) {
        return rundeckJobScheduleCalendarManager.getCalendar(project, jobId)
    }

    @Override
    boolean isCalendarEnable() {
        return rundeckJobScheduleCalendarManager.isCalendarEnable()
    }

    @Override
    List getProjectCalendars(String project) {
        return rundeckJobScheduleCalendarManager.getProjectCalendars()
    }

    @Override
    List getSystemCalendars() {
        return rundeckJobScheduleCalendarManager.getSystemCalendars()
    }
}

/**
 * Internal manager to schedule calendar
 */
class LocalScheduleCalendarManager implements JobScheduleCalendarManager{

    @Override
    Map<String, Object>  getCalendar(String project, String jobId) {
        return null
    }

    @Override
    boolean isCalendarEnable() {
        return false
    }

    @Override
    List getProjectCalendars(String project) {
        return null
    }

    @Override
    List getSystemCalendars() {
        return null
    }
}
