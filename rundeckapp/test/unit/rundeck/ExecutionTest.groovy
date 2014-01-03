package rundeck

import grails.test.GrailsUnitTestCase

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 5/14/13
 * Time: 11:25 AM
 */
class ExecutionTest extends GrailsUnitTestCase {
    void testValidateBasic() {
        mockDomain(Execution)
        Execution se = createBasicExecution()
        def validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testValidateServerNodeUUID() {
        mockDomain(Execution)
        Execution se = createBasicExecution()
        se.serverNodeUUID=UUID.randomUUID().toString()
        def validate = se.validate()
        assertTrue("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
    }
    void testInvalidServerNodeUUID() {
        mockDomain(Execution)
        Execution se = createBasicExecution()
        se.serverNodeUUID="not valid"
        def validate = se.validate()
        assertFalse("Invalid: "+se.errors.allErrors*.toString().join(","), validate)
        assertTrue(se.errors.hasFieldErrors('serverNodeUUID'))
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
}
