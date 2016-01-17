package rundeck.controllers

import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.app.internal.logging.RundeckLogFormat
import com.dtolabs.rundeck.app.support.ExecutionQuery
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.ControllerUnitTestCase
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ApiService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.LoggingService
import rundeck.services.logging.ExecutionLogState

@TestFor(ExecutionController)
@Mock([Workflow,ScheduledExecution,Execution])
class ExecutionControllerTests  {

    /**
     * utility method to mock a class
     */
    private mockWith(Class clazz, Closure clos) {
        def mock = mockFor(clazz)
        mock.demand.with(clos)
        return mock.createMock()
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
        def logControl = mockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e->
            [state: ExecutionLogState.NOT_FOUND]
        }
        ec.loggingService = logControl.createMock()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) {a,b->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) {project ->
            assert project=='test1'
            null
        }
        ec.frameworkService = fwkControl.createMock()

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
        def logControl = mockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.NOT_FOUND]
        }
        ec.loggingService = logControl.createMock()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) {project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.createMock()

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
        controller.params.id=e1.id
        controller.frameworkService=mockWith(FrameworkService){
            getAuthContextForSubjectAndProject{ subj,proj-> null }
            authorizeProjectExecutionAll{ ctx, exec, actions-> false }
        }
        controller.ajaxExecState()
        assertEquals(403,response.status)
        assertEquals("Unauthorized: Read Execution ${e1.id}",response.json.error)
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

        def logControl = mockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = logControl.createMock()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) { project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.createMock()
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
        def logControl = mockFor(LoggingService, true)
        logControl.demand.getLogReader { Execution e ->
            [state: ExecutionLogState.AVAILABLE, reader: new FSStreamingLogReader(tf1, "UTF-8", new RundeckLogFormat())]
        }
        ec.loggingService = logControl.createMock()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession(1..1) { a, b ->
            null
        }
        fwkControl.demand.getFrameworkPropertyResolver(1..1) { project ->
            assert project == 'test1'
            null
        }
        ec.frameworkService = fwkControl.createMock()
        ExecutionService.metaClass.static.exportContextForExecution = { Execution data ->
            [:]
        }


        ec.params.id = e1.id.toString()
        ec.params.formatted = 'true'

        def result=ec.downloadOutput()
        assertNotNull(ec.response.getHeader('Content-Disposition'))
        def strings = ec.response.contentAsString.split("[\r\n]+") as List
        println strings
        assertEquals(["03:21:50 [admin@centos5 _][NORMAL] blah blah test monkey","03:21:51 [null@null _][ERROR] Execution failed on the following 1 nodes: [centos5]"], strings)
    }

    public void testApiExecutionsQueryRequireVersion() {
        def controller = new ExecutionController()
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, min ->
            response.status=400
            false
        }
        controller.apiService = svcMock.createMock()
        controller.apiExecutionsQuery(null)
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_lessthan() {
        def controller = new ExecutionController()
        controller.request.api_version = 4

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireVersion { request,response, int min ->
            assertEquals(5,min)
            response.status=400
            return false
        }
        controller.apiService = svcMock.createMock()
        controller.apiExecutionsQuery(null)
        assert 400 == controller.response.status
    }

    public void testApiExecutionsQueryRequireV5_ok() {
        def controller = new ExecutionController()
        ApiController.metaClass.message = { params -> params?.code ?: 'messageCodeMissing' }
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll {fwk,results,actions->
            return []
        }
        controller.frameworkService=fwkControl.createMock()
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.queryExecutions { ExecutionQuery query, int offset, int max ->
            return [results:[],total:0]
        }
        execControl.demand.respondExecutionsXml { response, List<Execution> execs ->
            return true
        }
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        controller.apiService = svcMock.createMock()
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

        Execution e1 = new Execution(
                scheduledExecution: se1,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'adam',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test1 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e1.save()

        Execution e2 = new Execution(
                scheduledExecution: se2,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'bob',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test2 buddy', argString: '-delay 12 -monkey cheese -particle'])]).save()

        )
        assert null != e2.save()
        Execution e3 = new Execution(
                scheduledExecution: se3,
                project: "Test",
                status: "true",
                dateStarted: new Date(),
                dateCompleted: new Date(),
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

        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.filterAuthorizedProjectExecutionsAll { framework, List<Execution> results, Collection actions ->
            assert results == []
            []
        }
        controller.frameworkService = fwkControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "WRONG"
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.queryExecutions { ExecutionQuery query, int offset, int max ->
            assert null!=query
            assert "WRONG"==query.projFilter
            return [result: [], total: 0]
        }
        execControl.demand.respondExecutionsXml { response, List<Execution> execsx ->
            return true
        }
        controller.executionService = execControl.createMock()

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.demand.renderSuccessXml { response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.createMock()
        controller.apiExecutionsQuery(new ExecutionQuery())

        assert 200 == controller.response.status
    }

    /**
     * Test abort authorized
     */
    public void testApiExecutionAbortAuthorized() {
        def controller = new ExecutionController()
        def execs = createTestExecs()
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', failedreason: null]
        }
        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return true }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }
        svcMock.demand.renderSuccessXml { response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.createMock()
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
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert null == killas
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', failedreason: null]
        }
        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.createMock()
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
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', failedreason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.createMock()
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
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', failedreason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return true }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }
        svcMock.demand.requireVersion { request,response,int min ->
            assertEquals(5,min)
            return true
        }
        svcMock.demand.renderSuccessXml { response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.createMock()
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
        def fwkControl = mockFor(FrameworkService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        def execControl = mockFor(ExecutionService, false)
        execControl.demand.abortExecution { se, e, user, framework, killas ->
            assert killas == 'testuser'
            [abortstate: 'aborted', jobstate: 'running', statusStr: 'blah', failedreason: null]
        }

        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return !('killAs' in privs) }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> true }

        svcMock.demand.requireVersion { request, response, int min ->
            assertEquals(5, min)
            return true
        }
        svcMock.demand.renderSuccessXml { response, Closure clos ->
            return true
        }
        controller.apiService = svcMock.createMock()
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
        def fwkControl = mockFor(FrameworkService, false)
        def execControl = mockFor(ExecutionService, false)
        fwkControl.demand.getAuthContextForSubjectAndProject{ subj,proj -> return null }
        fwkControl.demand.authorizeProjectExecutionAll { framework, e, privs -> return false }

        controller.frameworkService = fwkControl.createMock()
        controller.executionService = execControl.createMock()
        controller.request.api_version = 5
        controller.params.project = "Test"
        controller.params.id = execs[2].id.toString()
        controller.params.asUser = "testuser"

        def svcMock = mockFor(ApiService, false)
        svcMock.demand.requireApi { req, resp -> true }
        svcMock.demand.requireExists { resp,e,args -> true }
        svcMock.demand.requireAuthorized { test,resp,args -> resp.status=403;false }
        controller.apiService = svcMock.createMock()
        controller.apiExecution()

        assert 403 == controller.response.status
        assert null == controller.flash.errorCode
    }
}
