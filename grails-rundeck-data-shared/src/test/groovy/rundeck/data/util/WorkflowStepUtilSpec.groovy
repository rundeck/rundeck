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
            configuration >> [jobref:jobrefcfg]
        }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == expectedSummary
        where:
        expectedSummary                 | jobrefcfg
        "job: jobgroup/jobname -- arg"  | [uuid: "uuid", useName: "true", group:"jobgroup", name:"jobname", argString: "arg"]
        "job: uuid -- arg"              | [uuid: "uuid", group:"jobgroup", name:"jobname", argString: "arg"]
    }

    def "test summarize method command"() {
        given:
        def step = Mock(WorkflowStepData){
            pluginType >> "builtin-command"
            description >> "description"
            configuration >> [
                scriptInterpreter: "interpreter",
                interpreterArgsQuoted: true,
                exec: "remote",
                script: "local",
                scriptfile: "/filepath",
                args: "arg",
                fileExtension: "extension"
            ]
        }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == "interpreter'remotelocal/filepath -- arg' ('description') [extension]"
    }

    def "test summarize method with RdWorkflowStep"() {
        given:
        def step = new RdWorkflowStep()
        step.pluginType = "builtin-command"
        step.description = "descriptionTest"
        step.configuration = [
            scriptInterpreter: "interpreter",
            interpreterArgsQuoted: true,
            exec: "remote",
            script: "local",
            scriptfile: "/filepath",
            args: "arg",
            fileExtension: "extension"
        ] as Map<String, Object>

        when:
        def summary = WorkflowStepUtil.summarize(step as WorkflowStepData)
        then:
        summary == "interpreter'remotelocal/filepath -- arg' ('descriptionTest') [extension]"
    }

}