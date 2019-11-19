package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobCalendarBase
import grails.gorm.transactions.Transactional
import org.quartz.Calendar
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.springframework.context.ApplicationContextAware
import rundeck.Project
import rundeck.ScheduleDef
import rundeck.ScheduledExecution
import rundeck.controllers.ScheduleDefYAMLException
import rundeck.quartzjobs.ExecutionJob

@Transactional
class SchedulerService implements ApplicationContextAware{

    Scheduler quartzScheduler
    FrameworkService frameworkService
    JobSchedulerCalendarService jobSchedulerCalendarService
    def messageSource

    /**
     * It retrieves all of the schedules that match with the criteria expressed on its params
     * @param project returned schedules belong to this project
     * @param containsName returned schedules contains this string on its name
     * @param paginationParams
     * @return List<ScheduleDef>
     */
    def retrieveProjectSchedulesDefinitionsWithFilters(String projectName, String containsName, Map<String, Integer> paginationParams) {
        if(!containsName) containsName = "";
        def results = ScheduleDef.createCriteria().list (max: paginationParams.max, offset: paginationParams.offset) {
            and {
                like("name", "%"+containsName+"%")
                eq("project", projectName)
            }
            order("name", "asc")
        }
        [
                totalRecords    :results.getTotalCount(),
                schedules       :results
        ]
    }

    /**
     * It retrieves all of the schedules that match with the projectName
     * @param project returned schedules belong to this project
     * @return List<ScheduleDef>
     */
    def findAllByProject(String projectName) {
        def results = ScheduleDef.findAllByProject(projectName)
        return results
    }

    def reassociate(scheduleDefId, jobUuidsToAssociate, jobUuidsToDeassociate) {
        def scheduleDef = ScheduleDef.findById(scheduleDefId);

        jobUuidsToAssociate?.each { jobUuid ->
            def scheduledExecution = ScheduledExecution.findByUuid(jobUuid);

            scheduledExecution.addToScheduleDefinitions(scheduleDef);

            try {
                scheduledExecution.save(failOnError: true)
                this.handleScheduleDefinitions(scheduledExecution, true);
            }catch(Exception ex){
                log.error("Persist ScheduledExecution ${scheduleDef} failed when associating a new scheduleDef:",ex)
            }
        }

        jobUuidsToDeassociate?.each { jobUuid ->
            def scheduledExecution = ScheduledExecution.findByUuid(jobUuid);

            scheduledExecution.removeFromScheduleDefinitions(scheduleDef);

            try {
                scheduledExecution.save(failOnError: true)
                this.handleScheduleDefinitions(scheduledExecution, true);
            }catch(Exception ex){
                log.error("Persist ScheduledExecution ${scheduleDef} failed when deassociating a scheduleDef:",ex)
            }
        }
    }

    def persistScheduleDef(Map scheduleDef){
        def currentSchedule = null
        def newSchedule = ScheduleDef.fromMap(scheduleDef)
        def failed = false
        if(scheduleDef.id){
            currentSchedule = ScheduleDef.findById(scheduleDef.id)
            updateScheduleDef(currentSchedule, newSchedule)
        }else{
            currentSchedule = newSchedule
        }
        if(!currentSchedule.validate()){
            failed = true
        }else{
            currentSchedule.save(true)
            for(ScheduledExecution se : currentSchedule.scheduledExecutions){
                handleScheduleDefinitions(se, true)
            }
        }
        return ["schedule":currentSchedule, "failed": failed]
    }

    def delete(Map scheduleMap){
        ScheduleDef sd = ScheduleDef.findById(scheduleMap.id)
        def scheduledExecutions = sd.scheduledExecutions.findAll()
        scheduledExecutions.each {
            it.removeFromScheduleDefinitions(sd)
            handleScheduleDefinitions(it, true)
        }
        sd.delete()
    }

    def updateScheduleDef(ScheduleDef oldSchedule, newSchedule){
        oldSchedule.crontabString = newSchedule.crontabString
        newSchedule.parseCrontabString(newSchedule.crontabString)
        oldSchedule.seconds = newSchedule.seconds
        oldSchedule.minute = newSchedule.minute
        oldSchedule.hour = newSchedule.hour
        oldSchedule.dayOfMonth = newSchedule.dayOfMonth
        oldSchedule.month = newSchedule.month
        oldSchedule.dayOfWeek = newSchedule.dayOfWeek
        oldSchedule.year = newSchedule.year
        oldSchedule.name = newSchedule.name
        oldSchedule.description = newSchedule.description
        oldSchedule.type = newSchedule.type
        return oldSchedule
    }

