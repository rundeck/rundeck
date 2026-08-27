package org.rundeck.tests.functional.api.job.input

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.execution.Execution
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml

/**
 * RUN-4693: end-to-end coverage for the opt-in option-value allowlist
 * ({@code project.option.input.validation.default.pattern}) against the Inline Script sink.
 *
 * <p>The Inline Script step expands {@code @option.x@} verbatim into the executed temp script, so an
 * option value carrying shell metacharacters is a command-injection vector. These tests run the
 * actual PoC through a real execution:
 * <ul>
 *   <li>with the allowlist configured, the malicious value is rejected before any execution runs;</li>
 *   <li>with no allowlist (the default), the same value is executed — documenting that the control is
 *   opt-in and the sink is still vulnerable by default.</li>
 * </ul>
 */
@APITest
class OptionInputValidationSpec extends BaseContainer {

    // Project WITH the allowlist configured — malicious option values must be rejected.
    public static final String BLOCKED_PROJECT = 'OptionInputValidationBlocked'
    // Project WITHOUT the allowlist — the default, still-vulnerable configuration.
    public static final String CONTROL_PROJECT = 'OptionInputValidationControl'

    // Allowlist that permits only benign characters; shell metacharacters (" ; etc.) are rejected.
    public static final String ALLOWLIST = '[a-zA-Z0-9 ._-]+'

    // Project-level config key for the allowlist (mirrors AppConstants.PROJECT_OPTION_INPUT_DEFAULT_PATTERN;
    // duplicated here to avoid the test module depending on server-side classes).
    public static final String PROJECT_PATTERN_KEY = 'project.option.input.validation.default.pattern'

    // Inline Script step that echoes the option value through the vulnerable @option.x@ sink.
    public static final String SCRIPT = '''#!/bin/bash
echo "target=@option.target@"
'''

    def setupSpec() {
        startEnvironment()
        setupProject(BLOCKED_PROJECT, [(PROJECT_PATTERN_KEY): ALLOWLIST])
        setupProject(CONTROL_PROJECT)
    }

    private static Map<String, Object> inlineScriptJob(String name, String uuid) {
        return [
            name            : name,
            uuid            : uuid,
            description     : '',
            executionEnabled: true,
            loglevel        : 'INFO',
            options         : [
                [name: 'target', type: 'text']
            ],
            sequence        : [
                commands : [
                    [script: SCRIPT]
                ],
                keepgoing: false,
                strategy : 'node-first'
            ]
        ]
    }

    def "option value violating the configured allowlist is rejected before any execution (RUN-4693 PoC)"() {
        given: 'a job whose Inline Script step references @option.target@, in a project with the allowlist enabled'
            String jobUuid = UUID.randomUUID().toString()
            def yamlJob = new Yaml().dump([inlineScriptJob('inline script option injection - blocked', jobUuid)])
            JobUtils.createJob(BLOCKED_PROJECT, yamlJob, client, 'application/yaml')

        when: 'the H1 PoC payload is submitted as the option value'
            def response = JobUtils.executeJobWithOptions(jobUuid, client, [options: [target: '"; id; echo "']])
            def body = response.body().string()

        then: 'the run is rejected by the allowlist and no execution is started'
            response.code() == 400
            body.contains('options-invalid') || body.toLowerCase().contains('validation pattern')
    }

    def "without an allowlist the inline-script sink executes the injected command (RUN-4693 default is vulnerable)"() {
        given: 'the same job in a project with NO allowlist configured (the default)'
            String jobUuid = UUID.randomUUID().toString()
            def yamlJob = new Yaml().dump([inlineScriptJob('inline script option injection - control', jobUuid)])
            JobUtils.createJob(CONTROL_PROJECT, yamlJob, client, 'application/yaml')

        when: 'an option value that injects a second command is submitted'
            def execResponse = JobUtils.executeJobWithOptions(jobUuid, client, [options: [target: 'x; echo INJECTED']])
            Execution exec = jsonValue(execResponse.body(), Execution.class)

        then: 'the execution is accepted and starts'
            exec.status == 'running'

        when:
            def execFinal = waitForExecutionFinish(exec.id as String, WaitingTime.EXCESSIVE)

        then: 'it succeeds'
            execFinal.status == 'succeeded'

        when:
            def output = JobUtils.getExecutionOutput(exec.id, client)
            def logs = output.entries.collect { it.log }

        then: 'the injected command ran — proving the sink is still exploitable without the allowlist'
            logs.any { it.contains('INJECTED') }
    }
}
