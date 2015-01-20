package rundeck.controllers

import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.support.ExtraCommand
import com.dtolabs.rundeck.app.support.RunJobCommand
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.FrameworkResource

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
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest
import rundeck.codecs.URIComponentCodec
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
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
    }
    public void setUp(){

        mockCodec(URIComponentCodec)
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

    public void testExpandUrlOptionValueSimple() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'monkey', controller.expandUrl(option, '${option.test1.value}', se,[test1:'monkey'])
    }
    public void testExpandUrlOptionValueUrl() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://some.host/path/a%20monkey', controller.expandUrl(option, 'http://some.host/path/${option.test1.value}', se,[test1:'a monkey'])
    }
    public void testExpandUrlOptionValueParam() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://some.host/path/?a+monkey', controller.expandUrl(option, 'http://some.host/path/?${option.test1.value}', se,[test1:'a monkey'])
    }
    public void testExpandUrlOptionName() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'test1', controller.expandUrl(option, '${option.name}', se)
    }

    public void testExpandUrlJobName() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'blue', controller.expandUrl(option, '${job.name}', se)
    }

    public void testExpandUrlJobGroup() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'some%2Fwhere', controller.expandUrl(option, '${job.group}', se)
    }

    public void testExpandUrlJobDesc() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'a%20job', controller.expandUrl(option, '${job.description}', se)
    }

    public void testExpandUrlJobDescParam() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals '?a+job', controller.expandUrl(option, '?${job.description}', se)
    }

    public void testExpandUrlJobProject() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'AProject', controller.expandUrl(option, '${job.project}', se)
    }

    public void testExpandUrlJobProp_nonexistent() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals '${job.noexist}', controller.expandUrl(option, '${job.noexist}', se)
    }

    public void testExpandUrlJobMultipleValues() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'http://test/action?name=blue&option=test1&project=AProject',
            controller.expandUrl(option, 'http://test/action?name=${job.name}&option=${option.name}&project=${job.project}', se)

    }

    public void testExpandUrlJobUsernameAnonymous() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'anonymous', controller.expandUrl(option, '${job.user.name}', se)
    }

    public void testExpandUrlJobUsername() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        controller.session.user='bob'
        assertEquals 'bob', controller.expandUrl(option, '${job.user.name}', se)
    }

    public void testExpandUrlJobRundeckNodename() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'server1', controller.expandUrl(option, '${job.rundeck.nodename}', se)
    }

    public void testExpandUrlJobRundeckServerUUID() {
        def (Option option, ScheduledExecution se) = setupExpandUrlJob(controller)
        assertEquals 'xyz', controller.expandUrl(option, '${job.rundeck.serverUUID}', se)
    }

    protected List setupExpandUrlJob(def controller) {
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath: 'some/where',
                description: 'a job', project: 'AProject', argString: '-a b -c d')

        final Option option = new Option(name: 'test1', enforced: false)
        se.addToOptions(option)
        se.save()
        assertNotNull(option.properties)
        controller.frameworkService = mockWith(FrameworkService) {
            getFrameworkNodeName(1..1) {->
                'server1'
            }
            getServerUUID(1..12) {->
                'xyz'
            }
        }
        [option, se]
    }

    public void testSaveBasic() {
        def sec = controller
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

            setupFormTokens(sec)

        request.method='POST'
            sec.save()

            assertNotNull sec.flash.savedJob
            assertNotNull sec.flash.savedJobMessage
            assertNull view, view
            assertEquals("/scheduledExecution/show/1", response.redirectedUrl)
    }
    public void testSave_invalidToken() {
        def sec = controller
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

            //don't include request token

        request.method='POST'
            sec.save()

            assertNull sec.flash.savedJob
            assertNull sec.flash.savedJobMessage
            assertEquals( '/common/error', view)
            assertEquals("request.error.invalidtoken.message", request.getAttribute('errorCode'))
    }

    protected void setupFormTokens(ScheduledExecutionController sec) {
        def token = SynchronizerTokensHolder.store(session)
        sec.params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        sec.params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
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
            setupFormTokens(sec)
            request.method='POST'
            sec.update()

            assertNotNull sec.flash.savedJob
            assertNotNull sec.flash.savedJobMessage
            assertNull view
            assertEquals("/scheduledExecution/show/1", response.redirectedUrl)
    }
    public void testUpdate_invalidToken() {
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
            //don't include token
        request.method='POST'
            sec.update()



        assertNull sec.flash.savedJob
        assertNull sec.flash.savedJobMessage
        assertEquals('/common/error', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('errorCode'))
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

        setupFormTokens(sec)
        request.method='POST'
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

        setupFormTokens(sec)
        request.method='POST'
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

        setupFormTokens(sec)
        request.method='POST'
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

    public void testRunJobNow() {
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
        assertNotNull se.id

        controller.frameworkService = mockWith(FrameworkService){
            getAuthContextForSubject { Subject subject -> return null }
            authorizeProjectJobAll { framework, resource, actions, project -> return true }
        }

        controller.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            userAuthorizedForAdhoc { request, scheduledExecution, framework -> return true }
            _dovalidate { params, user, rolelist, framework ->
                assertEquals('Temporary_Job', params.jobName)
                assertEquals('adhoc', params.groupPath)
                [failed: false, scheduledExecution: se]
            }
            scheduleTempJob { auth, exec ->
                return exec.id
            }
            logJobChange { changeinfo, properties -> }
        }

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                )
        assertNotNull exec.save()

        controller.executionService = mockWith(ExecutionService){
            executeJob{ ScheduledExecution scheduledExecution, AuthContext authContext, String user, Map input->
                [executionId:exec.id]
            }
        }


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}
        final subject = new Subject()
        controller.session.setAttribute("subject", subject)

        def command = new RunJobCommand()
        command.id=se.id.toString()
        def extra = new ExtraCommand()

        setupFormTokens(controller)

        request.method='POST'
        controller.runJobNow(command,extra)

        assertEquals('/scheduledExecution/show',response.redirectedUrl)
    }
    public void testRunJobNow_missingToken() {
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
//
        assertNotNull se.id

        controller.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubject { Subject subject -> return null }
            authorizeProjectJobAll { framework, resource, actions, project -> return true }
        }

        controller.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            userAuthorizedForAdhoc { request, scheduledExecution, framework -> return true }
            _dovalidate { params, user, rolelist, framework ->
                assertEquals('Temporary_Job', params.jobName)
                assertEquals('adhoc', params.groupPath)
                [failed: false, scheduledExecution: se]
            }
            scheduleTempJob { auth, exec ->
                return exec.id
            }
            logJobChange { changeinfo, properties -> }
        }

        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                )
        assertNotNull exec.save()

        controller.executionService = mockWith(ExecutionService){
            executeJob{ ScheduledExecution scheduledExecution, AuthContext authContext, String user, Map input->
                [executionId:exec.id]
            }
        }


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


        def command = new RunJobCommand()
        command.id=se.id.toString()
        def extra = new ExtraCommand()

        //setupFormTokens(controller)//XXX: don't set up tokens

        request.method='POST'
        controller.runJobNow(command,extra)

        assertEquals(400,response.status)
        assertEquals('request.error.invalidtoken.message',request.errorCode)
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
            seServiceControl.demand.scheduleTempJob { auth, exec ->
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
            eServiceControl.demand.createExecutionAndPrep {params, user ->
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
            assertTrue model.success
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
        seServiceControl.demand.scheduleTempJob { auth, exec ->
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
        eServiceControl.demand.createExecutionAndPrep {params, user ->
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
            seServiceControl.demand.scheduleTempJob { auth, exec ->
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
            eServiceControl.demand.createExecutionAndPrep { params, user ->
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
        eServiceControl.demand.executeJob { scheduledExecution, authctx, user, inparams ->
            assert 'anonymous' == user
            return [executionId: exec.id, name: scheduledExecution.jobName, execution: exec,success:true]
        }
        eServiceControl.demand.respondExecutionsXml { response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.createMock()

        def svcMock = mockFor(ApiService, true)
        svcMock.demand.requireApi { req,resp ->
            true
        }
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
        session.user='anonymous'
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
        eServiceControl.demand.executeJob { ScheduledExecution scheduledExecution, AuthContext authContext,
                                            String user,
                                            Map input ->
            assert userName == user
            return [executionId: exec.id, name: scheduledExecution.jobName, execution: exec,success:true]

        }
        eServiceControl.demand.respondExecutionsXml { response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.createMock()

        def svcMock = mockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp ->
            true
        }
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

        seServiceControl.demand.scheduleTempJob { auth, exec ->
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep { params, user ->
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
        svcMock.demand.requireApi { req, resp ->
            true
        }
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

    public void testApiBulkJobDeleteRequest_validation() {
            def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.ids = ['a9cd7388-05c5-45ce-8cdb-93f5f0325218', '669ee20d-24bc-4b31-8552-001bb396edd0',
                'api-test-job-run-scheduled', 'api-v5-test-exec-query2', 'api-v5-test-exec-query',
                'b2767051-6669-492c-aaba-e78c4d2d9ce8', 'bc6bc234-4ed3-4fda-8909-579041835575',
                '7a1f4c82-5f30-413f-99ff-3be44976c958', '000523a2-4f6e-4718-a0b2-25252a25dd79',
                '4bbd6b2f-7c92-4ad2-8067-9adcda4fcb65', 'fd043bd4-e3c9-443e-9eb2-1e78988dba04',
                'c1421959-510b-4075-9860-d801c013d7ec', '068c29db-6e6f-4f6b-b3bf-7bae31238814']
            def valid=cmd.validate()
            cmd.errors.allErrors.each{
                println(it)
            }
            assertTrue(valid)
    }
    public void testApiBulkJobDeleteRequest_validation_idlist() {
            def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.idlist = ['a9cd7388-05c5-45ce-8cdb-93f5f0325218', '669ee20d-24bc-4b31-8552-001bb396edd0',
                'api-test-job-run-scheduled', 'api-v5-test-exec-query2', 'api-v5-test-exec-query',
                'b2767051-6669-492c-aaba-e78c4d2d9ce8', 'bc6bc234-4ed3-4fda-8909-579041835575',
                '7a1f4c82-5f30-413f-99ff-3be44976c958', '000523a2-4f6e-4718-a0b2-25252a25dd79',
                '4bbd6b2f-7c92-4ad2-8067-9adcda4fcb65', 'fd043bd4-e3c9-443e-9eb2-1e78988dba04',
                'c1421959-510b-4075-9860-d801c013d7ec', '068c29db-6e6f-4f6b-b3bf-7bae31238814'].join(",")
            def valid=cmd.validate()
            cmd.errors.allErrors.each{
                println(it)
            }
            assertTrue(valid)
    }
    public void testApiBulkJobDeleteRequest_validation_failure_blank_ids() {
        def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.ids = ['']
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('ids'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_ids() {
        def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.ids = ['asdf/monkey']
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('ids'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_id() {
        def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.id = 'asdf/monkey'
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('id'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_idlist() {
        def cmd = mockCommandObject(ApiBulkJobDeleteRequest)
        cmd.idlist = '068c29db-6e6f-4f6b-b3bf-7bae31238814,asdf/monkey'
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('idlist'))
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

        seServiceControl.demand.scheduleTempJob { auth, exec ->
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep { params, user ->
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
        svcMock.demand.requireApi { req, resp ->
            true
        }
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

        seServiceControl.demand.scheduleTempJob { auth, exec ->
            'fakeid'
        }

        sec.scheduledExecutionService = seServiceControl.createMock()

        def eServiceControl = mockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep { params, user ->
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
        svcMock.demand.requireApi { req, resp ->
            true
        }
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
        def sec = controller

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

        setupFormTokens(sec)

        sec.uploadPost()
        def result=sec.modelAndView.model
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
    /**
     * test application/x-www-form-urlencoded instead of multipart
     */
    public void testUpload_invalidToken() {
        def sec = controller

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

        //don't set up form tokens

        sec.uploadPost()

        assertNull sec.flash.savedJob
        assertNull sec.flash.savedJobMessage
        assertEquals('/scheduledExecution/upload', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('warn'))
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

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
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

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
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

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
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

        setupFormTokens(sec)


        sec.uploadPost()
        def result = sec.modelAndView.model
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

        setupFormTokens(sec)


        sec.uploadPost()
        def result = sec.modelAndView.model
        assertEquals('No file was uploaded.', sec.request.getAttribute('message'))
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


        setupFormTokens(sec)


        sec.uploadPost()
        def result = sec.modelAndView.model
        assertEquals "No file was uploaded.", sec.request.getAttribute('message')
    }
}
