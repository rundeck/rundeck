package rundeck
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

//import grails.test.GrailsUnitTestCase

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Workflow
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

/*
 * rundeck.JobExecTests.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 5/14/12 11:41 AM
 *
 */
class JobExecTests extends Specification implements DataTest{
    @Override
    Class[] getDomainClassesToMock() {
        [JobExec,CommandExec,Workflow]
    }

    def testBasicToMap() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name')
        then:
        assertEquals([jobref: [group:'group',name:'name'], enabled: true], t.toMap())
    }

    def testBasicToMapDesc() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey')
        then:
        assertEquals([jobref: [group:'group',name:'name'], description: 'a monkey', enabled: true], t.toMap())
    }

    def testBasicToMapNodeFilter() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def')
        then:
        assertEquals([jobref: [group:'group',name:'name', nodefilters:[filter: 'abc def']],
                description: 'a monkey', enabled: true], t.toMap())
    }

    def testBasicToMapNodeFilter_threadcount() {
        when:
        JobExec t = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeFilter: 'abc def',
                nodeThreadcount: 2
        )
        then:
        assertEquals(
                [
                        jobref: [
                                group: 'group',
                                name: 'name',
                                nodefilters: [
                                        filter: 'abc def',
                                        dispatch: [
                                                threadcount: 2
                                        ]
                                ]
                        ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicToMapNodeFilter_keepgoing() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true)
        then:
        assertEquals(
                [
                        jobref:
                                [
                                        group:'group',
                                        name:'name',
                                        nodefilters: [
                                                filter: 'abc def',
                                                dispatch: [
                                                        threadcount: 2,
                                                        keepgoing: true
                                                ]
                                        ]
                                ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicToMapNodeFilter_keepgoingFalse() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: false)
        then:
        assertEquals(
                [
                        jobref:
                                [
                                        group:'group',
                                        name:'name',
                                        nodefilters: [
                                                filter: 'abc def',
                                                dispatch: [
                                                        threadcount: 2,
                                                        keepgoing: false
                                                ]
                                        ]
                                ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicToMapNodeFilter_rankAttribute() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankAttribute: 'rank')
        then:
        assertEquals(
                [
                        jobref:
                                [
                                        group:'group',
                                        name:'name',
                                        nodefilters: [
                                                filter: 'abc def',
                                                dispatch: [
                                                        threadcount: 2,
                                                        keepgoing: true,
                                                        rankAttribute: 'rank'
                                                ]
                                        ]
                                ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicToMapNodeFilter_rankOrder() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankOrderAscending: true)
        then:
        assertEquals(
                [
                        jobref:
                                [
                                        group:'group',
                                        name:'name',
                                        nodefilters: [
                                                filter: 'abc def',
                                                dispatch: [
                                                        threadcount: 2,
                                                        keepgoing: true,
                                                        rankOrder: 'ascending'
                                                ]
                                        ]
                                ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicToMapNodeFilter_rankOrderDescending() {
        when:
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankOrderAscending: false)
        then:
        assertEquals(
                [
                        jobref:
                                [
                                        group:'group',
                                        name:'name',
                                        nodefilters: [
                                                filter: 'abc def',
                                                dispatch: [
                                                        threadcount: 2,
                                                        keepgoing: true,
                                                        rankOrder: 'descending'
                                                ]
                                        ]
                                ],
                        description: 'a monkey',
                        enabled: true
                ],
                t.toMap()
        )
    }

    def testBasicArgsToMap() {
        when:
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name',argString: 'job args')
        then:
        assertEquals([jobref: [group: 'group', name: 'name',args: 'job args'], enabled: true], t.toMap())
    }

    def testSimpleToMap() {
        when:
        JobExec t = new JobExec(jobName: 'name')
        then:
        assertEquals([jobref: [group:'',name:'name'], enabled: true], t.toMap())
    }

    def testSimpleArgsToMap() {
        when:
        JobExec t = new JobExec( jobName: 'name',argString: 'job args')
        then:
        assertEquals([jobref: [group: '', name: 'name',args: 'job args'], enabled: true], t.toMap())
    }

    def testErrorHandlerExecToMap() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name', argString: 'job args')
        t.errorHandler=h
        then:
        assertEquals([jobref: [group: 'group', name: 'name', args: 'job args'], errorhandler: [exec: 'testerr', enabled: true], enabled: true], t.toMap())
    }

    def testErrorHandlerJobRefToMap() {
        when:
        JobExec h = new JobExec(jobGroup: 'group1', jobName: 'name1')
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name', argString: 'job args')
        t.errorHandler = h
        then:
        assertEquals([jobref: [group: 'group', name: 'name', args: 'job args'], errorhandler: [jobref: [group: 'group1', name: 'name1'], enabled: true], enabled: true], t.toMap())
    }

    def testErrorHandlerForExecToMap() {
        when:
        JobExec h = new JobExec(jobGroup: 'group1', jobName: 'name1',argString: 'job args1')
        CommandExec t = new CommandExec(adhocRemoteString: 'testerr', argString: 'job args')
        t.errorHandler = h
        then:
        assertEquals([jobref: [group: 'group1', name: 'name1', args: 'job args1'], enabled: true], t.toMap().errorhandler)
    }

    //test jobExecFromMap
    def testFromMap(){
        when:

        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1']])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals(null, h.nodeFilter)
        assertEquals(null, h.nodeThreadcount)
        assertEquals(null, h.nodeKeepgoing)
        assertNull(h.errorHandler)
    }
    /**test jobExecFromMap with description */
    def testFromMapDesc(){
        when:

        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1'], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals(null,h.nodeFilter)
        assertNull(h.errorHandler)
    }

    /** fromMapw with nodeFilter*/
    def testFromMapNodeFilter(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter:'abc def']], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals('abc def',h.nodeFilter)
        assertNull(h.nodeKeepgoing)
        assertNull(h.nodeThreadcount)
        assertNull(h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }
    /** fromMapw with nodeFilter, nodeThreadcount*/
    def testFromMapNodeFilterThreadCount(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount:3]]], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals('abc def',h.nodeFilter)
        assertEquals(3,h.nodeThreadcount)
        assertNull(h.nodeKeepgoing)
        assertNull(h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }
    /** fromMapw with nodeFilter, nodeThreadcount, nodeKeepgoing*/
    def testFromMapNodeFilterThreadCountKeepgoing(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:true]]], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals('abc def',h.nodeFilter)
        assertEquals(3,h.nodeThreadcount)
        assertEquals(true,h.nodeKeepgoing)
        assertNull(h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }

    /** fromMapw with nodeFilter, nodeThreadcount, nodeKeepgoing=false*/
    def testFromMapNodeFilterThreadCountKeepgoingFalse(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:false]]], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals('abc def',h.nodeFilter)
        assertEquals(3,h.nodeThreadcount)
        assertEquals(false,h.nodeKeepgoing)
        assertNull(h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }
    /** fromMapw with nodeFilter, nodeThreadcount, nodeKeepgoing='false' (String*/
    def testFromMapNodeFilterThreadCountKeepgoingFalseString(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:'false']]], description: 'a blue'])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals('abc def',h.nodeFilter)
        assertEquals(3,h.nodeThreadcount)
        assertEquals(false,h.nodeKeepgoing)
        assertNull(h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }
    /** fromMap with nodeFilter, rankAttribute*/
    def testFromMapNodeFilterRankAttribute() {
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                                                     args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [rankAttribute: 'rank']]], description: 'a blue'])
        then:
        assertEquals('group1', h.jobGroup)
        assertEquals('name1', h.jobName)
        assertEquals('job args1', h.argString)
        assertEquals('a blue', h.description)
        assertEquals('abc def', h.nodeFilter)
        assertNull(h.nodeThreadcount)
        assertNull(h.nodeKeepgoing)
        assertEquals('rank',h.nodeRankAttribute)
        assertNull(h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }
    /** fromMap with nodeFilter, rankAttribute and rankOrder*/
    def testFromMapNodeFilterRankOrder() {
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                                                     args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [rankOrder: 'descending']]], description: 'a blue'])
        then:
        assertEquals('group1', h.jobGroup)
        assertEquals('name1', h.jobName)
        assertEquals('job args1', h.argString)
        assertEquals('a blue', h.description)
        assertEquals('abc def', h.nodeFilter)
        assertNull(h.nodeThreadcount)
        assertNull(h.nodeKeepgoing)
        assertNull(h.nodeRankAttribute)
        assertEquals(false,h.nodeRankOrderAscending)
        assertNull(h.errorHandler)
    }

    def testFromMapNoHandler(){
        when:
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1'],
            errorhandler: [jobref: [group: 'group1', name: 'name1']]])
        then:
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertNull(h.errorHandler)
    }

    //test create clone
    def testCreateClone() {
        when:
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1')
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }
    //test create clone

    def testCreateCloneDesc() {
        when:
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',description: 'elf monkey')
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals('elf monkey', j1.description)
        assertNull(j1.errorHandler)
    }

    def testCreateCloneNoHandler() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h)
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }
    def testCreateCloneNodeFilter() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeFilter: 'abc')
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals('abc', j1.nodeFilter)
        assertNull(j1.errorHandler)
    }
    def testCreateCloneNodeThreadcount() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeThreadcount: 2)
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(2, j1.nodeThreadcount)
        assertNull(j1.errorHandler)
    }
    def testCreateCloneNodeKeepgoing() {
        when:
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeKeepgoing: true)
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(true, j1.nodeKeepgoing)
        assertNull(j1.errorHandler)
    }

    def testCreateCloneKeepgoing() {
        when:
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',keepgoingOnSuccess: true)
        JobExec j1 = t.createClone()
        then:
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }
}