    /**
     * It removes from quartz scheduler all the schedules that are no longer associated to the job
     * @param ScheduledExecution
     */
    def cleanRemovedScheduleDef(ScheduledExecution scheduledExecution){
        def toDelete = getTriggerNamesToRemoveFromQuartz(scheduledExecution)
        if(toDelete) {
            toDelete?.each {
                quartzScheduler.unscheduleJob(TriggerKey.triggerKey(it, scheduledExecution.generateJobGroupName()))
            }
        }
    }

    /**
     * It returns all the quartz triggers that are no longer associated to the job
     * @param ScheduledExecution
     * @return List<String> quartz job triggers
     */
    def getTriggerNamesToRemoveFromQuartz(scheduledExecution){
        def toDelete = []
        def jobScheduledDefinitionCrons = scheduledExecution?.getJobScheduleDefinitionMap()
        List<Trigger> triggers = quartzScheduler.getTriggersOfJob(JobKey.jobKey(scheduledExecution.generateJobScheduledName(), scheduledExecution.generateJobGroupName()))
        if(jobScheduledDefinitionCrons){
            triggers.each{ Trigger trigger ->
                if(!jobScheduledDefinitionCrons.containsKey(trigger.getKey().name)){
                    toDelete << trigger.getKey().name
                }
            }
        }else{
            triggers.each { Trigger trigger ->
                if(trigger.getKey().name != scheduledExecution?.generateJobScheduledName()){
                    toDelete << trigger.getKey().name
                }
            }
        }
        return toDelete
    }

    /**
     * It handles the cleaning of no longer associated schedule definitions and triggers the new ones
     * @param ScheduledExecution
     * @return boolean it returns true if at least one job was scheduled
     */
    def handleScheduleDefinitions(ScheduledExecution scheduledExecution, isUpdate = false){
        if(scheduledExecution){
            def calendar = handleJobCalendar(scheduledExecution)

            def nextTime = null
            def calendarName = null
            if(calendar){
                //register calendar quartz if is needed
                this.registerCalendar(calendar )
                calendarName = calendar.registerName
            }

            cleanRemovedScheduleDef(scheduledExecution)
            def jobDetail = quartzScheduler.getJobDetail(JobKey.jobKey(scheduledExecution.generateJobScheduledName(), scheduledExecution.generateJobGroupName()))
            if(!jobDetail){
                jobDetail = createJobDetail(scheduledExecution)
            }
            if(scheduledExecution.scheduleDefinitions){
                Set triggerList = []
                scheduledExecution.getJobScheduleDefinitionMap().each { triggerName, cronExpression ->
                    if(!quartzScheduler.checkExists(TriggerKey.triggerKey(triggerName, scheduledExecution.generateJobGroupName()))){
                        triggerList << createTrigger(scheduledExecution, calendarName, cronExpression, triggerName)
                        log.info("scheduling new trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: ${triggerName}")
                    }else if(isUpdate){
                        triggerList << createTrigger(scheduledExecution, calendarName, cronExpression, triggerName)
                        log.info("scheduling updated trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: ${triggerName}")
                    }
                }
                //scheduledExecution.scheduled = true
                quartzScheduler.scheduleJob(jobDetail, triggerList, true)
                nextTime = nextExecutionTime(scheduledExecution)

            }else if(scheduledExecution.generateCrontabExression() && scheduledExecution.shouldScheduleExecution()){
                Set triggerList = []
                def trigger = createTrigger(scheduledExecution, calendarName)
                triggerList << trigger

                try{
                    nextTime = quartzScheduler.scheduleJob(jobDetail, triggerList, isUpdate)
                }catch(Exception e){
                    log.error(e.getMessage())
                }

                log.info("scheduling trigger for job ${scheduledExecution.generateJobScheduledName()} in project ${scheduledExecution.project} ${scheduledExecution.extid}: " +
                        "${scheduledExecution.generateJobScheduledName()}")

            }

            if(calendar) {
                this.cleanCalendar(calendar)
            }

            return [scheduled   : true,
                    nextTime    : nextTime
            ]
        }
        return [scheduled   : false]
    }

