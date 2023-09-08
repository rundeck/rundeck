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

import grails.testing.gorm.DataTest
import spock.lang.Specification

/**
 * @author greg
 * @since 6/26/17
 */
class JobExecSpec extends Specification implements DataTest {

    def setupSpec() { mockDomains JobExec, ScheduledExecution, Workflow, CommandExec }

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
                description: 'a monkey',
                enabled: true
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
                description: 'a monkey',
                enabled: true
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
                description: 'a monkey',
                enabled: true
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

    def "from map use name without useName property"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name',
                        uuid       : uuid,
                        useName    : useName
                        ],
                description: 'a monkey'
        ]
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.useName == useNameResult
        where:
        uuid   | useName | useNameResult
        null   | null    | true
        'uuid' | null    | false
        null   | false   | false
        null   | true    | true
        'uuid' | true    | true

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

    def "to map with importOptions"() {
        when:
        Map map = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeFilter: 'abc def',
                importOptions: importOption,
                nodeThreadcount: 2,
        ).toMap()

        then:

        if(importOption) {
            map == [
                    jobref     : [
                            group        : 'group',
                            name         : 'name',
                            importOptions: 'true',
                            nodefilters  : [
                                    filter  : 'abc def',
                                    dispatch: [
                                            threadcount: 2
                                    ]
                            ]
                    ],
                    description: 'a monkey'
            ]
        }else{
            map == [
                    jobref     : [
                            group        : 'group',
                            name         : 'name',
                            nodefilters  : [
                                    filter  : 'abc def',
                                    dispatch: [
                                            threadcount: 2
                                    ]
                            ]
                    ],
                    description: 'a monkey'
            ]
        }

        where:
        importOption    | _
        true            | _
        false           | _
        null            | _
    }

    def "from map with importOptions"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name'
                ],
                description: 'a monkey'
        ]
        if(importOption != null) {
            map.jobref.importOptions = importOption
        }
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.importOptions == (importOption?.equals('true')?:null)
        where:
        importOption    | _
        'true'          | _
        'false'         | _
        null            | _

    }

    def "to map with failOnDisable"() {
        when:
        Map map = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeFilter: 'abc def',
                failOnDisable: failOnDisable,
                nodeThreadcount: 2,
        ).toMap()

        then:

        if(failOnDisable) {
            map == [
                    jobref     : [
                            group        : 'group',
                            name         : 'name',
                            failOnDisable: 'true',
                            nodefilters  : [
                                    filter  : 'abc def',
                                    dispatch: [
                                            threadcount: 2
                                    ]
                            ]
                    ],
                    description: 'a monkey'
            ]
        }else{
            map == [
                    jobref     : [
                            group        : 'group',
                            name         : 'name',
                            nodefilters  : [
                                    filter  : 'abc def',
                                    dispatch: [
                                            threadcount: 2
                                    ]
                            ]
                    ],
                    description: 'a monkey'
            ]
        }

        where:
        failOnDisable    | _
        true            | _
        false           | _
        null            | _
    }

    def "from map with failOnDisable"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name'
                ],
                description: 'a monkey'
        ]

        if(failOnDisable != null) {
            map.jobref.failOnDisable = failOnDisable
        }
      
        when:
        def result = JobExec.jobExecFromMap(map)

        then:

        result.failOnDisable == (failOnDisable?.equals('true')?:null)
        where:
        failOnDisable    | _
        'true'          | _
        'false'         | _
        null            | _

    }
    def "from map with failOnDisable (backwards compat)"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name'
                ],
                description: 'a monkey'
        ]
        if(failOnDisable != null) {
            map.failOnDisable = failOnDisable
        }
        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.failOnDisable == (failOnDisable?.equals('true')?:null)
        where:
        failOnDisable    | _
        'true'          | _
        'false'         | _
        null            | _

    }

    def "map with importOptions"() {
        given:
        def map = [
                jobref     : [
                        group      : 'group',
                        name       : 'name'
                ],
                description: 'a monkey'
        ]

        if(importOption != null) {
            map.importOptions = importOption
        }

        when:
        def result = JobExec.jobExecFromMap(map)

        then:
        result.importOptions == (importOption?.equals('true')?:null)

        where:
        importOption    | _
        'true'          | _
        'false'         | _
        null            | _

    }  
  
    static uuid1 = UUID.randomUUID().toString()
    static uuid2 = UUID.randomUUID().toString()
    static uuid3 = UUID.randomUUID().toString()

    def "find job"() {
        given:
            def job1 = new ScheduledExecution(
                    jobName: 'jobname1',
                    groupPath: 'group1',
                    project: 'projectA',
                    uuid: uuid1,
                    workflow: new Workflow(
                            commands: [
                                    new CommandExec(adhocRemoteString: 'asdf').save()
                            ]
                    ).save()
            ).save()
            def job2 = new ScheduledExecution(
                    jobName: 'jobname2',
                    groupPath: 'group2',
                    project: 'projectB',
                    uuid: uuid2,
                    workflow: new Workflow(
                            commands: [
                                    new CommandExec(adhocRemoteString: 'asdf').save()
                            ]
                    ).save()
            ).save()
            def job3 = new ScheduledExecution(
                    jobName: 'jobname2',
                    groupPath: 'group2',
                    project: 'projectC',
                    uuid: uuid3,
                    workflow: new Workflow(
                            commands: [
                                    new CommandExec(adhocRemoteString: 'asdf').save()
                            ]
                    ).save()
            ).save()
            def je = new JobExec(
                    jobGroup: jobGroup,
                    jobName: jobName,
                    jobProject: jobProject,
                    useName: useName,
                    uuid: uuid

            )
        when:
            def result = je.findJob(project)
        then:
            if (expect) {
                result != null
                result.uuid == expect
            } else {
                result == null
            }
            job1.id
            job2.id
            job3.id

        where:
            //useName defaults true when uuid is null
            useName | uuid  | jobName    | jobGroup | jobProject | project    | expect
            true    | null  | 'jobname1' | 'group1' | null       | 'projectA' | uuid1
            true    | uuid2 | 'jobname1' | 'group1' | null       | 'projectA' | uuid1
            false   | null  | 'jobname1' | 'group1' | null       | 'projectA' | uuid1
            true    | null  | 'jobname1' | 'group1' | null       | 'projectB' | null
            true    | uuid2 | 'jobname1' | 'group1' | null       | 'projectB' | null
            false   | null  | 'jobname1' | 'group1' | null       | 'projectB' | null
            true    | null  | 'jobname1' | 'group1' | 'projectA' | 'projectB' | uuid1
            true    | uuid2 | 'jobname1' | 'group1' | 'projectA' | 'projectB' | uuid1
            false   | null  | 'jobname1' | 'group1' | 'projectA' | 'projectB' | uuid1
            true    | null  | 'jobname1' | 'group1' | 'projectA' | null       | uuid1
            true    | uuid2 | 'jobname1' | 'group1' | 'projectA' | null       | uuid1
            false   | null  | 'jobname1' | 'group1' | 'projectA' | null       | uuid1

            //same jobname/group, different projects
            true    | null  | 'jobname2' | 'group2' | 'projectB' | null       | uuid2
            true    | null  | 'jobname2' | 'group2' | 'projectB' | 'projectC' | uuid2
            true    | null  | 'jobname2' | 'group2' | null       | 'projectB' | uuid2

            true    | null  | 'jobname2' | 'group2' | 'projectC' | null       | uuid3
            true    | null  | 'jobname2' | 'group2' | 'projectC' | 'projectB' | uuid3
            true    | null  | 'jobname2' | 'group2' | null       | 'projectC' | uuid3

            //search by uuid
            false   | uuid1 | null       | null     | null       | 'projectA' | uuid1
            false   | uuid1 | null       | null     | null       | 'projectB' | uuid1
            false   | uuid1 | null       | null     | null       | 'projectC' | uuid1


    }
}
