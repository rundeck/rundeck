package rundeck

import grails.test.GrailsUnitTestCase

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 4/19/13
 * Time: 6:06 PM
 */
class ScheduledExecutionTest extends GrailsUnitTestCase {

    void testToMapOptions(){
        mockDomain(Option)
        mockDomain(ScheduledExecution)
        ScheduledExecution se = new ScheduledExecution(
                jobName: "test",
                groupPath: "",
                description: "",
                workflow: new Workflow(
                        commands: [
                                new CommandExec(adhocRemoteString: "exec")
                        ]
                ),
                options: [
                        new Option(name: 'abc-4', defaultValue: '12', sortIndex: 4),
                        new Option(name: 'bcd-2', defaultValue: '12', sortIndex: 2),
                        new Option(name: 'cde-3', defaultValue: '12', sortIndex: 3),
                        new Option(name: 'def-1', defaultValue: '12', sortIndex: 1),
                ]
        )
        def jobMap = se.toMap()
        assertNotNull(jobMap)
        assertNotNull(jobMap.options)
        assertEquals(4,jobMap.options.size())
    }
}
