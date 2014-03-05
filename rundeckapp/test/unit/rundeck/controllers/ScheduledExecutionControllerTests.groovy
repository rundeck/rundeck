package rundeck.controllers
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
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest
import rundeck.services.ApiService
import rundeck.services.NotificationService

import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.Group
import rundeck.ScheduledExecution
import rundeck.Option
import rundeck.Workflow
import rundeck.CommandExec
import rundeck.Execution
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService

/*
* ScheduledExecutionControllerTests.java
*
* User: greg
* Created: Jun 11, 2008 5:12:47 PM
* $Id$
*/
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution,Option,Workflow,CommandExec,Execution])
class ScheduledExecutionControllerTests  {
    public void setUp(){

//        loadCodec(org.codehaus.groovy.grails.plugins.codecs.URLCodec)
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
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath:'some/where',description:'a job',project:'AProject',argString:'-a b -c d')

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
        assertEquals '${job.noexist}', ctrl.expandUrl(option, '${job.noexist}', se)
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            ctrl.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se)
        assertEquals 'anonymous', ctrl.expandUrl(option, '${job.user.name}', se)
        ctrl.session.user='bob'
        assertEquals 'bob', ctrl.expandUrl(option, '${job.user.name}', se)
    }

    public void testSaveBasic() {
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
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getAuthContextForSubject {subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, user, rolelist, framework, authctx, changeinfo ->
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
            assertNull view, view
            assertEquals("/scheduledExecution/show/1", response.redirectedUrl)
        }
    }
    public void testtransferSessionEditStateOpts() {
        def se = new ScheduledExecutionController()
        def params = [:]
        se.transferSessionEditState([:], params,'1')
        assertEquals(0, params.size())

        params=[_sessionopts:true]
        se.transferSessionEditState([:], params, '1')
        assertEquals(1, params.size())
        se.transferSessionEditState([editOPTS: [:]], params, '1')
        assertEquals(1, params.size())
        se.transferSessionEditState([editOPTS: ['1':['test':true]]], params, '1')
        assertEquals(2, params.size())
        assertEquals(['test':true], params['_sessionEditOPTSObject'])

        params = [_sessionopts: true]
        se.transferSessionEditState([editOPTS: ['1': []]], params, '1')
        assertEquals(2, params.size())
        assertEquals([], params['_sessionEditOPTSObject'])
    }
    public void testtransferSessionEditStateWF() {
        def se = new ScheduledExecutionController()
        def params = [:]
        se.transferSessionEditState([:], params,'1')
        assertEquals(0, params.size())

        params=[_sessionwf:true]
        se.transferSessionEditState([:], params, '1')
        assertEquals(1, params.size())
        se.transferSessionEditState([editWF: [:]], params, '1')
        assertEquals(1, params.size())
        se.transferSessionEditState([editWF: ['1':['test':true]]], params, '1')
        assertEquals(2, params.size())
        assertEquals(['test':true], params['_sessionEditWFObject'])

        params = [_sessionwf: true]
        se.transferSessionEditState([editWF: ['1': []]], params, '1')
        assertEquals(2, params.size())
        assertEquals([], params['_sessionEditWFObject'])
    }
    public void testUpdateSessionOptsEmptyList() {
        def sec = new ScheduledExecutionController()
            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    options: [new Option(name: 'blah',enforced:false)],
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            if(!se.validate()){
                println(se.errors.allErrors.collect{it.toString()}.join(", "))
            }
            assertNotNull(se.save())
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject {subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._doupdate {params, user, roleList, framework, authctx, changeinfo = [:] ->
                assertNotNull(params['_sessionEditOPTSObject'])
                assertEquals([],params['_sessionEditOPTSObject'])
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()


            sec.metaClass.message={params -> params?.code?:'messageCodeMissing'}

            def params = [
                    id:se.id.toString(),
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _sessionopts:true,
            ]
            sec.session['editOPTS']= [
                    (se.id.toString()): [
                            //empty list
                    ]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.update()

            assertNotNull sec.flash.savedJob
            assertNotNull sec.flash.savedJobMessage
            assertNull view
            assertEquals("/scheduledExecution/show/1", response.redirectedUrl)
    }
    public void testUpdateSessionWFEditEmptyList() {
        def sec = new ScheduledExecutionController()

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    options: [new Option(name: 'blah',enforced:false)],
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            assertNotNull(se.save())
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._doupdate { params, user, roleList, framework, authctx, changeinfo = [:] ->
                assertNotNull(params['_sessionEditWFObject'])
                assertEquals([],params['_sessionEditWFObject'])
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()


            sec.metaClass.message={params -> params?.code?:'messageCodeMissing'}

            def params = [
                    id:se.id.toString(),
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _sessionwf: true,
            ]
            sec.session['editWF']= [
                    (se.id.toString()): [
                            //empty list
                    ]
            ]
            sec.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            sec.update()

            assertNotNull sec.flash.savedJob
            assertNotNull sec.flash.savedJobMessage
            assertNull view
        assertEquals("/scheduledExecution/show/1", response.redirectedUrl)
    }

    public void testSaveFail() {
        def sec = new ScheduledExecutionController()

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._dosave { params, user, rolelist, framework, authctx, changeinfo ->
                [success: false]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()
            def nServiceControl = mockFor(NotificationService, true)
            nServiceControl.demand.listNotificationPlugins { []}
            sec.notificationService = nServiceControl.createMock()

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
            assertEquals '/scheduledExecution/create', view
            assertNull model.scheduledExecution
    }
    public void testSaveUnauthorized() {
        def sec = new ScheduledExecutionController()

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._dosave { params, user, rolelist, framework, authctx, changeinfo ->
                [success: false,unauthorized:true,error:'unauthorizedMessage']
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()

            def nServiceControl = mockFor(NotificationService, true)
            nServiceControl.demand.listNotificationPlugins { [] }
            sec.notificationService = nServiceControl.createMock()

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
            assertEquals '/scheduledExecution/create', view
            assertNull model.scheduledExecution
    }

    public void testSaveAndExecBasic() {
        def sec = new ScheduledExecutionController()

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._dosave { params, user, rolelist, framework, authctx, changeinfo ->
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
            assertNull view
            assertEquals("/scheduledExecution/execute/1", response.redirectedUrl)
    }
    public void testSaveAndExecFailed() {
        def sec = new ScheduledExecutionController()

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._dosave { params, user, rolelist, framework, authctx, changeinfo ->
                [success: false, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.createMock()
            def nServiceControl = mockFor(NotificationService, true)
            nServiceControl.demand.listNotificationPlugins { [] }
            sec.notificationService = nServiceControl.createMock()


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
            assertEquals '/scheduledExecution/create', view
            assertNull  response.redirectedUrl
    }

    public void testRunAdhocBasic() {

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            controller.frameworkService = fwkControl.createMock()
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
        controller.scheduledExecutionService = seServiceControl.createMock()

            def eServiceControl = mockFor(ExecutionService, true)
            def exec = new Execution(
                    user: "testuser", project: "testproj", loglevel: 'WARN',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                    )
            assertNotNull exec.save()
            eServiceControl.demand.createExecutionAndPrep {params, framework, user ->
                return exec
            }
        controller.executionService = eServiceControl.createMock()


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

            def params = [
                    jobName: 'monkey1',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
        controller.params.putAll(params)
            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        controller.request.setAttribute("subject", subject)

            def model= controller.runAdhoc()

            assertNull model.failed
            assertNotNull model.execution
            assertNotNull exec.id
            assertEquals exec, model.execution
            assertEquals('notequal',exec.id.toString(), model.id.toString())
    }
    /**
     * User input provides old node filters, runAdhoc should supply new filter string to scheduleTempJob
     */
    public void testRunAdhocOldNodeFilters() {

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                doNodedispatch: true,
                nodeIncludeTags: 'balogna',
                nodeIncludeName: 'rambo',
                nodeExcludeOsArch: 'x86',
                nodeExclude: 'somehostname',
        )
        se.save()
//
        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.projects {return []}
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
        seServiceControl.demand._dovalidate {params, user, rolelist, framework ->
            assertEquals('Temporary_Job',params.jobName)
            assertEquals('adhoc',params.groupPath)
            [failed: false, scheduledExecution: se]
        }
        seServiceControl.demand.scheduleTempJob {user,subject,params,exec ->
            assertNotNull(params.filter)
            assertEquals("name: rambo tags: balogna !hostname: somehostname !os-arch: x86",params.filter)
            return exec.id
        }
        seServiceControl.demand.logJobChange {changeinfo, properties ->}
        controller.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep {params, framework, user ->
            return exec
        }
        controller.executionService = eServiceControl.createMock()


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}

        def params = [
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
        ]
        controller.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        controller.request.setAttribute("subject", subject)

        def model= controller.runAdhoc()

        assertNull model.failed
        assertNotNull model.execution
        assertNotNull exec.id
        assertEquals exec, model.execution
        assertEquals('notequal',exec.id.toString(), model.id.toString())
    }

    public void testRunAdhocFailed() {
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
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
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


    public void testApiRunJob() {
        def sec = new ScheduledExecutionController()

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
        assertNotNull se.id

        //try to do api job run
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework { -> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, List actions, project ->
            assert 'run' in actions
            return true
        }
        sec.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand.getByIDorUUID { id -> return se }
        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.executeScheduledExecution { scheduledExecution, framework, authctx, subject, inparams ->
            assert 'anonymous' == inparams.user
            return [executionId: exec.id, name: scheduledExecution.jobName, execution: exec,success:true]
        }
        eServiceControl.demand.respondExecutionsXml { response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.createMock()

        def svcMock = mockFor(ApiService, true)
        svcMock.demand.requireExists { response, exists, args ->
            true
        }
        svcMock.demand.renderErrorXml { response, Map error ->
            println(error)
            fail("Should not have error")
        }
        sec.apiService = svcMock.createMock()

        sec.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }

        def params = [id: se.id.toString()]
        sec.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        ExecutionController.metaClass.renderApiExecutionListResultXML={List execs->
            assert 1==execs.size()
        }

        sec.apiJobRun()
    }
    public void testApiRunJob_AsUser() {
        assertRunJobAsUser([jobName: 'monkey1', project: 'testProject', description: 'blah',],
                null,
                'differentUser')
    }
    public void testApiRunJob_ScheduledJob_AsUser() {
        assertRunJobAsUser([scheduled: true, user: 'bob', jobName: 'monkey1', project: 'testProject', description: 'blah',],
                'bob',
                'differentUser')
    }

    private void assertRunJobAsUser(Map job, String expectJobUser, String userName) {
        def sec = new ScheduledExecutionController()
        def se = new ScheduledExecution(job)
        se.workflow= new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        se.save()
        assertNotNull se.id
        assertEquals(expectJobUser,se.user)

        //try to do api job run
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }

        def x = 0
        fwkControl.demand.authorizeProjectJobAll(2) { framework, resource, List actions, project ->
            if (0 == x) {
                assert 'run' in actions
                x++
            } else {
                assert 'runAs' in actions
            }
            return true
        }
        sec.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand.getByIDorUUID { id -> return se }
        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.executeScheduledExecution { scheduledExecution, framework, authctx, subject, inparams ->
            assert userName == inparams.user
            return [executionId: exec.id, name: scheduledExecution.jobName, execution: exec,success:true]

        }
        eServiceControl.demand.respondExecutionsXml { response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.createMock()

        def svcMock = mockFor(ApiService, true)
        svcMock.demand.requireExists { response, exists, args ->
            true
        }
        if(userName){
            //asUser requires v5
            svcMock.demand.requireVersion { request,response, min ->
                assertEquals(5,min)
                return true
            }
        }

        svcMock.demand.renderErrorXml { response, Map error ->
            println(error)
            fail("Should not have error")
        }
        sec.apiService = svcMock.createMock()

        sec.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }

        def params = [id: se.id.toString(), asUser: userName]
        sec.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 5)
//        sec.request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML={List execs->
            assert 1==execs.size()
            assert execs.contains(exec)
        }

        sec.apiJobRun()
    }

    public void testApiRunCommandNoProject() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.existsFrameworkProject(1..1) { project, fwk ->
            true
        }

        fwkControl.demand.authorizeProjectResource(1..1) { framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidate(1..1) { params, user, roleList, framework ->
            assert 'testuser' == user
            new ScheduledExecution()
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1) { request, scheduledExecution, framework ->
            true
        }

        seServiceControl.demand.scheduleTempJob(1..1) { user, subject, params, e ->
            assert 'testuser' == user
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep(1..1) { params, framework, user ->
            assert 'testuser' == user
            exec
        }
        sec.executionService = eServiceControl.createMock()


        def params = [exec: 'blah']
        sec.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 5)
//        sec.request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded = false
        def svcMock = mockFor(ApiService, true)
        def requireFailed=false
        svcMock.demand.requireParameters { reqparams, response, List needparams ->
            assertTrue('project' in needparams)
            assertTrue('exec' in needparams)
            assertNotNull(reqparams.exec)
            assertNull(reqparams.project)
            requireFailed=true
            return false
        }
        sec.apiService = svcMock.createMock()
        def result = sec.apiRunCommand()
        assert !succeeded
        assert null == view
        assertNull(response.redirectedUrl)
        assert null==sec.flash.error
        assert !sec.chainModel
        assert requireFailed
    }

    public void testApiRunCommand() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject(1..1) { project, fwk ->
            true
        }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidate(1..1){params, user, roleList, framework->
            assert null==user
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.scheduleTempJob(1..1){ user, subject, params, e->
            assert 'anonymous'==user
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep(1..1){ params, framework, user->
            assert 'anonymous' == user
            exec
        }
        sec.executionService = eServiceControl.createMock()


        def params = [exec:'blah',project: 'test']
        sec.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 5)
//        sec.request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = mockFor(ApiService, true)
        def requireFailed = true
        svcMock.demand.requireParameters { reqparams, response, List needparams ->
            assertTrue('project' in needparams)
            assertTrue('exec' in needparams)
            assertNotNull(reqparams.exec)
            assertNotNull(reqparams.project)
            requireFailed = false
            return true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project','test'],args)
            return true
        }
        svcMock.demand.renderSuccessXml { request,response, closure ->
            succeeded=true
            return true
        }
        sec.apiService = svcMock.createMock()
        def result=sec.apiRunCommand()
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
        assert !requireFailed
    }

    public void testApiRunCommandAsUser() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject(1..1) { project, fwk ->
            true
        }

        fwkControl.demand.authorizeProjectResource(1..1) { framework, res, action, project ->
            assert 'runAs' == action
            return true
        }
        sec.frameworkService = fwkControl.createMock()
        def seServiceControl = mockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidate(1..1) { params, user, roleList, framework ->
            assert null == user
            [scheduledExecution: new ScheduledExecution(), failed: false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1) { request, scheduledExecution, framework ->
            true
        }

        seServiceControl.demand.scheduleTempJob(1..1) { user, subject, params, e ->
            assert 'anotheruser' == user
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep(1..1) { params, framework, user ->
            assert 'anotheruser' == user
            exec
        }
        sec.executionService = eServiceControl.createMock()


        def params = [exec: 'blah', project: 'test', asUser: 'anotheruser']
        sec.params.putAll(params)
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 5)
//        sec.request.api_version = 5
//        registerMetaClass(ExecutionController)
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded = false

        def svcMock = mockFor(ApiService, true)
        def requireFailed = true
        svcMock.demand.requireParameters { reqparams, response, List needparams ->
            assertTrue('project' in needparams)
            assertTrue('exec' in needparams)
            assertNotNull(reqparams.exec)
            assertNotNull(reqparams.project)
            requireFailed = false
            return true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project', 'test'], args)
            return true
        }
        svcMock.demand.requireVersion { request,response, int min->
            assertEquals(5, min)
            return true
        }
        svcMock.demand.renderSuccessXml { request, response, closure ->
            succeeded = true
            return true
        }
        sec.apiService = svcMock.createMock()
        def result = sec.apiRunCommand()
        assert succeeded
        assert null == view
        assertNull(response.redirectedUrl)
        assert !model
    }

    public void testCopy() {
        mockDomain(ScheduledExecution)
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2')
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getAuthContextForSubject { subject -> return null }
            fwkControl.demand.projects { return [] }
            fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID { id -> return se }
            seServiceControl.demand.userAuthorizedForJob { user, schedexec, framework -> return true }
            sec.scheduledExecutionService = seServiceControl.createMock()

            def pControl = mockFor(NotificationService)
            pControl.demand.listNotificationPlugins() {->
                []
            }
            sec.notificationService = pControl.createMock()

            def params = [id: se.id.toString()]
            sec.params.putAll(params)
            sec.copy()
            assertNull sec.response.redirectedUrl
            def copied = sec.modelAndView.model.scheduledExecution
            assertNotNull(copied)
            assertEquals(se.jobName, copied.jobName)
        }
    }

    /**
     * test application/x-www-form-urlencoded instead of multipart
     */
    public void testUploadFormContentShouldCreate() {
        def sec = new ScheduledExecutionController()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                description: 'desc',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input,format ->
            [jobset:[expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, user, roleList, changeinfo, framework, authctx ->
            assert jobset==[expectedJob]
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        params.xmlBatch = xml.toString()
        request.method='POST'
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.params.project="project1"

        def result = sec.upload()
        assertNull sec.response.redirectedUrl
        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNull "Result had an error: ${sec.flash.message}", sec.flash.message
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertEquals "test1", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
    }


    public void testUploadProjectParameter() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'BProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input,format ->
            [jobset: [expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, user, roleList, changeinfo, framework, authctx ->
            assertEquals('BProject', jobset[0].project)
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>AProject</project>
             <options>
                <option name='testopt' value='`ls -t1 /* | head -n1`' values="a,b,c" />
              </options>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        sec.params.project = "BProject"
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
    }

    public void testUploadOptions() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, user, roleList, changeinfo, framework, authctx ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
             <options>
                <option name='testopt' value='`ls -t1 /* | head -n1`' values="a,b,c" />
              </options>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertNotNull job.options
        assertEquals 1, job.options.size()
        Option opt = job.options.iterator().next()
        assertEquals "testopt", opt.name
        assertEquals "`ls -t1 /* | head -n1`", opt.defaultValue
        assertNotNull opt.values
        assertEquals 3, opt.values.size()
        assertTrue opt.values.contains("a")
        assertTrue opt.values.contains("b")
        assertTrue opt.values.contains("c")
    }

    public void testUploadOptions2() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, user, roleList, changeinfo, framework, authctx ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()
        def xml = '''
-
  project: project1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo test
  description: desc
  name: test1
  group: testgroup
  options:
    testopt:
      value: '`ls -t1 /* | head -n1`'
      values: [a,b,c]

'''
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)

        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/yaml', xml as byte[]))
        sec.params.fileformat = "yaml"
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertNotNull job.options
        assertEquals 1, job.options.size()
        Option opt = job.options.iterator().next()
        assertEquals "testopt", opt.name
        assertEquals "`ls -t1 /* | head -n1`", opt.defaultValue
        assertNotNull opt.values
        assertEquals 3, opt.values.size()
        assertTrue opt.values.contains("a")
        assertTrue opt.values.contains("b")
        assertTrue opt.values.contains("c")
    }

    public void testUploadShouldCreate() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'testgroup',
                description: 'desc',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, user, roleList, changeinfo, framework, authctx ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''

        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.params.project = "project1"

        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertEquals "test1", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
    }

    /**
     * test normal get request has no error
     */
    public void testUploadGetRequest() {
        def sec = new ScheduledExecutionController()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.createMock()

        request.method="GET"
        def result = sec.upload()
        assertNull(sec.flash.message)
        assertNull result

    }
    /**
     * test missing content
     */
    public void testUploadMissingContent() {
        def sec = new ScheduledExecutionController()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.createMock()

        request.method="POST"
        def result = sec.upload()
        assertEquals('No file was uploaded.', sec.flash.message)
        assertNull result

    }
    /**
     * test missing File content
     */
    public void testUploadMissingFile() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubject { subject -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.createMock()

        def result = sec.upload()
        assertEquals "No file was uploaded.", sec.flash.message
        assertNull result

    }
}
