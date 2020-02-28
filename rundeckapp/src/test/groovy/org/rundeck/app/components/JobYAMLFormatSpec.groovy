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
            def sut = new JobYAMLFormat()
            def writer = new StringWriter()
            def data = [[a: text]]
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            text      | expected
            'ab\nc'   | '- a: |-\n    ab\n    c\n'
            'ab\r\nc' | '- a: |-\n    ab\n    c\n'
            'ab\rc'   | '- a: |-\n    ab\n    c\n'
    }
}
