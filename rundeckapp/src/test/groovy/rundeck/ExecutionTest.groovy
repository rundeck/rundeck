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

import grails.testing.gorm.DataTest
import rundeck.services.ExecutionService
import spock.lang.Specification

import static org.junit.Assert.*


/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/14/13
 * Time: 11:25 AM
 */

class ExecutionTest extends Specification implements DataTest  {

    def setupSpec() { mockDomains Execution, Workflow, CommandExec }

    void "testValidateBasic"() {
        when:
        Execution se = createBasicExecution()
        def validate = se.validate()
        then:
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void "testValidateServerNodeUUID"() {
        when:
        Execution se = createBasicExecution()
        se.serverNodeUUID=UUID.randomUUID().toString()
        def validate = se.validate()

        then:
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void "testInvalidServerNodeUUID"() {
        when:
        Execution se = createBasicExecution()
        se.serverNodeUUID="not valid"
        def validate = se.validate()
        then:
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        assertTrue(se.errors.hasFieldErrors('serverNodeUUID'))
    }

    void "testUserRoleListAsString"() {
        when:
        Execution se = createBasicExecution()
        se.userRoleList = userRolesListAsString
        def x = se.getUserRoles()
        then:
        assertEquals "incorrect number of roles found", userRoles.size(), x.size()
        assertEquals "invalid role item", userRoles, x

        where:
        userRoles         | userRolesListAsString
        ["a", "b", "c"]   | "a,b,c"
        []                | null
    }

    void "testSetUserRoles"() {
        when:
        Execution se = createBasicExecution()
        se.setUserRoles(userRoles)
        then:
        assertEquals "incorrect string value", userRolesList, se.userRoleList
        assertEquals "incorrect user roles list", result, se.getUserRoles()

        where:
        userRoles                                | userRolesList                                    | result
        ["a", "b", "c"]                          | "[\"a\",\"b\",\"c\"]"                            | ["a", "b", "c"]
        ["a, with commas", "b with spaces", "c"] | "[\"a, with commas\",\"b with spaces\",\"c\"]"   | ["a, with commas", "b with spaces", "c"]
        null                                     | null                                             | []
    }
    void testValidRetry() {
        when:
        Execution se = createBasicExecution()
        se.retry='1'
        def validate = se.validate()
        then:
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        when:
        se.retry='0'
        validate = se.validate()
        then:
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        when:
        se.retry='-1'
        validate = se.validate()
        then:
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testInvalidRetry() {
        when:
        Execution se = createBasicExecution()
        se.retry='1 '
        def validate = se.validate()
        then:
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testExecutionState() {
        when:
        Execution se = createBasicExecution()

        Date now = new Date()
        Calendar cal = Calendar.getInstance()
        cal.setTime(now)
        cal.add(Calendar.HOUR, 2)
        Date future = cal.getTime()
        se.dateStarted = future
        then:
        assertEquals(ExecutionService.EXECUTION_SCHEDULED,se.executionState)
        when:
        se.dateStarted = null
        se.dateCompleted = null
        then:
        assertEquals(ExecutionService.EXECUTION_RUNNING,se.executionState)
        when:
        se.dateCompleted  = now
        se.status = 'true'
        then:
        assertEquals(ExecutionService.EXECUTION_SUCCEEDED,se.executionState)
        when:
        se.status = 'succeeded'
        then:
        assertEquals(ExecutionService.EXECUTION_SUCCEEDED,se.executionState)
        when:
        se.status = 'failed'
        then:
        assertEquals(ExecutionService.EXECUTION_FAILED,se.executionState)
        when:
        se.status = 'false'
        then:
        assertEquals(ExecutionService.EXECUTION_FAILED,se.executionState)
        when:
        se.cancelled = true
        then:
        assertEquals(ExecutionService.EXECUTION_ABORTED,se.executionState)
        when:
        se.cancelled = false
        se.willRetry = true
        then:
        assertEquals(ExecutionService.EXECUTION_FAILED_WITH_RETRY,se.executionState)
        when:
        se.cancelled = false
        se.willRetry = false
        se.timedOut = true
        then:
        assertEquals(ExecutionService.EXECUTION_TIMEDOUT,se.executionState)
        when:
        se.timedOut = false
        se.status = "custom"
        then:
        assertEquals(ExecutionService.EXECUTION_STATE_OTHER,se.executionState)
        when:
        se.status = "any string"
        then:
        assertEquals(ExecutionService.EXECUTION_STATE_OTHER,se.executionState)
    }

    void testIsCustomStatusString() {
        given:
        boolean result

        when:
        result = Execution.isCustomStatusString(statusValue)

        then:
        result == isCustom

        where:
        statusValue                                     | isCustom
        ExecutionService.EXECUTION_TIMEDOUT             | false
        ExecutionService.EXECUTION_FAILED_WITH_RETRY    | false
        ExecutionService.ABORT_ABORTED                  | false
        ExecutionService.EXECUTION_SUCCEEDED            | false
        ExecutionService.EXECUTION_FAILED               | false
        ExecutionService.EXECUTION_QUEUED               | false
        ExecutionService.EXECUTION_SCHEDULED            | false
        "CUSTOM STRING"                                 | true
    }

    void testStatusSucceededTrue() {
        when:
        Execution se = createBasicExecution()
        se.dateCompleted=new Date()
        se.status='true'
        then:
        assertTrue(se.statusSucceeded())
    }
    void testStatusSucceededSucceeded() {
        when:
        Execution se = createBasicExecution()
        se.dateCompleted=new Date()
        se.status='succeeded'
        then:
        assertTrue(se.statusSucceeded())
    }

    Execution createBasicExecution() {
        new Execution(
                project: "test",
                user: "test",
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                ),
        )
    }
    void testFromMapBlankThreadcount(){
        when:
        //blank threadcount
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:"",
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapNullThreadcount(){

        when://blank threadcount
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:null,
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapThreadcountString(){
        when:
        //blank threadcount
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:'2',
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(2, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapBlankKeepgoing(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "",
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapKeepgoingString(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceBlank(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:"",
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceNull(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:null,
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceString(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:'false',
                            include:[
                                    name:"test1"
                            ]
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(false, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromMapFilter(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:'false'
                    ],
                    filter: 'name: test1'
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertNull(exec.nodeIncludeName)
        assertEquals('name: test1', exec.filter)
    }
    void testFromMapDoNodedispatch_stringTrue(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: "true",
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:'false'
                    ],
                    filter: 'name: test1'
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals(true, exec.doNodedispatch)
    }
    void testFromMapDoNodedispatch_stringFalse(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: "false",

                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals(false, exec.doNodedispatch)
    }
    void testFromMapOldNodeFilter(){
        //blank threadcount
        when:
        def exec = Execution.fromMap([
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                nodefilters:[
                    dispatch:[
                            threadcount:1,
                            keepgoing: "true",
                            excludePrecedence:'false'
                    ],
                    include: [
                            name: "test1"
                    ]
                ],
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertNull(exec.nodeIncludeName)
        assertEquals('name: test1', exec.filter)
    }
    void testFromMapRetry(){
        when:
        def exec1 = new Execution(project:'test1',user:'user1',
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                )
        )
        exec1.validate()
        assertNotNull(exec1.errors.allErrors.collect{ it.toString() }.join(" "), exec1.save())
        def exec = Execution.fromMap([
                retry: '123',
                willRetry: 'true',
                retryAttempt: 12,
                retryExecutionId: exec1.id,
                retryOriginalId: exec1.id,
                status: 'true',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                doNodedispatch: true,
                project:'test1',
                user:'user1',
                workflow:[
                        keepgoing:true,
                        commands:[
                                [
                                        exec:"blah"
                                ]
                        ]
                ]
        ], null)
        then:
        assertNotNull(exec)
        assertEquals('123',exec.retry)
        assertEquals(12,exec.retryAttempt)
        assertEquals(exec1,exec.retryExecution)
        assertEquals(exec1.id,exec.retryOriginalId)
        assertEquals(true,exec.willRetry)
    }
    void testToMapRetry(){
        when:
        def exec1 = new Execution(project: 'test1', user: 'user1',
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                )
        )
        assertNotNull(exec1.save())
        def exec2 = new Execution(project: 'test1', user: 'user1',
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                )
        )
        exec2.retry='123'
        exec2.retryAttempt=12
        exec2.retryExecution=exec1
        exec2.willRetry=true
        assertNotNull(exec2.save())
        def map = exec2.toMap()
        then:
        assertNotNull(map)
        assertEquals('123',map.retry)
        assertEquals(12, map.retryAttempt)
        assertEquals(exec1.id, map.retryExecutionId)
        assertEquals(true, map.willRetry)
    }

    void testDeleteExecutionWorkflowCascadeAll() {

        when:
        WorkflowStep workflowStep = new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])

        ScheduledExecution se1 = new ScheduledExecution(
                uuid: 'test1',
                jobName: 'red color',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [workflowStep]).save(),
        )

        assert null != se1.save()

        Workflow workflow = new Workflow(keepgoing: true, commands: [workflowStep]).save()

        Execution e1 = new Execution(
                scheduledExecution: se1,
                project: "Test",
                status: "false",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: workflow

        )
        assert null != e1.save()

        then:
        assertNotNull Execution.findByScheduledExecution(se1)
        assertNotNull Workflow.findById(e1.workflowId)
        assertFalse workflow.commands.isEmpty()

        when:
        e1.workflow.commands = []
        e1.delete(flush: true)

        then:
        assertNull Execution.findByScheduledExecution(se1)
        assertFalse Workflow.findAll().any {Workflow w -> w.id == e1.workflowId}
        assertTrue workflow.commands.isEmpty()
    }
}
