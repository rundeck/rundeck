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
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import groovy.mock.interceptor.MockFor
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.*
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.WorkflowStateFileLoader

import java.sql.Time

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@TestFor(ExecutionController)
@Mock([Workflow,ScheduledExecution,Execution,CommandExec])
class ExecutionControllerTests  {

    /**
     * utility method to mock a class
     */
    private mockWith(Class clazz, Closure clos) {
        def mock = new MockFor(clazz)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }
    void testDownloadOutputNotFound() {

        def ec = new ExecutionController()
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
        def logControl = new MockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e->
            [state: ExecutionFileState.NOT_FOUND]
        }
        ec.loggingService = logControl.proxyInstance()
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) {a,b->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) {project ->
            assert project=='test1'
            null
        }
        ec.frameworkService = fwkControl.proxyInstance()

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution={ Execution data->
            [:]
        }

        def result = ec.downloadOutput()
        assertEquals(404,ec.response.status)
    }

    void testDownloadOutputNotAvailable() {

        def ec = new ExecutionController()
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
        def logControl = new MockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionFileState.NOT_FOUND]
        }
        ec.loggingService = logControl.proxyInstance()
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) {project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.proxyInstance()

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }


        def result = ec.downloadOutput()
        assertEquals(404,ec.response.status)
    }

    void testAjaxExecState_missing(){
        controller.params.id=123
        controller.ajaxExecState()
        assertEquals(404,response.status)
        assertEquals("Execution not found for id: 123",response.json.error)
    }
    void testAjaxExecState_unauthorized(){
        Execution e1 = new Execution( project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def jobexec = mockWith(JobExecutionContext){}
        controller.scheduledExecutionService = mockWith(ScheduledExecutionService){
            findExecutingQuartzJob{id -> jobexec}
        }
        controller.params.id=e1.id
        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubjectAndProject{ subj,proj-> null }
            authorizeProjectExecutionAny{ ctx, exec, actions-> false }
        }
        controller.ajaxExecState()
        assertEquals(403,response.status)
        assertEquals("Unauthorized: View Execution ${e1.id}".toString(),response.json.error)
    }
    void testDownloadOutput(){

        def ec = new ExecutionController()
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

        def logControl = new MockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionFileState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = logControl.proxyInstance()
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) { project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.proxyInstance()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        ec.params.id = e1.id.toString()


        def result=ec.downloadOutput()
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        assertEquals("blah blah test monkey\n" + "Execution failed on the following 1 nodes: [centos5]\n", ec.response.contentAsString)
    }

    void testDownloadOutputFormatted(){

        def ec = new ExecutionController()
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
        def logControl = new MockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionFileState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = logControl.proxyInstance()
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) { project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.proxyInstance()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }


        ec.params.id = e1.id.toString()
        ec.params.formatted = 'true'
        ec.params.timeZone = 'GMT'

        def result=ec.downloadOutput()
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        def strings = ec.response.contentAsString.split("[\r\n]+") as List
        println strings
        assertEquals(["03:21:50 [admin@centos5 _][NORMAL] blah blah test monkey","03:21:51 [null@null _][ERROR] Execution failed on the following 1 nodes: [centos5]"], strings)
    }

    public void testApiExecutionsQueryRequireVersion() {
        def controller = new ExecutionController()
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, min ->
            response.status=400
            false
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionsQuery(null)
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_lessthan() {
        def controller = new ExecutionController()
        controller.request.api_version = 4

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireVersion { request,response, int min ->
            assertEquals(5,min)
            response.status=400
            return false
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionsQuery(null)
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_ok() {
        def controller = new ExecutionController()
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk,results,actions->
            return []
        }
        controller.frameworkService=fwkControl.proxyInstance()
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.queryExecutions { ExecutionQuery query, int offset, int max ->
            return [results:[],total:0]
        }
        execControl.demand.respondExecutionsXml { request, response, List<Execution> execs, paging ->
            return true
        }
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionsQuery(new ExecutionQuery())

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
        def controller = new ExecutionController()

        def execs = createTestExecs()

        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll { framework, List<Execution> results, Collection actions ->
            assert results == []
            []
        }
        controller.frameworkService = fwkControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "WRONG"
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.queryExecutions { ExecutionQuery query, int offset, int max ->
            assert null!=query
            assert "WRONG"==query.projFilter
            return [result: [], total: 0]
        }
        execControl.demand.respondExecutionsXml { request, response, List<Execution> execsx, paging ->
            return true
        }
        controller.executionService = execControl.proxyInstance()

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.demand.renderSuccessXml { request, response ->
            return true
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionsQuery(new ExecutionQuery())

        assert 200 == controller.response.status
    }

    /**
     * Test abort authorized
     */
    public void testApiExecutionAbortAuthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas, force ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }
        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return true }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }
        svcMock.demand.renderSuccessXml { request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionAbort()

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test abort unauthorized
     */
    public void testApiExecutionAbortUnauthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', reason: null]
        }
        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionAbort()

        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserUnauthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', reason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionAbort()

        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserAuthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas,force ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return true }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }
        svcMock.demand.requireVersion { request,response,int min ->
            assertEquals(5,min)
            return true
        }
        svcMock.demand.renderSuccessXml { request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionAbort()

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }
    /**
     * Test abort as user, abortAs denied
     */
    public void testApiExecutionAbortAsUserNotAuthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = new MockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas, force ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return !('killAs' in privs) }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }

        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.demand.renderSuccessXml { request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecutionAbort()

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }

    /**
     * Test get execution unauthorized
     */
    public void testApiExecutionUnauthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = new MockFor(FrameworkService, false)
        def execControl = new MockFor(ExecutionService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.authorizeProjectExecutionAny { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.proxyInstance()
        controller.executionService = execControl.proxyInstance()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = new MockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.proxyInstance()
        controller.apiExecution()

        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }

    void testAjaxExecState_ok(){
        Execution e1 = new Execution( project: 'test1', user: 'bob', dateStarted: new Date())
        assert e1.validate(), e1.errors.allErrors.collect { it.toString() }.join(",")
        assert e1.save()
        def jobexec = mockWith(JobExecutionContext){}
        controller.scheduledExecutionService = mockWith(ScheduledExecutionService){
            findExecutingQuartzJob{id -> jobexec}
        }
        controller.params.id=e1.id
        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubjectAndProject{ subj,proj-> null }
            authorizeProjectExecutionAny{ ctx, exec, actions-> true }
            isClusterModeEnabled{->false}
        }

        controller.executionService = mockWith(ExecutionService){
            getExecutionState{e -> ExecutionService.EXECUTION_ABORTED}
        }
        def loader = new WorkflowStateFileLoader()
        loader.state = ExecutionFileState.AVAILABLE
        controller.workflowService = mockWith(WorkflowService){
            requestStateSummary{e,nodes,selectedOnly, perform,steps-> loader}
        }
        controller.ajaxExecState()
        assertEquals(200,response.status)
    }

    /**
     * Test metrics calculations.
     */
    public void testApiExecutionsMetrics() {

        def controller = new ExecutionController()

        controller.request.api_version = 29
        controller.request.contentType = "application/json"
        controller.params.project = "Test"

        def apiMock = new MockFor(ApiService, false)

        apiMock.demand.requireVersion { request, response, int min ->
            assertEquals(29, min)
            return true
        }
        controller.apiService = apiMock.proxyInstance()

        // mock exec service
        controller.executionService = new ExecutionService()
        controller.executionService.applicationContext = mockWith(ApplicationContext){
            getBeansOfType { jobQuery -> [] }
        }

        // Mock metrics criteria
        mockDomain Execution
        def metricCriteria = new Expando()
        metricCriteria.get = { Closure c -> [
                count      : 3,
                durationMax: new Time(0, 9, 0),
                durationMin: new Time(0, 2, 0),
                durationSum: new Time(0, 15, 0)
            ]
        }
        Execution.metaClass.static.createCriteria = { metricCriteria }

        // Call controller
        controller.apiExecutionMetrics(new ExecutionQuery())

        // Parse response.
        def resp = new JsonSlurper().parseText(response.text)

        // Check respose.
        assert 200 == controller.response.status
        assert resp.total == 3
//        assert resp.status.succeeded == 3
        assert resp.duration.average == "5m"
        assert resp.duration.min == "2m"
        assert resp.duration.max == "9m"

    }


    /**
     * Test execution mode status api
     */
    public void testApiExecutionsStatusWhenActive() {

        def controller = new ExecutionController()

        controller.request.api_version = 32
        controller.request.contentType = "application/json"

        def apiMock = new MockFor(ApiService, false)
        apiMock.demand.requireVersion { request, response, int min ->
            assertEquals(32, min)
            return true
        }
        controller.apiService = apiMock.proxyInstance()

        // mock exec service
        controller.configurationService=mockWith(ConfigurationService){
            getExecutionModeActive { ->true }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject { subj -> null }
            authorizeApplicationResource {ctx, res, action -> true}
        }

            // Call controller
        controller.apiExecutionModeStatus()

        // Parse response.
        def resp = new JsonSlurper().parseText(response.text)

        // Check respose.
        assert 200 == controller.response.status
        assert resp.executionMode == "active"
    }

    /**
     * Test execution mode status api
     */
    public void testApiExecutionsStatusWhenPassive() {

        def controller = new ExecutionController()

        controller.request.api_version = 32
        controller.request.contentType = "application/json"

        def apiMock = new MockFor(ApiService, false)
        apiMock.demand.requireVersion { request, response, int min ->
            assertEquals(32, min)
            return true
        }
        controller.apiService = apiMock.proxyInstance()

        // mock exec service
        controller.configurationService=mockWith(ConfigurationService){
            getExecutionModeActive { ->false }
        }
        controller.frameworkService=mockWith(FrameworkService) {
            getAuthContextForSubject { subj -> null }
            authorizeApplicationResource {ctx, res, action -> true}
        }

            // Call controller
        controller.apiExecutionModeStatus()

        // Parse response.
        def resp = new JsonSlurper().parseText(response.text)

        // Check respose.
        assert 503 == controller.response.status
        assert resp.executionMode == "passive"
    }


}
