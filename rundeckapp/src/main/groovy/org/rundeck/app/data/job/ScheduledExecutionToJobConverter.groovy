package org.rundeck.app.data.job


import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.data.job.RdJob.RdLogConfig
import org.rundeck.app.data.job.RdJob.RdNodeConfig
import org.rundeck.app.data.job.RdJob.RdNotificationData
import org.rundeck.app.data.job.RdJob.RdOptionData
import org.rundeck.app.data.job.RdJob.RdOptionValueData
import org.rundeck.app.data.job.RdJob.RdWorkflowData
import org.rundeck.app.data.job.RdJob.RdWorkflowStep
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.Orchestrator
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep

@GrailsCompileStatic
class ScheduledExecutionToJobConverter {

    static ObjectMapper mapper = new ObjectMapper()

    static JobData convert(ScheduledExecution se) {
        if(!se) return null
        RdJob job = new RdJob()
        job.id = se.id
        job.uuid = se.uuid
        job.jobName = se.jobName
        job.description = se.description
        job.project = se.project
        job.argString = se.argString
        job.user = se.user
        job.timeout = se.timeout
        job.retry = se.retry
        job.retryDelay = se.retryDelay
        job.groupPath = se.groupPath
        job.userRoleList = se.userRoleList
        job.scheduled = se.scheduled
        job.scheduleEnabled = se.scheduleEnabled
        job.executionEnabled = se.executionEnabled
        job.multipleExecutions = se.multipleExecutions
        job.notifyAvgDurationThreshold = se.notifyAvgDurationThreshold
        job.timeZone = se.timeZone
        job.defaultTab = se.defaultTab
        job.maxMultipleExecutions = se.maxMultipleExecutions
        job.dateCreated = se.dateCreated
        job.lastUpdated = se.lastUpdated
        job.nodesSelectedByDefault = se.nodesSelectedByDefault
        job.nodeKeepgoing = se.nodeKeepgoing
        job.doNodedispatch = se.doNodedispatch
        job.nodeRankAttribute = se.nodeRankAttribute
        job.nodeRankOrderAscending = se.nodeRankOrderAscending
        job.nodeFilterEditable = se.nodeFilterEditable
        job.nodeThreadcount = se.nodeThreadcount
        job.nodeThreadcountDynamic = se.nodeThreadcountDynamic
        job.logConfig = convertLogConfig(se)
        job.nodeConfig = convertNodeConfig(se)
        job.optionSet = convertJobOptionSet(se)
        job.notificationSet = convertNotificationSet(se)
        job.workflow = convertWorkflow(se.workflow)
        job.schedule = convertSchedule(se)
        job.orchestrator = convertOrchestrator(se.orchestrator)
        job.pluginConfigMap = se.getPluginConfigMap()

        return job
    }

    static RdLogConfig convertLogConfig(ScheduledExecution se) {
        new RdLogConfig(loglevel: se.loglevel,
                logOutputThreshold: se.logOutputThreshold,
                logOutputThresholdAction: se.logOutputThresholdAction,
                logOutputThresholdStatus: se.logOutputThresholdStatus
        )
    }

    static RdNodeConfig convertNodeConfig(ScheduledExecution se) {
        new RdNodeConfig(
                nodeInclude : se.nodeInclude,
                nodeExclude : se.nodeExclude,
                nodeIncludeName : se.nodeIncludeName,
                nodeExcludeName : se.nodeExcludeName,
                nodeIncludeTags : se.nodeIncludeTags,
                nodeExcludeTags : se.nodeExcludeTags,
                nodeIncludeOsName : se.nodeIncludeOsName,
                nodeExcludeOsName : se.nodeExcludeOsName,
                nodeIncludeOsFamily : se.nodeIncludeOsFamily,
                nodeExcludeOsFamily : se.nodeExcludeOsFamily,
                nodeIncludeOsArch : se.nodeIncludeOsArch,
                nodeExcludeOsArch : se.nodeExcludeOsArch,
                nodeIncludeOsVersion : se.nodeIncludeOsVersion,
                nodeExcludeOsVersion : se.nodeExcludeOsVersion,
                nodeExcludePrecedence : se.nodeExcludePrecedence,
                successOnEmptyNodeFilter: se.successOnEmptyNodeFilter,
                filter: se.filter,
                filterExclude: se.filterExclude,
                excludeFilterUncheck: se.excludeFilterUncheck
        )
    }

