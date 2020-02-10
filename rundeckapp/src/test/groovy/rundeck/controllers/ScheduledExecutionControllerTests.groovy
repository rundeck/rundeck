/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

package rundeck.controllers

import com.dtolabs.rundeck.core.common.PluginControlService
import groovy.mock.interceptor.MockFor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.components.jobs.JobDefinitionManager
import rundeck.services.ExecutionLifecyclePluginService
import rundeck.services.feature.FeatureService
import rundeck.services.optionvalues.OptionValuesService
import rundeck.ScheduledExecutionStats

import static org.junit.Assert.*

import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.api.ApiRunAdhocRequest
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest
import rundeck.JobExec
import rundeck.ReferencedExecution
import rundeck.codecs.URIComponentCodec
import rundeck.services.ApiService
import rundeck.services.NotificationService
import rundeck.services.OrchestratorPluginService
import rundeck.services.PluginService

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

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/*
* ScheduledExecutionControllerTests.java
*
* User: greg
* Created: Jun 11, 2008 5:12:47 PM
* $Id$
*/
@TestFor(ScheduledExecutionController)
@Mock([ScheduledExecution,Option,Workflow,CommandExec,Execution,JobExec, ReferencedExecution, ScheduledExecutionStats])
class ScheduledExecutionControllerTests  {
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz,false)
        mock.demand.with(clos)
        return mock.proxyInstance()
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
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)
        seServiceControl.demand. _docreateJobOrParams{ijob,params,auth,job->
            [success: true, scheduledExecution: se]
        }
        seServiceControl.demand.issueJobChangeEvent {event->}
        seServiceControl.demand.logJobChange {changeinfo, properties ->}
        seServiceControl.demand.getByIDorUUID {id -> return se }
            sec.scheduledExecutionService = seServiceControl.proxyInstance()

			def oServiceControl = new MockFor(OrchestratorPluginService, true)
			sec.orchestratorPluginService = oServiceControl.proxyInstance()

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
            assertEquals("/job/show/1", response.redirectedUrl)
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
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._dosave {params, authctx, changeinfo ->
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.proxyInstance()


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
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._doupdate {params, authctx, changeinfo = [:] ->
                assertNotNull(params['_sessionEditOPTSObject'])
                assertEquals([],params['_sessionEditOPTSObject'])
                [success: true, scheduledExecution: se]
            }
        seServiceControl.demand.issueJobChangeEvent {evt->}
        seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.proxyInstance()


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
            assertEquals("/job/show/1", response.redirectedUrl)
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
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand._doupdate {params, authctx, changeinfo = [:] ->
                assertNotNull(params['_sessionEditOPTSObject'])
                assertEquals([],params['_sessionEditOPTSObject'])
                [success: true, scheduledExecution: se]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.proxyInstance()


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
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand._doupdate { params, authctx, changeinfo = [:] ->
                assertNotNull(params['_sessionEditWFObject'])
                assertEquals([],params['_sessionEditWFObject'])
                [success: true, scheduledExecution: se]
            }
        seServiceControl.demand.issueJobChangeEvent {evt->}
        seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.proxyInstance()


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
        assertEquals("/job/show/1", response.redirectedUrl)
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
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getPluginControlService {  }
        fwkControl.demand.getNodeStepPluginDescriptions { [] }
        fwkControl.demand.getStepPluginDescriptions { [] }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects {return []}
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()

            sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
                _docreateJobOrParams{ijob,params,auth,job->
                    [success: false]
                }
//                getByIDorUUID {id -> return se }
//                logJobChange {changeinfo, properties ->}
                issueJobChangeEvent {event->}
                getWorkflowStrategyPluginDescriptions{->[]}
            }
            def nServiceControl = new MockFor(NotificationService, true)
            nServiceControl.demand.listNotificationPlugins { []}
            sec.notificationService = nServiceControl.proxyInstance()

			def oServiceControl = new MockFor(OrchestratorPluginService, true)
			oServiceControl.demand.listDescriptions{[]}
			sec.orchestratorPluginService = oServiceControl.proxyInstance()
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.rundeckJobDefinitionManager=mockWith(RundeckJobDefinitionManager){
            getJobDefinitionComponents{->[:]}
            getJobDefinitionComponentValues{job->[:]}
        }
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
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getPluginControlService {  }
        fwkControl.demand.getNodeStepPluginDescriptions { [] }
        fwkControl.demand.getStepPluginDescriptions { [] }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects {return []}
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()

            sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
                _docreateJobOrParams{ijob,params,auth,job->
                    [success: false,unauthorized:true,error:'unauthorizedMessage']
                }
                issueJobChangeEvent {event->}
                getWorkflowStrategyPluginDescriptions{->[]}
            }

            def nServiceControl = new MockFor(NotificationService, true)
            nServiceControl.demand.listNotificationPlugins { [] }
            sec.notificationService = nServiceControl.proxyInstance()

			def oServiceControl = new MockFor(OrchestratorPluginService, true)
			oServiceControl.demand.listDescriptions{[]}
			sec.orchestratorPluginService = oServiceControl.proxyInstance()
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.rundeckJobDefinitionManager=mockWith(RundeckJobDefinitionManager){
            getJobDefinitionComponents{->[:]}
            getJobDefinitionComponentValues{job->[:]}
        }
			
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

    /**
     * XXX: Moved to ScheduledExecutionControllerSpec
     */
