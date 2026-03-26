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

import java.util.concurrent.CountDownLatch

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
     * Verifies that a command using the shell background operator ({@code &}) completes
     * without hanging, even though the background process keeps the inherited pipe FDs open.
     * <p>
     * Prior to the fix, {@code errthread.join()} / {@code outthread.join()} would block
     * indefinitely because the background {@code sleep} process held the write-end of the
     * stderr pipe open. After the fix, the streams are forcibly closed after
     * {@link ScriptExecUtil#STREAM_DRAIN_TIMEOUT_MS} and the step completes successfully.
     */
    @Timeout(10)
    def "background command with & operator completes without hanging"() {
        given:
        Map<String, String> envMap = [:]
        // sleep 60 keeps inherited FDs open — without the fix this would hang for 60s
        String[] command = ["/bin/sh", "-c", "sleep 60 &"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "shell exits with code 0 and execution does not hang"
        result == 0
    }

    /**
     * Verifies that stdout output produced before the {@code &} background operator
     * is captured correctly, confirming that the drain window is sufficient.
     */
    @Timeout(10)
    def "background command output before & is captured"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "echo hello; sleep 60 &"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "exit code is 0 and stdout output from the foreground echo is captured"
        result == 0
        outStream.toString().trim() == "hello"
    }

    /**
     * Verifies that a normal (non-background) command that fails with a non-zero exit code
     * is still reported correctly — the fix must not affect error handling for regular commands.
     */
    @Timeout(10)
    def "non-background command failure is still reported"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "exit 42"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "non-zero exit code is propagated correctly"
        result == 42
    }

    /**
     * Verifies that stderr output from a normal (non-background) command is still
     * captured correctly after the fix.
     */
    @Timeout(10)
    def "normal command stderr output is captured"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "echo error-output >&2"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then:
        result == 0
        errStream.toString().trim() == "error-output"
    }


    /**
     * Verifies that a command using the shell background operator ({@code &}) completes
     * without hanging, even though the background process keeps the inherited pipe FDs open.
     * <p>
     * Prior to the fix, {@code errthread.join()} / {@code outthread.join()} would block
     * indefinitely because the background {@code sleep} process held the write-end of the
     * stderr pipe open. After the fix, the streams are forcibly closed after
     * {@link ScriptExecUtil#STREAM_DRAIN_TIMEOUT_MS} and the step completes successfully.
     */
    @Timeout(10)
    def "background command with & operator completes without hanging"() {
        given:
        Map<String, String> envMap = [:]
        // sleep keeps the inherited FDs open for 60s — without the fix this would hang
        String[] command = ["/bin/sh", "-c", "sleep 60 &"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "shell exits with code 0 and execution does not hang"
        result == 0
    }

    /**
     * Verifies that a background command that also produces stdout output before
     * backgrounding completes correctly and the output is captured.
     */
    @Timeout(10)
    def "background command output before & is captured"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "echo hello; sleep 60 &"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "exit code is 0 and stdout output from the foreground echo is captured"
        result == 0
        outStream.toString().trim() == "hello"
    }

    /**
     * Verifies that a normal (non-background) command that fails with a non-zero exit code
     * is still reported correctly — i.e. the fix does not affect error handling for regular
     * commands.
     */
    @Timeout(10)
    def "non-background command failure is still reported"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "exit 42"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then: "non-zero exit code is propagated correctly"
        result == 42
    }

    /**
     * Verifies that stderr output from a normal (non-background) command is still
     * captured and that stream exceptions are still propagated when no background
     * process is involved.
     */
    @Timeout(10)
    def "normal command stderr output is captured"() {
        given:
        Map<String, String> envMap = [:]
        String[] command = ["/bin/sh", "-c", "echo error-output >&2"]
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        ByteArrayOutputStream errStream = new ByteArrayOutputStream()

        when:
        int result = ScriptExecUtil.runLocalCommand(command, envMap, null, outStream, errStream)

        then:
        result == 0
        errStream.toString().trim() == "error-output"
    }
}
