package org.rundeck.tests.functional.api.job.input

import org.rundeck.util.annotations.APISecureInputTest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml

@APISecureInputTest
class SecureInputSpec extends BaseContainer {

    public static final String TEST_PROJECT = 'SecureInputSpec'

    def setupSpec() {
        startEnvironment()
        //the project configuration defines stub resource model with a node using node-executor "newlocal"
        setupProjectArchiveDirectoryResource(TEST_PROJECT, "/projects-import/SecureInputSpec")
    }
    /**
     * template base job definition
     */
    static final Map<String, Object> BASE_JOB = [
        name              : 'a job',
        defaultTab        : 'nodes',
        description       : '',
        executionEnabled  : true,
        loglevel          : 'INFO',
        nodeFilterEditable: false,
        options           : [
        ],
        scheduleEnabled   : true,
        sequence          : [
            commands : [
                [exec: 'echo hi']
            ],
            keepgoing: false,
            strategy : 'node-first'
        ],
    ]

    def "secure option value available in script"() {
        given:
            String jobUuid = UUID.randomUUID().toString()
            def jobdef = new HashMap(BASE_JOB)
            jobdef.name = 'script secure input test' + (autoMode ? ' auto' : ' manual')
            jobdef.uuid = jobUuid
            jobdef.options = [
                [name: 'password', secure: true, type: 'text'],
                [name: 'plain', type: 'text', value: 'asdf'],
                [name: 'insecurepass', secure: true, type: 'text', valueExposed: true]
            ]
            jobdef.nodefilters = [
                filter: 'tags: "newlocal"'
            ]
            jobdef.sequence.commands = [
                [
                    autoSecureInput: autoMode.toString(),
                    passSecureInput: 'true',
                    secureFormat   : 'shell',
                    script         : '''#!/bin/bash
'''+(!autoMode?'eval $(</dev/stdin)\n':'')+'''
echo password was: "$RD_OPTION_PASSWORD"
if [ -n "$RD_OPTION_PASSWORD" ] ; then
    echo "OK: password was seen"
else
    echo "NO: password not seen"
    exit 1
fi'''
                ]
            ]
            def yamlJob = new Yaml().dump([jobdef])
        when:
            def result = JobUtils.createJob(TEST_PROJECT, yamlJob, client, 'application/yaml')
        then:
            result.successful
        when:
            def execResponse = JobUtils.executeJobWithOptions(
                jobUuid,
                client,
                [options: [password: 'mypassword', plain: 'plain text']]
            )
            Execution exec = jsonValue(execResponse.body(), Execution.class)
        then:
            exec.status == "running"
        when:
            def execFinal = waitForExecutionFinish(exec.id as String, WaitingTime.EXCESSIVE)
        then:
            execFinal.status == "succeeded"
        when:
            def output = JobUtils.getExecutionOutput(exec.id, client)
            def logs = output.entries.collect { it.log }
        then:
            logs.contains("password was: mypassword")
            logs.contains("OK: password was seen")
        where:
            autoMode | _
            true     | _
            false    | _
    }

    def "secure option value not available in script ENV vars"() {
        given:
            String jobUuid = UUID.randomUUID().toString()
            def jobdef = new HashMap(BASE_JOB)
            jobdef.name = 'script secure input env var test'+ (autoMode ? ' auto' : ' manual')
            jobdef.uuid = jobUuid
            jobdef.options = [
                [name: 'password', secure: true, type: 'text'],
                [name: 'plain', type: 'text', value: 'asdf'],
                [name: 'insecurepass', secure: true, type: 'text', valueExposed: true]
            ]
            jobdef.nodefilters = [
                filter: 'tags: "newlocal"'
            ]
            jobdef.sequence.commands = [
                [
                    autoSecureInput: autoMode.toString(),
                    passSecureInput: 'true',
                    secureFormat   : 'shell',
                    script         : '''#!/bin/bash
''' + (!autoMode ? 'eval $(</dev/stdin)\n' : '') + '''
env | grep RD_OPTION_PASSWORD
if [ $? = 1 ] ; then
    echo "OK: env var not found"
else
    echo "FAIL: env var was present"
    exit 1
fi
'''
                ]
            ]
            def yamlJob = new Yaml().dump([jobdef])
        when:
            def result = JobUtils.createJob(TEST_PROJECT, yamlJob, client, 'application/yaml')
        then:
            result.successful
        when:
            def execResponse = JobUtils.executeJobWithOptions(
                jobUuid,
                client,
                [options: [password: 'mypassword', plain: 'plain text']]
            )
            Execution exec = jsonValue(execResponse.body(), Execution.class)
        then:
            exec.status == "running"
        when:
            def execFinal = waitForExecutionFinish(exec.id as String, WaitingTime.EXCESSIVE)
        then:
            execFinal.status == "succeeded"
        when:
            def output = JobUtils.getExecutionOutput(exec.id, client)
            def logs = output.entries.collect { it.log }
        then:
            logs.contains("OK: env var not found")
        where:
            autoMode | _
            true     | _
            false    | _
    }

}