//    public void testRunJobNow() {
//
//    }
    /**
     * XXX: Moved to ScheduledExecutionControllerSpec
     */
//    public void testRunJobNow_execModePassive() {
//
//    }
    /**
     * XXX: Moved to ScheduledExecutionControllerSpec
     */
//    public void testRunJobNow_missingToken() {
//    }
    public void testRunAdhocBasic() {

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            controller.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc {params, auth ->
            assertEquals('Temporary_Job',params.jobName)
            assertEquals('adhoc',params.groupPath)
            [failed: false, scheduledExecution: se]
        }
        seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:exec.id,execution:exec,success:true]
        }
        seServiceControl.demand.getByIDorUUID {id -> return se }
        seServiceControl.demand.logJobChange {changeinfo, properties ->}
        controller.scheduledExecutionService = seServiceControl.proxyInstance()

            def eServiceControl = new MockFor(ExecutionService, true)
            def exec = new Execution(
                    user: "testuser", project: "testproj", loglevel: 'WARN',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                    )
            assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep {params, user ->
                return exec
            }
        controller.executionService = eServiceControl.proxyInstance()


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        controller.request.setAttribute("subject", subject)

        def model= controller.runAdhoc(new ApiRunAdhocRequest(exec:'a remote string',nodeKeepgoing: true,nodeThreadcount: 1,project:'testProject'))

        assertNull model.failed
        assertTrue model.success
        assertNotNull model.execution
        assertNotNull exec.id
        assertEquals exec, model.execution
        assertEquals('notequal',exec.id.toString(), model.id.toString())
    }

    public void testRunAdhocBasic_execModePassive() {

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()
//
        def executionModeActive = false
        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework { -> return null }
        fwkControl.demand.projects { return [] }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getRundeckFramework { -> return null }
        fwkControl.demand.getRundeckFramework { -> return null }
        controller.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc { params, auth ->
            assertEquals('Temporary_Job', params.jobName)
            assertEquals('adhoc', params.groupPath)
            [failed: false, scheduledExecution: se]
        }
        seServiceControl.demand.userAuthorizedForAdhoc { request, scheduledExecution, framework -> return true }
        seServiceControl.demand.getByIDorUUID { id -> return se }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id: exec.id, execution: exec, success: true]
        }
        seServiceControl.demand.logJobChange { changeinfo, properties -> }
        controller.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive { -> executionModeActive }
        eServiceControl.demand.createExecutionAndPrep { params, user ->
            return exec
        }
        controller.executionService = eServiceControl.proxyInstance()


        controller.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        controller.request.setAttribute("subject", subject)

        def model = controller.runAdhoc(
                new ApiRunAdhocRequest(
                        exec: 'a remote string',
                        nodeKeepgoing: true,
                        nodeThreadcount: 1,
                        project: 'testProject'
                )
        )

        assertTrue model.failed
        assertFalse model.success
        assertNull model.execution
        assertEquals 'disabled', model.error
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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects {return []}
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
        controller.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc {params, auth ->
            assertEquals('Temporary_Job',params.jobName)
            assertEquals('adhoc',params.groupPath)
            [failed: false, scheduledExecution: se]
        }
        seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:exec.id,execution:exec,success:true]
        }
        seServiceControl.demand.logJobChange {changeinfo, properties ->}

        controller.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep {params, user ->
            return exec
        }
        controller.executionService = eServiceControl.proxyInstance()


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        controller.request.setAttribute("subject", subject)

        def model = controller.runAdhoc(
                new ApiRunAdhocRequest(
                        exec: 'a remote string',
                        nodeThreadcount: 1,
                        nodeKeepgoing: true,
                        project: 'testProject'
                )
        )

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
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            seServiceControl.demand._dovalidateAdhoc {params, auth ->
                assertEquals('Temporary_Job',params.jobName)
                assertEquals('adhoc',params.groupPath)
                [failed: true, scheduledExecution: se]
            }
            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand.userAuthorizedForAdhoc {request, scheduledExecution, framework -> return true }
            seServiceControl.demand.scheduleTempJob { auth, exec ->
                [id:exec.id,execution:exec,success:true]
            }
            seServiceControl.demand.logJobChange {changeinfo, properties ->}
            sec.scheduledExecutionService = seServiceControl.proxyInstance()

            def eServiceControl = new MockFor(ExecutionService, true)
            def exec = new Execution(
                    user: "testuser", project: "testproj", loglevel: 'WARN',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
                    )
            assertNotNull exec.save()
            eServiceControl.demand.createExecutionAndPrep { params, user ->
                return exec
            }
            sec.executionService = eServiceControl.proxyInstance()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            sec.request.setAttribute("subject", subject)

            def model=sec.runAdhoc(new ApiRunAdhocRequest(exec:'a remote string',project:'testProject',nodeThreadcount: 1,nodeKeepgoing: true))

            assertTrue model.failed
            assertNotNull model.scheduledExecution
            assertEquals 'Job configuration was incorrect.', model.message
        }
    }
    private ScheduledExecution createTestJob(){
        def se=new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
        assertNotNull se.id
        return se
    }
    public void testApiJobExecutions_basic() {
        def sec = controller

        def se = createTestJob()


        def testStatus=null
        def testOffset=0
        def testMax=-1
        def testResultSize=0


        assertApiJobExecutions(se, sec, testStatus, testOffset, testMax, testResultSize,[])

        def params = [id: se.id.toString()]
        sec.params.putAll(params)
        def result=sec.apiJobExecutions()
        assertEquals(200,response.status)
    }
    public void testApiJobExecutions_single() {
        def sec = controller

        def se = createTestJob()
        def exec = new Execution(
                scheduledExecution: se,
                dateStarted: new Date(),
                dateCompleted: new Date(),
                status: 'true',
                user: "testuser",
                project: "testProject",
                loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        if(!exec.validate()){
            exec.errors.allErrors.each{println(it.toString())}
        }
        assertNotNull exec.save()

        def testStatus=null
        def testOffset=0
        def testMax=-1
        def testResultSize=1


        assertApiJobExecutions(se, sec, testStatus, testOffset, testMax, testResultSize,[exec])

        def params = [id: se.id.toString()]
        sec.params.putAll(params)
        def result=sec.apiJobExecutions()
        assertEquals(200,response.status)
    }
    public void testApiJobExecutions_statusParam() {
        def sec = controller

        def se = createTestJob()
        def exec = new Execution(
                scheduledExecution: se,
                dateStarted: new Date(),
                dateCompleted: new Date(),
                status: 'true',
                user: "testuser",
                project: "testProject",
                loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        if(!exec.validate()){
            exec.errors.allErrors.each{println(it.toString())}
        }
        assertNotNull exec.save()


        def testStatus='succeeded'
        def testOffset=0
        def testMax=-1
        def testResultSize=1


        assertApiJobExecutions(se, sec, testStatus, testOffset, testMax, testResultSize,[exec])

        def params = [id: se.id.toString(),status:'succeeded']
        sec.params.putAll(params)
        def result=sec.apiJobExecutions()
        assertEquals(200,response.status)
    }

    private void assertApiJobExecutions(
            se,
            ScheduledExecutionController sec,
            testStatus,
            testOffset,
            testMax,
            testResultSize,
            testResultList
    )
    {
        sec.apiService = mockWith(ApiService) {
            requireApi(1) { req, resp -> true }
            requireParameters(1) { params, resp, keys ->
                assertTrue( 'id' in keys)
                true
            }
            requireExists(1) { response, exists, args ->
                assertTrue( se == exists)
                true
            }
        }
        sec.scheduledExecutionService = mockWith(ScheduledExecutionService) {
            getByIDorUUID(1) { id ->
                assertTrue( id == se.id.toString())
                se
            }
        }
        sec.frameworkService = mockWith(FrameworkService) {
            getAuthContextForSubjectAndProject(1) { subj,proj -> null }
            authorizeProjectJobAny(1) { ctx, job, actions, proj ->
                assert job == se
                assert proj == se.project
                assert 'read' in actions
                assert 'view' in actions
                true
            }
            authorizeProjectResourceAll(1) { ctx, res, actions, proj ->
                assert res == [type: 'resource', kind: 'event']
                assert proj == se.project
                assert 'read' in actions

                true
            }
        }

        sec.executionService = mockWith(ExecutionService) {
            queryJobExecutions(1) { job, stat, offset, max ->
                assert job == se
                assert stat == testStatus
                assert offset == testOffset
                assert max == testMax
                [total: 0, result: testResultList]
            }
            respondExecutionsXml(1) { HttpServletRequest request,HttpServletResponse response, List<Execution> executions, Map params ->
                assertEquals(executions.size() ,testResultSize)
                [result:true]
            }
        }

        sec.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, List actions, project ->
            assert 'run' in actions
            return true
        }
        fwkControl.demand.getRundeckFramework { -> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand.getByIDorUUID { id -> return se }
        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.executeJob { scheduledExecution, authctx, user, inparams ->
            
            return [executionId: exec.id, name: scheduledExecution.jobName, execution: exec,success:true]
        }
        eServiceControl.demand.respondExecutionsXml { request, response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.proxyInstance()

        def svcMock = new MockFor(ApiService, true)
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
        sec.apiService = svcMock.proxyInstance()

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

        def x = 0
        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.authorizeProjectJobAll(2) { framework, resource, List actions, project ->
            if (0 == x) {
                assert 'run' in actions
                x++
            } else {
                assert 'runAs' in actions
            }
            return true
        }
        fwkControl.demand.getRundeckFramework {-> return null }


        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand.getByIDorUUID { id -> return se }
        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
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
        eServiceControl.demand.respondExecutionsXml { request, response, List<Execution> execs ->
            return true
        }
        sec.executionService = eServiceControl.proxyInstance()

        def svcMock = new MockFor(ApiService, true)
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
        sec.apiService = svcMock.proxyInstance()

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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.existsFrameworkProject(1..1) { project, fwk ->
            true
        }

        fwkControl.demand.authorizeProjectResource(1..1) { framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand.userAuthorizedForAdhoc(1..1) { request, scheduledExecution, framework ->
            true
        }

        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }

        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.createExecutionAndPrep { params, user ->
            assert 'testuser' == user
            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


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
        def svcMock = new MockFor(ApiService, true)

        svcMock.demand.renderErrorFormat { response, Map error ->
            assert error.status == 400
            assert error.code == 'api.error.invalid.request'
        }

        sec.apiService = svcMock.proxyInstance()
        def result = sec.apiRunCommand(new ApiRunAdhocRequest(exec: 'blah'))
        assert !succeeded
        assert null == view
        assertNull(response.redirectedUrl)
        assert null==sec.flash.error
        assert !sec.chainModel
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

    public void testApiRunScript_RequiresPOST() {
        def sec = new ScheduledExecutionController()

        sec.request.setAttribute("api_version", 14)
        def result=sec.apiRunScript(new ApiRunAdhocRequest(script:'blah',project: 'test'))
        assert 405==response.status
    }
    public void testApiRunScript_v14_RequiresPOST() {
        def sec = new ScheduledExecutionController()

        sec.request.setAttribute("api_version", 14)
        def result=sec.apiRunScriptv14(new ApiRunAdhocRequest(script:'blah',project: 'test'))
        assert 405==response.status
    }

    public void testApiRunScriptUrl_v14() {
        def sec = new ScheduledExecutionController()


        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }

        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 14)
//        sec.request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def apiverslist=[14,4]
        sec.apiService = mockWith(ApiService) {
            requireVersion(1..1){ req, resp, apivers ->
                def val=apiverslist.remove(0)
                assert apivers == val
                assert req.api_version==14
                true
            }
            requireApi { req, resp ->
                true
            }
            requireVersion(1..1){ req, resp, apivers ->
                def val=apiverslist.remove(0)
                assert apivers == val
                assert req.api_version==14
                true
            }
            requireExists { response, exists, args ->
                assertEquals(['project','test'],args)
                return true
            }
            renderSuccessXml { request,response, closure ->
                succeeded=true
                return true
            }
        }
        def result=sec.apiRunScriptUrlv14(new ApiRunAdhocRequest(url: 'blah',project: 'test'))
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }

    private UserAndRolesAuthContext testUserAndRolesContext(String user='test',String roleset='test') {
        [getUsername: { user }, getRoles: { roleset.split(',') as Set }] as UserAndRolesAuthContext
    }
    public void testApiRunCommand_v14() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }


        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 14)
//        sec.request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        sec.apiService = mockWith(ApiService) {
            requireVersion { req, resp, apivers ->
                assert apivers == 14
                assert req.api_version==14
                true
            }
            requireApi { req, resp ->
                true
            }
            requireExists { response, exists, args ->
                assertEquals(['project','test'],args)
                return true
            }
            renderSuccessXml { request,response, closure ->
                succeeded=true
                return true
            }
        }
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_XML() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }

        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


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
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp ->
            true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project','test'],args)
            return true
        }
        svcMock.demand.renderSuccessXml { request,response, closure ->
            succeeded=true
            return true
        }
        sec.apiService = svcMock.proxyInstance()
        def result=sec.apiRunCommand(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_executionModePassive() {
        def sec = new ScheduledExecutionController()
        def executionModeActive=false
        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }

        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->executionModeActive}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


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
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp ->
            true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project','test'],args)
            return true
        }
        svcMock.demand.renderErrorFormat { response, data ->
            assertEquals(500,data.status)
            assertEquals(['disabled.execution.run'],data.args)
        }
        sec.apiService = svcMock.proxyInstance()
        def result=sec.apiRunCommand(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        assert !succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_JSON_apiversionInvalid() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }


        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 5)
