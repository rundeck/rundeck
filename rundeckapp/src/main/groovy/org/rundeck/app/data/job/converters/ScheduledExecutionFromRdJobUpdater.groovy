package org.rundeck.app.data.job.converters

import com.dtolabs.rundeck.plugins.option.OptionValue
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.util.Holders
import groovy.transform.CompileStatic
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.data.job.RdJob
import rundeck.data.job.RdLogConfig
import rundeck.data.job.RdNodeConfig
import rundeck.data.job.RdNotification
import rundeck.data.job.RdOption
import rundeck.data.job.RdOptionValue
import rundeck.data.job.RdOrchestrator
import rundeck.data.job.RdSchedule
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
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
class ScheduledExecutionFromRdJobUpdater {
    static ObjectMapper mapper = new ObjectMapper()

    static void update(ScheduledExecution se, RdJob job) {
        se.uuid = job.uuid
        if(!se.id && !se.uuid) se.uuid = UUID.randomUUID().toString()
        se.jobName = job.jobName
        se.description = job.description
        se.project = job.project
        se.argString = job.argString
        se.user = job.user
        se.timeout = job.timeout
        se.retry = job.retry
        se.retryDelay = job.retryDelay
        se.groupPath = job.groupPath
        if(job.userRoles) se.setUserRoles(job.userRoles)
        se.scheduled = job.scheduled
        se.scheduleEnabled = job.scheduleEnabled
        se.executionEnabled = job.executionEnabled
        se.multipleExecutions = job.multipleExecutions
        se.notifyAvgDurationThreshold = job.notifyAvgDurationThreshold
        se.timeZone = job.timeZone
        se.defaultTab = job.defaultTab
        se.maxMultipleExecutions = job.maxMultipleExecutions
        se.save() //This is necessary for inserts to happen in the correct order
        if(!se.workflow) se.workflow = new Workflow(commands: [])
        WorkflowUpdater.updateWorkflow(se.workflow, job.workflow)
        updateSchedule(se, job.schedule)
        updateLogConfig(se, job.logConfig)
        updateNodeConfig(se, job.nodeConfig)
        updateJobOptions(se, job.optionSet)
        updateOrchestrator(se, job.orchestrator)
        updateNotifications(se, job)
        updatePluginConfig(se, job)
    }

    static updateOrchestrator(ScheduledExecution se, RdOrchestrator rdo) {
        if(!se.orchestrator && !rdo) return
        if(se.orchestrator && !rdo) {
            se.orchestrator = null
            return
        }
        if(!se.orchestrator) se.orchestrator = new Orchestrator()
        OrchestratorUpdater.updateOrchestrator(se.orchestrator, rdo)
    }

    static updatePluginConfig(ScheduledExecution se, RdJob job) {
        se.pluginConfig = mapper.writeValueAsString(job.pluginConfigMap)
    }

    static updateNotifications(ScheduledExecution se, RdJob job) {
        //remove all notifications
        for(int i = 0; i < se.notifications?.size(); i++) {
            def notif = se.notifications[i]
            se.removeFromNotifications(notif)
            notif.delete()
        }
        //add new notifications
        job.notificationSet?.each { rdn ->
            def n = new Notification()
            se.addToNotifications(n)
            updateNotification(n, rdn)
            n.save(failOnError: true)
        }
    }

    static void updateNotification(Notification n, RdNotification rdn) {
        n.type = rdn.type
        n.configuration = rdn.configuration
        n.eventTrigger = rdn.eventTrigger
        n.format = rdn.format
    }

    static void updateJobOptions(ScheduledExecution se, SortedSet<RdOption> rdopts) {
        //remove old options
        for(int i = 0; i < se.options?.size(); i++) {
            def opt = se.options[i]
            se.removeFromOptions(opt)
            opt.delete()
        }
        if(!rdopts) return
        //add options
        rdopts.each { rdopt ->
            def opt = new Option()
            se.addToOptions(opt)
            updateJobOption(opt, rdopt)
            opt.save(failOnError:true)
        }
    }

