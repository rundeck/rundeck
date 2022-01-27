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

import com.dtolabs.rundeck.app.api.ApiBulkJobDeleteRequest
import com.dtolabs.rundeck.app.api.ApiRunAdhocRequest
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.*
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.mock.interceptor.MockFor
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.RundeckAuthorizedServicesProvider
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import org.rundeck.app.spi.ServicesProvider
import org.rundeck.core.auth.AuthConstants
import org.springframework.mock.web.MockMultipartFile
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.*
import rundeck.services.feature.FeatureService
import rundeck.services.optionvalues.OptionValuesService

import javax.security.auth.Subject
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.junit.Assert.*

/*
* ScheduledExecutionControllerTests.java
*
* User: greg
* Created: Jun 11, 2008 5:12:47 PM
* $Id$
*/

class ScheduledExecutionController2Spec extends HibernateSpec implements ControllerUnitTest<ScheduledExecutionController>{

    List<Class> getDomainClasses() { [ScheduledExecution,Option,Workflow,CommandExec,Execution,JobExec, ReferencedExecution, ScheduledExecutionStats] }
    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, Closure clos) {
        def mock = new MockFor(clazz,false)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }
    def setup(){

        mockCodec(URIComponentCodec)
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
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
        when:
        def sec = controller
            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.getRundeckFramework {-> return null }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()
//            1*authorizeProjectResourceAll (*_ )>>true
//            1*authorizeProjectJobAll (*_ )>>true
            0 * _(*_)
        }
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
            request.setAttribute("subject", subject)

            setupFormTokens(sec)

        request.method='POST'
            sec.save()

            then:
            assert null!=sec.flash.savedJob
            assert null!=sec.flash.savedJobMessage
            assertEquals("/job/show/1", response.redirectedUrl)
    }
    public void testSave_invalidToken() {
        when:
        def sec = controller
            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.getRundeckFramework {-> return null }
                        fwkControl.demand.projects {return []}
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
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
            request.setAttribute("subject", subject)

            //don't include request token

        request.method='POST'
            sec.save()

            then:
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
        when:
        def se = controller
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

        then:
        assertEquals(2, params.size())
        assertEquals([], params['_sessionEditOPTSObject'])
    }
    public void testtransferSessionEditStateWF() {
        when:
        def se = controller
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
        then:
        assertEquals(2, params.size())
        assertEquals([], params['_sessionEditWFObject'])
    }
    public void testUpdateSessionOptsEmptyList() {
        given:
        def sec = controller
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
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
            fwkControl.demand.projects {return []}
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.getRundeckFramework {-> return null }
            sec.frameworkService = fwkControl.proxyInstance()
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

//                1*authorizeProjectResourceAll (*_ )>>true
//                1*authorizeProjectJobAll (*_ )>>true
                0 * _(*_)
            }
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
            request.setAttribute("subject", subject)
            setupFormTokens(sec)
            request.method='POST'
        when:
            sec.update()

            then:
            assert null!=sec.flash.savedJob
            assert null!=sec.flash.savedJobMessage
            assertEquals("/job/show/"+se.id, response.redirectedUrl)
    }
    def testUpdate_invalidToken() {
        when:
        def sec = controller
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
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }
                    fwkControl.demand.projects {return []}
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
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
            request.setAttribute("subject", subject)
            //don't include token
        request.method='POST'
            sec.update()


        then:
        assertNull sec.flash.savedJob
        assertNull sec.flash.savedJobMessage
        assertEquals('/common/error', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('errorCode'))
    }
    def testUpdateSessionWFEditEmptyList() {
        when:
            def sec = controller
            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    options: [new Option(name: 'blah',enforced:false)],
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            assertNotNull(se.save())
//
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }



            fwkControl.demand.projects {return []}
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
            request.setAttribute("subject", subject)

        setupFormTokens(sec)
        request.method='POST'
            sec.update()

        then:
            assert null!=sec.flash.savedJob
            assert null!=sec.flash.savedJobMessage
        assertEquals("/job/show/"+se.id, response.redirectedUrl)
    }


    def testRunAdhocBasic() {
        given:
            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()

            def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            assert null!=exec.save()

            assert null!=se.id

            controller.frameworkService = Mock(FrameworkService) {
            }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

            }
            controller.scheduledExecutionService = Mock(ScheduledExecutionService) {
                1 * _dovalidateAdhoc(_, _) >> {
                    assertEquals('Temporary_Job', it[0].jobName)
                    assertEquals('adhoc', it[0].groupPath)
                    [failed: false, scheduledExecution: se]
                }
                1 * userAuthorizedForAdhoc(_, _, _) >> true
                1 * isProjectExecutionEnabled(_) >> true
                1 * scheduleTempJob(_, _) >> [id: exec.id, execution: exec, success: true]

            }



            controller.executionService = Mock(ExecutionService) {
                1 * getExecutionsAreActive() >> true
                1 * createExecutionAndPrep(_, _) >> exec
            }


        controller.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        controller.request.setAttribute("subject", subject)

        when:
        def model= controller.runAdhoc(new ApiRunAdhocRequest(exec:'a remote string',nodeKeepgoing: true,nodeThreadcount: 1,project:'testProject'))

        then:
        assertNull model.failed
        assertTrue model.success
        assert null!=model.execution
        assert null!=exec.id
        assertEquals exec, model.execution
        assertEquals('notequal',exec.id.toString(), model.id.toString())
    }

    public void testRunAdhocBasic_execModePassive() {
        when:
        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save()
        )
        se.save()
