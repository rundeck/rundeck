package org.rundeck.app.data.job.converters

import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep

class WorkflowToRdWorkflowConverter {

    static RdWorkflow convertWorkflow(Workflow w) {
        def wkf = new RdWorkflow()
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
}
