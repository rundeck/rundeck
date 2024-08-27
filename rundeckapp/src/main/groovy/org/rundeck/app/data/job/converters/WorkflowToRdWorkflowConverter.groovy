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
            rds.configuration = convertConfiguration(jrstep.toMap())
        } else if(wstep instanceof CommandExec) {
            def cstep = (CommandExec)wstep
            rds.configuration = convertConfiguration(cstep.toMap())
        }
        rds.nodeStep = wstep.nodeStep
        rds.pluginType = wstep.getPluginType()
        return rds
    }

    static Map<String,Object> convertConfiguration(Map config) {
        Map convertedCfg = new HashMap<>(config)
        convertedCfg.remove("plugins")
        convertedCfg.remove("description")
        convertedCfg.remove("errorHandler")
        convertedCfg.remove("keepgoingOnSuccess")
        return convertedCfg
    }
}
