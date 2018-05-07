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

package com.dtolabs.rundeck.app.internal.logging

import spock.lang.Specification

/**
 * @author greg
 * @since 5/26/17
 */
class RundeckLogFormatSpec extends Specification {
    def "parseLine: first multiline entry should have newline"() {
        given:
        def format = new RundeckLogFormat()

        def expectPrefix1 = "^2013-05-24T01:31:02Z||DEBUG|{something=else|test=1}|"

        def line = expectPrefix1 + partial
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        !item.lineComplete
        item.entry.message == partial + '\n'


        where:
        partial     | _
        'a message' | _
    }

    def "parseLine: no prefix should be partial"() {
        given:
        def format = new RundeckLogFormat()


        def line = partial
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        !item.lineComplete
        item.entry == null
        item.partial == partial + '\n'


        where:
        partial     | _
        'a message' | _
    }
    def "parseLine: delimiter only should finish a partial"() {
        given:
        def format = new RundeckLogFormat()


        def line = '^'
        when:
        LineLogFormat.FormatItem item = format.parseLine(line)

        then:
        item.lineComplete
        item.entry == null
        item.partial == ''


    }
}
