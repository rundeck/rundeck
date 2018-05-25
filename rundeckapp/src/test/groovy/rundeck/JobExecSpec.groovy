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

package rundeck

import spock.lang.Specification

/**
 * @author greg
 * @since 6/26/17
 */
class JobExecSpec extends Specification {
    def "to map with node filter"() {
        when:
        Map map = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeFilter: 'abc def',
                nodeThreadcount: 2,
                nodeIntersect: nodeIntersect
        ).toMap()

        then:

        map == [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        nodefilters: [
                                filter  : 'abc def',
                                dispatch: [
                                        threadcount  : 2,
                                        nodeIntersect: nodeIntersect
                                ]
                        ]
                ],
                description: 'a monkey'
        ]

        where:
        nodeIntersect | _
        true          | _
        false         | _
    }
    def "to map with node intersect without filter"() {
        when:
        Map map = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeIntersect: nodeIntersect
        ).toMap()

        then:

        map == [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        nodefilters: [
                                dispatch: [
                                        nodeIntersect: nodeIntersect
                                ]
                        ]
                ],
                description: 'a monkey'
        ]

        where:
        nodeIntersect | _
        true          | _
        false         | _
    }

    def "to map with project"() {
        when:
        Map map = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                jobProject:'projectB',
                description: 'a monkey',
        ).toMap()

        then:

        map == [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        project: 'projectB',
                ],
                description: 'a monkey'
        ]

    }

    def "from map with node intersect"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        nodefilters: [
                                dispatch: [
                                        nodeIntersect: nodeIntersect
                                ]
                        ]
                ],
                description: 'a monkey'
        ]
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.nodeIntersect == nodeIntersect
        where:
        nodeIntersect | _
        true          | _
        false         | _

    }
    def "from map with jobref.project"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        project:'projectB',
                ],
                description: 'a monkey'
        ]
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.jobProject == 'projectB'

    }
    def "from map with project"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                ],
                project:'projectB',
                description: 'a monkey'
        ]
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.jobProject == 'projectB'

    }
}
