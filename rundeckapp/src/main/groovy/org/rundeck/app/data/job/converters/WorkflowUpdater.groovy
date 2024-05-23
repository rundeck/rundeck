package org.rundeck.app.data.job.converters

import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.constants.WorkflowStepConstants

class WorkflowUpdater {

    static void updateWorkflow(Workflow wkf, WorkflowData rdw) {
        if(!wkf || !rdw) return
        wkf.threadcount = rdw.threadcount
        wkf.keepgoing = rdw.keepgoing
        wkf.strategy = rdw.strategy
        wkf.pluginConfigMap = rdw.pluginConfigMap
        //remove all previous steps
        for(int x = 0; x < wkf.commands.size(); x++) {
            def cmd = wkf.commands[x]
            wkf.removeFromCommands(cmd)
            cmd.delete()
        }
        rdw.steps.each { rdstep ->
            def wfstep = createWorkflowStep(rdstep)
            wkf.addToCommands(wfstep)
            updateWorkflowStep(wfstep, rdstep)
            wfstep.save(failOnError:true)
        }
        wkf.save(failOnError:true)

    }

    static WorkflowStep createWorkflowStep(WorkflowStepData step) {
        if(step.pluginType == WorkflowStepConstants.TYPE_JOB_REF) return new JobExec()
        else if(step.pluginType.startsWith("builtin-")) return new CommandExec()
        return new PluginStep()
    }

    static void updateWorkflowStep(WorkflowStep step, WorkflowStepData rdstep) {
        if(step instanceof JobExec) {
            def jstep = (JobExec)step
            JobExec.updateFromMap(jstep, rdstep.configuration)
            jstep.nodeStep = rdstep.nodeStep
        } else if(step instanceof CommandExec) {
            def cstep = (CommandExec)step
            CommandExec.updateFromMap(cstep, rdstep.configuration)
        } else if(step instanceof PluginStep) {
            def pstep = (PluginStep)step
            PluginStep.updateFromMap(pstep, rdstep.configuration)
            pstep.nodeStep = rdstep.nodeStep
            pstep.type = rdstep.pluginType
        }
    }
}
