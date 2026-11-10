/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.common

import spock.lang.Specification
import spock.lang.TempDir

/**
 * Verifies that YAML parse failures identify the offending file.
 */
class NodesYamlParserSpec extends Specification {
    @TempDir
    File tempDir

    def "parse failure for a file includes the file path"() {
        given:
        def file = new File(tempDir, 'bad-nodes.yaml')
        file.text = 'node1: [unclosed'
        def parser = new NodesYamlParser(file, Mock(NodeReceiver))

        when:
        parser.parse()

        then:
        NodeFileParserException e = thrown()
        e.message.contains(file.absolutePath)
        e.cause != null
    }

    def "parse failure for a stream has no file path"() {
        given:
        def parser = new NodesYamlParser(new ByteArrayInputStream('node1: [unclosed'.bytes), Mock(NodeReceiver))

        when:
        parser.parse()

        then:
        NodeFileParserException e = thrown()
        !e.message.contains('Error parsing YAML file')
    }
}
