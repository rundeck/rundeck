import grails.test.GrailsUnitTestCase
import rundeck.JobExec
import rundeck.CommandExec
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
class JobExecTests extends GrailsUnitTestCase{

    void testBasicToMap() {
        JobExec t = new JobExec(jobGroup: 'group',jobName: 'name')
        assertEquals([jobref: [group:'group',name:'name']], t.toMap())
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
        mockDomain(JobExec)
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1')
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }

    void testCreateCloneNoHandler() {
        mockDomain(JobExec)
        mockDomain(CommandExec)
        CommandExec h = new CommandExec(adhocRemoteString: 'testerr')
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',errorHandler: h)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertNull(j1.errorHandler)
    }

    void testCreateCloneKeepgoing() {
        mockDomain(JobExec)
        mockDomain(CommandExec)
        JobExec t = new JobExec(jobGroup: 'group1', jobName: 'name1', argString: 'job args1',keepgoingOnSuccess: true)
        JobExec j1 = t.createClone()
        assertEquals('group1', j1.jobGroup)
        assertEquals('name1', j1.jobName)
        assertEquals('job args1', j1.argString)
        assertEquals(true, !!j1.keepgoingOnSuccess)
        assertNull(j1.errorHandler)
    }
}
