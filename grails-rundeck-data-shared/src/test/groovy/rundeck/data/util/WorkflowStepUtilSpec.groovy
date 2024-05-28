package rundeck.data.util

import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.data.job.RdWorkflowStep
import spock.lang.Specification

class WorkflowStepUtilSpec extends Specification {

    def "test summarize method"() {
        given:
        def step = Mock(WorkflowStepData){
            pluginType >> "builtin-script"
            nodeStep >> true
        }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == "Plugin[builtin-script, nodeStep: true]"
    }


    def "test summarize method job"() {
        given:

        def step = Mock(WorkflowStepData){
            pluginType >> "builtin-jobref"
            nodeStep >> false
        }
        step.metaClass.getJobIdentifier = { -> "uuid" }
        step.metaClass.getArgString = { -> "arg" }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == "job: uuid -- arg"
    }

    def "test summarize method command"() {
        given:
        def step = Mock(WorkflowStepData){
            pluginType >> "builtin-command"
            description >> "description"
        }
        step.metaClass.getScriptInterpreter = { -> "interpreter" }
        step.metaClass.getInterpreterArgsQuoted = { -> true }
        step.metaClass.getAdhocRemoteString = { -> "remote" }
        step.metaClass.getAdhocLocalString = { -> "local" }
        step.metaClass.getAdhocFilepath = { -> "filepath" }
        step.metaClass.getArgString = { -> "arg" }
        step.metaClass.getFileExtension = { -> "extension" }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == "interpreter'remotelocalfilepath -- arg' ('description') [extension]"
    }

    def "test summarize method with RdWorkflowStep"() {
        given:
        def step = new RdWorkflowStep()
        step.pluginType = "builtin-command"
        step.description = "descriptionTest"
        step.metaClass.getScriptInterpreter = { -> "interpreter" }
        step.metaClass.getInterpreterArgsQuoted = { -> true }
        step.metaClass.getAdhocRemoteString = { -> "remote" }
        step.metaClass.getAdhocLocalString = { -> "local" }
        step.metaClass.getAdhocFilepath = { -> "filepath" }
        step.metaClass.getArgString = { -> "arg" }
        step.metaClass.getFileExtension = { -> "extension" }

        when:
        def summary = WorkflowStepUtil.summarize(step as WorkflowStepData)
        then:
        summary == "interpreter'remotelocalfilepath -- arg' ('descriptionTest') [extension]"
    }

}