//
        def executionModeActive = false
        assert null!=se.id

        //try to do update of the ScheduledExecution
        def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

//                1*authorizeProjectResourceAll (*_ )>>true
//                1*authorizeProjectJobAll (*_ )>>true
                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework { -> return null }
        fwkControl.demand.projects { return [] }


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
        assert null!=exec.save()
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

        then:
        assertTrue model.failed
        assertFalse model.success
        assertNull model.execution
        assertEquals 'disabled', model.error
    }
    /**
     * User input provides old node filters, runAdhoc should supply new filter string to scheduleTempJob
     */
    public void testRunAdhocOldNodeFilters() {
        when:
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
        assert null!=se.id

        //try to do update of the ScheduledExecution
        def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

//                1*authorizeProjectResourceAll (*_ )>>true
//                1*authorizeProjectJobAll (*_ )>>true
                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework {-> return null }
        fwkControl.demand.projects {return []}

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
        assert null!=exec.save()
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

        then:
        assertNull model.failed
        assert null!=model.execution
        assert null!=exec.id
        assertEquals exec, model.execution
        assertEquals('notequal',exec.id.toString(), model.id.toString())
    }

    public void testRunAdhocFailed() {
        when:
        def sec = controller
        if (true) {//test basic copy action

            def se = new ScheduledExecution(
                    jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
            )
            se.save()
//
            assert null!=se.id

            //try to do update of the ScheduledExecution
            def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
            fwkControl.demand.getRundeckFramework {-> return null }
            fwkControl.demand.projects {return []}


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
            assert null!=exec.save()
            eServiceControl.demand.createExecutionAndPrep { params, user ->
                return exec
            }
            sec.executionService = eServiceControl.proxyInstance()


            sec.metaClass.message = {params -> params?.code ?: 'messageCodeMissing'}


            final subject = new Subject()
            subject.principals << new Username('test')
            subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
            request.setAttribute("subject", subject)

            def model=sec.runAdhoc(new ApiRunAdhocRequest(exec:'a remote string',project:'testProject',nodeThreadcount: 1,nodeKeepgoing: true))

            assertTrue model.failed
            assert null!=model.scheduledExecution
            assertEquals 'Job configuration was incorrect.', model.message
        }
        then:
        1 == 1
    }
    private ScheduledExecution createTestJob(){
        def se=new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
        assert null!=se.id
        return se
    }
    public void testApiJobExecutions_basic() {
        when:
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
        then:
        assertEquals(200,response.status)
    }
    public void testApiJobExecutions_single() {
        when:
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
        assert null!=exec.save()

        def testStatus=null
        def testOffset=0
        def testMax=-1
        def testResultSize=1


        assertApiJobExecutions(se, sec, testStatus, testOffset, testMax, testResultSize,[exec])

        def params = [id: se.id.toString()]
        sec.params.putAll(params)
        def result=sec.apiJobExecutions()
        then:
        assertEquals(200,response.status)
    }
    public void testApiJobExecutions_statusParam() {
        when:
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
        assert null!=exec.save()


        def testStatus='succeeded'
        def testOffset=0
        def testMax=-1
        def testResultSize=1


        assertApiJobExecutions(se, sec, testStatus, testOffset, testMax, testResultSize,[exec])

        def params = [id: se.id.toString(),status:'succeeded']
        sec.params.putAll(params)
        def result=sec.apiJobExecutions()
        then:
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
        sec.frameworkService = Mock(FrameworkService) {

        }
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){

            1 * authorizeProjectJobAny(_,se,['read','view'],se.project)>>true
            1 * authorizeProjectResource(_,[type: 'resource', kind: 'event'],'read',se.project)>>true
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
        when:
        def sec = controller

        def se = new ScheduledExecution(
                jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        )
        se.save()
        assert null!=se.id

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                1 * authorizeProjectJobAll(_,se,['run'],'testProject')>>true
                0 * _(*_)
            }
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
        assert null!=exec.save()
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
        request.setAttribute("subject", subject)
        ExecutionController.metaClass.renderApiExecutionListResultXML={List execs->
            assert 1==execs.size()
        }
        session.user='anonymous'
        sec.apiJobRun()

        then:
        1 == 1
    }
    public void testApiRunJob_AsUser() {
        expect:
        assertRunJobAsUser([jobName: 'monkey1', project: 'testProject', description: 'blah',],
                null,
                'differentUser')
    }
    public void testApiRunJob_ScheduledJob_AsUser() {
        expect:
        assertRunJobAsUser([scheduled: true, user: 'bob', jobName: 'monkey1', project: 'testProject', description: 'blah',],
                'bob',
                'differentUser')
    }

    private void assertRunJobAsUser(Map job, String expectJobUser, String userName) {
        def sec = controller
        def se = new ScheduledExecution(job)
        se.workflow= new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]).save()
        se.save()
        assert null!=se.id
        assertEquals(expectJobUser,se.user)

        def x = 0
        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                1*authorizeProjectJobAll (_,_,['run'],_ )>>true
                1*authorizeProjectJobAll (_,_,['runAs'],_ )>>true
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
        assert null!=exec.save()
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
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 5)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML={List execs->
            assert 1==execs.size()
            assert execs.contains(exec)
        }

        sec.apiJobRun()
    }

    public void testApiRunCommandNoProject() {
        when:
        def sec = controller

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
        assert null!=exec.save()
        eServiceControl.demand.createExecutionAndPrep { params, user ->
            assert 'testuser' == user
            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 5)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded = false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp, version ->
            version=14
            true
        }

        svcMock.demand.renderErrorFormat { response, Map error ->
            assert error.status == 400
            assert error.code == 'api.error.invalid.request'
        }

        sec.apiService = svcMock.proxyInstance()

        def result = sec.apiRunCommandv14(new ApiRunAdhocRequest(exec: 'blah'))

        then:
        assert !succeeded
        assert null == view
        assertNull(response.redirectedUrl)
        assert null==sec.flash.error
        assert !sec.chainModel
    }

    public void testApiBulkJobDeleteRequest_validation() {
        when:
            def cmd = new ApiBulkJobDeleteRequest()
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
        then:
            assertTrue(valid)
    }
    public void testApiBulkJobDeleteRequest_validation_idlist() {
        when:
            def cmd = new ApiBulkJobDeleteRequest()
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

        then:
            assertTrue(valid)
    }
    public void testApiBulkJobDeleteRequest_validation_failure_blank_ids() {
        when:
        def cmd = new ApiBulkJobDeleteRequest()
        cmd.ids = ['']
        then:
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('ids'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_ids() {
        when:
        def cmd = new ApiBulkJobDeleteRequest()
        cmd.ids = ['asdf/monkey']
        then:
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('ids'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_id() {
        when:
        def cmd = new ApiBulkJobDeleteRequest()
        cmd.id = 'asdf/monkey'
        then:
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('id'))
    }
    public void testApiBulkJobDeleteRequest_validation_failure_invalid_idlist() {
        when:
        def cmd = new ApiBulkJobDeleteRequest()
        cmd.idlist = '068c29db-6e6f-4f6b-b3bf-7bae31238814,asdf/monkey'
        then:
        assertFalse(cmd.validate())
        assertTrue(cmd.errors.hasFieldErrors('idlist'))
    }

    public void testApiRunScript_RequiresPOST() {
        when:
        def sec = controller

        request.setAttribute("api_version", 14)
        def result=sec.apiRunScriptv14(new ApiRunAdhocRequest(script:'blah',project: 'test'))
        then:
        assert 405==response.status
    }
    public void testApiRunScript_v14_RequiresPOST() {
        when:
        def sec = controller

        request.setAttribute("api_version", 14)
        def result=sec.apiRunScriptv14(new ApiRunAdhocRequest(script:'blah',project: 'test'))
        then:
        assert 405==response.status
    }

    public void testApiRunScriptUrl_v14() {
        when:
        def sec = controller


        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 14)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def apiverslist=[4,14]
        sec.apiService = mockWith(ApiService) {
            requireApi(1..1){ req, resp, apivers ->
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
        then:
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }

    private UserAndRolesAuthContext testUserAndRolesContext(String user='test',String roleset='test') {
        [getUsername: { user }, getRoles: { roleset.split(',') as Set }] as UserAndRolesAuthContext
    }
    private Services servicesWith(UserAndRolesAuthContext authContext) {
        new RundeckAuthorizedServicesProvider.AuthedServices(authContext )
    }
    public void testApiRunCommand_v14() {
        when:
        def sec = controller

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 14)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        sec.apiService = mockWith(ApiService) {
            requireApi { req, resp, apivers ->
                assert apivers == 14
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
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        then:
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_XML() {
        when:
        def sec = controller

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 5)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp, version ->
            version=14
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
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        then:
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_executionModePassive() {
        when:
        def sec = controller
        def executionModeActive=false
        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->executionModeActive}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 5)
//        request.api_version = 5
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp, version ->
            version=14
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
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))
        then:
        assert !succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_JSON_apiversionInvalid() {
        when:
        def sec = controller

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 5)
//        request.api_version = 5
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp, version ->
            version=14
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
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))

        then:
        assert !succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }
    public void testApiRunCommand_JSON_apiversionValid() {
        when:
        def sec = controller

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                0 * _(*_)
            }
        fwkControl.demand.getRundeckFramework(1..2) {-> return null }

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
        assert null!=exec.save()
        eServiceControl.demand.getExecutionsAreActive{->true}
        eServiceControl.demand.createExecutionAndPrep { params, user ->

            exec
        }
        sec.executionService = eServiceControl.proxyInstance()


        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect { new Group(it) })
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 14)
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded=false
        def svcMock = new MockFor(ApiService, true)
        svcMock.demand.requireApi { req, resp, version ->
            version=14
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
        def result=sec.apiRunCommandv14(new ApiRunAdhocRequest(exec:'blah',project: 'test'))

        then:
        assert succeeded
        assert null==view
        assertNull(response.redirectedUrl)
        assert !model
    }

    public void testApiRunCommandAsUser() {
        when:
        def sec = controller

        //try to do api job run
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.existsFrameworkProject(1..1) { project ->
            true
        }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                1 * authorizeProjectResource(_,[type:'adhoc'],'runAs','test')>>true
                0 * _(*_)
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
        assert null!=exec.save()
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
        request.setAttribute("subject", subject)
        request.setAttribute("api_version", 11)
//        request.api_version = 5
//        registerMetaClass(ExecutionController)
        ExecutionController.metaClass.renderApiExecutionListResultXML = { List execs ->
            assert 1 == execs.size()
            assert execs.contains(exec)
        }
        sec.metaClass.message = { params2 -> params2?.code ?: 'messageCodeMissing' }
        def succeeded = false

        def svcMock = new MockFor(ApiService, true)
        def requireFailed = true
        svcMock.demand.requireApi { req, resp, version ->
            version=14
            true
        }
        svcMock.demand.requireExists { response, exists, args ->
            assertEquals(['project', 'test'], args)
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
        def result = sec.apiRunCommandv14()

        then:
        assert succeeded
        response.status==200
        response.format=='xml'
        assertNull(response.redirectedUrl)
        assert !model
    }


    public void testShow() {
        when:
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


        assert null!=se.id

        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService){

            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            isClusterModeEnabled{-> false }
            projectNames { _ -> return []}
            projects { return [] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getRundeckFramework {-> return [getFrameworkNodeName:{->'fwnode'}] }
            getNodeStepPluginDescriptions { [] }
            getStepPluginDescriptions { [] }
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()

                1 * authResourceForProject('project1')
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_SCM_EXPORT])>> false
                1 * authorizeApplicationResourceAny(_,_,[AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_IMPORT, AuthConstants.ACTION_SCM_IMPORT])>>false
                1 * authorizeProjectJobAny(_,se,['read', 'view'],'project1')>>true
                0 * _(*_)
            }

        sec.scheduledExecutionService = mockWith(ScheduledExecutionService){
            getByIDorUUID { id -> return se }
            isScheduled(1..1){ job -> return se.scheduled }
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
        then:
        assertNull sec.response.redirectedUrl
        assert null!=model
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

    /**
     * test application/x-www-form-urlencoded instead of multipart
     */
    public void testUploadFormContentShouldCreate() {
        when:
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


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        mock2.demand.isScheduled {job-> job.scheduled}
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
        request.setAttribute("subject", subject)
        sec.params.project="project1"

        setupFormTokens(sec)

        sec.uploadPost()
        def result=sec.modelAndView.model
        assertNull sec.response.redirectedUrl
        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNull "Result had an error: ${sec.flash.message}", sec.flash.message
        assert null!=result
        assertTrue result.didupload
        assert null!=result.jobs
        assert null!=result.errjobs
        assert null!=result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]

        then:
        assertEquals "test1", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
    }
    /**
     * test application/x-www-form-urlencoded instead of multipart
     */
    public void testUpload_invalidToken() {
        when:
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

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        request.setAttribute("subject", subject)
        sec.params.project="project1"

        //don't set up form tokens

        sec.uploadPost()

        then:
        assertNull sec.flash.savedJob
        assertNull sec.flash.savedJobMessage
        assertEquals('/scheduledExecution/upload', view)
        assertEquals("request.error.invalidtoken.message", request.getAttribute('warn'))
    }


    public void testUploadProjectParameter() {
        when:
        def sec = controller


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


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        mock2.demand.isScheduled { job -> job.scheduled }
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
        request.setAttribute("subject", subject)
        request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        sec.params.project = "BProject"

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]

        then:
        assert null!=result
        assertTrue result.didupload
        assert null!=result.jobs
        assert null!=result.errjobs
        assert null!=result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
    }

    public void testUploadOptions() {
        when:
        def sec = controller


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


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        mock2.demand.isScheduled { job -> job.scheduled }
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
        request.setAttribute("subject", subject)
        request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assert null!=result
        assertTrue result.didupload
        assert null!=result.jobs
        assert null!=result.errjobs
        assert null!=result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assert null!=job.options
        assertEquals 1, job.options.size()
        Option opt = job.options.iterator().next()

        then:
        assertEquals "testopt", opt.name
        assertEquals "`ls -t1 /* | head -n1`", opt.defaultValue
        assert null!=opt.optionValues
        assertEquals 3, opt.optionValues.size()
        assertTrue opt.optionValues.contains("a")
        assertTrue opt.optionValues.contains("b")
        assertTrue opt.optionValues.contains("c")
    }

    public void testUploadOptions2() {
        when:
        def sec = controller


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


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        mock2.demand.isScheduled { job -> job.scheduled }
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
        request.setAttribute("subject", subject)

        request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/yaml', xml as byte[]))
        sec.params.fileformat = "yaml"

        setupFormTokens(sec)

        sec.uploadPost()
        def result = sec.modelAndView.model
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assert null!=result
        assertTrue result.didupload
        assert null!=result.jobs
        assert null!=result.errjobs
        assert null!=result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assert null!=job.options
        assertEquals 1, job.options.size()
        Option opt = job.options.iterator().next()

        then:
        assertEquals "testopt", opt.name
        assertEquals "`ls -t1 /* | head -n1`", opt.defaultValue
        assert null!=opt.optionValues
        assertEquals 3, opt.optionValues.size()
        assertTrue opt.optionValues.contains("a")
        assertTrue opt.optionValues.contains("b")
        assertTrue opt.optionValues.contains("c")
    }

    public void testUploadShouldCreate() {

        when:
        def sec = controller

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


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }

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
        mock2.demand.isScheduled { job -> job.scheduled }
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
        request.setAttribute("subject", subject)
        sec.params.project = "project1"

        request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))

        setupFormTokens(sec)


        sec.uploadPost()
        def result = sec.modelAndView.model
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assert null!=result
        assertTrue result.didupload
        assert null!=result.jobs
        assert null!=result.errjobs
        assert null!=result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]

        then:
        assertEquals "test1", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
    }

    /**
     * test normal get request has no error
     */
    public void testUploadGetRequest() {

        when:
        def sec = controller

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getRundeckFramework {-> return null }

            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                getAuthContextForSubjectAndProject(_,_)>> testUserAndRolesContext()


                0 * _(*_)
            }
        fwkControl.demand.existsFrameworkProject { project -> return true }
        sec.frameworkService = fwkControl.proxyInstance()
        //mock the scheduledExecutionService
        def mock2 = new MockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes { joblist -> return [] }
        sec.scheduledExecutionService = mock2.proxyInstance()

        request.method="GET"
        def result = sec.upload()
        then:
        assertNull(sec.flash.message)
        assertNull result

    }
    /**
     * test missing content
     */
    def testUploadMissingContent() {
        given:
        def sec = controller

        sec.frameworkService = Mock(FrameworkService){
            0 * _(*_)
        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_,_)
                0 * _(*_)
            }
        request.method="POST"

        setupFormTokens(sec)

        when:

        sec.uploadPost()

        then:
        request.message=='No file was uploaded.'
    }
    /**
     * test missing File content
     */
    def testUploadMissingFile() {
        when:
        def sec = controller

        sec.frameworkService = Mock(FrameworkService){

        }
            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForSubjectAndProject(_,_)
                0 * _(*_)
            }

        setupFormTokens(sec)
        request.addFile('wrongname','asdf'.bytes)

        sec.uploadPost()
        then:
        request.message=="No file was uploaded."
    }

}
