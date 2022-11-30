package org.rundeck.app.data.job.converters


import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.RdLogConfig
import org.rundeck.app.data.job.RdNodeConfig
import org.rundeck.app.data.job.RdNotification
import org.rundeck.app.data.job.RdOption
import org.rundeck.app.data.job.RdOrchestrator
import org.rundeck.app.data.job.RdSchedule
import org.rundeck.app.data.job.RdWorkflow
import org.rundeck.app.data.job.RdWorkflowStep
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
        job.userRoles = se.userRoles
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
        job.logConfig = se.logConfig
        job.nodeConfig = se.nodeConfig
        job.optionSet = convertJobOptionSet(se)
        job.notificationSet = convertNotificationSet(se)
        job.workflow = convertWorkflow(se.workflow)
        job.schedule = se.schedule
        job.orchestrator = convertOrchestrator(se.orchestrator)
        job.pluginConfigMap = se.getPluginConfigMap()

        return job
    }

//    static RdLogConfig convertLogConfig(ScheduledExecution se) {
//        new RdLogConfig(loglevel: se.loglevel,
//                logOutputThreshold: se.logOutputThreshold,
//                logOutputThresholdAction: se.logOutputThresholdAction,
//                logOutputThresholdStatus: se.logOutputThresholdStatus
//        )
//    }

//    static RdNodeConfig convertNodeConfig(ScheduledExecution se) {
//        new RdNodeConfig(
//                nodeInclude : se.nodeInclude,
//                nodeExclude : se.nodeExclude,
//                nodeIncludeName : se.nodeIncludeName,
//                nodeExcludeName : se.nodeExcludeName,
//                nodeIncludeTags : se.nodeIncludeTags,
//                nodeExcludeTags : se.nodeExcludeTags,
//                nodeIncludeOsName : se.nodeIncludeOsName,
//                nodeExcludeOsName : se.nodeExcludeOsName,
//                nodeIncludeOsFamily : se.nodeIncludeOsFamily,
//                nodeExcludeOsFamily : se.nodeExcludeOsFamily,
//                nodeIncludeOsArch : se.nodeIncludeOsArch,
//                nodeExcludeOsArch : se.nodeExcludeOsArch,
//                nodeIncludeOsVersion : se.nodeIncludeOsVersion,
//                nodeExcludeOsVersion : se.nodeExcludeOsVersion,
//                nodeExcludePrecedence : se.nodeExcludePrecedence,
//                successOnEmptyNodeFilter: se.successOnEmptyNodeFilter,
//                filter: se.filter,
//                filterExclude: se.filterExclude,
//                excludeFilterUncheck: se.excludeFilterUncheck
//        )
//    }

    static RdOrchestrator convertOrchestrator(Orchestrator o) {
        if(!o) return null
        RdOrchestrator orchestrator = new RdOrchestrator()
        orchestrator.id = o.id
        orchestrator.type = o.type
        orchestrator.configuration = o.configuration
        return orchestrator
    }

//    static RdSchedule convertSchedule(ScheduledExecution se) {
//        RdSchedule schedule = new RdSchedule()
//        schedule.year = se.year
//        schedule.month = se.month
//        schedule.dayOfWeek = se.dayOfWeek
//        schedule.dayOfMonth = se.dayOfMonth
//        schedule.hour = se.hour
//        schedule.minute = se.minute
//        schedule.seconds = se.seconds
//        schedule.crontabString = se.crontabString
//        return schedule
//    }

    static RdWorkflow convertWorkflow(Workflow w) {
        def wkf = new RdWorkflow()
        wkf.id = w.id
        wkf.threadcount = w.threadcount
        wkf.keepgoing = w.keepgoing
        wkf.strategy = w.strategy
        wkf.pluginConfigMap = w.pluginConfigMap
        wkf.steps = w.commands.collect { step -> convertWorkflowStep(step) }
        return wkf
    }

    static RdWorkflowStep convertWorkflowStep(WorkflowStep wstep) {
        if(!wstep) return null
        def rds = new RdWorkflowStep()
        rds.id = wstep.id
        rds.description = wstep.description
        rds.keepgoingOnSuccess = wstep.keepgoingOnSuccess
        rds.pluginConfig = wstep.pluginConfig
        rds.errorHandler = convertWorkflowStep(wstep.errorHandler)
        if(wstep instanceof PluginStep) {
            def pstep = (PluginStep)wstep
            rds.configuration = pstep.configuration
        } else if(wstep instanceof JobExec) {
            def jrstep = (JobExec)wstep
            rds.configuration = new HashMap<>(jrstep.toMap())
            rds.configuration.remove("plugins")
            rds.configuration.remove("description")
            rds.configuration.remove("keepgoingOnSuccess")
        } else if(wstep instanceof CommandExec) {
            def cstep = (CommandExec)wstep
            rds.configuration = new HashMap<>(cstep.toMap()) as Map<String, Object>
            rds.configuration.remove("plugins")
            rds.configuration.remove("description")
            rds.configuration.remove("keepgoingOnSuccess")
        }
        rds.nodeStep = wstep.nodeStep
        rds.pluginType = wstep.getPluginType()
        return rds
    }

    static TreeSet<RdOption> convertJobOptionSet(ScheduledExecution se) {
        if(!se.options) return
        def jobOptions = new TreeSet<RdOption>()
        se.options.each { opt ->
            jobOptions.add(convertRdOption(opt))
        }
        return jobOptions
    }

    static Set<RdNotification> convertNotificationSet(ScheduledExecution se) {
        if(!se.notifications) return
        def notifications = new HashSet<RdNotification>()
        se.notifications.each {n ->
            notifications.add(convertNotification(n))
        }
        return notifications
    }

    static RdNotification convertNotification(Notification rdn) {
        RdNotification n = new RdNotification()
        n.id = rdn.id
        n.type = rdn.type
        n.configuration = rdn.configuration
        n.eventTrigger = rdn.eventTrigger
        n.format = rdn.format
        return n
    }

    @GrailsCompileStatic(TypeCheckingMode.SKIP)
    static RdOption convertRdOption(Option opt) {
        RdOption o = new RdOption()
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
        o.realValuesUrl = opt.realValuesUrl
        o.label = opt.label
        o.regex = opt.regex
        o.valuesList = opt.valuesList
        o.valuesListDelimiter = opt.valuesListDelimiter
        o.delimiter = opt.delimiter
        o.multivalued = opt.multivalued
        o.secureInput = opt.secureInput
        o.secureExposed = opt.secureExposed
        o.optionType = opt.optionType
        o.configMap = opt.configMap
        o.multivalueAllSelected = opt.multivalueAllSelected
        o.optionValuesPluginType = opt.optionValuesPluginType
        o.valuesFromPlugin = opt.valuesFromPlugin?.collect { oval -> new RdOption.RdOptionValue(name: oval.name, value: oval.value)}
        o.hidden = opt.hidden
        o.sortValues = opt.sortValues
        o.optionValues = opt.optionValues ? new ArrayList(opt.optionValues) : null
        return o
    }

}