    static void updateJobOption(Option opt, RdOption rdo) {
        opt.sortIndex = rdo.sortIndex
        opt.name = rdo.name
        opt.description = rdo.description
        opt.defaultValue = rdo.defaultValue
        opt.defaultStoragePath = rdo.defaultStoragePath
        opt.enforced = rdo.enforced
        opt.required = rdo.required
        opt.isDate = rdo.isDate
        opt.dateFormat = rdo.dateFormat
        opt.label = rdo.label
        opt.valuesUrlLong = rdo.realValuesUrl
        opt.regex = rdo.regex
        opt.valuesList = rdo.valuesList
        opt.valuesListDelimiter = rdo.valuesListDelimiter
        opt.delimiter = rdo.delimiter
        opt.multivalued = rdo.multivalued
        opt.secureInput = rdo.secureInput
        opt.secureExposed = rdo.secureExposed
        opt.optionType = rdo.optionType
        opt.configMap = rdo.configMap
        opt.multivalueAllSelected = rdo.multivalueAllSelected
        opt.optionValuesPluginType = rdo.optionValuesPluginType
        opt.valuesFromPlugin = rdo.valuesFromPlugin?.collect { oval -> new RdOptionValue(name: oval.name, value: oval.value) as OptionValue}
        opt.hidden = rdo.hidden
        opt.sortValues = rdo.sortValues
        opt.optionValues = rdo.optionValues ? new ArrayList(rdo.optionValues) : null
    }

    static void updateLogConfig(ScheduledExecution se, RdLogConfig logConfig) {
        se.loglevel = logConfig.loglevel
        se.logOutputThreshold = logConfig.logOutputThreshold
        se.logOutputThresholdAction = logConfig.logOutputThresholdAction
        se.logOutputThresholdStatus = logConfig.logOutputThresholdStatus
    }

    static void updateNodeConfig(ScheduledExecution se, RdNodeConfig nodeConfig) {
        se.nodeInclude = nodeConfig.nodeInclude
        se.nodeIncludeName = nodeConfig.nodeIncludeName
        se.nodeIncludeTags = nodeConfig.nodeIncludeTags
        se.nodeIncludeOsName = nodeConfig.nodeIncludeOsName
        se.nodeIncludeOsArch = nodeConfig.nodeIncludeOsArch
        se.nodeIncludeOsFamily = nodeConfig.nodeIncludeOsFamily
        se.nodeIncludeOsVersion = nodeConfig.nodeIncludeOsVersion
        se.nodeExclude = nodeConfig.nodeExclude
        se.nodeExcludeName = nodeConfig.nodeExcludeName
        se.nodeExcludeTags = nodeConfig.nodeExcludeTags
        se.nodeExcludeOsName = nodeConfig.nodeExcludeOsName
        se.nodeExcludeOsArch = nodeConfig.nodeExcludeOsArch
        se.nodeExcludeOsFamily = nodeConfig.nodeExcludeOsFamily
        se.nodeExcludeOsVersion = nodeConfig.nodeExcludeOsVersion
        se.nodeExcludePrecedence = nodeConfig.nodeExcludePrecedence
        se.successOnEmptyNodeFilter = nodeConfig.successOnEmptyNodeFilter
        se.nodesSelectedByDefault = nodeConfig.nodesSelectedByDefault
        se.nodeKeepgoing = nodeConfig.nodeKeepgoing
        se.doNodedispatch = nodeConfig.doNodedispatch
        se.nodeRankAttribute = nodeConfig.nodeRankAttribute
        se.nodeRankOrderAscending = nodeConfig.nodeRankOrderAscending
        se.nodeFilterEditable = nodeConfig.nodeFilterEditable
        se.nodeThreadcount = nodeConfig.nodeThreadcount
        se.nodeThreadcountDynamic = nodeConfig.nodeThreadcountDynamic
        se.filter = nodeConfig.filter
        se.filterExclude = nodeConfig.filterExclude
        se.excludeFilterUncheck = nodeConfig.excludeFilterUncheck
    }

    static void updateSchedule(ScheduledExecution se, RdSchedule schedule) {
        se.year = schedule?.year
        se.month = schedule?.month
        se.dayOfWeek = schedule?.dayOfWeek
        se.dayOfMonth = schedule?.dayOfMonth
        se.hour = schedule?.hour
        se.minute = schedule?.minute
        se.seconds = schedule?.seconds
        se.crontabString = schedule?.crontabString
    }



}
