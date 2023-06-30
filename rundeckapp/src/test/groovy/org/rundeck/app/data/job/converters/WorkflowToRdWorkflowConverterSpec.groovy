package org.rundeck.app.data.job.converters

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import spock.lang.Specification
import spock.lang.Unroll

class WorkflowToRdWorkflowConverterSpec extends Specification implements DataTest {

    def "setupSpec"() {
        mockDomains(Workflow, JobExec, CommandExec, PluginStep)
    }

    def "test updateWorkflow method"() {
        given:
        Workflow wkf = new Workflow(commands: [])
        RdWorkflow rdw = new RdWorkflow()
        rdw.threadcount = 5
        rdw.keepgoing = true
        rdw.strategy = "parallel"
        rdw.pluginConfigMap = [key1: "value1", key2: "value2"]
        RdWorkflowStep step1 = new RdWorkflowStep()
        step1.pluginType = WorkflowStepConstants.TYPE_JOB_REF
        step1.nodeStep = true
        step1.configuration = [jobref:[name:"job1",group:"grp1","uuid":"jobuuid"]]
        RdWorkflowStep step2 = new RdWorkflowStep()
        step2.pluginType = WorkflowStepConstants.TYPE_COMMAND
        step2.nodeStep = true
        step2.configuration = [exec:"echo hello"]
        RdWorkflowStep step3 = new RdWorkflowStep()
        step3.pluginType = "custom-plugin"
        step3.nodeStep = true
        step3.configuration = [plugincfg:"val1"]
        rdw.steps = [step1, step2, step3]

        when:
        WorkflowUpdater.updateWorkflow(wkf, rdw)

        then:
        wkf.threadcount == 5
        wkf.keepgoing == true
        wkf.strategy == "parallel"
        wkf.pluginConfigMap == [key1: "value1", key2: "value2"]
        wkf.commands.size() == 3
        wkf.commands[0] instanceof JobExec
        wkf.commands[1] instanceof CommandExec
        wkf.commands[2] instanceof PluginStep
        wkf.commands[0].jobName == step1.configuration.jobref.name
        wkf.commands[0].jobGroup == step1.configuration.jobref.group
        wkf.commands[0].uuid == step1.configuration.jobref.uuid
        wkf.commands[1].adhocRemoteString == step2.configuration.exec
        wkf.commands[2].configuration.plugincfg == step3.configuration.plugincfg
    }

    def "test createWorkflowStep method"() {
        when:
        WorkflowStep step1 = WorkflowUpdater.createWorkflowStep(new RdWorkflowStep(pluginType: "builtin-jobref"))
        WorkflowStep step2 = WorkflowUpdater.createWorkflowStep(new RdWorkflowStep(pluginType: "builtin-command"))
        WorkflowStep step3 = WorkflowUpdater.createWorkflowStep(new RdWorkflowStep(pluginType: "custom-plugin"))

        then:
        step1 instanceof JobExec
        step2 instanceof CommandExec
        step3 instanceof PluginStep
    }

//    @Unroll
//    def "test updateWorkflowStep method for #stepType"() {
//        given:
//        WorkflowStep step
//        RdWorkflowStep rdstep = new RdWorkflowStep()
//        rdstep.configuration = [key1: "value1", key2: "value2"]
//
//        when:
//        WorkflowUpdater.updateWorkflowStep(step, rdstep)
//
//        then:
//        step.configuration == rdstep.configuration
//
//        where:
//        stepType << [JobExec, CommandExec, PluginStep]
//    }
}
