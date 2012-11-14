/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

import grails.test.ControllerUnitTestCase
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.Group
import rundeck.ScheduledExecution
import rundeck.Option
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.Execution

/*
* ScheduledExecutionControllerTests.java
*
* User: greg
* Created: Jun 11, 2008 5:12:47 PM
* $Id$
*/
class ScheduledExecutionControllerTests extends ControllerUnitTestCase {
    void setUp(){
        super.setUp()
        
        loadCodec(org.codehaus.groovy.grails.plugins.codecs.URLCodec)
    }
    void testEmpty(){

    }

    private void assertMap(key, map, value) {
        assertEquals "invalid ${key} ${map[key]}", value, map[key]
    }

    private void assertMap(expected, value) {
        expected.each {k, v ->
            assertMap(k, value, v)
        }
    }

    public void testExpandUrl() {
        mockDomain(ScheduledExecution)
        mockDomain(Option)
        ConfigurationHolder.metaClass.getConfig = {-> [:] }
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath:'some/where',description:'a job',project:'AProject',argString:'-a b -c d',adhocExecution:false)

        final Option option = new Option(name: 'test1', enforced: false)
        se.addToOptions(option)
        se.save()
        assertNotNull(option.properties)
        System.err.println("properties: ${option.properties}");

        ScheduledExecutionController ctrl = new ScheduledExecutionController()
        assertEquals 'test1', ctrl.expandUrl(option, '${option.name}', se)
        assertEquals 'blue', ctrl.expandUrl(option, '${job.name}', se)
        assertEquals 'some%2Fwhere', ctrl.expandUrl(option, '${job.group}', se)
        assertEquals 'a+job', ctrl.expandUrl(option, '${job.description}', se)
        assertEquals 'AProject', ctrl.expandUrl(option, '${job.project}', se)
        assertEquals '-a+b+-c+d', ctrl.expandUrl(option, '${job.argString}', se)
        assertEquals 'false', ctrl.expandUrl(option, '${job.adhoc}', se)
        assertEquals '${job.noexist}', ctrl.expandUrl(option, '${job.noexist}', se)
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            ctrl.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se)

    }

    public void testSaveBasic() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, changeinfo ->
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()


            sec.metaClass.message={params -> params?.code?:'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.save()

            assertNotNull sec.flash.savedJob
            assertNotNull sec.flash.savedJobMessage
            assertNull sec.modelAndView.viewName, sec.modelAndView.viewName
            assertEquals("show",sec.redirectArgs.action)
            assertEquals("scheduledExecution",sec.redirectArgs.controller)
        }
    }

    public void testSaveFail() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, changeinfo ->
                [success: false]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.metaClass.message={parms -> parms?.code ?: 'messageCodeMissing'}

            sec.save()

            assertNull sec.response.redirectedUrl
            assertNotNull sec.request.message
            assertEquals 'create', sec.modelAndView.viewName
            assertNull sec.modelAndView.model.scheduledExecution
        }
    }
    public void testSaveUnauthorized() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, changeinfo ->
                [success: false,unauthorized:true,error:'unauthorizedMessage']
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.metaClass.message={parms -> parms?.code ?: 'messageCodeMissing'}

            sec.save()

            assertNull sec.response.redirectedUrl
            assertEquals 'unauthorizedMessage',sec.request.message
            assertEquals 'create', sec.modelAndView.viewName
            assertNull sec.modelAndView.model.scheduledExecution
        }
    }

    public void testSaveAndExecBasic() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, changeinfo ->
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.saveAndExec()

            assertNull sec.flash.message
            assertNull sec.modelAndView.viewName, sec.modelAndView.viewName
            assertEquals('execute', sec.redirectArgs.action.toString())
        }
    }
    public void testSaveAndExecFailed() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, changeinfo ->
                [success: false, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.saveAndExec()

            assertEquals('ScheduledExecutionController.save.failed', sec.flash.message)
            assertEquals 'create', sec.modelAndView.viewName
            assertNull sec.redirectArgs.action
        }
    }

    public void testRunAdhocBasic() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        mockDomain(Execution)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
            seServiceControl.demand._dovalidate {params, user, rolelist, framework ->
                assertEquals('Temporary_Job',params.jobName)
                assertEquals('adhoc',params.groupPath)
                [failed: false, scheduledExecution: se]
            }
            seServiceControl.demand.scheduleTempJob {user,subject,params,exec ->
                return exec.id
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()

            def eServiceControl = mockFor(ExecutionService, true)
            def exec = new Execution(
                    user: "testuser", project: "testproj", loglevel: 'WARN',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                    )
            assertNotNull exec.save()
            eServiceControl.demand.createExecutionAndPrep {params, framework, user ->
                return exec
            }
            sec.executionService = eServiceControl.createMock()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            def model=sec.runAdhoc()

            assertNull model.failed
            assertNotNull model.execution
            assertNotNull exec.id
            assertEquals exec, model.execution
            assertEquals('notequal',exec.id.toString(), model.id.toString())
        }
    }

    public void testRunAdhocFailed() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        mockDomain(Execution)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
            seServiceControl.demand._dovalidate {params, user, rolelist, framework ->
                assertEquals('Temporary_Job',params.jobName)
                assertEquals('adhoc',params.groupPath)
                [failed: true, scheduledExecution: se]
            }
            seServiceControl.demand.scheduleTempJob {user,subject,params,exec ->
                return exec.id
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()

            def eServiceControl = mockFor(ExecutionService, true)
            def exec = new Execution(
                    user: "testuser", project: "testproj", loglevel: 'WARN',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                    )
            assertNotNull exec.save()
            eServiceControl.demand.createExecutionAndPrep {params, framework, user ->
                return exec
            }
            sec.executionService = eServiceControl.createMock()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            def model=sec.runAdhoc()

            assertTrue model.failed
            assertNotNull model.scheduledExecution
            assertEquals 'Job configuration was incorrect.', model.message
        }
    }
}