package rundeck.services

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.test.GrailsUnitTestCase
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.controllers.ExecutionController

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 8/7/13
 * Time: 3:07 PM
 */
@TestFor(ExecutionService)
@Mock([Execution, FrameworkService, Workflow, ScheduledExecution])
class ExecutionServiceTests {
    private List createTestExecs() {

        ScheduledExecution se1 = new ScheduledExecution(
                uuid: 'test1',
                jobName: 'red color',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se1.save()
        ScheduledExecution se2 = new ScheduledExecution(
                uuid: 'test2',
                jobName: 'green and red color',
                project: 'Test',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se2.save()
        ScheduledExecution se3 = new ScheduledExecution(
                uuid: 'test3',
                jobName: 'blue green and red color',
                project: 'Test',
                groupPath: 'some/where/else',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
        )
        assert null != se3.save()

        Execution e1 = new Execution(
                scheduledExecution: se1,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()

        Execution e2 = new Execution(
                scheduledExecution: se2,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'bob',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test2 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e2.save()
        Execution e3 = new Execution(
                scheduledExecution: se3,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'chuck',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test3 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e3.save()

        [e1, e2, e3]
    }

    /**
     * Test jobExecutions empty
     */
    public void testApiJobExecutions_empty() {
        def svc = new ExecutionService()

        def execs = createTestExecs()

        ScheduledExecution se4 = new ScheduledExecution(
                uuid: 'test4',
                jobName: 'blah',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]).save(),
                )
        assert null != se4.save()
        def result = svc.queryJobExecutions(se4, null)

        assert 0 == result.total
    }
    /**
     * Test jobExecutions simple
     */
    public void testApiJobExecutions_simple() {
        def svc = new ExecutionService()

        def execs = createTestExecs()

        ScheduledExecution se=execs[0].scheduledExecution
        assert null != se
        def result = svc.queryJobExecutions(se, null)

        assert 1 == result.total
    }
    /**
     * Test jobExecutions succeeded
     */
    public void testApiJobExecutions_success() {
        def svc = new ExecutionService()

        def execs = createTestExecs()

        ScheduledExecution se=execs[0].scheduledExecution
        assert null != se
        def result = svc.queryJobExecutions(se, 'succeeded')

        assert 1 == result.total
    }
    /**
     * Test jobExecutions failed
     */
    public void testApiJobExecutions_failed() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        Execution e1 = new Execution(
                scheduledExecution: execs[0].scheduledExecution,
                project: "Test",
                status: "false",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()
        execs[0].delete()

        ScheduledExecution se=execs[0].scheduledExecution
        assert null != se
        def result = svc.queryJobExecutions(se, 'failed',0,20)

        assert 1 == result.total
    }
    /**
     * Test jobExecutions failed
     */
    public void testApiJobExecutions_custom() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        Execution e1 = new Execution(
                scheduledExecution: execs[0].scheduledExecution,
                project: "Test",
                status: "custom status",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()
        execs[0].delete()

        ScheduledExecution se=execs[0].scheduledExecution
        assert null != se
        def result = svc.queryJobExecutions(se, 'custom status',0,20)

        assert 1 == result.total
    }
    /**
     * Test groupPath
     */
    public void testExecutionsQueryGroupPath() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some"),0,20)

        assert 3 == result.total
    }

    /**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub1() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some/where"), 0, 20)

        assert 2 == result.total
    }/**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub2() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPath: "some/where/else"), 0, 20)

        assert 1 == result.total
    }
    /**
     * Test excludeGroupPath subpath 3
     */
    public void testApiExecutionsQueryExcludeGroupPathSub3() {

        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some/where/else"), 0, 20)

        assert 2 == result.total
    }
    /**
     * Test excludeGroupPath subpath 2
     */
    public void testApiExecutionsQueryExcludeGroupPathSub2() {

        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some/where"), 0, 20)

        assert 1 == result.total
    }
    /**
     * Test excludeGroupPath subpath equal
     */
    public void testApiExecutionsQueryExcludeGroupPathSubEqual() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPath: "some"), 0, 20)

        assert 0 == result.total
    }

    /**
     * Test groupPathExact (top)
     */
    public void testApiExecutionsGroupPathExact() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some"), 0, 20)

        assert 1 == result.total
    }

    /**
     * Test groupPathExact (mid)
     */
    public void testApiExecutionsGroupPathExactSub1() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some/where"), 0, 20)

        assert 1 == result.total
    }

    /**
     * Test groupPathExact (bot)
     */
    public void testApiExecutionsGroupPathExactSub2() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", groupPathExact: "some/where/else"), 0, 20)

        assert 1 == result.total
    }

    /**
     * Test excludeGroupPathExact level1
     */
    public void testApiExecutionsExcludeGroupPathExact1() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some"), 0, 20)

        assert 2 == result.total
    }

    /**
     * Test excludeGroupPathExact level2
     */
    public void testApiExecutionsExcludeGroupPathExact2() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some/where"), 0, 20)

        assert 2 == result.total
    }

    /**
     * Test excludeGroupPathExact level3
     */
    public void testApiExecutionsExcludeGroupPathExact3() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeGroupPathExact: "some/where/else"), 0, 20)

        assert 2 == result.total
    }
    /**
     * Test excludeJob wildcard
     */
    public void testApiExecutionsExcludeJob() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%red color"), 0, 20)

        assert 0 == result.total
    }
    /**
     * Test excludeJob wildcard 2
     */
    public void testApiExecutionsExcludeJob2() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%green and red color"), 0, 20)

        assert 1 == result.total
    }
    /**
     * Test excludeJob wildcard 3
     */
    public void testApiExecutionsExcludeJob3() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "%blue green and red color"), 0, 20)

        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 1
     */
    public void testApiExecutionsExcludeJobExact1() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobExactFilter: "red color"), 0, 20)

        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 2
     */
    public void testApiExecutionsExcludeJobExact2() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobExactFilter: "green and red color"), 0, 20)

        assert 2 == result.total
    }
    /**
     * Test excludeJobExact 3
     */
    public void testApiExecutionsExcludeJobExact3() {
        def svc = new ExecutionService()

        def execs = createTestExecs()
        def result = svc.queryExecutions(new ExecutionQuery(projFilter: "Test", excludeJobFilter: "blue green and red color"), 0, 20)

        assert 2 == result.total
    }

}
