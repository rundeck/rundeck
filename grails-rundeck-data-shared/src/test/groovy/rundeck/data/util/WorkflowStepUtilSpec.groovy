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

    def "summarize command plugin steps deserialized from workflow json"() {
        given:
        def step = Mock(WorkflowStepData){
            pluginType >> type
            nodeStep >> true
            configuration >> config
        }
        when:
        def summary = WorkflowStepUtil.summarize(step)
        then:
        summary == expected
        where:
        type              | config                                                                  | expected
        'exec-command'    | [adhocRemoteString: 'echo hello']                                       | 'echo hello'
        'script-inline'   | [adhocLocalString: 'echo script', argString: 'a b']                     | 'echo script -- a b'
        'script-inline'   | [adhocLocalString: 'echo s', scriptInterpreter: 'bash -c',
                             interpreterArgsQuoted: 'true', fileExtension: 'sh']                    | "bash -c'echo s' [sh]"
        'script-file-url' | [adhocFilepath: '/tmp/a.sh', interpreterArgsQuoted: 'false']            | '/tmp/a.sh'
        'script-file-url' | [adhocFilepath: 'https://host/a.sh', argString: 'x']                   | 'https://host/a.sh -- x'
        'exec-command'    | null                                                                    | ''
    }

    def "getCommandString"() {
        given:
        def step = Mock(WorkflowStepData){
            pluginType >> type
            configuration >> config
        }
        expect:
        WorkflowStepUtil.getCommandString(step) == expected
        where:
        type              | config                            | expected
        'builtin-command' | [exec: 'echo legacy']             | 'echo legacy'
        'exec-command'    | [adhocRemoteString: 'echo hello'] | 'echo hello'
        'script-inline'   | [adhocLocalString: 'echo hello']  | null
        'some-plugin'     | [adhocRemoteString: 'echo hello'] | null
    }

    def "getCommandString null step"() {
        expect:
        WorkflowStepUtil.getCommandString(null) == null
    }
}
