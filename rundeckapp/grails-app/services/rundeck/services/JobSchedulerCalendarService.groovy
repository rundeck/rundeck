package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobCalendarBase
import com.dtolabs.rundeck.core.schedule.JobScheduleCalendarManager
import rundeck.ScheduledExecution

class JobSchedulerCalendarService implements JobScheduleCalendarManager{
    static transactional = false

    JobScheduleCalendarManager rundeckJobScheduleCalendarManager

    @Override
    JobCalendarBase getQuartzCalendar(String project, String jobId) {
        return rundeckJobScheduleCalendarManager.getQuartzCalendar(project, jobId)
    }

    @Override
    boolean isCalendarEnable() {
        return rundeckJobScheduleCalendarManager.isCalendarEnable()
    }

    @Override
    List getJobCalendarDef(String jobUuid) {
        return rundeckJobScheduleCalendarManager.getJobCalendarDef(jobUuid)
    }

    @Override
    List getProjectCalendarDef(String project, boolean applyAll) {
        return rundeckJobScheduleCalendarManager.getProjectCalendarDef(project, applyAll)
    }

    @Override
    Map updateJobCalendarDef(String name, String jobUuid) {
        return rundeckJobScheduleCalendarManager.updateJobCalendarDef(name, jobUuid)
    }

    def setJobCalendars(ScheduledExecution se){
        if(this.isCalendarEnable()){
            def calendarNames = this.getJobCalendarDef(se.uuid)
            se.calendars = calendarNames
        }
    }
}

/**
 * Internal manager to schedule calendar
 */
class LocalScheduleCalendarManager implements JobScheduleCalendarManager{

    @Override
    JobCalendarBase getQuartzCalendar(String project, String jobId) {
        return null
    }

    @Override
    boolean isCalendarEnable() {
        return false
    }

    @Override
    List getJobCalendarDef(String jobUuid) {
        return null
    }

    @Override
    List getProjectCalendarDef(String project, boolean applyAll) {
        return null
    }

    @Override
    Map updateJobCalendarDef(String name, String jobUuid) {
        return false
    }
}