//        sec.request.api_version = 5
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp ->
            true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project','test'],args)
            return true
        }
        svcMock.demand.renderErrorFormat{response,data->
            assertEquals([
                    status:HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    code: 'api.error.item.unsupported-format',
                    args: ['json']
            ],data)
        }
        svcMock.demand.renderSuccessJson { response, closure ->
            succeeded=true
            return true
        }
        sec.apiService = svcMock.proxyInstance()
        sec.response.format='json'
        def result=sec.apiRunCommand(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        assert !succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_JSON_apiversionValid() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        fwkControl.demand.authorizeProjectResource(1..1){framework, res, action, project ->
            assert 'runAs' in actions
            return true
        }
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1){params, auth->
            
            [scheduledExecution:new ScheduledExecution(),failed:false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1){ request, scheduledExecution, framework->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }


        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        sec.request.setAttribute("subject", subject)
        sec.request.setAttribute("api_version", 14)
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp ->
            true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project','test'],args)
            return true
        }
        svcMock.demand.renderSuccessJson { response, closure ->
            succeeded=true
            return true
        }
        sec.apiService = svcMock.proxyInstance()
        sec.response.format='json'
        def result=sec.apiRunCommand(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }

    public void testApiRunCommandAsUser() {
        def sec = new ScheduledExecutionController()

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.authorizeProjectResource(1..1) { framework, res, action, project ->
            assert 'runAs' == action
            return true
        }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)

        seServiceControl.demand._dovalidateAdhoc(1..1) { params, auth ->

            [scheduledExecution: new ScheduledExecution(), failed: false]
        }
        seServiceControl.demand.userAuthorizedForAdhoc(1..1) { request, scheduledExecution, framework ->
            true
        }

        seServiceControl.demand.isProjectExecutionEnabled{ project -> true
        }
        seServiceControl.demand.scheduleTempJob { auth, exec ->
            [id:'fakeid',execution:exec,success:true]
        }

        sec.scheduledExecutionService = seServiceControl.proxyInstance()

        def eServiceControl = new MockFor(ExecutionService, true)
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        assertNotNull exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->
            assert 'anotheruser' == user
            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


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

        def svcMock = new MockFor(ApiService, true)
        def requireFailed = true
        svcMock.demand.requireApi { req, resp ->
            true
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
        svcMock.demand.requireParameters { reqparams, response, List needparams ->
            assertTrue('project' in needparams)
            assertTrue('exec' in needparams)
            assertNotNull(reqparams.exec)
            assertNotNull(reqparams.project)
            requireFailed = false
            return true
        }
        sec.apiService = svcMock.proxyInstance()
        def result = sec.apiRunCommand()
        assert succeeded
        assert null == view
        assertNull(response.redirectedUrl)
        assert !model
    }

    public void testCopy() {
        def sec = new ScheduledExecutionController()
        //test basic copy action
        sec.rundeckJobDefinitionManager=new RundeckJobDefinitionManager()
            def se = new ScheduledExecution(
                    uuid: 'testUUID',
                    jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    groupPath: 'testgroup',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [
                                    new CommandExec([
                                            adhocRemoteString: 'test buddy',
                                            argString: '-delay 12 -monkey cheese -particle'
                                    ])
                            ]
                    )
            )

            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            fwkControl.demand.getPluginControlService { null }
            fwkControl.demand.projectNames { _ -> return []}
            fwkControl.demand.getProjectGlobals { [:] }
            sec.frameworkService = fwkControl.proxyInstance()
            def seServiceControl = new MockFor(ScheduledExecutionService, true)

            sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
                getByIDorUUID{id->se}
                getWorkflowStrategyPluginDescriptions{->[]}
                userAuthorizedForJob { user, schedexec, framework -> return true }
            }

			def oServiceControl = new MockFor(OrchestratorPluginService, true)
			oServiceControl.demand.listDescriptions{[]}
			sec.orchestratorPluginService = oServiceControl.proxyInstance()
			
            def pControl = new MockFor(NotificationService)
            pControl.demand.listNotificationPlugins() {->
                []
            }
            sec.notificationService = pControl.proxyInstance()
            sec.pluginService = mockWith(PluginService){
                listPlugins(){[]}
            }

            sec.executionLifecyclePluginService = mockWith(ExecutionLifecyclePluginService){
                listEnabledExecutionLifecyclePlugins{ctrl->[:]}
            }

            def params = [id: se.id.toString()]
            sec.params.putAll(params)
            sec.copy()
            assertNull sec.response.redirectedUrl
            def copied = sec.modelAndView.model.scheduledExecution
            assertNotNull(copied)
            assertEquals(se.jobName, copied.jobName)

    }

    public void testShow() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        assertNotNull se.id

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }
        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertNull(model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals(null,model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(null,model.nodes)
        assertEquals(null,model.selectedNodes)
        assertEquals(null,model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals(null,model.nodesSelectedByDefault)
        assertEquals([:],model.remoteOptionData)
    }
    public void testShowWithOptionValuesPlugin() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                ),
                options: [
                        new Option(name: 'optvals', optionValuesPluginType: 'test', required: true, enforced: false)
                ]
        )

        se.save()


        assertNotNull se.id

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }
        sec.optionValuesService = mockWith(OptionValuesService) {
            getOptions { project, plugin -> return [[name:"opt1",value:"o1"]] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }
        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertNull(model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals(null,model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(null,model.nodes)
        assertEquals(null,model.selectedNodes)
        assertEquals(null,model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(['optvals'],model.optionordering)
        assertEquals(null,model.nodesSelectedByDefault)
        assertEquals([optvals:[optionDependencies:null,optionDeps:null,localOption:true]],model.remoteOptionData)
        assertEquals(1,se.options[0].valuesFromPlugin.size())
        assertEquals('opt1',se.options[0].valuesFromPlugin[0].name)
        assertEquals('o1',se.options[0].valuesFromPlugin[0].value)
    }
    /**
     * model contains node dispatch target nodes information
     */
    public void testShowNodeDispatch() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: nodea,nodeb',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        assertNotNull se.id

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            filterNodeSet{NodesSelector selector, String project->
                null
            }
            filterAuthorizedNodes{final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                testNodeSet
            }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }

        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertEquals(null, model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals('name: nodea,nodeb',model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(true,model.nodesSelectedByDefault)
        assertEquals(testNodeSet.nodes,model.nodes)
        assertEquals([:],model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals([:],model.remoteOptionData)
    }
    /**
     * nodes not selected by default
     */
    public void testShowNodeDispatchSelectedFalse() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodesSelectedByDefault: false,
                filter:'name: nodea,nodeb',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        assertNotNull se.id

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            filterNodeSet{NodesSelector selector, String project->
                null
            }
            filterAuthorizedNodes{final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                testNodeSet
            }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }

        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertEquals([], model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals('name: nodea,nodeb',model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(false,model.nodesSelectedByDefault)
        assertEquals(testNodeSet.nodes,model.nodes)
        assertEquals([:],model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals([:],model.remoteOptionData)
    }
    /**
     * select nodes by default
     */
    public void testShowNodeDispatchSelectedTrue() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                nodesSelectedByDefault: true,
                filter:'name: nodea,nodeb',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        assertNotNull se.id

        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            filterNodeSet{NodesSelector selector, String project->
                null
            }
            filterAuthorizedNodes{final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                testNodeSet
            }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }

        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertEquals(null, model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals('name: nodea,nodeb',model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(true,model.nodesSelectedByDefault)
        assertEquals(testNodeSet.nodes,model.nodes)
        assertEquals([:],model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals([:],model.remoteOptionData)
    }
    /**
     * model contains node dispatch target nodes information
     */
    public void testShowNodeDispatchEmpty() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: nodea,nodeb',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        se.save()


        assertNotNull se.id

        NodeSetImpl testNodeSet = new NodeSetImpl()
//        testNodeSet.putNode(new NodeEntryImpl("nodea"))
//        testNodeSet.putNode(new NodeEntryImpl("nodeb"))

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            filterNodeSet{NodesSelector selector, String project->
                null
            }
            filterAuthorizedNodes{final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                testNodeSet
            }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }
        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }

        def params = [id: se.id.toString(),project:'project1']
        sec.params.putAll(params)
        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertNull(model.selectedNodes)
        assertEquals('fwnode',model.localNodeName)
        assertEquals('name: nodea,nodeb',model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(true,model.nodesetempty)
        assertEquals(null,model.nodes)
        assertEquals(null,model.selectedNodes)
        assertEquals(null,model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals(null,model.nodesSelectedByDefault)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals([:],model.remoteOptionData)
    }
    /**
     * model contains node dispatch target nodes information
     */
    public void testShowNodeDispatchRetryExecId() {
        def sec = controller

        def se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                groupPath: 'testgroup',
                doNodedispatch: true,
                filter:'name: nodea,nodeb',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        )

        assertNotNull se.save()
        def exec = new Execution(
                user: "testuser",
                project: "project1",
                loglevel: 'WARN',
                doNodedispatch: true,
                filter:'name: nodea',
                succeededNodeList:'nodea,fwnode',
                failedNodeList: 'nodeb',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save(),
                scheduledExecution: se
        )
        exec.validate()
        if(exec.hasErrors()){
            exec.errors.allErrors.each{
            }
        }
        assertFalse(exec.hasErrors())
        assertNotNull exec.save()


        NodeSetImpl testNodeSet = new NodeSetImpl()
        testNodeSet.putNode(new NodeEntryImpl("nodea"))
        testNodeSet.putNode(new NodeEntryImpl("nodeb"))
        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
            authorizeProjectJobAny { AuthContext authContext, ScheduledExecution job, Collection actions, String project ->
                return true
            }
            isClusterModeEnabled{-> false }
            filterNodeSet(1){NodesSelector selector, String project->
                selector.acceptNode(new NodeEntryImpl("nodeb"))?testNodeSet:testNodeSetB
            }
            filterAuthorizedNodes(1){final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                unfiltered
            }
            filterNodeSet(1){NodesSelector selector, String project->
                selector.acceptNode(new NodeEntryImpl("nodeb"))?testNodeSet:testNodeSetB
            }
            filterAuthorizedNodes(1){final String project, final Set<String> actions, final INodeSet unfiltered,
                                                                                                  AuthContext authContext->
                unfiltered
            }
            authResourceForProject{p->null}
            authorizeApplicationResourceAny(2..2){AuthContext authContext, Map resource, List actions->false}
            projectNames { _ -> return []}
            projects { return [] }
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAny { framework, resource, actions, project -> return true }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            nextExecutionTime { job -> null }
            getWorkflowStrategyPluginDescriptions{->[]}
            userAuthorizedForJob { user, schedexec, framework -> return true }
        }

        sec.notificationService = mockWith(NotificationService){
            listNotificationPlugins() {->
                []
            }
        }
        sec.orchestratorPluginService=mockWith(OrchestratorPluginService){
            getOrchestratorPlugins(){->null}
        }
        sec.pluginService = mockWith(PluginService) {
            listPlugins(){[]}
        }

        sec.featureService=mockWith(FeatureService){
            featurePresent(){name->false}
        }
        def params = [id: se.id.toString(),project:'project1',retryExecId:exec.id.toString()]
        sec.params.putAll(params)

        def model = sec.show()
        assertNull sec.response.redirectedUrl
        assertNotNull model
        assertNotNull(model.scheduledExecution)
        assertEquals('fwnode',model.localNodeName)
        assertEquals('name: nodea,nodeb',model.nodefilter)
        assertEquals(null,model.nodesetvariables)
        assertEquals(null,model.failedNodes)
        assertEquals(null,model.nodesetempty)
        assertEquals(testNodeSet.nodes,model.nodes)
        assertEquals(['nodea'],model.selectedNodes)
        assertEquals([:],model.grouptags)
        assertEquals(null,model.selectedoptsmap)
        assertEquals(true,model.nodesSelectedByDefault)
        assertEquals([:],model.dependentoptions)
        assertEquals([:],model.optiondependencies)
        assertEquals(null,model.optionordering)
        assertEquals([:],model.remoteOptionData)
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
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:expectedJob,associations: [:])

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.parseUploadedFile { input,format ->
            [jobset:[importedJob]]
        }
        mock2.demand.loadImportedJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            assert jobset==[importedJob]
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        mock2.demand.issueJobChangeEvents {event->}
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        mock2.demand.parseUploadedFile { input,format ->
            [jobset:[expectedJob]]
        }
        mock2.demand.loadJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            assert jobset==[expectedJob]
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:expectedJob,associations: [:])

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.parseUploadedFile { input,format ->
            [jobset: [importedJob]]
        }
        mock2.demand.loadImportedJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            assertEquals('BProject', jobset[0].job.project)
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        mock2.demand.issueJobChangeEvents {event->}
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:expectedJob,associations: [:])

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [importedJob]]
        }
        mock2.demand.loadImportedJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        mock2.demand.issueJobChangeEvents {event->}
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        assertNotNull opt.optionValues
        assertEquals 3, opt.optionValues.size()
        assertTrue opt.optionValues.contains("a")
        assertTrue opt.optionValues.contains("b")
        assertTrue opt.optionValues.contains("c")
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
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:expectedJob,associations: [:])

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [importedJob]]
        }
        mock2.demand.loadImportedJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        mock2.demand.issueJobChangeEvents {event->}
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()
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
        assertNotNull opt.optionValues
        assertEquals 3, opt.optionValues.size()
        assertTrue opt.optionValues.contains("a")
        assertTrue opt.optionValues.contains("b")
        assertTrue opt.optionValues.contains("c")
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
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:expectedJob,associations: [:])

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getRundeckFramework {-> return null }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.parseUploadedFile { input, format ->
            [jobset: [importedJob]]
        }
        mock2.demand.loadImportedJobs { jobset, dupeOption, uuidOption, changeinfo, authctx, validateJobref ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        mock2.demand.issueJobChangeEvents {event->}
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def fwkControl = new MockFor(FrameworkService, true)

        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()


        setupFormTokens(sec)


        sec.uploadPost()
        def result = sec.modelAndView.model
        assertEquals "No file was uploaded.", sec.request.getAttribute('message')
    }

    public void testCreateExcludeInactivePlugins() {
        def sec = new ScheduledExecutionController()
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2')
        se.save()
        assertNotNull se.id
        def plgControlSrv = new MockFor(PluginControlService)
        plgControlSrv.demand.isDisabledPlugin {name, type-> name=='test' }
        plgControlSrv.demand.isDisabledPlugin {name, type-> name=='test' }
        //try to do update of the ScheduledExecution
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getAuthContextForSubjectAndProject { subject,proj -> testUserAndRolesContext() }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getPluginControlService{proj->plgControlSrv.proxyInstance()}
        fwkControl.demand.getNodeStepPluginDescriptions { [[name:'test'],[name:'test2']] }
        fwkControl.demand.getStepPluginDescriptions { [] }
        fwkControl.demand.getProjectGlobals { [:] }
        fwkControl.demand.projectNames { _ -> return []}
        sec.frameworkService = fwkControl.proxyInstance()
        def seServiceControl = new MockFor(ScheduledExecutionService, true)
        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getWorkflowStrategyPluginDescriptions{->[]}
            getTimeZones{->[]}
        }
        def oServiceControl = new MockFor(OrchestratorPluginService, true)
        oServiceControl.demand.listDescriptions{[]}
        sec.orchestratorPluginService = oServiceControl.proxyInstance()
        def pControl = new MockFor(NotificationService)
        pControl.demand.listNotificationPlugins() {->
            []
        }
        sec.notificationService = pControl.proxyInstance()
        sec.pluginService = mockWith(PluginService){
            listPlugins(){[]}
        }

        sec.executionLifecyclePluginService = mockWith(ExecutionLifecyclePluginService){
            listEnabledExecutionLifecyclePlugins{ctrl->[:]}
        }
        sec.rundeckJobDefinitionManager=mockWith(RundeckJobDefinitionManager){
            getJobDefinitionComponents{->[:]}
            getJobDefinitionComponentValues{job->[:]}
        }
        def params = [id: se.id.toString()]
        sec.params.putAll(params)
        def ret = sec.create()
        assertNotNull(ret)
        assertNotNull(ret.nodeStepDescriptions)
        assertEquals(1,ret.nodeStepDescriptions.size())
    }
}
