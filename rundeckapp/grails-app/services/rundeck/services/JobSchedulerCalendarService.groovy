package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobCalendarBase
import com.dtolabs.rundeck.core.schedule.JobScheduleCalendarManager

class JobSchedulerCalendarService implements JobScheduleCalendarManager{
    static transactional = false

    def JobScheduleCalendarManager rundeckJobScheduleCalendarManager

    @Override
    JobCalendarBase getCalendar(String project, String jobId) {
        return rundeckJobScheduleCalendarManager.getCalendar(project, jobId)
    }

    @Override
    boolean isCalendarEnable() {
        return rundeckJobScheduleCalendarManager.isCalendarEnable()
    }

}

/**
 * Internal manager to schedule calendar
 */
class LocalScheduleCalendarManager implements JobScheduleCalendarManager{

    @Override
    JobCalendarBase getCalendar(String project, String jobId) {
        return null
    }

    @Override
    boolean isCalendarEnable() {
        return false
    }

}
