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
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.hibernate.JDBCException
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
import org.springframework.context.ApplicationContext
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.*
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Unroll

import javax.security.auth.Subject
import java.lang.annotation.Annotation
import java.sql.Time

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class ExecutionController2Spec extends HibernateSpec implements ControllerUnitTest<ExecutionController>  {

    List<Class> getDomainClasses() { [Workflow,ScheduledExecution,Execution,CommandExec]}

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
        svcMock.requireApi(*_)>>{ request, response, version ->
            version=14
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
        svcMock.requireApi(*_)>>{ request, response, version ->
            version=14
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
        def svcMock = Mock(ApiService)
        svcMock.requireApi(*_)>>{ request, response, version ->
            version=14
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
        svcMock.requireApi(*_)>>{ request, response, version ->
            version=14
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

            controller.apiService = Mock(ApiService) {
            1 * requireApi(_,_) >> true
            1 * renderSuccessXml(*_) >> { request, response, Closure clos ->
                return true
            }
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

        assert 200 == controller.response.status
        assert null == controller.request.apiErrorCode
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
    public void testApiExecutionsMetrics() {
        when:
        def controller = controller

        controller.request.api_version = 29
        controller.request.contentType = "application/json"
        controller.params.project = "Test"

        def apiMock = Mock(ApiService)

        apiMock.requireApi(*_)>>{ request, response, int min ->
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


}
