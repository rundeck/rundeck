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
}
