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

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import groovy.mock.interceptor.MockFor
import groovy.time.TimeCategory
import org.hibernate.JDBCException
import org.quartz.JobExecutionContext
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.*
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.WorkflowStateFileLoader

import java.sql.Time

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class ExecutionController2Spec extends HibernateSpec implements ControllerUnitTest<ExecutionController>  {

    List<Class> getDomainClasses() { [Workflow,ScheduledExecution,Execution,CommandExec]}

    void testDownloadOutputNotFound() {
        given:
        def ec = controller
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath, project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def logControl = Mock(LoggingService)
        logControl.getLogReader(*_)>>{ Execution e->
            [state: ExecutionFileState.NOT_FOUND]
        }
        ec.loggingService = logControl
        def fwkControl = Mock(FrameworkService)
        fwkControl.getFrameworkPropertyResolver(*_)>>{project ->
            assert project=='test1'
            null
        }
        ec.frameworkService = fwkControl
        controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
            1 * getAuthContextForSubjectAndProject(_,_)
        }
        controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            1 * authorizeProjectExecutionAny(*_)>>true
        }

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution={ Execution data->
            [:]
        }
        when:
        def result = ec.downloadOutput()
        then:
        assertEquals(404,ec.response.status)
    }

    void testDownloadOutputNotAvailable() {

        when:
        def ec = controller
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath, project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def logControl = Mock(LoggingService)
        logControl.getLogReader(*_)>>{ Execution e ->
            [state: ExecutionFileState.NOT_FOUND]
        }
        ec.loggingService = logControl
        def fwkControl = Mock(FrameworkService)
        fwkControl.getFrameworkPropertyResolver(*_)>>{project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl
            controller.rundeckAuthContextProvider=Mock(AuthContextProvider){
                1 * getAuthContextForSubjectAndProject(_,_)
            }
            controller.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
                1 * authorizeProjectExecutionAny(*_)>>true
            }

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }


        def result = ec.downloadOutput()
        then:
        assertEquals(404,ec.response.status)
    }

    void testAjaxExecState_missing(){
        when:
        controller.params.id=123
        controller.ajaxExecState()
        then:
        assertEquals(404,response.status)
        assertEquals("Execution not found for id: 123",response.json.error)
    }
    void testAjaxExecState_unauthorized(){
        when:
        Execution e1 = new Execution( project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def jobexec = Mock(JobExecutionContext){}
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            findExecutingQuartzJob(_)>> jobexec
        }
        controller.params.id=e1.id
        controller.frameworkService=Mock(FrameworkService){
        }
            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAny(*_) >> false
            }
        controller.ajaxExecState()
        then:
        assertEquals(403,response.status)
        assertEquals("Unauthorized: View Execution ${e1.id}".toString(),response.json.error)
    }
    void testDownloadOutput(){
        when:
        def ec = controller
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath,project:'test1',user:'bob',dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect {it.toString()}.join(",")
        assert e1.save()

        def logControl = Mock(LoggingService)
        logControl.getLogReader(*_)>>{ Execution e ->
            [state: ExecutionFileState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = logControl
        def fwkControl = Mock(FrameworkService)
        fwkControl.getFrameworkPropertyResolver(*_)>>{ project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }
            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAny(*_) >> true
            }
        ec.params.id = e1.id.toString()


        def result=ec.downloadOutput()
        then:
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        assertEquals("blah blah test monkey\n" + "Execution failed on the following 1 nodes: [centos5]\n", ec.response.contentAsString)
    }

    void testDownloadOutputFormatted(){
        given:
        def ec = controller
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1))
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||blah blah test monkey^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()

        Execution e1 = new Execution(outputfilepath: tf1.absolutePath,project:'test1',user:'bob',dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect {it.toString()}.join(",")
        assert e1.save()
        def logControl = Mock(LoggingService)
        logControl.getLogReader(*_)>>{ Execution e ->
            new ExecutionLogReader([state: ExecutionFileState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())])
        }
        ec.loggingService = logControl
        def fwkControl = Mock(FrameworkService)
        controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
            1 * getAuthContextForSubjectAndProject(_, _)
        }
        controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
            1 * authorizeProjectExecutionAny(*_) >> true
        }
        fwkControl.getFrameworkPropertyResolver(*_)>>{ project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }


        ec.params.id = e1.id.toString()
        ec.params.formatted = 'true'
        ec.params.timeZone = 'GMT'

        when:
        def result=ec.downloadOutput()
        then:
        assertNotNull(response.getHeader('Content-Disposition'))
        def strings = response.text.split("[\r\n]+") as List

        assertEquals(["03:21:50 [admin@centos5 _][NORMAL] blah blah test monkey","03:21:51 [null@null _][ERROR] Execution failed on the following 1 nodes: [centos5]"], strings)
    }

    public void testApiExecutionsQueryRequireVersion() {
        when:
        def controller = controller
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def svcMock = Mock(ApiService)
        svcMock.requireVersion(*_)>>{ request, response, min ->
            response.status=400
            false
        }
        controller.apiService = svcMock
        controller.apiExecutionsQuery(null)
        then:
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_lessthan() {
        when:
        def controller = controller
        controller.request.api_version = 4

        def svcMock = Mock(ApiService)
        svcMock.requireVersion(*_)>>{ request,response, int min ->
            assertEquals(5,min)
            response.status=400
            return false
        }
        controller.apiService = svcMock
        controller.apiExecutionsQuery(null)
        then:
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_ok() {
        given:
        def controller = controller
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def fwkControl = Mock(FrameworkService)
        fwkControl.existsFrameworkProject( _ )>>{ return true }

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * filterAuthorizedProjectExecutionsAll(*_) >> []
            }
        controller.frameworkService=fwkControl
        def execControl = Mock(ExecutionService)
        execControl.queryExecutions(*_)>>{ ExecutionQuery query, int offset, int max ->
            return [results:[],total:0]
        }
        execControl.respondExecutionsXml(*_)>>{ request, response, List<Execution> execs, paging ->
            return true
        }
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        def svcMock = Mock(ApiService)
        svcMock.requireVersion(*_)>>{ request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.requireExists(*_)>>{  response, test,args ->
            assertEquals(['Project','Test'], args)
            return true
        }
        controller.apiService = svcMock
        when:
        controller.apiExecutionsQuery(new ExecutionQuery())

        then:
        assert 200 == controller.response.status
    }

    public List createTestExecs() {

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

        // we'll use a fixed start date to easily calc execution durations.
        def startDate = new Date()

        Execution e1 = new Execution(
                scheduledExecution: se1,
                project: "Test",
                status: "true",
                dateStarted: startDate,
                dateCompleted: new Date(startDate.getTime() + (1000 * 60 * 4)), // 4 min duration
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()

        Execution e2 = new Execution(
                scheduledExecution: se2,
                project: "Test",
                status: "true",
                dateStarted: startDate,
                dateCompleted: new Date(startDate.getTime() + (1000 * 60 * 9)), // 9 min duration
                user: 'bob',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test2 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e2.save()
        Execution e3 = new Execution(
                scheduledExecution: se3,
                project: "Test",
                status: "true",
                dateStarted: startDate,
                dateCompleted: new Date(startDate.getTime() + (1000 * 60 * 2)), // 2 min duration
                user: 'chuck',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test3 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e3.save()

        [e1, e2, e3]
    }
    /**
     * Test no results
     */
    public void testApiExecutionsQueryProjectParameter() {
        given:
        def controller = controller

        def execs = createTestExecs()

        def fwkControl = Mock(FrameworkService)
        fwkControl.existsFrameworkProject( _ )>>{ return true }

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * filterAuthorizedProjectExecutionsAll(_,[],_) >> []
            }
        controller.frameworkService = fwkControl
        controller.request.api_version = 5
        controller.params.project = "WRONG"
        def execControl = Mock(ExecutionService)
        execControl.queryExecutions(*_)>>{ ExecutionQuery query, int offset, int max ->
            assert null!=query
            assert "WRONG"==query.projFilter
            return [result: [], total: 0]
        }
        execControl.respondExecutionsXml(*_)>>{ request, response, List<Execution> execsx, paging ->
            return true
        }
        controller.executionService = execControl

        def svcMock = Mock(ApiService)
        svcMock.requireVersion(*_)>>{ request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.requireExists(*_)>>{  response, test,args ->
            assertEquals(['Project','WRONG'], args)
            return true
        }
        svcMock.renderSuccessXml(*_)>>{ request, response ->
            return true
        }
        controller.apiService = svcMock
        when:
        controller.apiExecutionsQuery(new ExecutionQuery())

        then:
        assert 200 == controller.response.status
    }

    /**
     * Test abort authorized
     */
    public void testApiExecutionAbortAuthorized() {
        when:
        def controller = controller
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(*_)>>{ se, e, user, framework, killas, force ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAll(_,_,_) >> true
            }
        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> true }
        svcMock.renderSuccessXml(*_)>>{ request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock
        controller.apiExecutionAbort()

        then:
        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test abort unauthorized
     */
    public void testApiExecutionAbortUnauthorized() {
        when:
        def controller = controller
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(*_)>>{ se, e, user, framework, killas ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', reason: null]
        }
            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAll(_,_,_) >> false
            }
        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock
        controller.apiExecutionAbort()

        then:
        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserUnauthorized() {
        when:
        def controller = controller
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(*_)>>{ se, e, user, framework, killas ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', reason: null]
        }


            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAll(_,_,_) >> false
            }
        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock
        controller.apiExecutionAbort()

        then:
        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserAuthorized() {
        when:
        def controller = controller
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(*_)>>{ se, e, user, framework, killas,force ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAll(_,_,_) >> true
            }

        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> true }
        svcMock.requireVersion(*_)>>{ request,response,int min ->
            assertEquals(5,min)
            return true
        }
        svcMock.renderSuccessXml(*_)>>{ request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock
        controller.apiExecutionAbort()

        then:
        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test abort as user, abortAs denied
     */
    public void testApiExecutionAbortAsUserNotAuthorized() {
        given:
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(*_)>>{ se, e, user, framework, killas, force ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }

        controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
            1 * getAuthContextForSubjectAndProject(_, _)
        }
        controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
            1 * authorizeProjectExecutionAll(_,_, { it.contains('killAs') }) >> false
        }

        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> true }

        svcMock.requireVersion(*_)>>{ request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.renderSuccessXml(*_)>>{ request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock
        when:
        controller.apiExecutionAbort()

        then:
        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test get execution unauthorized
     */
    public void testApiExecutionUnauthorized() {
        when:
        def controller = controller
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAny(_,_, _) >> false
            }
        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.requireExists(*_)>>{ resp,e,args -> true }
        svcMock.requireAuthorized(*_)>>{ test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock
        controller.apiExecution()

        then:
        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    void testAjaxExecState_ok(){
        when:
        Execution e1 = new Execution( project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def jobexec = Mock(JobExecutionContext){}
        controller.scheduledExecutionService = Mock(ScheduledExecutionService){
            findExecutingQuartzJob(_)>> jobexec
        }
        controller.params.id=e1.id
        controller.frameworkService=Mock(FrameworkService){
            _*isClusterModeEnabled()>>false
        }
            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubjectAndProject(_, _)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeProjectExecutionAny(_,_, _) >> true
            }

        controller.executionService = Mock(ExecutionService){
            getExecutionState(_)>> ExecutionService.EXECUTION_ABORTED
        }
        def loader = new WorkflowStateFileLoader()
        loader.state = ExecutionFileState.AVAILABLE
        controller.workflowService = Mock(WorkflowService){
            requestStateSummary(*_)>> loader
        }
        controller.ajaxExecState()
        then:
        assertEquals(200,response.status)
    }

    /**
     * Test metrics calculations.
     */
    public void testApiExecutionsMetrics() {
        when:
        def controller = controller

        controller.request.api_version = 29
        controller.request.contentType = "application/json"
        controller.params.project = "Test"

        def apiMock = Mock(ApiService)

        apiMock.requireVersion(*_)>>{ request, response, int min ->
            assertEquals(29, min)
            return true
        }
        controller.apiService = apiMock

        // mock exec service
        controller.executionService = new ExecutionService()
        controller.executionService.applicationContext = Mock(ApplicationContext){
            getBeansOfType ()>> []
        }

        def listOnMemory = {
            Date now = new Date()
            Date dateStarted1 = now
            Date dateStarted2 = now
            Date dateStarted3 = now
            Date dateCompleted = now
            use (TimeCategory) {
                dateStarted1 = new Date() - 10.minute
                dateStarted2 = new Date() - 3.minute
                dateStarted3 = new Date() - 5.minute
            }
            [
                    [dateStarted: dateStarted1, dateCompleted: dateCompleted],
                    [dateStarted: dateStarted2, dateCompleted: dateCompleted],
                    [dateStarted: dateStarted3, dateCompleted: dateCompleted]
            ]
        }


        def metricCriteria = new Expando()
        metricCriteria.get = { Closure c -> [
                count      : 3,
                durationMax: new Time(0, 9, 0),
                durationMin: new Time(0, 2, 0),
                durationSum: new Time(0, 15, 0)
            ]
        }
        metricCriteria.list = {Map maxResult=null, Closure c ->
            println(maxResult)
            if(!maxResult){
                return listOnMemory()
            }

            if(isCompatible) {
                return [1]
            } else {
                throw new JDBCException("some sql exception", null, "")
            }
        }

        Execution.metaClass.static.createCriteria = { metricCriteria }

        // Call controller
        controller.apiExecutionMetrics(new ExecutionQuery())

        // Parse response.
        def resp = new JsonSlurper().parseText(response.text)

        then:
        // Check respose.
        assert 200 == controller.response.status
        assert resp.total == 3
//        assert resp.status.succeeded == 3
        assert resp.duration.average == "5m"
        assert resp.duration.min == "2m"
        assert resp.duration.max == "9m"

        where:
        isCompatible |_
        true         |_
        false        |_

    }


    /**
     * Test execution mode status api
     */
    public void testApiExecutionsStatusWhenActive() {

        given:
        def controller = controller

        params.api_version = 32
        request.contentType = "application/json"


        controller.apiService = Mock(ApiService)
        1 * controller.apiService.requireVersion(_,_,32)>>true

        // mock exec service
        controller.configurationService=Mock(ConfigurationService){
            isExecutionModeActive()>>true
        }
        controller.frameworkService=Mock(FrameworkService) {
        }
            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeApplicationResource(_,_, _) >> true
            }
        when:
            // Call controller
        controller.apiExecutionModeStatus()


        then:
        // Check respose.
        assert 200 == response.status
        assert response.json.executionMode == "active"
    }

    /**
     * Test execution mode status api
     */
    public void testApiExecutionsStatusWhenPassive() {

        when:
        def controller = controller

        controller.request.api_version = apiVersion
        controller.request.contentType = "application/json"
        params.passiveAs503 = passiveAs503

        def apiMock = Mock(ApiService)
        apiMock.requireVersion(*_)>>{ request, response, int min ->
            assertEquals(32, min)
            return true
        }
        controller.apiService = apiMock

        // mock exec service
            controller.configurationService=Mock(ConfigurationService){
                isExecutionModeActive()>>false
            }
            controller.frameworkService=Mock(FrameworkService) {
            }

            controller.rundeckAuthContextProvider = Mock(AuthContextProvider) {
                1 * getAuthContextForSubject(_)
            }
            controller.rundeckAuthContextEvaluator = Mock(AppAuthContextEvaluator) {
                1 * authorizeApplicationResource(_,_, _) >> true
            }
            // Call controller
        controller.apiExecutionModeStatus()

        // Parse response.
        def resp = new JsonSlurper().parseText(response.text)

        then:
        // Check respose.
        assert expectedStatus == controller.response.status
        assert resp.executionMode == "passive"

        where:
        apiVersion | passiveAs503 | expectedStatus
        35         | 'false'      | 503             //ignore passive parameter before V36
        36         | 'false'      | 200
        36         | 'true'       | 503
    }


}
