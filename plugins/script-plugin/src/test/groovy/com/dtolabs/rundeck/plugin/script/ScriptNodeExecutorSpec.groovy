/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.plugin.script

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for ScriptNodeExecutor child-process cleanup on job abort.
 *
 * RUN-4429: child processes must be killed when a job is aborted.
 *
 * NOTE: ProcessBuilder is a final Java class; GroovyMock cannot intercept constructor
 * calls from Java code (only Groovy callers). These tests run real subprocesses and verify
 * OS-level process state rather than mocking the process-spawn layer.
 */
@IgnoreIf({ System.getProperty("os.name", "").toLowerCase().contains("windows") })
class ScriptNodeExecutorSpec extends Specification {

    /**
     * Failing test (TDD): proves that before the fix, aborting the execution thread leaves the
     * child process alive because ScriptNodeExecutor did not call any kill handler.
     *
     * After the fix (ScriptExecUtil.runLocalCommand with killProcessHandleDescend), the child
     * process is killed: first SIGTERM, then SIGKILL after the grace period.
     */
    @Timeout(60)
    def "executeCommand kills child processes when execution thread is interrupted"() {
        given: "a temp script that forks a sleep child and records its PID"
        def pidFile = File.createTempFile("rundeck-child-pid-", ".txt")
        pidFile.deleteOnExit()
        def scriptFile = File.createTempFile("rundeck-test-script-", ".sh")
        scriptFile.deleteOnExit()
        scriptFile.text = """\
#!/bin/bash
sleep 300 &
echo \$! > '${pidFile.absolutePath}'
wait
""".stripIndent()
        scriptFile.setExecutable(true)

        and: "a minimal ExecutionContext wired to a mock Framework"
        def listener = Mock(ExecutionListener)
        def framework = Mock(Framework) {
            getProjectProperty(_, _) >> null
        }
        def executionContext = Mock(ExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> "test-project"
            getDataContext() >> [:]
            getExecutionListener() >> listener
            getOutputContext() >> null
        }

        and: "a node whose script-exec runs the temp script via /bin/bash"
        def node = Mock(INodeEntry) {
            getAttributes() >> [
                "script-exec"      : scriptFile.absolutePath,
                "script-exec-shell": "/bin/bash"
            ]
            getNodename() >> "test-node"
        }

        when: "the executor runs in a background thread, then is interrupted to simulate job abort"
        def executor = new ScriptNodeExecutor()
        def executionDone = new CountDownLatch(1)
        def execThread = new Thread({
            executor.executeCommand(executionContext, [] as String[], node)
            executionDone.countDown()
        })
        execThread.start()

        // Wait up to 10 s for the child PID file to appear
        boolean childStarted = false
        for (int i = 0; i < 100 && !childStarted; i++) {
            Thread.sleep(100)
            childStarted = pidFile.length() > 0
        }
        assert childStarted : "Child process did not write PID within 10 seconds — script: ${scriptFile.absolutePath}"

        def childPid = pidFile.text.trim().toLong()
        execThread.interrupt()
        executionDone.await(15, TimeUnit.SECONDS)
        // Allow a brief moment for the kill signal to propagate
        Thread.sleep(500)

        then: "the child process (sleep 300) must be dead after abort — fails before the fix"
        !ProcessHandle.of(childPid).map { it.isAlive() }.orElse(false)

        cleanup:
        pidFile.delete()
        scriptFile.delete()
    }
}
