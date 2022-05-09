package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.rundeck.plugin.jobs.ExecutionStatusJob
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.rundeck.core.projects.ProjectConfigurable

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class UpdateModeProjectService implements ProjectConfigurable {

    public static final String CONF_PROJECT_EXECUTION_LATER_ENABLE = 'project.later.executions.enable'
    public static final String CONF_PROJECT_EXECUTION_LATER_DISABLE = 'project.later.executions.disable'

    public static final String CONF_PROJECT_EXECUTION_LATER_ENABLE_VALUE = 'project.later.executions.enable.value'
    public static final String CONF_PROJECT_EXECUTION_LATER_DISABLE_VALUE = 'project.later.executions.disable.value'

    public static final String CONF_PROJECT_SCHEDULE_LATER_ENABLE = 'project.later.schedule.enable'
    public static final String CONF_PROJECT_SCHEDULE_LATER_DISABLE = 'project.later.schedule.disable'

    public static final String CONF_PROJECT_SCHEDULE_LATER_ENABLE_VALUE = 'project.later.schedule.enable.value'
    public static final String CONF_PROJECT_SCHEDULE_LATER_DISABLE_VALUE = 'project.later.schedule.disable.value'

    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    public static final String CONF_PROJECT_DISABLE_EXECUTION = 'project.disable.executions'
    public static final String CONF_PROJECT_DISABLE_SCHEDULE = 'project.disable.schedule'
    public static final String EXECUTIONS_JOB_GROUP_NAME = 'ProjectExecutionLater'

    public  List<Property> ProjectConfigProperties = []

    def scheduledExecutionService
    def frameworkService
    def Scheduler quartzScheduler

    void init() throws Exception {

    }

    public static final LinkedHashMap<String, String> ConfigPropertiesMapping = [
            executionLaterEnable: CONF_PROJECT_EXECUTION_LATER_ENABLE,
            executionLaterEnableValue: CONF_PROJECT_EXECUTION_LATER_ENABLE_VALUE,
            executionLaterDisable: CONF_PROJECT_EXECUTION_LATER_DISABLE,
            executionLaterDisableValue: CONF_PROJECT_EXECUTION_LATER_DISABLE_VALUE,
            scheduledLaterEnable: CONF_PROJECT_SCHEDULE_LATER_ENABLE,
            scheduledLaterEnableValue: CONF_PROJECT_SCHEDULE_LATER_ENABLE_VALUE,
            scheduledLaterDisable: CONF_PROJECT_SCHEDULE_LATER_DISABLE,
            scheduledLaterDisableValue: CONF_PROJECT_SCHEDULE_LATER_DISABLE_VALUE,
    ]
    @Override
    Map<String, String> getCategories() {
        [executionLaterEnable: 'executionMode',
         executionLaterEnableValue: 'executionMode',
         executionLaterDisable: 'executionMode',
         executionLaterDisableValue: 'executionMode',
         scheduledLaterEnable: 'executionMode',
         scheduledLaterEnableValue: 'executionMode',
         scheduledLaterDisable: 'executionMode',
         scheduledLaterDisableValue: 'executionMode',
        ]
    }

    @Override
    List<Property> getProjectConfigProperties() {
        loadProperties()
        ProjectConfigProperties
    }

    @Override
    Map<String, String> getPropertiesMapping() { ConfigPropertiesMapping }

    def loadProperties() {
        List<Property> projectConfigProperties = []

        Map<String, Object> renderingNow = new HashMap<>()
        renderingNow.put('booleanTrueDisplayValueClass', 'text-warning')
        renderingNow.put(StringRenderingConstants.GROUP_NAME, 'Enable/Disable Now')

        Map<String, Object> renderingExecution = new HashMap<>()
        renderingExecution.put('projectConfigCategory', 'gui')
        renderingExecution.put(StringRenderingConstants.GROUP_NAME, 'Enable/Disable Execution Later')

        Map<String, Object> renderingScheduled = new HashMap<>()
        renderingScheduled.put('projectConfigCategory', 'gui')
        renderingScheduled.put(StringRenderingConstants.GROUP_NAME, 'Enable/Disable Schedule Later')


        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    booleanType "executionLaterDisable"
                    title "Disable Execution Later"
                    required(false)
                    defaultValue null
                    renderingOptions renderingExecution
                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    string "executionLaterDisableValue"
                    title 'Disable Execution after time'
                    description "Time to disable execution : 2h, 2m, 30s"
                    required(false)
                    defaultValue '0'
                    renderingOptions renderingExecution
                    validator TIME_VALIDATOR

                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    booleanType "executionLaterEnable"
                    title "Enable Execution Later"
                    required(false)
                    defaultValue null
                    renderingOptions renderingExecution
                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    string "executionLaterEnableValue"
                    title 'Enable Execution after time'
                    description "Time to enable execution : 2h, 2m, 30s"
                    required(false)
                    defaultValue null
                    renderingOptions renderingExecution
                    validator TIME_VALIDATOR

                }.build()
        )


        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    booleanType "scheduledLaterDisable"
                    title "Disable Scheduled Later"
                    required(false)
                    defaultValue null
                    renderingOptions renderingScheduled
                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    string "scheduledLaterDisableValue"
                    title  'Disable Scheduled after Time'
                    description "Time to disable schedule : 2h, 2m, 30s"
                    required(false)
                    defaultValue null
                    renderingOptions renderingScheduled
                    validator TIME_VALIDATOR
                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    booleanType "scheduledLaterEnable"
                    title "Enable Scheduled Later"
                    required(false)
                    defaultValue null
                    renderingOptions renderingScheduled
                }.build()
        )

        projectConfigProperties.add(
                PropertyBuilder.builder().with {
                    string "scheduledLaterEnableValue"
                    title  'Enable Scheduled after time'
                    description "Time to enable schedule : 2h, 2m, 30s"
                    required(false)
                    defaultValue null
                    renderingOptions renderingScheduled
                    validator TIME_VALIDATOR
                }.build()
        )

        this.ProjectConfigProperties = projectConfigProperties

    }


    public static final PropertyValidator TIME_VALIDATOR = new PropertyValidator() {
        public boolean isValid(String value) throws ValidationException {
            PluginUtil.validateTimeDuration(value)
        }
    };

    boolean saveExecutionLaterSettings(String project, Properties properties) {
        String executionLaterPath="extraConfig/executionLater.properties"
        IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)

        def isExecutionDisabledNow = properties[CONF_PROJECT_DISABLE_EXECUTION] == 'true'
        def isScheduleDisabledNow = properties[CONF_PROJECT_DISABLE_SCHEDULE] == 'true'
        def removeExecutionLater = false
        def removeScheduleLater = false

        //get saved value
        def settings = getScheduleExecutionLater(rundeckProject, executionLaterPath)
        if(settings){
            if(settings.global){
                def isExecutionDisabled = settings.global.executionDisable
                def isScheduleDisabled = settings.global.scheduleDisable

                if(isExecutionDisabled !=isExecutionDisabledNow){
                    removeExecutionLater = true
                }

                if(isScheduleDisabled !=isScheduleDisabledNow){
                    removeScheduleLater = true
                }
            }
        }

        def result = [:]

        def executionLater = getExecutionLaterValues(rundeckProject, executionLaterPath)
        def scheduleLater = getScheduleLaterValues(rundeckProject, executionLaterPath)

        def newExecutionSettings = convertToMap(properties,"project.later.executions")
        def newScheduleSettings = convertToMap(properties,"project.later.schedule")

        //compare
        def compareLater = { Map savedSettings, Map newSettings->
            boolean newData = false

            if(newSettings.isEmpty()){
                return false
            }

            if(!savedSettings){
                newData = true
            }else{
                //compare if something change to save it
                newData = compareLaterData(savedSettings, newSettings)
            }

            if(newData){
                newSettings.active = true
                Date date = new Date()
                DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                newSettings.dateSaved = dateFormat.format(date)
                return true
            }

            return false
        }

        boolean save = false

        if(removeExecutionLater){
            //remove schedule job
            save = true
            removeScheduleJob(project, "executions")
            if(executionLater?.active){
                executionLater.active = false
            }

        }

        if(compareLater(executionLater,newExecutionSettings )){
            save = true
            result.executions = newExecutionSettings
            scheduleExecutionsLaterJob(project, "executions", [project: project,
                                                               type: "executions",
                                                               rundeckProject: rundeckProject,
                                                               config: newExecutionSettings,
                                                               editProjectService: this])

        }else{
            result.executions = executionLater
        }


        if(removeScheduleLater){
            //remove schedule job
            save = true
            removeScheduleJob(project, "schedule")

            if(scheduleLater?.active){
                scheduleLater.active = false
            }
        }

        if(compareLater(scheduleLater,newScheduleSettings )){
            save = true
            result.schedule = newScheduleSettings
            scheduleExecutionsLaterJob(project, "schedule", [project: project,
                                                             type: "schedule",
                                                             rundeckProject: rundeckProject,
                                                             config: newScheduleSettings,
                                                             editProjectService: this])

        }else{
            result.schedule = scheduleLater
        }

        result.global = [executionDisable: isExecutionDisabledNow, scheduleDisable: isScheduleDisabledNow]

        if(save){
            saveExecutionLater(rundeckProject, executionLaterPath , result)
        }
        save
    }

    def getScheduleExecutionLater(def rundeckProject, String path){

        if(rundeckProject.existsFileResource(path)){

            Map executionLater
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            try {
                rundeckProject.loadFileResource(path, output)
                executionLater =  new JsonSlurper().parseText(output.toString())
            } catch (Exception e) {
                return null
            }

            return executionLater
        }
    }

    def getExecutionLaterValues(def rundeckProject, String path){

        if(rundeckProject.existsFileResource(path)){

            Map executionLater
            ByteArrayOutputStream output = new ByteArrayOutputStream()
            try {
                rundeckProject.loadFileResource(path, output)
                executionLater =  new JsonSlurper().parseText(output.toString())
            } catch (Exception e) {
                return null
            }

            Map result = executionLater.executions

            return result
        }
    }

    def getScheduleLaterValues(def rundeckProject, String path){

        if(rundeckProject.existsFileResource(path)) {
            Map executionLater

            ByteArrayOutputStream output = new ByteArrayOutputStream()
            try {
                rundeckProject.loadFileResource(path, output)
                executionLater = new JsonSlurper().parseText(output.toString())
            } catch (Exception e) {
                return null
            }

            Map result = executionLater.schedule

            return result
        }
    }

    def saveExecutionLater(def rundeckProject, String path, def data){
        def mapAsJson = JsonOutput.toJson(data)
        rundeckProject.storeFileResource(path, new ByteArrayInputStream(mapAsJson.getBytes('UTF-8')))
    }

    def convertToMap(Properties properties, String path){
        def newSettings = [:]
        Map result = [:]

        properties.forEach{ key, value ->
            if(key.toString().startsWith(path)){
                newSettings.put(key.toString().replaceFirst("${path}.",""), value)
            }
        }

        if(newSettings.enable == "true"){
            result = [action: "enable", value: newSettings."enable.value" ]
        }

        if(newSettings.disable == "true"){
            result = [action: "disable", value: newSettings."disable.value" ]
        }

        return result
    }


    def compareLaterData(Map savedSetting, Map newSettings){
        def savedCompare = [action: savedSetting.action, value: savedSetting.value ]

        if(!savedCompare.equals(newSettings)){
            return true
        }else{
            return false
        }
    }

    def editProject(def rundeckProject,
                    String  project,
                    Boolean disable,
                    Boolean executionLater,
                    Boolean scheduleLater){

        Map properties = rundeckProject.getProjectProperties()
        Set removePrefixes=[]
        Properties projProps = new Properties()
        projProps.putAll(properties)

        boolean newExecutionDisabledStatus  = false
        boolean newScheduleDisabledStatus  = false

        def isExecutionDisabledNow = projProps[CONF_PROJECT_DISABLE_EXECUTION] == 'true'
        def isScheduleDisabledNow = projProps[CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

        if(executionLater){
            newExecutionDisabledStatus = disable
            newScheduleDisabledStatus = projProps[CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            projProps[CONF_PROJECT_DISABLE_EXECUTION] = disable.toString()
            removePrefixes.add(CONF_PROJECT_DISABLE_EXECUTION)

            removePrefixes.add("project.later.executions")
            cleanExecutionLaterSettings(projProps)
        }

        if(scheduleLater){
            newScheduleDisabledStatus = disable
            newExecutionDisabledStatus = projProps[CONF_PROJECT_DISABLE_EXECUTION] == 'true'

            projProps[CONF_PROJECT_DISABLE_SCHEDULE]  = disable.toString()
            removePrefixes.add(CONF_PROJECT_DISABLE_SCHEDULE)

            removePrefixes.add("project.later.schedule")
            cleanScheduleLaterSettings(projProps)
        }

        def reschedule = ((isExecutionDisabledNow != newExecutionDisabledStatus)
                || (isScheduleDisabledNow != newScheduleDisabledStatus))
        def active = (!newExecutionDisabledStatus && !newScheduleDisabledStatus)

        frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)
        if(reschedule){
            if(active){
                scheduledExecutionService.rescheduleJobs(frameworkService.isClusterModeEnabled()?frameworkService.getServerUUID():null, project)
            }else{
                scheduledExecutionService.unscheduleJobsForProject(project,frameworkService.isClusterModeEnabled()?frameworkService.getServerUUID():null)
            }
        }

        return projProps
    }

    def cleanExecutionLaterSettings(Properties properties){
        properties.remove(CONF_PROJECT_EXECUTION_LATER_DISABLE)
        properties.remove(CONF_PROJECT_EXECUTION_LATER_DISABLE_VALUE)
        properties.remove(CONF_PROJECT_EXECUTION_LATER_ENABLE)
        properties.remove(CONF_PROJECT_EXECUTION_LATER_ENABLE_VALUE)
    }

    def cleanScheduleLaterSettings(Properties properties){
        properties.remove(CONF_PROJECT_SCHEDULE_LATER_DISABLE)
        properties.remove(CONF_PROJECT_SCHEDULE_LATER_DISABLE_VALUE)
        properties.remove(CONF_PROJECT_SCHEDULE_LATER_ENABLE)
        properties.remove(CONF_PROJECT_SCHEDULE_LATER_ENABLE_VALUE)
    }

    Date scheduleExecutionsLaterJob(String projectName, String type, Map data) {
        Date nextTime
        Date startAt = PluginUtil.laterDate(data.config.dateSaved, data.config.value, DATE_FORMAT)

        String jobName = "${projectName}-${type}"
        def trigger = PluginUtil.createTrigger(jobName, EXECUTIONS_JOB_GROUP_NAME, startAt)
        JobDetail jobDetail = createExecutionLaterJobDetail(jobName,
                                                            EXECUTIONS_JOB_GROUP_NAME,
                                                            data,
                                                           "Execution Later ${projectName}")

        if ( scheduledExecutionService.hasJobScheduled(jobName, EXECUTIONS_JOB_GROUP_NAME) ) {
            log.info("rescheduling existing schedule/execution enable/disable job in project ${projectName}")

            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(jobName, EXECUTIONS_JOB_GROUP_NAME), trigger)
        } else {
            log.info("scheduling new schedule/execution later enable/disable job in project ${projectName}")
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        log.info("scheduled schedule/execution job next run: " + nextTime.toString())
        return nextTime
    }

    def createExecutionLaterJobDetail(String jobname, String jobgroup, Map config , String description) {
        def jobDetailBuilder = JobBuilder.newJob(ExecutionStatusJob)
                .withIdentity(jobname, jobgroup)
                .withDescription(description)
                .usingJobData(new JobDataMap(config))

        return jobDetailBuilder.build()
    }
    def getProjectModeChangeStatus(String project, String type) {
        IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
        String executionLaterPath="extraConfig/executionLater.properties"

        def settings = this.getScheduleExecutionLater(rundeckProject, executionLaterPath)

        if(!settings){
            return [active: false, msg: null]
        }

        def action = "enable"
        if(type == "executions"){

            if(!settings.executions){
                return [active: false, msg: null]
            }
            if(settings.executions){
                if(!settings.executions.active){
                    return [active: false, msg: null]
                }
                action = settings.executions.action
            }

        }

        if(type == "schedule"){
            if(!settings.schedule){
                return [active: false, msg: null]
            }
            if(settings.schedule){
                if(!settings.schedule.active){
                    return [active: false, msg: null]
                }
                action = settings.schedule.action
            }
        }

        String jobName = "${project}-${type}"
        def trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(jobName, EXECUTIONS_JOB_GROUP_NAME))
        def nextFireTime = trigger?.nextFireTime

        if(nextFireTime){
            return [active: true, action: action, nextFireTime: nextFireTime]
        }else{
            return [active: false, action: action, msg: null]
        }
    }

    def removeScheduleJob(String project, String type){
        String jobName = "${project}-${type}"
        def trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(jobName , EXECUTIONS_JOB_GROUP_NAME))
        if(trigger){
            quartzScheduler.deleteJob(new JobKey(jobName, EXECUTIONS_JOB_GROUP_NAME))
        }
    }

    def getProjectExecutionStatus(def rundeckProject) {
        Map properties = rundeckProject.getProjectProperties()
        Properties projProps = new Properties()
        projProps.putAll(properties)

        def isExecutionDisabledNow = projProps[CONF_PROJECT_DISABLE_EXECUTION] == 'true'
        def isScheduleDisabledNow = projProps[CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

        return [isExecutionDisable: isExecutionDisabledNow, isScheduleDisabled: isScheduleDisabledNow]
    }

    void initProcess(){
        try{
            def projects  = frameworkService.projectNames()
            projects.each {project->
                String executionLaterPath="extraConfig/executionLater.properties"
                IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)

                def settings = getScheduleExecutionLater(rundeckProject, executionLaterPath)
                if(settings){
                    if(settings?.executions?.active){

                        Date startAt = PluginUtil.laterDate(settings.executions?.dateSaved, settings.executions?.value, DATE_FORMAT)
                        Date now = new Date()
                        def difference = PluginUtil.getDateDiff(now, startAt, TimeUnit.MINUTES)

                        if(difference>0){
                            scheduleExecutionsLaterJob(project, "executions",   [project: project,
                                                                                       type: "executions",
                                                                                       rundeckProject: rundeckProject,
                                                                                       config: settings.executions,
                                                                                       editProjectService: this])
                        }else{
                            settings.executions.active=false
                            settings.executions.action=null
                            settings.executions.value=null

                            saveExecutionLater(rundeckProject, executionLaterPath, settings)
                        }


                    }

                    if(settings?.schedule?.active){
                        Date startAt = PluginUtil.laterDate(settings.schedule?.dateSaved, settings.schedule?.value, DATE_FORMAT)
                        Date now = new Date()
                        def difference = PluginUtil.getDateDiff(now, startAt, TimeUnit.MINUTES)

                        if(difference>0){
                            scheduleExecutionsLaterJob(project, "schedule", [project: project,
                                                                             type: "schedule",
                                                                             rundeckProject: rundeckProject,
                                                                             config: settings.schedule,
                                                                             editProjectService: this])
                        }else{
                            settings.schedule.active=false
                            settings.schedule.action=null
                            settings.schedule.value=null

                            saveExecutionLater(rundeckProject, executionLaterPath, settings)
                        }
                    }
                }
            }

        }catch(Exception e){
            log.warn("error initProcess: ${e.message}")
        }
    }
}