    static RdJob.RdOrchestratorData convertOrchestrator(Orchestrator o) {
        if(!o) return null
        RdJob.RdOrchestratorData orchestrator = new RdJob.RdOrchestratorData()
        orchestrator.id = o.id
        orchestrator.type = o.type
        orchestrator.configuration = o.configuration
        return orchestrator
    }

    static RdJob.RdScheduleData convertSchedule(ScheduledExecution se) {
        RdJob.RdScheduleData schedule = new RdJob.RdScheduleData()
        schedule.year = se.year
        schedule.month = se.month
        schedule.dayOfWeek = se.dayOfWeek
        schedule.dayOfMonth = se.dayOfMonth
        schedule.hour = se.hour
        schedule.minute = se.minute
        schedule.seconds = se.seconds
        schedule.crontabString = se.crontabString
        return schedule
    }

    static RdWorkflowData convertWorkflow(Workflow w) {
        def wkf = new RdWorkflowData()
        wkf.id = w.id
        wkf.threadcount = w.threadcount
        wkf.keepgoing = w.keepgoing
        wkf.strategy = w.strategy
        wkf.pluginConfig = w.pluginConfig
        wkf.steps = w.commands.collect { step -> convertWorkflowStep(step) }
        return wkf
    }

    static RdWorkflowStep convertWorkflowStep(WorkflowStep wstep) {
        if(!wstep) return null
        def rds = new RdWorkflowStep()
        rds.id = wstep.id
        rds.description = wstep.description
        rds.keepgoingOnSuccess = wstep.keepgoingOnSuccess
        rds.configuration = wstep.configuration
        rds.errorHandler = convertWorkflowStep(wstep.errorHandler)
        if(wstep instanceof PluginStep) {
            def pstep = (PluginStep)wstep
            rds.pluginConfig = mapper.readValue(pstep.jsonData, HashMap)
        } else if(wstep instanceof JobExec) {
            def jrstep = (JobExec)wstep
            rds.pluginConfig = new HashMap<>(jrstep.toMap())
        } else if(wstep instanceof CommandExec) {
            def cstep = (CommandExec)wstep
            rds.pluginConfig = new HashMap<>(cstep.toMap()) as Map<String, Object>
        }
        rds.nodeStep = wstep.nodeStep
        rds.pluginType = wstep.getPluginType()
        return rds
    }

    static TreeSet<RdOptionData> convertJobOptionSet(ScheduledExecution se) {
        def jobOptions = new TreeSet<RdOptionData>()
        se.options.each { opt ->
            jobOptions.add(convertRdOption(opt))
        }
        return jobOptions
    }

    static Set<RdNotificationData> convertNotificationSet(ScheduledExecution se) {
        def notifications = new HashSet<RdNotificationData>()
        se.notifications.each {n ->
            notifications.add(convertNotification(n))
        }
        return notifications
    }

    static RdNotificationData convertNotification(Notification rdn) {
        RdNotificationData n = new RdNotificationData()
        n.id = rdn.id
        n.type = rdn.type
        n.content = rdn.content
        n.eventTrigger = rdn.eventTrigger
        n.format = rdn.format
        return n
    }

    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    static RdOptionData convertRdOption(Option opt) {
        RdOptionData o = new RdOptionData()
        o.id = opt.id
        o.sortIndex = opt.sortIndex
        o.name = opt.name
        o.description = opt.description
        o.defaultValue = opt.defaultValue
        o.defaultStoragePath = opt.defaultStoragePath
        o.enforced = opt.enforced
        o.required = opt.required
        o.isDate = opt.isDate
        o.dateFormat = opt.dateFormat
        o.valuesUrl = opt.valuesUrl
        o.label = opt.label
        o.valuesUrlLong = opt.valuesUrlLong
        o.regex = opt.regex
        o.valuesList = opt.valuesList
        o.valuesListDelimiter = opt.valuesListDelimiter
        o.delimiter = opt.delimiter
        o.multivalued = opt.multivalued
        o.secureInput = opt.secureInput
        o.secureExposed = opt.secureExposed
        o.optionType = opt.optionType
        o.configData = opt.configData
        o.multivalueAllSelected = opt.multivalueAllSelected
        o.optionValuesPluginType = opt.optionValuesPluginType
        o.valuesFromPlugin = opt.valuesFromPlugin?.collect { oval -> new RdOptionValueData(name: oval.name, value: oval.value)}
        o.hidden = opt.hidden
        o.sortValues = opt.sortValues
        o.optionValues = opt.optionValues ? new ArrayList(opt.optionValues) : null
        return o
    }

}
