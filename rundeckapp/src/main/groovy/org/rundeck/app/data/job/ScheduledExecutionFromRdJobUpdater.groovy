package org.rundeck.app.data.job

import com.dtolabs.rundeck.plugins.option.OptionValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.job.RdJob.RdLogConfig
import org.rundeck.app.data.job.RdJob.RdNodeConfig
import org.rundeck.app.data.job.RdJob.RdNotificationData
import org.rundeck.app.data.job.RdJob.RdOptionData
import org.rundeck.app.data.job.RdJob.RdOrchestratorData
import org.rundeck.app.data.job.RdJob.RdScheduleData
import org.rundeck.app.data.job.RdJob.RdWorkflowData
import org.rundeck.app.data.job.RdJob.RdWorkflowStep
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.Orchestrator
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep

class ScheduledExecutionFromRdJobUpdater {
    static ObjectMapper mapper = new ObjectMapper()

    static void update(ScheduledExecution se, RdJob job) {
        se.uuid = job.uuid
        se.jobName = job.jobName
        se.description = job.description
        se.project = job.project
        se.argString = job.argString
        se.user = job.user
        se.timeout = job.timeout
        se.retry = job.retry
        se.retryDelay = job.retryDelay
        se.groupPath = job.groupPath
        se.userRoleList = job.userRoleList
        se.scheduled = job.scheduled
        se.scheduleEnabled = job.scheduleEnabled
        se.executionEnabled = job.executionEnabled
        se.multipleExecutions = job.multipleExecutions
        se.notifyAvgDurationThreshold = job.notifyAvgDurationThreshold
        se.timeZone = job.timeZone
        se.defaultTab = job.defaultTab
        se.maxMultipleExecutions = job.maxMultipleExecutions
        se.dateCreated = job.dateCreated
        se.lastUpdated = job.lastUpdated
        updateWorkflow(se.workflow, job.workflow)
        updateSchedule(se, job.schedule)
        updateLogConfig(se, job.logConfig)
        updateNodeConfig(se, job.nodeConfig)
        updateJobOptions(se, job.options)
        updateOrchestrator(se.orchestrator, job.orchestrator)
        updateNotifications(se, job)
        updatePluginConfig(se, job)
    }

    static updatePluginConfig(ScheduledExecution se, RdJob job) {
        se.pluginConfig = mapper.writeValueAsString(job.pluginConfig)
    }

    static updateNotifications(ScheduledExecution se, RdJob job) {
        def rdnIds = job.notifications.collect {it.id } - null
        se.notifications.findAll { !rdnIds.contains(it.id)}.each {
            se.removeFromNotifications(it)
            it.delete()
        }
        job.notifications.each { rdn ->
            def n = se.notifications.find { it.id == rdn.id }
            if(!n) {
                n = new Notification()
                se.addToNotifications(n)
            }
            updateNotification(n, rdn)
            n.save(failOnError: true)
        }
    }

    static void updateNotification(Notification n, RdNotificationData rdn) {
        n.type = rdn.type
        n.content = rdn.content
        n.eventTrigger = rdn.eventTrigger
        n.format = rdn.format
    }

    static void updateOrchestrator(Orchestrator o, RdOrchestratorData rdo) {
        if(!o && !rdo) return
        if(!o) o = new Orchestrator()
        o.type = rdo.type
        o.content = mapper.writeValueAsString(rdo.configuration)
    }

    static void updateJobOptions(ScheduledExecution se, SortedSet<RdOptionData> rdopts) {
        def rdOptIds = rdopts.collect { it.id } - null
        se.options.findAll { opt -> !rdOptIds.contains(opt.id)}.each {
            se.removeFromOptions(it)
            it.delete()
        } //remove options that are not in updated list
        rdopts.each { rdopt ->
            def opt = se.options.find { it.id == rdopt.id }
            if(!opt) {
                opt = new Option()
                se.addToOptions(opt)
            }
            updateJobOption(opt, rdopt)
            opt.save(failOnError:true)
        }
    }

