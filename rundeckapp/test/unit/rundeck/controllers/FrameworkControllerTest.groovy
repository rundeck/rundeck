/*
 Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExtNodeFilters
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.services.FrameworkService

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 1/30/14
 * Time: 5:19 PM
 */
@TestFor(FrameworkController)
@Mock([ScheduledExecution, Workflow, WorkflowStep, CommandExec, Execution])
class FrameworkControllerTest {
    public void testAdhocRetryFailedExecId(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                failedNodeList: "abc,xyz"
        )
        assertNotNull exec.save()
        params.retryFailedExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: abc,xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_nodeDispatch(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: true,
                nodeIncludeName: "abc",
                nodeIncludeTags: "xyz"
        )
        assertNotNull exec.save()
        params.fromExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: abc tags: xyz",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
    public void testAdhocFromExecId_local(){
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true,
                        adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: false,
        )
        assertNotNull exec.save()
        params.fromExecId=exec.id

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectExecutionAll {ctx,e,actions->
            assertEquals(exec,e)
            assertEquals([AuthConstants.ACTION_READ],actions)
            true
        }
        fwkControl.demand.getFrameworkNodeName { -> return "monkey1" }
        controller.frameworkService = fwkControl.createMock()

        def result=controller.adhoc(new ExtNodeFilters())
        assertNotNull(result.query)
        assertEquals("name: monkey1",result.query.filter)
        assertNotNull(result.runCommand)
        assertEquals("a remote string",result.runCommand)
    }
}
