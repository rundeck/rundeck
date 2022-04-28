/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components

import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat
import spock.lang.Specification
import spock.lang.Unroll

class JobYAMLFormatSpec extends Specification {

    @Unroll
    def "decode expects list of maps"() {
        given:
            def sut = new JobYAMLFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            JobDefinitionException e = thrown()
            e.message.contains(expected)

        where:
            input              | expected
            '{"a":"b"}'        | 'Expected list data'
            'asdf'             | 'Expected list data'
            '1123'             | 'Expected list data'
            'true'             | 'Expected list data'
            '[1]'              | 'Expected list of Maps'
            '[asdf]'           | 'Expected list of Maps'
            '[[1]]'            | 'Expected list of Maps'
            '[true]'           | 'Expected list of Maps'
            '[{"a":"b"},1]'    | 'Expected list of Maps'
            '[{"a":"b"},asdf]' | 'Expected list of Maps'
            '[{"a":"b"},true]' | 'Expected list of Maps'
            '[{"a":"b"},[1]]'  | 'Expected list of Maps'
    }

    @Unroll
    def "encode"() {
        given:
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(true, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input | expected
            [[:]] | '- {}\n'
    }

    @Unroll
    def "encode option preserveId"() {
        given:
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(preserve, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                          | preserve | expected
            [[id: 'test1', uuid: 'test2']] | true     | '- id: test1\n  uuid: test2\n'
            [[id: 'test1', uuid: 'test2']] | false    | '- {}\n'
    }

    @Unroll
    def "encode option replaceIds"() {
        given:
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(false, replacements, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                          | replacements | expected
            [[id: 'test1', uuid: 'test1']] | [:]          | '- {}\n'
            [[id: 'test1', uuid: 'test1']] | [test1: 'x'] | '- id: x\n  uuid: x\n'
            [[id: 'test1', uuid: 'test1']] | [test2: 'x'] | '- {}\n'
    }

    @Unroll
    def "encoding is canonical"() {
        given:
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                               | expected
            [[z: 'xyz', a: 'pqr']]              | '- a: pqr\n  z: xyz\n'
            [[a: 'pqr', z: 'xyz']]              | '- a: pqr\n  z: xyz\n'
            [[z: [Z: 'z', A: 'A'], a: 'pqr']]   | '- a: pqr\n  z:\n    A: A\n    Z: z\n'
            [[z: [[Z: 'z', A: 'A']], a: 'pqr']] | '- a: pqr\n  z:\n  - A: A\n    Z: z\n'
    }

    @Unroll
    def "encoding multiline line endings are unix"() {
        given:
            def sut = new JobYAMLFormat(trimSpacesFromLines: trimSpaces)
            def writer = new StringWriter()
            def data = [[a: text]]
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            text            | trimSpaces    | expected
            'ab\nc'         | false         | '- a: |-\n    ab\n    c\n'
            'ab\r\nc'       | false         | '- a: |-\n    ab\n    c\n'
            'ab\rc'         | false         | '- a: |-\n    ab\n    c\n'
            'ab \rc'        | false         | '- a: "ab \\nc"\n'
            'ab \rc'        | true          | '- a: |-\n    ab\n    c\n'
            'ab\n \nc'      | false         | '- a: "ab\\n \\nc"\n'
            'ab\n \nc'      | true          | '- a: |-\n    ab\n\n    c\n'
            'ab\n \nc \n '  | true          | '- a: |\n    ab\n\n    c\n'
            'ab\n \n c \n ' | true          | '- a: |\n    ab\n\n     c\n'
    }
    @Unroll
    def "encoding comma strings are quoted"() {
        given:
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = [[a: text]]
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            text   | expected
            'abc'  | '- a: abc\n'
            '123'  | '- a: \'123\'\n'
            '1,23' | '- a: \'1,23\'\n'
            'a,bc' | '- a: \'a,bc\'\n'
    }
    @Unroll
    def "should return a notification map with email and webhook notifs"() {
        given:
        def input = "" +
                "- defaultTab: nodes\n" +
                "  description: ''\n" +
                "  executionEnabled: true\n" +
                "  loglevel: INFO\n" +
                "  name: a\n" +
                "  nodeFilterEditable: false\n" +
                "  notification:\n" +
                "    onsuccess:\n" +
                "    - email:\n" +
                "        attachLog: true\n" +
                "        attachLogInFile: true\n" +
                "        recipients: leojesus.juarez@gmail.com\n" +
                "        subject: RD-SUCCESS\n" +
                "    - email:\n" +
                "        attachLog: true\n" +
                "        attachLogInline: true\n" +
                "        recipients: 2@gmail.com\n" +
                "        subject: RD-SUCCESS\n" +
                "    - format: xml\n" +
                "      httpMethod: get\n" +
                "      urls: http://localhost:4440/project\n" +
                "    - format: json\n" +
                "      httpMethod: post\n" +
                "      urls: http://localhost:4440/project\n" +
                "  notifyAvgDurationThreshold: null\n" +
                "  plugins:\n" +
                "    ExecutionLifecycle: null\n" +
                "  scheduleEnabled: true\n" +
                "  schedules: []\n" +
                "  sequence:\n" +
                "    commands:\n" +
                "    - exec: asd\n" +
                "    keepgoing: false\n" +
                "    strategy: node-first"
        def sut = new JobYAMLFormat()
        when:
        def result = sut.decode(new StringReader(input))
        then:
        result[0].notification['onsuccess'].size() == 4
        result[0].notification['onsuccess'].findAll{ it['email'] != null }.size() == 2
        result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 2
    }

    @Unroll
    def "should return an xml str with email and webhook tags"() {
        given:
        List<Map> input = [[
                                   id: 0,
                                   defaultTab: "nodes",
                                   description: "",
                                   loglevel: "INFO",
                                   name: "a",
                                   nodeFilterEditable: false,
                                   notification: [
                                           onsuccess: [
                                                   [email: [recipients: "mail@example.com"]],
                                                   [email: [recipients: "mail2@example.com"]],
                                                   [
                                                           format: "xml",
                                                           httpMethod: "get",
                                                           urls: "http://example1.com/1"
                                                   ],
                                                   [
                                                           format: "json",
                                                           httpMethod: "post",
                                                           urls: "https://example2.com/2"
                                                   ]
                                           ]
                                   ],
                                   notifyAvgDurationThreshold: "",
                                   plugins: "",
                                   scheduledEnabled: "true",
                                   schedules: "",
                                   sequence: [
                                           keepgoing: false,
                                           strategy: "node-first",
                                           commands: [[exec:"asd"]]
                                   ],
                                   executionEnabled: true,
                                   multipleExecutions: false
                           ]]
        def sut = new JobYAMLFormat()
        def writer = new StringWriter()
        def options = JobFormat.options(true, [:], (String) null)

        when:
        sut.encode(input, options, writer)
        String notifStr = writer.toString()
        notifStr = notifStr.substring(notifStr.indexOf("onsuccess:") + "onsuccess:".length()).replaceAll("\\s","")

        then:
        notifStr.contains("-email:recipients:mail@example.com")
        notifStr.contains("-email:recipients:mail@example.com")
        notifStr.contains("urls:http://example1.com/1")
        notifStr.contains("urls:https://example2.com/2")
    }
}