    static void updateJobOption(Option opt, RdOptionData rdo) {
        opt.sortIndex = rdo.sortIndex
        opt.name = rdo.name
        opt.description = rdo.description
        opt.defaultValue = rdo.defaultValue
        opt.defaultStoragePath = rdo.defaultStoragePath
        opt.enforced = rdo.enforced
        opt.required = rdo.required
        opt.isDate = rdo.isDate
        opt.dateFormat = rdo.dateFormat
        opt.valuesUrl = rdo.valuesUrl ? URI.create(rdo.valuesUrl).toURL() : null
        opt.label = rdo.label
        opt.valuesUrlLong = rdo.valuesUrlLong ? URI.create(rdo.valuesUrlLong).toURL() : null
        opt.regex = rdo.regex
        opt.valuesList = rdo.valuesList
        opt.valuesListDelimiter = rdo.valuesListDelimiter
        opt.delimiter = rdo.delimiter
        opt.multivalued = rdo.multivalued
        opt.secureInput = rdo.secureInput
        opt.secureExposed = rdo.secureExposed
        opt.optionType = rdo.optionType
        opt.configData = rdo.configData
        opt.multivalueAllSelected = rdo.multivalueAllSelected
        opt.optionValuesPluginType = rdo.optionValuesPluginType
        opt.valuesFromPlugin = rdo.valuesFromPlugin?.collect { oval -> new StringOptionValue(name: oval.name, value: oval.value)}
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
        se.nodesSelectedByDefault = nodeConfig.selectedByDefault
        se.nodeKeepgoing = nodeConfig.keepgoing
        se.doNodedispatch = nodeConfig.doNodedispatch
        se.nodeRankAttribute = nodeConfig.rankAttribute
        se.nodeRankOrderAscending = nodeConfig.rankOrderAscending
        se.nodeFilterEditable = nodeConfig.filterEditable
        se.nodeThreadcount = nodeConfig.threadcount
        se.nodeThreadcountDynamic = nodeConfig.threadcountDynamic
        se.nodeExcludePrecedence = nodeConfig.excludePrecedence
        se.successOnEmptyNodeFilter = nodeConfig.successOnEmptyNodeFilter
        se.filter = nodeConfig.filter
        se.filterExclude = nodeConfig.filterExclude
        se.excludeFilterUncheck = nodeConfig.excludeFilterUncheck
    }

    static void updateSchedule(ScheduledExecution se, RdScheduleData schedule) {
        se.year = schedule.year
        se.month = schedule.month
        se.dayOfWeek = schedule.dayOfWeek
        se.dayOfMonth = schedule.dayOfMonth
        se.hour = schedule.hour
        se.minute = schedule.minute
        se.seconds = schedule.seconds
        se.crontabString = schedule.crontabString
    }

    static void updateWorkflow(Workflow wkf, RdWorkflowData rdw) {
        wkf.threadcount = rdw.threadcount
        wkf.keepgoing = rdw.keepgoing
        wkf.strategy = rdw.strategy
        wkf.pluginConfig = rdw.pluginConfig
        def existingStepIds = rdw.commands.collect { it.id } - null
        wkf.commands.findAll { cmd -> !existingStepIds.contains(cmd.id)}.each {
            println "deleting workflow step: ${it.id}"
            wkf.removeFromCommands(it)
            it.delete()
        }
        rdw.commands.each { rdstep ->
            def wfstep = wkf.commands.find { it.id == rdstep.id }
            if(!wfstep) {
                wfstep = createWorkflowStep(rdstep)
                wkf.addToCommands(wfstep)
                println "created step of type: ${wfstep.getClass().getSimpleName()}"
            }
            updateWorkflowStep(wfstep, rdstep)
            wfstep.save(failOnError:true)
        }
        wkf.save(failOnError:true)

    }

    static WorkflowStep createWorkflowStep(RdWorkflowStep step) {
        if(step.pluginType == "builtin-jobref") return new JobExec()
        else if(step.pluginType.startsWith("builtin-")) return new CommandExec()
        return new PluginStep()
    }

    static void updateWorkflowStep(WorkflowStep step, RdWorkflowStep rdstep) {
        if(step instanceof JobExec) {
            def jstep = (JobExec)step
            jstep.id = rdstep.id
            JobExec.updateFromMap(jstep, rdstep.pluginConfiguration)
        } else if(step instanceof CommandExec) {
            def cstep = (CommandExec)step
            cstep.id = rdstep.id
            CommandExec.updateFromMap(cstep, rdstep.pluginConfiguration)
        } else if(step instanceof PluginStep) {
            def pstep = (PluginStep)step
            pstep.id = rdstep.id
            PluginStep.updateFromMap(pstep, rdstep.pluginConfiguration)
            pstep.type = rdstep.pluginType
        }
    }

    static class StringOptionValue implements OptionValue {
        String name
        String value
    }
}
