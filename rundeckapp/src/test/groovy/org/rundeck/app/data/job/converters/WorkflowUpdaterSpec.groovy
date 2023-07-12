package org.rundeck.app.data.job.converters

import grails.testing.gorm.DataTest
import liquibase.pro.packaged.C
import liquibase.pro.packaged.W
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import spock.lang.Specification

class WorkflowUpdaterSpec extends Specification implements DataTest {

    def "setupSpec"() {
        mockDomains(Workflow, JobExec, CommandExec, PluginStep)
    }

    def "should not update workflow when both Workflow and RdWorkflow objects are null"() {
        when:
        WorkflowUpdater.updateWorkflow(wkf, rdw)

        then:
        noExceptionThrown()

        where:
        wkf             | rdw
        null            | null
        new Workflow()  | null
        null            | new RdWorkflow()
    }

    def "update Workflow with RdWorkflow"() {
        given:
        Workflow wkf = new Workflow(commands: [])
        RdWorkflow rdw = new RdWorkflow(
                threadcount: 2,
                keepgoing: true,
                strategy: "parallel",
                pluginConfigMap: [key: "value"],
                steps: [
                        new RdWorkflowStep(pluginType: "builtin-jobref", configuration: [jobref:[name:"jobname"]]),
                        new RdWorkflowStep(pluginType: "builtin-command", configuration: [exec:"echo hello"]),
                        new RdWorkflowStep(pluginType: "custom-plugin", nodeStep: false, configuration: [configuration:[key1:"val1"]])
                ]
        )

        when:
        WorkflowUpdater.updateWorkflow(wkf, rdw)

        then:
        wkf.threadcount == 2
        wkf.keepgoing == true
        wkf.strategy == "parallel"
        wkf.pluginConfigMap == [key: "value"]
        wkf.commands.size() == 3
    }

    def "should create JobExec workflow step when RdWorkflowStep has pluginType 'builtin-jobref'"() {
        given:
        RdWorkflowStep rdstep = new RdWorkflowStep(pluginType: WorkflowStepConstants.TYPE_JOB_REF)

        when:
        WorkflowStep step = WorkflowUpdater.createWorkflowStep(rdstep)

        then:
        step instanceof JobExec
    }

    def "should create CommandExec workflow step when RdWorkflowStep has pluginType  'builtin-*' that is not a job ref"() {
        given:
        RdWorkflowStep rdstep = new RdWorkflowStep(pluginType: pluginType)

        when:
        WorkflowStep step = WorkflowUpdater.createWorkflowStep(rdstep)

        then:
        step instanceof CommandExec

        where:
        _ | pluginType
        _ | WorkflowStepConstants.TYPE_COMMAND
        _ | WorkflowStepConstants.TYPE_SCRIPT
        _ | WorkflowStepConstants.TYPE_SCRIPT_FILE
        _ | WorkflowStepConstants.TYPE_SCRIPT_URL

    }

    def "should create PluginStep workflow step when RdWorkflowStep has custom pluginType"() {
        given:
        RdWorkflowStep rdstep = new RdWorkflowStep(pluginType: "custom-plugin")

        when:
        WorkflowStep step = WorkflowUpdater.createWorkflowStep(rdstep)

        then:
        step instanceof PluginStep
    }

}
