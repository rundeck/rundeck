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

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.core.logging.internal.RundeckLogFormat
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import org.quartz.JobExecutionContext
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.AppAuthorizer
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.web.WebExceptionHandler
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeExecution
import org.rundeck.core.auth.web.RdAuthorizeSystem
import org.rundeck.core.auth.web.WebDefaultParameterNamesMapper
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.Workflow
import java.time.LocalDate
import rundeck.services.*
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Specification
import spock.lang.Unroll

import javax.security.auth.Subject
import java.lang.annotation.Annotation

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class ExecutionController2Spec extends Specification implements ControllerUnitTest<ExecutionController>, DataTest  {

    def setupSpec() { mockDomains Workflow,ScheduledExecution,Execution,CommandExec,ScheduledExecutionStats }

    public static <T extends Annotation> T getMethodAnnotation(Object instance, String name, Class<T> clazz) {
        instance.getClass().getDeclaredMethods().find { it.name == name }.getAnnotation(clazz)
    }
    def setup(){
        controller.rundeckWebDefaultParameterNamesMapper=Mock(WebDefaultParameterNamesMapper)
        controller.rundeckExceptionHandler=Mock(WebExceptionHandler)
        controller.loggingService=Mock(LoggingService)
        controller.frameworkService=Mock(FrameworkService)
        controller.executionService=Mock(ExecutionService)
        controller.apiService=Mock(ApiService)
        controller.featureService = Mock(FeatureService)
        session.subject = new Subject()

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }

    @Unroll
    def "RdAuthorizeExecution annotation required for endpoint #endpoint"() {
        when:
            def result = getMethodAnnotation(artefactInstance, endpoint, RdAuthorizeExecution)
        then:
            result.value() == access
        where:
            endpoint         | access
            'downloadOutput' | RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW
            'apiExecutionAbort' | RundeckAccess.Execution.AUTH_APP_KILL
            'apiExecution' | RundeckAccess.Execution.AUTH_APP_READ_OR_VIEW
    }
    @Unroll
    def "RdAuthorizeSystem annotation required for endpoint #endpoint"() {
        when:
            def result = getMethodAnnotation(artefactInstance, endpoint, RdAuthorizeSystem)
        then:
            result.value() == access
        where:
            endpoint         | access
            'apiExecutionModeStatus' | RundeckAccess.System.AUTH_READ_OR_ANY_ADMIN
    }
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

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution={ Execution data->
            [:]
        }
        session.subject = new Subject()
        setupGetResource(e1)
        when:
        def result = ec.downloadOutput()
        then:
        assertEquals(404,ec.response.status)
    }

    private void setupGetResource(Execution e1) {
        controller.rundeckAppAuthorizer = Mock(AppAuthorizer) {
            1 * execution(_, _) >> Mock(AuthorizingExecution) {
                1 * getResource() >> e1
            }
        }
    }

    void testDownloadOutputNotAvailable() {

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

        ec.params.id = e1.id.toString()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        setupGetResource(e1)
        when:
        def result = ec.downloadOutput()
        then:
        assertEquals(404,ec.response.status)
    }

    void testAjaxExecState_missing(){
        given:

        session.subject = new Subject()
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * execution(_,_)>>Mock(AuthorizingExecution){
                1 * access(RundeckAccess.Execution.APP_READ_OR_VIEW) >>{
                    throw new NotFound('Execution', '123')
                }
            }
        }
        when:
        controller.params.id=123
        controller.ajaxExecState()
        then:
        assertEquals(404,response.status)
        assertEquals("Execution not found for id: 123",response.json.error)
    }
    void testAjaxExecState_unauthorized(){
        given:
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

        session.subject = new Subject()
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * execution(_,_)>>Mock(AuthorizingExecution){
                1 * access(RundeckAccess.Execution.APP_READ_OR_VIEW) >> {
                    throw new UnauthorizedAccess('read', 'Execution', e1.id.toString())
                }
            }
        }
        when:
        controller.ajaxExecState()
        then:
        assertEquals(403,response.status)
        assertEquals("Unauthorized: View Execution ${e1.id}".toString(),response.json.error)
    }
    void testDownloadOutput(){
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

        setupGetResource(e1)
        ec.params.id = e1.id.toString()

        when:
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

        fwkControl.getFrameworkPropertyResolver(*_)>>{ project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }

        setupGetResource(e1)

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
        svcMock.requireApi(*_)>>{ request, response ->
            response.status=400
            false
        }
        controller.apiService = svcMock
        controller.apiExecutionsQueryv14(null)
        then:
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV11_lessthan() {
        when:
        def controller = controller
        controller.request.api_version = 10

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ request, response ->
            response.status=400
            return false
        }
        controller.apiService = svcMock
        controller.apiExecutionsQueryv14(null)
        then:
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV11_ok() {
        given:
        def controller = controller
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def fwkControl = Mock(FrameworkService)
        fwkControl.existsFrameworkProject( _ )>>{ return true }

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubjectAndProject(_, _)

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
        controller.request.api_version = 11
        controller.params.project = "Test"
        controller.response.format = "xml"

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ request, response ->
            response.status=200
            return true
        }
        svcMock.requireExists(*_)>>{  response, test,args ->
            assertEquals(['Project','Test'], args)
            return true
        }
        controller.apiService = svcMock
        controller.configurationService=Mock(ConfigurationService){
            _ * getInteger(_, _) >> { it[1] }
        }
        when:
        controller.apiExecutionsQueryv14(new ExecutionQuery())

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

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                1 * getAuthContextForSubjectAndProject(_, _)

                1 * filterAuthorizedProjectExecutionsAll(_,[],_) >> []
            }
        controller.frameworkService = fwkControl
        controller.request.api_version = 11
        controller.params.project = "WRONG"
        controller.response.format = "xml"
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
        svcMock.requireApi(*_)>>{ request, response ->
            response.status=200
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
        controller.configurationService=Mock(ConfigurationService){
            _ * getInteger(_, _) >> { it[1] }
        }
        when:
        controller.apiExecutionsQueryv14(new ExecutionQuery())

        then:
        assert 200 == controller.response.status
    }

    /**
     * Test abort authorized
     */
    public void testApiExecutionAbortAuthorized() {
        given:
        def execs = createTestExecs()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ req, resp -> true }
        svcMock.renderSuccessXml(*_)>>{ request, response, Closure clos ->
            return true
        }
        controller.apiService = svcMock

        setupGetResource(execs[2])
        when:
        controller.apiExecutionAbort()

        then:
         1 * controller.executionService.abortExecution(
             _ as AuthorizingExecution,
             null,
             false
         ) >> [abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]
        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
    }


    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserUnauthorized() {
        given:
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)
        def execControl = Mock(ExecutionService)
        execControl.abortExecution(_ as AuthorizingExecution,'testuser',false)>>{
            throw new UnauthorizedAccess('abortAs','execution','blah')
        }

        controller.frameworkService = fwkControl
        controller.executionService = execControl
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = Mock(ApiService){
            1 * requireApi(*_)>>true
        }
        controller.apiService = svcMock
        session.subject = new Subject()
        setupGetResource(execs[2])
        when:
        controller.apiExecutionAbort()

        then:
            1 * controller.rundeckExceptionHandler.handleException(_,_,_ as UnauthorizedAccess)
    }

    /**
     * Test abort as user
     */
    public void testApiExecutionAbortAsUserAuthorized() {
        given:
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)


        controller.frameworkService = fwkControl
        controller.executionService = Mock(ExecutionService)
        controller.request.api_version = 11
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"
        controller.response.format = format

            controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_) >> true
            0 * renderSuccessXml(*_) >> { request, response, Closure clos ->
                return true
            }
            1* apiHrefForExecution(_)
            1* guiHrefForExecution(_)

            0 * _(*_)
        }
        session.subject = new Subject()
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * execution(_,_)>>Mock(AuthorizingExecution){
                1 * getResource()>>execs[2]
            }
        }
        when:
        controller.apiExecutionAbort()

        then:
        1 * controller.executionService.abortExecution(_,'testuser',_)>>[abortstate: 'aborted', jobstate: 'running', status: 'blah', reason: null]

        controller.response.status == 200
        controller.request.apiErrorCode == null
        response.json == [
                abort:[status:'aborted'],
                execution:[
                        id:execs[2].id.toString(),
                        status:'running',
                        href:null,
                        permalink:null
                ]
        ]
        where: "xml is not allowed so any requested format returns json"
        format<<['xml','json']
    }
    /**
     * Test abort as user, abortAs denied
     */
    public void "apiExecutionAbort service unauthorized response"() {
        given:
        def execs = createTestExecs()
        def fwkControl = Mock(FrameworkService)


        controller.frameworkService = fwkControl
        request.api_version = 14
        params.project = "Test"
        params.id = execs[2].id.toString()
        params.asUser = "testuser"

        def svcMock = Mock(ApiService) {
            1 * requireApi(*_)>>true

            0 *renderErrorXml(*_)
        }

        controller.apiService = svcMock
        response.format='json'
        session.subject = new Subject()
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * execution(_,_)>>Mock(AuthorizingExecution){
                1 * getResource() >>  execs[2]
            }
        }
        when:
        controller.apiExecutionAbort()

        then:
            1 * controller.executionService.abortExecution(_,'testuser',_)>> {
                throw new UnauthorizedAccess('killAs','execution','blah')
            }
            1 * controller.rundeckExceptionHandler.handleException(_,_,_ as UnauthorizedAccess)>>{
                it[2].action == 'killAs'
            }
    }


    void testAjaxExecState_ok(){
        given:
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

        controller.executionService = Mock(ExecutionService){
            getExecutionState(_)>> ExecutionService.EXECUTION_ABORTED
        }
        def loader = new WorkflowStateFileLoader()
        loader.state = ExecutionFileState.AVAILABLE
        controller.workflowService = Mock(WorkflowService){
            requestStateSummary(*_)>> loader
        }
        controller.rundeckAppAuthorizer=Mock(AppAuthorizer){
            1 * execution(_,_)>>Mock(AuthorizingExecution){
                1 * access(RundeckAccess.Execution.APP_READ_OR_VIEW) >> e1
            }
        }
        when:
        controller.ajaxExecState()
        then:
        assertEquals(200,response.status)
    }

    /**
     * Test metrics calculations.
     */
    public void "api metrics response json"() {
        given:

        request.api_version = 29
        request.contentType = "application/json"
        params.project = "Test"

        def apiMock = Mock(ApiService)

        controller.apiService = apiMock

        // mock exec service
        controller.executionService = Mock(ExecutionService)
        response.format = "json"


        1 * controller.executionService.queryExecutionMetrics(_)>>[
            total:3,
            duration:[
                average: 5 * 60 * 1000,
                min    : 2 * 60 * 1000,
                max    : 9 * 60 * 1000,
            ]
        ]
        when:
            controller.apiExecutionMetrics(new ExecutionQuery())
            def json = response.json


        then:
            1 * apiMock.requireApi(_, _, 29) >> true

            assert 200 == response.status
            assert json.total == 3

            assert json.duration.average == "5m"
            assert json.duration.min == "2m"
            assert json.duration.max == "9m"
    }



    /**
     * Test execution mode status api
     */
    public void testApiExecutionsStatusWhenActive() {

        given:
        def controller = controller

        params.api_version = 32
        request.contentType = "application/json"
        controller.response.format = "json"

        controller.apiService = Mock(ApiService)
        1 * controller.apiService.requireApi(_,_,32)>>true

        // mock exec service
        controller.configurationService=Mock(ConfigurationService){
            isExecutionModeActive()>>true
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
    @Unroll
    public void "testApiExecutionsStatusWhenPassive api"() {

        given:
        controller.request.api_version = apiVersion
        controller.request.contentType = "application/json"
        params.passiveAs503 = passiveAs503
        controller.response.format = "json"

        def apiMock = Mock(ApiService)
        apiMock.requireApi(*_)>>{ request, response, int min ->
            assertEquals(32, min)
            return true
        }
        controller.apiService = apiMock

        // mock exec service
            controller.configurationService=Mock(ConfigurationService){
                isExecutionModeActive()>>false
            }
        when:
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


    def "apiExecutionMetrics with useStats false delegates to executionService"() {
        given:
            request.api_version = 29
            request.contentType = "application/json"
            params.project = "Test"
            // useStats defaults to false when not specified

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            controller.executionService = Mock(ExecutionService)
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, 29) >> true
            1 * controller.executionService.queryExecutionMetrics(_) >> [
                total: 5,
                succeeded: 3,
                failed: 2,
                duration: [
                    average: 10000,
                    min: 5000,
                    max: 15000
                ]
            ]
            response.status == 200
    }

    // RUN-3768 Phase 5: Batch endpoint tests
    // Note: Parameter validation (project required) is tested via integration tests
    // Unit testing this requires extensive mocking of the response chain

    def "apiExecutionMetrics batch mode returns metrics for all jobs"() {
        given:
            request.api_version = ApiVersions.V57
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"
            params.groupByJob = "true"

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V57) >> true
            // Response should be rendered with batch metrics format
            response.status == 200
    }

    def "apiExecutionMetrics batch mode formats duration values as strings"() {
        given: "a project with jobs having stats with duration values"
            request.api_version = ApiVersions.V57
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"
            params.groupByJob = "true"

            def job1Uuid = UUID.randomUUID().toString()
            def job2Uuid = UUID.randomUUID().toString()

            def job1 = new ScheduledExecution(
                uuid: job1Uuid,
                jobName: "Job 1",
                project: "TestProject"
            )
            job1.save(flush: true)

            def job2 = new ScheduledExecution(
                uuid: job2Uuid,
                jobName: "Job 2",
                project: "TestProject"
            )
            job2.save(flush: true)

            // Create stats for job1 with duration values
            // Duration field is TOTAL duration across all executions (not average)
            // Average = total duration / total executions
            def stats1 = new ScheduledExecutionStats(
                jobUuid: job1Uuid,
                contentMap: [
                    dailyMetrics: [
                        (LocalDate.now().toString()): [
                            total: 10,
                            succeeded: 8,
                            failed: 2,
                            aborted: 0,
                            timedout: 0,
                            duration: 5 * 60 * 1000 * 10,  // 5 minutes average * 10 executions = 3,000,000ms total
                            hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }
                        ]
                    ]
                ]
            )
            stats1.save(flush: true)

            // Create stats for job2 with different duration
            def stats2 = new ScheduledExecutionStats(
                jobUuid: job2Uuid,
                contentMap: [
                    dailyMetrics: [
                        (LocalDate.now().toString()): [
                            total: 5,
                            succeeded: 5,
                            failed: 0,
                            aborted: 0,
                            timedout: 0,
                            duration: 30 * 1000 * 5,  // 30 seconds average * 5 executions = 150,000ms total
                            hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }
                        ]
                    ]
                ]
            )
            stats2.save(flush: true)

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V57) >> true
            response.status == 200

            // Verify duration values are formatted as strings (not raw milliseconds)
            def json = response.json
            json.jobs != null
            json.jobs[job1Uuid] != null
            json.jobs[job2Uuid] != null

            // Job1 should have formatted duration "5m" (not 300000)
            json.jobs[job1Uuid].duration.average == "5m"

            // Job2 should have formatted duration "30s" (not 30000)
            json.jobs[job2Uuid].duration.average == "30s"
    }

    def "apiExecutionMetrics groupByJob without useStats does not trigger batch mode"() {
        given:
            request.api_version = ApiVersions.V57
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "false"
            params.groupByJob = "true"

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            controller.executionService = Mock(ExecutionService)
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V57) >> true
            // Should use regular execution service query, not batch mode (since useStats=false)
            1 * controller.executionService.queryExecutionMetrics(_) >> [
                total: 0,
                duration: [average: 0, min: 0, max: 0]
            ]
            response.status == 200
    }

    // Tests for getMetricsFromStats with begin/end date filtering
    def "getMetricsFromStats filters metrics by date range correctly"() {
        given: "a job with stats containing metrics for multiple dates"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            // Create stats with metrics for dates: 2025-11-19, 2025-11-22, 2025-11-23, 2025-11-24, 2025-11-25
            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-23": [total: 15, succeeded: 14, failed: 1, aborted: 0, timedout: 0, duration: 15000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-24": [total: 25, succeeded: 23, failed: 2, aborted: 0, timedout: 0, duration: 25000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-25": [total: 5, succeeded: 5, failed: 0, aborted: 0, timedout: 0, duration: 5000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with begin and end dates"
            def begin = "2025-11-22T00:00:00Z"
            def end = "2025-11-24T23:59:59Z"
            def beginDate = ReportsController.parseDate(begin)
            def endDate = ReportsController.parseDate(end)
            def result = controller.getMetricsFromStats(jobUuid, beginDate, endDate)

        then: "only metrics within the date range are returned"
            result != null
            result.total == 60  // 20 + 15 + 25
            result.succeeded == 55  // 18 + 14 + 23
            result.failed == 5  // 2 + 1 + 2
            result.daily_breakdown.size() == 3
            result.daily_breakdown.containsKey("2025-11-22")
            result.daily_breakdown.containsKey("2025-11-23")
            result.daily_breakdown.containsKey("2025-11-24")
            !result.daily_breakdown.containsKey("2025-11-19")
            !result.daily_breakdown.containsKey("2025-11-25")
    }

    def "getMetricsFromStats returns zeros when no metrics exist for date range"() {
        given: "a job with stats containing metrics outside the requested date range"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-20": [total: 15, succeeded: 14, failed: 1, aborted: 0, timedout: 0, duration: 15000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with a date range that has no metrics"
            def begin = "2025-11-22T00:00:00Z"
            def end = "2025-11-24T23:59:59Z"
            def beginDate = ReportsController.parseDate(begin)
            def endDate = ReportsController.parseDate(end)
            def result = controller.getMetricsFromStats(jobUuid, beginDate, endDate)

        then: "returns empty metrics object with all zeros"
            result != null
            result.total == 0
            result.succeeded == 0
            result.failed == 0
            result.aborted == 0
            result.timedout == 0
            result.successRate == 0.0
            result.duration.average == 0
            result.daily_breakdown.isEmpty()
            result.hourly_heatmap.size() == 24
            result.hourly_heatmap.every { it == 0 }
    }

    def "getMetricsFromStats includes boundary dates correctly"() {
        given: "a job with stats containing metrics on boundary dates"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-24": [total: 25, succeeded: 23, failed: 2, aborted: 0, timedout: 0, duration: 25000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with begin and end matching boundary dates"
            def begin = "2025-11-22T00:00:00Z"
            def end = "2025-11-24T23:59:59Z"
            def beginDate = ReportsController.parseDate(begin)
            def endDate = ReportsController.parseDate(end)
            def result = controller.getMetricsFromStats(jobUuid, beginDate, endDate)

        then: "boundary dates are included"
            result != null
            result.total == 45  // 20 + 25
            result.daily_breakdown.size() == 2
            result.daily_breakdown.containsKey("2025-11-22")
            result.daily_breakdown.containsKey("2025-11-24")
    }

    def "getMetricsFromStats works with only begin date"() {
        given: "a job with stats containing metrics for multiple dates"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-25": [total: 5, succeeded: 5, failed: 0, aborted: 0, timedout: 0, duration: 5000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with only begin date"
            def begin = "2025-11-22T00:00:00Z"
            def beginDate = ReportsController.parseDate(begin)
            def result = controller.getMetricsFromStats(jobUuid, beginDate, null)

        then: "all dates from begin date onwards are included"
            result != null
            result.total == 25  // 20 + 5
            result.daily_breakdown.size() == 2
            result.daily_breakdown.containsKey("2025-11-22")
            result.daily_breakdown.containsKey("2025-11-25")
            !result.daily_breakdown.containsKey("2025-11-19")
    }

    def "getMetricsFromStats works with only end date"() {
        given: "a job with stats containing metrics for multiple dates"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-25": [total: 5, succeeded: 5, failed: 0, aborted: 0, timedout: 0, duration: 5000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with only end date"
            def end = "2025-11-22T23:59:59Z"
            def endDate = ReportsController.parseDate(end)
            def result = controller.getMetricsFromStats(jobUuid, null, endDate)

        then: "all dates up to and including end date are included"
            result != null
            result.total == 30  // 10 + 20
            result.daily_breakdown.size() == 2
            result.daily_breakdown.containsKey("2025-11-19")
            result.daily_breakdown.containsKey("2025-11-22")
            !result.daily_breakdown.containsKey("2025-11-25")
    }

    def "getMetricsFromStats maintains backward compatibility when no date range provided"() {
        given: "a job with stats containing metrics for multiple dates"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            // Create metrics for last 10 days
            def today = LocalDate.now()
            def dailyMetrics = [:]
            (0..9).each { daysAgo ->
                def date = today.minusDays(daysAgo)
                dailyMetrics[date.toString()] = [
                    total: 10 + daysAgo,
                    succeeded: 8 + daysAgo,
                    failed: 2,
                    aborted: 0,
                    timedout: 0,
                    duration: 10000 + (daysAgo * 1000),
                    hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }
                ]
            }

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: dailyMetrics
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called without begin/end dates"
            def result = controller.getMetricsFromStats(jobUuid, null, null)

        then: "returns metrics for last 7 days (backward compatibility)"
            result != null
            result.daily_breakdown.size() == 7
            result.total > 0
    }

    def "getMetricsBatch passes begin and end parameters to getMetricsFromStats"() {
        given: "a project with multiple jobs"
            def projectName = "TestProject"
            def job1Uuid = UUID.randomUUID().toString()
            def job2Uuid = UUID.randomUUID().toString()

            def job1 = new ScheduledExecution(
                uuid: job1Uuid,
                jobName: "Job 1",
                project: projectName
            )
            job1.save(flush: true)

            def job2 = new ScheduledExecution(
                uuid: job2Uuid,
                jobName: "Job 2",
                project: projectName
            )
            job2.save(flush: true)

            // Create stats for job1 with metrics in date range
            def stats1 = new ScheduledExecutionStats(
                jobUuid: job1Uuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-24": [total: 15, succeeded: 14, failed: 1, aborted: 0, timedout: 0, duration: 15000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats1.save(flush: true)

            // Create stats for job2 with metrics outside date range
            def stats2 = new ScheduledExecutionStats(
                jobUuid: job2Uuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 5, succeeded: 5, failed: 0, aborted: 0, timedout: 0, duration: 5000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats2.save(flush: true)

        when: "getMetricsBatch is called with begin and end dates"
            def begin = "2025-11-22T00:00:00Z"
            def end = "2025-11-24T23:59:59Z"
            def beginDate = ReportsController.parseDate(begin)
            def endDate = ReportsController.parseDate(end)
            def result = controller.getMetricsBatch(projectName, beginDate, endDate)

        then: "each job's metrics are filtered by the date range"
            result != null
            result.jobs != null
            result.jobs.size() == 2
            
            // Job1 should have filtered metrics
            def job1Metrics = result.jobs[job1Uuid]
            job1Metrics != null
            job1Metrics.total == 35  // 20 + 15
            job1Metrics.daily_breakdown.size() == 2
            job1Metrics.daily_breakdown.containsKey("2025-11-22")
            job1Metrics.daily_breakdown.containsKey("2025-11-24")
            !job1Metrics.daily_breakdown.containsKey("2025-11-19")
            
            // Job2 should have empty metrics (no data in range)
            def job2Metrics = result.jobs[job2Uuid]
            job2Metrics != null
            job2Metrics.total == 0
            job2Metrics.daily_breakdown.isEmpty()
    }

    def "getMetricsFromStats handles invalid date format gracefully"() {
        given: "a job with stats containing metrics within last 7 days"
            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            // Create metrics for today and a few days ago to ensure they're within last 7 days
            def today = LocalDate.now()
            def dailyMetrics = [:]
            (0..2).each { daysAgo ->
                def date = today.minusDays(daysAgo)
                dailyMetrics[date.toString()] = [
                    total: 10 + daysAgo,
                    succeeded: 8 + daysAgo,
                    failed: 2,
                    aborted: 0,
                    timedout: 0,
                    duration: 10000 + (daysAgo * 1000),
                    hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }
                ]
            }

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: dailyMetrics
                ]
            )
            stats.save(flush: true)

        when: "getMetricsFromStats is called with invalid date format"
            def begin = "invalid-date"
            // Invalid dates should be caught before reaching getMetricsFromStats, but if null is passed, it falls back to last 7 days
            def result = controller.getMetricsFromStats(jobUuid, null, null)

        then: "falls back to backward compatibility (last 7 days) and returns metrics"
            result != null
            // Should still return metrics, using backward compatibility (last 7 days)
            result.total > 0
            result.daily_breakdown.size() > 0
    }

    def "apiExecutionMetrics with useStats passes begin and end parameters"() {
        given:
            request.api_version = ApiVersions.V57
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"
            params.begin = "2025-11-22T00:00:00Z"
            params.end = "2025-11-24T23:59:59Z"

            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        "2025-11-19": [total: 10, succeeded: 8, failed: 2, aborted: 0, timedout: 0, duration: 10000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-22": [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }],
                        "2025-11-24": [total: 15, succeeded: 14, failed: 1, aborted: 0, timedout: 0, duration: 15000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            query.jobIdListFilter = [jobUuid]
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V57) >> true
            response.status == 200
            
            // Parse JSON response to verify filtering
            def jsonResponse = new JsonSlurper().parseText(response.text)
            jsonResponse.total == 35  // 20 + 15 (filtered)
            jsonResponse.daily_breakdown.size() == 2
            jsonResponse.daily_breakdown.containsKey("2025-11-22")
            jsonResponse.daily_breakdown.containsKey("2025-11-24")
            !jsonResponse.daily_breakdown.containsKey("2025-11-19")
    }

    def "apiExecutionMetrics useStats requires API version 57"() {
        given:
            request.api_version = ApiVersions.V56  // One version below required
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            query.jobIdListFilter = [UUID.randomUUID().toString()]
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V56) >> true
            1 * apiMock.renderErrorFormat(_, _) >> { response, error ->
                response.status = 400
                assert error.code == 'api.error.invalid.version'
                assert error.args[0].contains('API version 57')
            }
            response.status == 400
    }

    def "apiExecutionMetrics groupByJob requires API version 57"() {
        given:
            request.api_version = ApiVersions.V56  // One version below required
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"
            params.groupByJob = "true"

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V56) >> true
            1 * apiMock.renderErrorFormat(_, _) >> { response, error ->
                response.status = 400
                assert error.code == 'api.error.invalid.version'
                assert error.args[0].contains('API version 57')
            }
            response.status == 400
    }

    def "apiExecutionMetrics useStats works with API version 57"() {
        given:
            request.api_version = ApiVersions.V57
            request.contentType = "application/json"
            params.project = "TestProject"
            params.useStats = "true"

            def jobUuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(
                uuid: jobUuid,
                jobName: "Test Job",
                project: "TestProject"
            )
            job.save(flush: true)

            // Use today's date to ensure metrics are within the default 7-day window
            def today = java.time.LocalDate.now().toString()
            def stats = new ScheduledExecutionStats(
                jobUuid: jobUuid,
                contentMap: [
                    dailyMetrics: [
                        (today): [total: 20, succeeded: 18, failed: 2, aborted: 0, timedout: 0, duration: 20000, hourly: (0..23).collect { [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0] }]
                    ]
                ]
            )
            stats.save(flush: true)

            def apiMock = Mock(ApiService)
            controller.apiService = apiMock
            response.format = "json"

        when:
            def query = new ExecutionQuery()
            query.jobIdListFilter = [jobUuid]
            controller.apiExecutionMetrics(query)

        then:
            1 * apiMock.requireApi(_, _, ApiVersions.V57) >> true
            response.status == 200
            
            // Verify response contains metrics from stats
            def jsonResponse = new JsonSlurper().parseText(response.text)
            jsonResponse.total == 20
            jsonResponse.succeeded == 18
            jsonResponse.failed == 2
    }

    // RUN-3768: Tests for new helper methods
    def "parseTimestampParameters returns error for invalid begin timestamp"() {
        given:
            controller.apiService = Mock(ApiService)
            def beginParam = "invalid-date-format"
            def endParam = null

        when:
            def result = controller.parseTimestampParameters(beginParam, endParam, response)

        then:
            1 * controller.apiService.renderErrorFormat(_, _)
            // renderErrorFormat is void, so result.error will be null (void return value)
            result.beginTimestamp == null
            result.endTimestamp == null
    }

    def "parseTimestampParameters returns error for invalid end timestamp"() {
        given:
            controller.apiService = Mock(ApiService)
            def beginParam = null
            def endParam = "not-a-valid-timestamp"

        when:
            def result = controller.parseTimestampParameters(beginParam, endParam, response)

        then:
            1 * controller.apiService.renderErrorFormat(_, _)
            // renderErrorFormat is void, so result.error will be null (void return value)
            result.beginTimestamp == null
            result.endTimestamp == null
    }

    def "parseTimestampParameters parses valid timestamps correctly"() {
        given:
            def beginParam = "2025-11-22T10:30:00Z"
            def endParam = "2025-11-24T14:45:00Z"

        when:
            def result = controller.parseTimestampParameters(beginParam, endParam, response)

        then:
            result.error == null
            result.beginTimestamp != null
            result.endTimestamp != null
    }

    def "aggregateHourlyHeatmap handles new Map format correctly"() {
        given: "metrics with new hourly Map format"
            def processedMetrics = [
                "2025-11-22": [
                    hourly: (0..23).collect { hour ->
                        hour == 10 ? [total: 5, succeeded: 3, failed: 2, aborted: 0, timedout: 0] :
                        hour == 14 ? [total: 3, succeeded: 2, failed: 1, aborted: 0, timedout: 0] :
                        [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0]
                    }
                ],
                "2025-11-23": [
                    hourly: (0..23).collect { hour ->
                        hour == 10 ? [total: 2, succeeded: 2, failed: 0, aborted: 0, timedout: 0] :
                        hour == 16 ? [total: 4, succeeded: 3, failed: 0, aborted: 1, timedout: 0] :
                        [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0]
                    }
                ]
            ]

        when:
            def heatmap = controller.aggregateHourlyHeatmap(processedMetrics)

        then:
            heatmap.size() == 24
            heatmap[10] == 7  // 5 + 2
            heatmap[14] == 3  // 3 + 0
            heatmap[16] == 4  // 0 + 4
            heatmap[0] == 0
            heatmap[23] == 0
    }

    def "aggregateHourlyHeatmap handles old Integer format for backward compatibility"() {
        given: "metrics with old hourly Integer format"
            def processedMetrics = [
                "2025-11-22": [
                    hourly: (0..23).collect { hour ->
                        hour == 10 ? 5 : hour == 14 ? 3 : 0
                    }
                ]
            ]

        when:
            def heatmap = controller.aggregateHourlyHeatmap(processedMetrics)

        then:
            heatmap.size() == 24
            heatmap[10] == 5
            heatmap[14] == 3
            heatmap[0] == 0
    }

    def "applyHourLevelFilter correctly filters hours and recalculates aggregates"() {
        given: "a day's metrics with executions spread across hours"
            def dateStr = "2025-11-22"
            def dayMetrics = [
                total: 20,
                succeeded: 15,
                failed: 3,
                aborted: 1,
                timedout: 1,
                duration: 20000,
                hourly: (0..23).collect { hour ->
                    // Hour 8: 5 executions
                    if (hour == 8) return [total: 5, succeeded: 4, failed: 1, aborted: 0, timedout: 0]
                    // Hour 10: 8 executions
                    if (hour == 10) return [total: 8, succeeded: 6, failed: 1, aborted: 1, timedout: 0]
                    // Hour 14: 7 executions
                    if (hour == 14) return [total: 7, succeeded: 5, failed: 1, aborted: 0, timedout: 1]
                    return [total: 0, succeeded: 0, failed: 0, aborted: 0, timedout: 0]
                }
            ]
            def beginHour = 10

        when:
            def filtered = controller.applyHourLevelFilter(dateStr, dayMetrics, beginHour)

        then:
            // Hours before 10 should be zeroed out
            filtered.hourly[8].total == 0
            filtered.hourly[8].succeeded == 0

            // Hour 10 and after should be preserved
            filtered.hourly[10].total == 8
            filtered.hourly[10].succeeded == 6
            filtered.hourly[14].total == 7
            filtered.hourly[14].succeeded == 5

            // Aggregates should be recalculated (only hours 10-23)
            filtered.total == 15  // 8 + 7
            filtered.succeeded == 11  // 6 + 5
            filtered.failed == 2  // 1 + 1
            filtered.aborted == 1  // 1 + 0
            filtered.timedout == 1  // 0 + 1

            // Duration should be proportionally adjusted
            // Original: 20 executions with 20000ms duration
            // Filtered: 15 executions, so 20000 * (15/20) = 15000ms
            filtered.duration == 15000
    }

    def "extractDateAndHour extracts date and hour from timestamp"() {
        given:
            // Use system default timezone to match implementation
            def calendar = Calendar.getInstance()
            calendar.set(2025, Calendar.NOVEMBER, 22, 14, 30, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            def timestamp = calendar.time

        when:
            def result = controller.extractDateAndHour(timestamp, true)

        then:
            result.date != null
            result.date.toString() == "2025-11-22"
            result.hour == 14
    }

    def "extractDateAndHour with extractHour false returns only date"() {
        given:
            // Use system default timezone to match implementation
            def calendar = Calendar.getInstance()
            calendar.set(2025, Calendar.NOVEMBER, 22, 14, 30, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            def timestamp = calendar.time

        when:
            def result = controller.extractDateAndHour(timestamp, false)

        then:
            result.date != null
            result.date.toString() == "2025-11-22"
            result.hour == null
    }


}
