import grails.test.GrailsUnitTestCase
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.Execution

/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */
 
/*
 * ExecutionControlTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 9/21/12 2:24 PM
 * 
 */
class ExecutionControlTests extends GrailsUnitTestCase{
    public void testApiExecutionsQueryRequireVersion() {
        def controller= new ExecutionController()
        controller.apiExecutionsQuery(null)
        assert 400==controller.response.status
        assert "api-version-unsupported"==controller.request.apiErrorCode
    }
    public void testApiExecutionsQueryRequireV5_lessthan() {
        def controller = new ExecutionController()
        controller.request.api_version=4
        controller.apiExecutionsQuery(null)
        assert 400 == controller.response.status
        assert "api-version-unsupported" == controller.request.apiErrorCode
    }
    public void testApiExecutionsQueryRequireV5_ok() {
        def controller = new ExecutionController()
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk,results,actions->
            return []
        }
        controller.frameworkService=fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project="Test"
        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode

    }
    public List createTestExecs(){

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

        [e1,e2,e3]
    }
    /**
     * Test no results
     */
    public void testApiExecutionsQueryNoMatch() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert results==[]
            []
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "WRONG"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test groupPath
     */
    public void testApiExecutionsGroupPath() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert execs.every {it in results}
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPath = "some"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub1() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert execs[1..2].every {it in results}
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPath = "some/where"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }/**
     * Test groupPath
     */
    public void testApiExecutionsGroupPathSub2() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert execs[2..2].every {it in results}
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPath = "some/where/else"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeGroupPath subpath 3
     */
    public void testApiExecutionsQueryExcludeGroupPathSub3() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            execs[0..1].every {
                assert it in results
            }
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPath = 'some/where/else'

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeGroupPath subpath 2
     */
    public void testApiExecutionsQueryExcludeGroupPathSub2() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert execs[0..0].every {it in results}
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPath='some/where'

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeGroupPath subpath equal
     */
    public void testApiExecutionsQueryExcludeGroupPathSubEqual() {
        def controller = new ExecutionController()

        def execs=createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert results==[]
            []
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPath='some'

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test groupPathExact (top)
     */
    public void testApiExecutionsGroupPathExact() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 1==results.size()
            assert execs[0] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPathExact = "some"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test groupPathExact (mid)
     */
    public void testApiExecutionsGroupPathExactSub1() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 1==results.size()
            assert execs[1] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPathExact = "some/where"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test groupPathExact (bot)
     */
    public void testApiExecutionsGroupPathExactSub2() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 1==results.size()
            assert execs[2] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.groupPathExact = "some/where/else"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test excludeGroupPathExact level1
     */
    public void testApiExecutionsExcludeGroupPathExact1() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2==results.size()
            assert execs[1] in results
            assert execs[2] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPathExact = "some"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test excludeGroupPathExact level2
     */
    public void testApiExecutionsExcludeGroupPathExact2() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2==results.size()
            assert execs[0] in results
            assert execs[2] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPathExact = "some/where"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test excludeGroupPathExact level3
     */
    public void testApiExecutionsExcludeGroupPathExact3() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2==results.size()
            assert execs[0] in results
            assert execs[1] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeGroupPathExact = "some/where/else"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJob wildcard
     */
    public void testApiExecutionsExcludeJob() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 0==results.size()
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobFilter = "%red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJob wildcard 2
     */
    public void testApiExecutionsExcludeJob2() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 1==results.size()
            assert execs[0] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobFilter = "%green and red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJob wildcard 3
     */
    public void testApiExecutionsExcludeJob3() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2==results.size()
            assert execs[0] in results
            assert execs[1] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobFilter = "%blue green and red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJobExact 1
     */
    public void testApiExecutionsExcludeJobExact1() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2 == results.size()
            assert execs[1] in results
            assert execs[2] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobExactFilter = "red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJobExact 2
     */
    public void testApiExecutionsExcludeJobExact2() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2 == results.size()
            assert execs[0] in results
            assert execs[2] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobExactFilter = "green and red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test excludeJobExact 3
     */
    public void testApiExecutionsExcludeJobExact3() {
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk, results, actions ->
            assert 2==results.size()
            assert execs[0] in results
            assert execs[1] in results
            results
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.excludeJobExactFilter = "blue green and red color"

        controller.apiExecutionsQuery(null)

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
}
