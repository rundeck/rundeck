import grails.test.GrailsUnitTestCase
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import rundeck.JobExec
import rundeck.CommandExec
import rundeck.Workflow

/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/*
 * JobExecTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 5/14/12 11:41 AM
 * 
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([Workflow,JobExec,CommandExec])
class JobExecTests{

    void testBasicToMap() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name')
        assertEquals([jobref: [group:'group',name:'name']], t.toMap())
    }

    void testBasicToMapDesc() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey')
        assertEquals([jobref: [group:'group',name:'name'], description: 'a monkey'], t.toMap())
    }

    void testBasicToMapNodeFilter() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def')
        assertEquals([jobref: [group:'group',name:'name', nodefilters:[filter: 'abc def']],
                description: 'a monkey'], t.toMap())
    }
    void testBasicToMapNodeFilter_threadcount() {
        JobExec t = new JobExec(
                jobGroup: 'group',
                jobName: 'name',
                description: 'a monkey',
                nodeFilter: 'abc def',
                nodeThreadcount: 2
        )
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }

    void testBasicToMapNodeFilter_keepgoing() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true)
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }
    void testBasicToMapNodeFilter_keepgoingFalse() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: false)
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }
    void testBasicToMapNodeFilter_rankAttribute() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankAttribute: 'rank')
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }
    void testBasicToMapNodeFilter_rankOrder() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankOrderAscending: true)
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }
    void testBasicToMapNodeFilter_rankOrderDescending() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name',description: 'a monkey',
                nodeFilter: 'abc def', nodeThreadcount: 2, nodeKeepgoing: true, nodeRankOrderAscending: false)
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
                        description: 'a monkey'
                ],
                t.toMap()
        )
    }

    void testBasicArgsToMap() {
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name',argString: 'job args')
        assertEquals([jobref: [group: 'group', name: 'name',args: 'job args']], t.toMap())
    }
    void testSimpleToMap() {
        JobExec t = new JobExec(jobName: 'name')
        assertEquals([jobref: [group:'',name:'name']], t.toMap())
    }

    void testSimpleArgsToMap() {
        JobExec t = new JobExec( jobName: 'name',argString: 'job args')
        assertEquals([jobref: [group: '', name: 'name',args: 'job args']], t.toMap())
    }

    void testErrorHandlerExecToMap() {
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name', argString: 'job args')
        t.errorHandler=h
        assertEquals([jobref: [group: 'group', name: 'name', args: 'job args'], errorhandler: [exec: 'testerr']], t.toMap())
    }

    void testErrorHandlerJobRefToMap() {
        JobExec h = new JobExec(jobGroup: 'group1', jobName: 'name1')
        JobExec t = new JobExec(jobGroup: 'group', jobName: 'name', argString: 'job args')
        t.errorHandler = h
        assertEquals([jobref: [group: 'group', name: 'name', args: 'job args'], errorhandler: [jobref: [group: 'group1', name: 'name1']]], t.toMap())
    }


    void testErrorHandlerForExecToMap() {
        JobExec h = new JobExec(jobGroup: 'group1', jobName: 'name1',argString: 'job args1')
        CommandExec t = new CommandExec(adhocRemoteString: 'testerr', argString: 'job args')
        t.errorHandler = h
        assertEquals([jobref: [group: 'group1', name: 'name1', args: 'job args1']], t.toMap().errorhandler)
    }

    //test jobExecFromMap
    void testFromMap(){

        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1']])
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals(null, h.nodeFilter)
        assertEquals(null, h.nodeThreadcount)
        assertEquals(null, h.nodeKeepgoing)
        assertNull(h.errorHandler)
    }
    /**test jobExecFromMap with description */
    void testFromMapDesc(){

        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1'], description: 'a blue'])
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertEquals('a blue',h.description)
        assertEquals(null,h.nodeFilter)
        assertNull(h.errorHandler)
    }
    /** fromMapw with nodeFilter*/
    void testFromMapNodeFilter(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter:'abc def']], description: 'a blue'])
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
    void testFromMapNodeFilterThreadCount(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount:3]]], description: 'a blue'])
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
    void testFromMapNodeFilterThreadCountKeepgoing(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:true]]], description: 'a blue'])
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
    void testFromMapNodeFilterThreadCountKeepgoingFalse(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:false]]], description: 'a blue'])
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
    void testFromMapNodeFilterThreadCountKeepgoingFalseString(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [threadcount: 3,keepgoing:'false']]], description: 'a blue'])
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
    void testFromMapNodeFilterRankAttribute() {
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                                                     args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [rankAttribute: 'rank']]], description: 'a blue'])
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
    void testFromMapNodeFilterRankOrder() {
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1',
                                                     args: 'job args1', nodefilters: [filter: 'abc def', dispatch: [rankOrder: 'descending']]], description: 'a blue'])
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

    void testFromMapNoHandler(){
        JobExec h = JobExec.jobExecFromMap([jobref: [group: 'group1', name: 'name1', args: 'job args1'],
            errorhandler: [jobref: [group: 'group1', name: 'name1']]])
        assertEquals('group1',h.jobGroup)
        assertEquals('name1',h.jobName)
        assertEquals('job args1',h.argString)
        assertNull(h.errorHandler)
    }

    //test create clone

    void testCreateClone() {
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1')
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }
    //test create clone

    void testCreateCloneDesc() {
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',description: 'elf monkey')
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals('elf monkey', j1.description)
        assertNull(j1.errorHandler)
    }

    void testCreateCloneNoHandler() {
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }
    void testCreateCloneNodeFilter() {
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeFilter: 'abc')
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals('abc', j1.nodeFilter)
        assertNull(j1.errorHandler)
    }
    void testCreateCloneNodeThreadcount() {
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeThreadcount: 2)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(2, j1.nodeThreadcount)
        assertNull(j1.errorHandler)
    }
    void testCreateCloneNodeKeepgoing() {
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h, nodeKeepgoing: true)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(true, j1.nodeKeepgoing)
        assertNull(j1.errorHandler)
    }

    void testCreateCloneKeepgoing() {
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',keepgoingOnSuccess: true)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }
}