    Trigger createTrigger(String jobName, String jobGroup, String cronExpression, int priority = 5, calendarName = null) {
        Trigger trigger
        try {
            trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroup)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withPriority(priority)
                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    Trigger createTrigger(ScheduledExecution se, String calendarName = null, cronExpression = null, triggerName = null) {
        Trigger trigger
        if(!cronExpression){
            cronExpression = se.generateCrontabExression()
        }
        try {

            if(se.timeZone){
                trigger = TriggerBuilder.newTrigger().withIdentity(triggerName? triggerName:se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(TimeZone.getTimeZone(se.timeZone)))
                        .modifiedByCalendar(calendarName)
                        .build()
            }else {
                trigger = TriggerBuilder.newTrigger().withIdentity(triggerName? triggerName:se.generateJobScheduledName(), se.generateJobGroupName())
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .modifiedByCalendar(calendarName)
                        .build()
            }
        } catch (java.text.ParseException ex) {
            log.error("Failed creating trigger", ex)
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    JobDetail createJobDetail(ScheduledExecution se, String jobname, String jobgroup) {
        def jobDetailBuilder = JobBuilder.newJob(ExecutionJob)
                .withIdentity(jobname, jobgroup)
                .withDescription(se.description)
                .usingJobData(new JobDataMap(createJobDetailMap(se)))
        return jobDetailBuilder.build()
    }

    JobDetail createJobDetail(ScheduledExecution se) {
        return createJobDetail(se, se.generateJobScheduledName(), se.generateJobGroupName())
    }

    Map createJobDetailMap(ScheduledExecution se) {
        Map data = [:]
        data.put("scheduledExecutionId", se.id.toString())
        data.put("rdeck.base", frameworkService.getRundeckBase())

        if(se.scheduled){
            data.put("userRoles", se.userRoleList)
            if(frameworkService.isClusterModeEnabled()){
                data.put("serverUUID", frameworkService.getServerUUID())
            }
        }

        return data
    }

    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    Date nextExecutionTime(ScheduledExecution se, boolean require=false) {
        if(!se.scheduled && !se.scheduleDefinitions){
            return new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }
        if(!require && (!se.scheduleEnabled || !se.executionEnabled)){
            return null
        }
        def dates = []

        def triggers = quartzScheduler.getTriggersOfJob(JobKey.jobKey(se.generateJobScheduledName(), se.generateJobGroupName()))
        if(triggers){
            triggers.each {
                if(it.getNextFireTime())
                    dates << it.getNextFireTime()
            }
            if(dates){
                Collections.sort(dates)
                return dates.get(0)
            }
        }else if (frameworkService.isClusterModeEnabled() &&
                se.serverNodeUUID != frameworkService.getServerUUID() || require) {
            //guess next trigger time for the job on the assigned cluster node
            def value= tempNextExecutionTime(se)
            return value
        } else {
            return null;
        }
    }

    /**
     * Return the Date for the next execution time for a scheduled job
     * @param se
     * @return
     */
    Date tempNextExecutionTime(ScheduledExecution se){
        def trigger = createTrigger(se)
        return trigger.getFireTimeAfter(new Date())
    }

    /**
     * It parses the input stream into schedule definitions
     * @param input either an inputStream, a File, or a String
     */
    def parseUploadedFile (input, project, update){
        def scheduleDefs
        def success = true
        try{
            scheduleDefs = input.decodeScheduleDefinitionYAML()
        } catch (ScheduleDefYAMLException ex){
            success = false
            log.error("Error parsing uploaded schedule definition Yaml: ${ex}")
            log.warn("Error parsing schedule definition Yaml", ex)
            return [errors  : ["${ex}"],
                    success : success]
        }catch (Exception e) {
            success = false
            log.error("Error parsing uploaded schedule definition Yaml", e)
            return [errors  : ["${e.getLocalizedMessage()}"],
                    success : success]
        }
        def errors = []
        def schedulesToSave = []
        scheduleDefs.each{ scheduleDef ->
            def existingSchedule = ScheduleDef.findByNameAndProject(scheduleDef.name, project)
            if(existingSchedule){
                scheduleDef = updateScheduleDef(existingSchedule, scheduleDef)
            }else{
                scheduleDef.project = project
            }
            schedulesToSave << scheduleDef
            if(!scheduleDef.validate()){
                errors << scheduleDef.errors.allErrors.collect {messageSource.getMessage(it,Locale.default)}.join(", ")
            }
        }
        if(errors.isEmpty()){
            schedulesToSave.each {
                it.save()
            }
        }else{
            success = false
        }

        return [scheduleDefs    : schedulesToSave,
                errors          : errors,
                success         : success]
    }

    /**
     * It checks whether a calendar should be applied to the job or not
     * @param ScheduledExecution
     */
    JobCalendarBase handleJobCalendar(ScheduledExecution se){
        if(jobSchedulerCalendarService.isCalendarEnable()){
            JobCalendarBase calendar = jobSchedulerCalendarService.getCalendar(se.project, se.uuid)
            if(calendar){
                return calendar
            }
        }
        return null
    }

    /**
     * add a calendar into the quartz scheduler
     * @param JobCalendarBase calendar
     */
    def registerCalendar(JobCalendarBase calendar){
        if(!quartzScheduler.getCalendar(calendar.registerName)) {
            quartzScheduler.addCalendar(calendar.registerName, calendar, false, false)

        }
    }

    /**
     * remove old calendar on quartz scheduler
     * @param JobCalendarBase calendar
     */
    def cleanCalendar(JobCalendarBase calendar){

        def removeCalendar = quartzScheduler.getCalendarNames().find {
            it.contains(calendar.getName()) && it != calendar.getRegisterName()
        }

        if(removeCalendar!=null){
            //removed old asigned calendar (calendar definition changed)
            try{
                log.debug("deleting calendar ${removeCalendar}")
                quartzScheduler.deleteCalendar(removeCalendar)
            }catch(Exception e){
                log.debug(e.message)
            }
        }
    }


    def persistScheduleDefFromMap(scheduleDefMap, project){
        def errors = []
        def scheduleDef = ScheduleDef.fromMap(scheduleDefMap)
        def existingSchedule = ScheduleDef.findByNameAndProject(scheduleDef.name, project)
        if(existingSchedule){
            scheduleDef = updateScheduleDef(existingSchedule, scheduleDef)
        }else{
            scheduleDef.project = project
        }
        if(!scheduleDef.validate()){
            errors << scheduleDef.errors.allErrors.collect {messageSource.getMessage(it,Locale.default)}.join(", ")
        }else{
            scheduleDef.save()
        }
        return [errors: errors]
    }



    /**
     * Return calendars from a scheduled job
     * @param se
     * @return
     */
    def hasCalendars(ScheduledExecution se) {
        if (!se.scheduled) {
            return null
        }

        if (!jobSchedulerCalendarService.isCalendarEnable()) {
            return null
        }

        def calendar = jobSchedulerCalendarService.getCalendar(se.project, se.uuid)
        if (calendar) {
            return calendar.toString()
        } else {
            return null
        }
    }

    /**
     * It will delete all of the schedule definitions inside the list
     * @param schedulesId List of schedules id to be deleted
     * @param projectName
     * @return
     */
    def massiveScheduleDelete(schedulesId, projectName){
        def totalDeleted = 0
        def totalRecieved = 0
        if(schedulesId && projectName) {
            totalRecieved = schedulesId.size()
            def schedulesToDelete = []
            schedulesId.each { scheduleId ->
                schedulesToDelete << ScheduleDef.findByIdAndProject(scheduleId, projectName)
            }
            totalDeleted = schedulesToDelete.size()
            schedulesToDelete.each {
                delete([id: it.id])
            }
        }
        [
                messages:  ["Total Schedule definitions to be deleted : ${totalRecieved}","Total Schedule definitions deleted : ${totalDeleted}"],
                success:    true
        ]
    }

    def findJobsAssociatedToSchedule(project, scheduleName, paginationParams){
        def results = ScheduledExecution.createCriteria().list (max: paginationParams.max, offset: paginationParams.offset) {
            createAlias('scheduleDefinitions', 'sd')
            and {
                eq("sd.name", scheduleName)
                eq("sd.project", project)
            }
            order("jobName", "asc")
        }
        [
                totalRecords        :results.getTotalCount(),
                scheduledExecutions :results
        ]
    }

}