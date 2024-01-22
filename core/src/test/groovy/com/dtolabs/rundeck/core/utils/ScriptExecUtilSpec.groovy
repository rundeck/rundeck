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
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // skip test on Windows
            return
        }

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
}
