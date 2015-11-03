package rundeck

import grails.test.GrailsUnitTestCase
import grails.test.mixin.TestFor
import junit.framework.Assert
import rundeck.services.ExecutionService

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/14/13
 * Time: 11:25 AM
 */
@TestFor(Execution)
class ExecutionTest {
    void testValidateBasic() {
        Execution se = createBasicExecution()
        def validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testValidateServerNodeUUID() {
        Execution se = createBasicExecution()
        se.serverNodeUUID=UUID.randomUUID().toString()
        def validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testInvalidServerNodeUUID() {
        Execution se = createBasicExecution()
        se.serverNodeUUID="not valid"
        def validate = se.validate()
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        assertTrue(se.errors.hasFieldErrors('serverNodeUUID'))
    }
    void testValidRetry() {
        Execution se = createBasicExecution()
        se.retry='1'
        def validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        se.retry='0'
        validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        se.retry='-1'
        validate = se.validate()
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testInvalidRetry() {
        Execution se = createBasicExecution()
        se.retry='1 '
        def validate = se.validate()
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testExecutionState() {
        Execution se = createBasicExecution()
        se.dateCompleted=null
        assertEquals(ExecutionService.EXECUTION_RUNNING,se.executionState)
        se.dateCompleted=new Date()
        se.status='true'
        assertEquals(ExecutionService.EXECUTION_SUCCEEDED,se.executionState)
        se.status='succeeded'
        assertEquals(ExecutionService.EXECUTION_SUCCEEDED,se.executionState)
        se.status='failed'
        assertEquals(ExecutionService.EXECUTION_FAILED,se.executionState)
        se.status='false'
        assertEquals(ExecutionService.EXECUTION_FAILED,se.executionState)
        se.cancelled=true
        assertEquals(ExecutionService.EXECUTION_ABORTED,se.executionState)
        se.cancelled=false
        se.willRetry=true
        assertEquals(ExecutionService.EXECUTION_FAILED_WITH_RETRY,se.executionState)
        se.cancelled=false
        se.willRetry=false
        se.timedOut=true
        assertEquals(ExecutionService.EXECUTION_TIMEDOUT,se.executionState)
        se.timedOut=false
        se.status="custom"
        assertEquals(ExecutionService.EXECUTION_STATE_OTHER,se.executionState)
        se.status="any string"
        assertEquals(ExecutionService.EXECUTION_STATE_OTHER,se.executionState)
    }
    void testStatusSucceededTrue() {
        Execution se = createBasicExecution()
        se.dateCompleted=new Date()
        se.status='true'
        assertTrue(se.statusSucceeded())
    }
    void testStatusSucceededSucceeded() {
        Execution se = createBasicExecution()
        se.dateCompleted=new Date()
        se.status='succeeded'
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapNullThreadcount(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapThreadcountString(){
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(2, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapBlankKeepgoing(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(false, exec.nodeKeepgoing)
    }
    void testFromMapKeepgoingString(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceBlank(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceNull(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(true, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromExcludePrecedenceString(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals("true", exec.status)
        assertEquals(true, exec.doNodedispatch)
        assertEquals(1, exec.nodeThreadcount)
        assertEquals(false, exec.nodeExcludePrecedence)
        assertEquals(true, exec.nodeKeepgoing)
    }
    void testFromMapFilter(){
        //blank threadcount
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
        assertNotNull(exec)
        assertNull(exec.nodeIncludeName)
        assertEquals('name: test1', exec.filter)
    }
    void testFromMapDoNodedispatch_stringTrue(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals(true, exec.doNodedispatch)
    }
    void testFromMapDoNodedispatch_stringFalse(){
        //blank threadcount
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
        assertNotNull(exec)
        assertEquals(false, exec.doNodedispatch)
    }
    void testFromMapOldNodeFilter(){
        //blank threadcount
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
        assertNotNull(exec)
        assertNull(exec.nodeIncludeName)
        assertEquals('name: test1', exec.filter)
    }
    void testFromMapRetry(){
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
        assertNotNull(exec)
        assertEquals('123',exec.retry)
        assertEquals(12,exec.retryAttempt)
        assertEquals(exec1,exec.retryExecution)
        assertEquals(true,exec.willRetry)
    }
    void testToMapRetry(){
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
        assertNotNull(map)
        assertEquals('123',map.retry)
        assertEquals(12, map.retryAttempt)
        assertEquals(exec1.id, map.retryExecutionId)
        assertEquals(true, map.willRetry)
    }
}
