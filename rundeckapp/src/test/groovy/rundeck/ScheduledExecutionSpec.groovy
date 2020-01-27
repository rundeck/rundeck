/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck

import spock.lang.Specification

/**
 * Created by greg on 10/21/15.
 */
class ScheduledExecutionSpec extends Specification {
    def "has nodes selected by default"() {
        given:
            def se = new ScheduledExecution(nodesSelectedByDefault: value)

        when:
            def result = se.hasNodesSelectedByDefault()

        then:
            result == expected

        where:
            value | expected
            null  | true
            true  | true
            false | false
    }

    def "from map options have ref to job"() {
        given:
            def map = [
                    jobName: 'abc',
                    options: [
                            [name: 'test1', required: false],
                            [name: 'test2', required: false],
                    ]
            ]
        when:
            def se = ScheduledExecution.fromMap(map)

        then:
            se.options
            se.options.size() == 2
            se.options.every { it.scheduledExecution == se }
    }

}
