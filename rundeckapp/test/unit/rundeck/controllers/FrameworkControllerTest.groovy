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
    public void testextractApiNodeFilterParamsEmpty(){
        def params = FrameworkController.extractApiNodeFilterParams([:])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFilters(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'hostname':'host1',
                'tags':'tags1',
                'name':'name1',
                'os-name':'osname1',
                'os-arch':'osarch1',
                'os-version':'osvers1',
                'os-family':'osfam1',
        ])
        assertEquals(7,params.size())
        assertEquals([
                'nodeInclude': 'host1',
                'nodeIncludeTags': 'tags1',
                'nodeIncludeName': 'name1',
                'nodeIncludeOsName': 'osname1',
                'nodeIncludeOsArch': 'osarch1',
                'nodeIncludeOsVersion': 'osvers1',
                'nodeIncludeOsFamily': 'osfam1',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersExclude(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-hostname':'host1',
                'exclude-tags':'tags1',
                'exclude-name':'name1',
                'exclude-os-name':'osname1',
                'exclude-os-arch':'osarch1',
                'exclude-os-version':'osvers1',
                'exclude-os-family':'osfam1',
        ])
        assertEquals(7,params.size())
        assertEquals([
                'nodeExclude': 'host1',
                'nodeExcludeTags': 'tags1',
                'nodeExcludeName': 'name1',
                'nodeExcludeOsName': 'osname1',
                'nodeExcludeOsArch': 'osarch1',
                'nodeExcludeOsVersion': 'osvers1',
                'nodeExcludeOsFamily': 'osfam1',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceWithFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
                'hostname':'boing'
        ])
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': true,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceWithoutFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'true',
        ])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
                'hostname':'boing'
        ])
        assertEquals(2,params.size())
        assertEquals([
                'nodeExcludePrecedence': false,
                'nodeInclude': 'boing',
        ],params)
    }
    public void testextractApiNodeFilterParamsLegacyFiltersPrecedenceFalseWithoutFilter(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'exclude-precedence':'false',
        ])
        assertEquals(0,params.size())
    }
    public void testextractApiNodeFilterParamsFilterString(){
        def params = FrameworkController.extractApiNodeFilterParams([
                'filter':'mynode !tags: blah',
        ])
        assertEquals(1,params.size())
        assertEquals([
                'filter': 'mynode !tags: blah',
        ],params)
    }
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
