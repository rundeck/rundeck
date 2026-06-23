/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.utils

import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.stream.Stream

/**
 * @author greg
 * @since 4/19/17
 */
class ScriptExecUtilSpec extends Specification {
    def "expand data in scriptinterpreter"() {
        given:
        Map<String, Map<String, String>> dcontext = [option: ["blah": "sam"], globals: ["sudo": "/bin/sudo"]]
        String scriptargs = argstring
        String[] scriptargsarr = argsarray
        when:
        def result = ScriptExecUtil.createScriptArgs(
                dcontext,
                scriptargs,
                scriptargsarr,
                scriptinterpreter,
                quoted,
                '/a/file'
        )

        then:
        result == expected

        where:
        argstring | argsarray    | scriptinterpreter        | quoted | expected
        null      | ["a", "arg"] | 'bob'                    | false  | ['bob', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${option.blah}'         | false  | ['sam', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${globals.sudo}'        | false  | ['/bin/sudo', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${globals.sudo} myexec' | false  | ['/bin/sudo', 'myexec', '/a/file', 'a', 'arg']
    }

    def "interrupt local command should kill subprocess"() {

        when:
        Map<String, String> envMap = [:]
        String[] command = ["sleep", "10"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        def started = new CountDownLatch(1)
        def main = new CountDownLatch(1)
        GroovyMock(Runtime, global: true)
        String interruptedMessageExpected = "Execution interrupted with code: 143"
        String interruptedMessage = ""
        def t = new Thread(
                {
                    started.countDown()
                    try {
                        ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)
                    }catch(InterruptedException e){
                        interruptedMessage = e.getMessage()
                        Thread.currentThread().interrupt()
                    } finally {
                        main.countDown()
                    }
                }
        )
        t.start()
        started.await()
        Thread.sleep(2000)
        t.interrupt()
        main.await()

        then:
        interruptedMessageExpected == interruptedMessage
    }

    /**
     * Failing test (TDD): proves that before the fix, killProcessHandleDescend only sends
     * SIGTERM and never escalates to SIGKILL for a process that ignores SIGTERM.
     *
     * After the fix (awaitOrKill helper with grace period), destroyForcibly() is called on
     * any process still alive after KILL_GRACE_PERIOD_MS.
     */
    @Timeout(10)
    def "killProcessHandleDescend escalates to SIGKILL when process ignores SIGTERM"() {
        given: "a CompletableFuture whose get(timeout, unit) immediately throws TimeoutException"
        // Using an immediate timeout avoids waiting the full KILL_GRACE_PERIOD_MS in the test
        def immediateTimeout = new CompletableFuture<ProcessHandle>() {
            @Override ProcessHandle get(long timeout, TimeUnit unit) throws TimeoutException {
                throw new TimeoutException("immediate timeout for test")
            }
        }

        and: "a child ProcessHandle that stays alive after destroy() (SIGTERM ignored)"
        def childHandle = Mock(ProcessHandle) {
            isAlive() >> true
            destroy() >> false
            destroyForcibly() >> true
            onExit() >> immediateTimeout
        }

        and: "a parent ProcessHandle whose descendants() includes the stubborn child"
        def parentHandle = Mock(ProcessHandle) {
            // Return a fresh stream each call — Java streams can only be consumed once
            descendants() >> { Stream.of(childHandle) }
            isAlive() >> true
            destroy() >> false
            destroyForcibly() >> true
            onExit() >> immediateTimeout
        }

        when:
        ScriptExecUtil.killProcessHandleDescend(parentHandle)

        then: "SIGKILL (destroyForcibly) must be called on the child that survived SIGTERM (fails before fix)"
        1 * childHandle.destroyForcibly()

        and: "SIGKILL must also be called on the parent (fails before fix)"
        1 * parentHandle.destroyForcibly()
    }
}
